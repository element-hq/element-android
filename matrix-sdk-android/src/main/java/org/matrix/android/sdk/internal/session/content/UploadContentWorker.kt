/*
 * Copyright 2019 New Vector Ltd
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.session.content

import android.content.Context
import android.graphics.BitmapFactory
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.extensions.tryThis
import org.matrix.android.sdk.api.session.content.ContentAttachmentData
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.message.MessageAudioContent
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.session.room.model.message.MessageFileContent
import org.matrix.android.sdk.api.session.room.model.message.MessageImageContent
import org.matrix.android.sdk.api.session.room.model.message.MessageVideoContent
import org.matrix.android.sdk.internal.crypto.attachments.MXEncryptedAttachments
import org.matrix.android.sdk.internal.crypto.model.rest.EncryptedFileInfo
import org.matrix.android.sdk.internal.network.ProgressRequestBody
import org.matrix.android.sdk.internal.session.DefaultFileService
import org.matrix.android.sdk.internal.session.room.send.CancelSendTracker
import org.matrix.android.sdk.internal.session.room.send.MultipleEventSendingDispatcherWorker
import org.matrix.android.sdk.internal.worker.SessionWorkerParams
import org.matrix.android.sdk.internal.worker.WorkerParamsFactory
import org.matrix.android.sdk.internal.worker.getSessionComponent
import timber.log.Timber
import java.io.File
import java.util.UUID
import javax.inject.Inject

private data class NewImageAttributes(
        val newWidth: Int?,
        val newHeight: Int?,
        val newFileSize: Int
)

/**
 * Possible previous worker: None
 * Possible next worker    : Always [MultipleEventSendingDispatcherWorker]
 */
internal class UploadContentWorker(val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    @JsonClass(generateAdapter = true)
    internal data class Params(
            override val sessionId: String,
            val events: List<Event>,
            val attachment: ContentAttachmentData,
            val isEncrypted: Boolean,
            val compressBeforeSending: Boolean,
            override val lastFailureMessage: String? = null
    ) : SessionWorkerParams

    @Inject lateinit var fileUploader: FileUploader
    @Inject lateinit var contentUploadStateTracker: DefaultContentUploadStateTracker
    @Inject lateinit var fileService: DefaultFileService
    @Inject lateinit var cancelSendTracker: CancelSendTracker
    @Inject lateinit var imageCompressor: ImageCompressor

    override suspend fun doWork(): Result {
        val params = WorkerParamsFactory.fromData<Params>(inputData)
                ?: return Result.success()
                        .also { Timber.e("Unable to parse work parameters") }

        Timber.v("Starting upload media work with params $params")

        if (params.lastFailureMessage != null) {
            // Transmit the error
            return Result.success(inputData)
                    .also { Timber.e("Work cancelled due to input error from parent") }
        }

        // Just defensive code to ensure that we never have an uncaught exception that could break the queue
        return try {
            internalDoWork(params)
        } catch (failure: Throwable) {
            Timber.e(failure)
            handleFailure(params, failure)
        }
    }

    private suspend fun internalDoWork(params: Params): Result {
        val sessionComponent = getSessionComponent(params.sessionId) ?: return Result.success()
        sessionComponent.inject(this)

        val allCancelled = params.events.all { cancelSendTracker.isCancelRequestedFor(it.eventId, it.roomId) }
        if (allCancelled) {
            // there is no point in uploading the image!
            return Result.success(inputData)
                    .also { Timber.e("## Send: Work cancelled by user") }
        }

        val attachment = params.attachment
        val filesToDelete = mutableListOf<File>()

        try {
            val inputStream = context.contentResolver.openInputStream(attachment.queryUri)
                    ?: return Result.success(
                            WorkerParamsFactory.toData(
                                    params.copy(
                                            lastFailureMessage = "Cannot openInputStream for file: " + attachment.queryUri.toString()
                                    )
                            )
                    )

            // always use a temporary file, it guaranties that we could report progress on upload and simplifies the flows
            val workingFile = File.createTempFile(UUID.randomUUID().toString(), null, context.cacheDir)
                    .also { filesToDelete.add(it) }
            workingFile.outputStream().use { outputStream ->
                inputStream.use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            val uploadThumbnailResult = dealWithThumbnail(params)

            val progressListener = object : ProgressRequestBody.Listener {
                override fun onProgress(current: Long, total: Long) {
                    notifyTracker(params) {
                        if (isStopped) {
                            contentUploadStateTracker.setFailure(it, Throwable("Cancelled"))
                        } else {
                            contentUploadStateTracker.setProgress(it, current, total)
                        }
                    }
                }
            }

            var uploadedFileEncryptedFileInfo: EncryptedFileInfo? = null

            return try {
                val fileToUpload: File
                var newImageAttributes: NewImageAttributes? = null

                if (attachment.type == ContentAttachmentData.Type.IMAGE && params.compressBeforeSending) {
                    fileToUpload = imageCompressor.compress(context, workingFile, MAX_IMAGE_SIZE, MAX_IMAGE_SIZE)
                            .also { compressedFile ->
                                // Get new Bitmap size
                                compressedFile.inputStream().use {
                                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                                    val bitmap = BitmapFactory.decodeStream(it, null, options)
                                    val fileSize = bitmap?.byteCount ?: 0
                                    newImageAttributes = NewImageAttributes(
                                            options.outWidth,
                                            options.outHeight,
                                            fileSize
                                    )
                                }
                            }
                            .also { filesToDelete.add(it) }
                } else {
                    fileToUpload = workingFile
                }

                val contentUploadResponse = if (params.isEncrypted) {
                    Timber.v("## FileService: Encrypt file")

                    val tmpEncrypted = File.createTempFile(UUID.randomUUID().toString(), null, context.cacheDir)
                            .also { filesToDelete.add(it) }

                    uploadedFileEncryptedFileInfo =
                            MXEncryptedAttachments.encrypt(fileToUpload.inputStream(), attachment.getSafeMimeType(), tmpEncrypted) { read, total ->
                                notifyTracker(params) {
                                    contentUploadStateTracker.setEncrypting(it, read.toLong(), total.toLong())
                                }
                            }

                    Timber.v("## FileService: Uploading file")

                    fileUploader
                            .uploadFile(tmpEncrypted, attachment.name, "application/octet-stream", progressListener)
                } else {
                    Timber.v("## FileService: Clear file")
                    fileUploader
                            .uploadFile(fileToUpload, attachment.name, attachment.getSafeMimeType(), progressListener)
                }

                Timber.v("## FileService: Update cache storage for ${contentUploadResponse.contentUri}")
                try {
                    context.contentResolver.openInputStream(attachment.queryUri)?.let {
                        fileService.storeDataFor(contentUploadResponse.contentUri, params.attachment.getSafeMimeType(), it)
                    }
                    Timber.v("## FileService: cache storage updated")
                } catch (failure: Throwable) {
                    Timber.e(failure, "## FileService: Failed to update file cache")
                }

                handleSuccess(params,
                        contentUploadResponse.contentUri,
                        uploadedFileEncryptedFileInfo,
                        uploadThumbnailResult?.uploadedThumbnailUrl,
                        uploadThumbnailResult?.uploadedThumbnailEncryptedFileInfo,
                        newImageAttributes)
            } catch (t: Throwable) {
                Timber.e(t, "## FileService: ERROR ${t.localizedMessage}")
                handleFailure(params, t)
            }
        } catch (e: Exception) {
            Timber.e(e, "## FileService: ERROR")
            notifyTracker(params) { contentUploadStateTracker.setFailure(it, e) }
            return Result.success(
                    WorkerParamsFactory.toData(
                            params.copy(
                                    lastFailureMessage = e.localizedMessage
                            )
                    )
            )
        } finally {
            // Delete all temporary files
            filesToDelete.forEach {
                tryThis { it.delete() }
            }
        }
    }

    private data class UploadThumbnailResult(
            val uploadedThumbnailUrl: String,
            val uploadedThumbnailEncryptedFileInfo: EncryptedFileInfo?
    )

    /**
     * If appropriate, it will create and upload a thumbnail
     */
    private suspend fun dealWithThumbnail(params: Params): UploadThumbnailResult? {
        return ThumbnailExtractor.extractThumbnail(context, params.attachment)
                ?.let { thumbnailData ->
                    val thumbnailProgressListener = object : ProgressRequestBody.Listener {
                        override fun onProgress(current: Long, total: Long) {
                            notifyTracker(params) { contentUploadStateTracker.setProgressThumbnail(it, current, total) }
                        }
                    }

                    try {
                        if (params.isEncrypted) {
                            Timber.v("Encrypt thumbnail")
                            notifyTracker(params) { contentUploadStateTracker.setEncryptingThumbnail(it) }
                            val encryptionResult = MXEncryptedAttachments.encryptAttachment(thumbnailData.bytes.inputStream(), thumbnailData.mimeType)
                            val contentUploadResponse = fileUploader.uploadByteArray(encryptionResult.encryptedByteArray,
                                    "thumb_${params.attachment.name}",
                                    "application/octet-stream",
                                    thumbnailProgressListener)
                            UploadThumbnailResult(
                                    contentUploadResponse.contentUri,
                                    encryptionResult.encryptedFileInfo
                            )
                        } else {
                            val contentUploadResponse = fileUploader.uploadByteArray(thumbnailData.bytes,
                                    "thumb_${params.attachment.name}",
                                    thumbnailData.mimeType,
                                    thumbnailProgressListener)
                            UploadThumbnailResult(
                                    contentUploadResponse.contentUri,
                                    null
                            )
                        }
                    } catch (t: Throwable) {
                        Timber.e(t, "Thumbnail upload failed")
                        null
                    }
                }
    }

    private fun handleFailure(params: Params, failure: Throwable): Result {
        notifyTracker(params) { contentUploadStateTracker.setFailure(it, failure) }

        return Result.success(
                WorkerParamsFactory.toData(
                        params.copy(
                                lastFailureMessage = failure.localizedMessage
                        )
                )
        )
    }

    private fun handleSuccess(params: Params,
                              attachmentUrl: String,
                              encryptedFileInfo: EncryptedFileInfo?,
                              thumbnailUrl: String?,
                              thumbnailEncryptedFileInfo: EncryptedFileInfo?,
                              newImageAttributes: NewImageAttributes?): Result {
        notifyTracker(params) { contentUploadStateTracker.setSuccess(it) }

        val updatedEvents = params.events
                .map {
                    updateEvent(it, attachmentUrl, encryptedFileInfo, thumbnailUrl, thumbnailEncryptedFileInfo, newImageAttributes)
                }

        val sendParams = MultipleEventSendingDispatcherWorker.Params(params.sessionId, updatedEvents, params.isEncrypted)
        return Result.success(WorkerParamsFactory.toData(sendParams)).also {
            Timber.v("## handleSuccess $attachmentUrl, work is stopped $isStopped")
        }
    }

    private fun updateEvent(event: Event,
                            url: String,
                            encryptedFileInfo: EncryptedFileInfo?,
                            thumbnailUrl: String? = null,
                            thumbnailEncryptedFileInfo: EncryptedFileInfo?,
                            newImageAttributes: NewImageAttributes?): Event {
        val messageContent: MessageContent = event.content.toModel() ?: return event
        val updatedContent = when (messageContent) {
            is MessageImageContent -> messageContent.update(url, encryptedFileInfo, newImageAttributes)
            is MessageVideoContent -> messageContent.update(url, encryptedFileInfo, thumbnailUrl, thumbnailEncryptedFileInfo)
            is MessageFileContent  -> messageContent.update(url, encryptedFileInfo)
            is MessageAudioContent -> messageContent.update(url, encryptedFileInfo)
            else                   -> messageContent
        }
        return event.copy(content = updatedContent.toContent())
    }

    private fun notifyTracker(params: Params, function: (String) -> Unit) {
        params.events
                .mapNotNull { it.eventId }
                .forEach { eventId -> function.invoke(eventId) }
    }

    private fun MessageImageContent.update(url: String,
                                           encryptedFileInfo: EncryptedFileInfo?,
                                           newImageAttributes: NewImageAttributes?): MessageImageContent {
        return copy(
                url = if (encryptedFileInfo == null) url else null,
                encryptedFileInfo = encryptedFileInfo?.copy(url = url),
                info = info?.copy(
                        width = newImageAttributes?.newWidth ?: info.width,
                        height = newImageAttributes?.newHeight ?: info.height,
                        size = newImageAttributes?.newFileSize ?: info.size
                )
        )
    }

    private fun MessageVideoContent.update(url: String,
                                           encryptedFileInfo: EncryptedFileInfo?,
                                           thumbnailUrl: String?,
                                           thumbnailEncryptedFileInfo: EncryptedFileInfo?): MessageVideoContent {
        return copy(
                url = if (encryptedFileInfo == null) url else null,
                encryptedFileInfo = encryptedFileInfo?.copy(url = url),
                videoInfo = videoInfo?.copy(
                        thumbnailUrl = if (thumbnailEncryptedFileInfo == null) thumbnailUrl else null,
                        thumbnailFile = thumbnailEncryptedFileInfo?.copy(url = thumbnailUrl)
                )
        )
    }

    private fun MessageFileContent.update(url: String,
                                          encryptedFileInfo: EncryptedFileInfo?): MessageFileContent {
        return copy(
                url = if (encryptedFileInfo == null) url else null,
                encryptedFileInfo = encryptedFileInfo?.copy(url = url)
        )
    }

    private fun MessageAudioContent.update(url: String,
                                           encryptedFileInfo: EncryptedFileInfo?): MessageAudioContent {
        return copy(
                url = if (encryptedFileInfo == null) url else null,
                encryptedFileInfo = encryptedFileInfo?.copy(url = url)
        )
    }

    companion object {
        private const val MAX_IMAGE_SIZE = 640
    }
}
