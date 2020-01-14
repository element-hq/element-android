/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.internal.session.content

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.squareup.moshi.JsonClass
import im.vector.matrix.android.api.session.content.ContentAttachmentData
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.toContent
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.message.*
import im.vector.matrix.android.internal.crypto.attachments.MXEncryptedAttachments
import im.vector.matrix.android.internal.crypto.model.rest.EncryptedFileInfo
import im.vector.matrix.android.internal.network.ProgressRequestBody
import im.vector.matrix.android.internal.session.room.send.SendEventWorker
import im.vector.matrix.android.internal.worker.SessionWorkerParams
import im.vector.matrix.android.internal.worker.WorkerParamsFactory
import im.vector.matrix.android.internal.worker.getSessionComponent
import timber.log.Timber
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import javax.inject.Inject

internal class UploadContentWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    @JsonClass(generateAdapter = true)
    internal data class Params(
            override val sessionId: String,
            val roomId: String,
            val event: Event,
            val attachment: ContentAttachmentData,
            val isRoomEncrypted: Boolean,
            override val lastFailureMessage: String? = null
    ) : SessionWorkerParams

    @Inject lateinit var fileUploader: FileUploader
    @Inject lateinit var contentUploadStateTracker: DefaultContentUploadStateTracker

    override suspend fun doWork(): Result {
        val params = WorkerParamsFactory.fromData<Params>(inputData)
                ?: return Result.success()
        Timber.v("Starting upload media work with params $params")

        if (params.lastFailureMessage != null) {
            // Transmit the error
            Timber.v("Stop upload media work due to input failure")
            return Result.success(inputData)
        }

        val sessionComponent = getSessionComponent(params.sessionId) ?: return Result.success()
        sessionComponent.inject(this)

        val eventId = params.event.eventId ?: return Result.success()
        val attachment = params.attachment

        val attachmentFile = try {
            File(attachment.path)
        } catch (e: Exception) {
            Timber.e(e)
            contentUploadStateTracker.setFailure(params.event.eventId, e)
            return Result.success(
                    WorkerParamsFactory.toData(params.copy(
                            lastFailureMessage = e.localizedMessage
                    ))
            )
        }

        var uploadedThumbnailUrl: String? = null
        var uploadedThumbnailEncryptedFileInfo: EncryptedFileInfo? = null

        ThumbnailExtractor.extractThumbnail(params.attachment)?.let { thumbnailData ->
            val thumbnailProgressListener = object : ProgressRequestBody.Listener {
                override fun onProgress(current: Long, total: Long) {
                    contentUploadStateTracker.setProgressThumbnail(eventId, current, total)
                }
            }

            try {
                val contentUploadResponse = if (params.isRoomEncrypted) {
                    Timber.v("Encrypt thumbnail")
                    contentUploadStateTracker.setEncryptingThumbnail(eventId)
                    val encryptionResult = MXEncryptedAttachments.encryptAttachment(ByteArrayInputStream(thumbnailData.bytes), thumbnailData.mimeType)
                    uploadedThumbnailEncryptedFileInfo = encryptionResult.encryptedFileInfo
                    fileUploader.uploadByteArray(encryptionResult.encryptedByteArray,
                            "thumb_${attachment.name}",
                            "application/octet-stream",
                            thumbnailProgressListener)
                } else {
                    fileUploader.uploadByteArray(thumbnailData.bytes,
                            "thumb_${attachment.name}",
                            thumbnailData.mimeType,
                            thumbnailProgressListener)
                }

                uploadedThumbnailUrl = contentUploadResponse.contentUri
            } catch (t: Throwable) {
                Timber.e(t)
                return handleFailure(params, t)
            }
        }

        val progressListener = object : ProgressRequestBody.Listener {
            override fun onProgress(current: Long, total: Long) {
                if (isStopped) {
                    contentUploadStateTracker.setFailure(eventId, Throwable("Cancelled"))
                } else {
                    contentUploadStateTracker.setProgress(eventId, current, total)
                }
            }
        }

        var uploadedFileEncryptedFileInfo: EncryptedFileInfo? = null

        return try {
            val contentUploadResponse = if (params.isRoomEncrypted) {
                Timber.v("Encrypt file")
                contentUploadStateTracker.setEncrypting(eventId)

                val encryptionResult = MXEncryptedAttachments.encryptAttachment(FileInputStream(attachmentFile), attachment.mimeType)
                uploadedFileEncryptedFileInfo = encryptionResult.encryptedFileInfo

                fileUploader
                        .uploadByteArray(encryptionResult.encryptedByteArray, attachment.name, "application/octet-stream", progressListener)
            } else {
                fileUploader
                        .uploadFile(attachmentFile, attachment.name, attachment.mimeType, progressListener)
            }

            handleSuccess(params, contentUploadResponse.contentUri, uploadedFileEncryptedFileInfo, uploadedThumbnailUrl, uploadedThumbnailEncryptedFileInfo)
        } catch (t: Throwable) {
            Timber.e(t)
            handleFailure(params, t)
        }
    }

    private fun handleFailure(params: Params, failure: Throwable): Result {
        contentUploadStateTracker.setFailure(params.event.eventId!!, failure)
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
                              thumbnailEncryptedFileInfo: EncryptedFileInfo?): Result {
        Timber.v("handleSuccess $attachmentUrl, work is stopped $isStopped")
        contentUploadStateTracker.setSuccess(params.event.eventId!!)
        val event = updateEvent(params.event, attachmentUrl, encryptedFileInfo, thumbnailUrl, thumbnailEncryptedFileInfo)
        val sendParams = SendEventWorker.Params(params.sessionId, params.roomId, event)
        return Result.success(WorkerParamsFactory.toData(sendParams))
    }

    private fun updateEvent(event: Event,
                            url: String,
                            encryptedFileInfo: EncryptedFileInfo?,
                            thumbnailUrl: String? = null,
                            thumbnailEncryptedFileInfo: EncryptedFileInfo?): Event {
        val messageContent: MessageContent = event.content.toModel() ?: return event
        val updatedContent = when (messageContent) {
            is MessageImageContent -> messageContent.update(url, encryptedFileInfo)
            is MessageVideoContent -> messageContent.update(url, encryptedFileInfo, thumbnailUrl, thumbnailEncryptedFileInfo)
            is MessageFileContent  -> messageContent.update(url, encryptedFileInfo)
            is MessageAudioContent -> messageContent.update(url, encryptedFileInfo)
            else                   -> messageContent
        }
        return event.copy(content = updatedContent.toContent())
    }

    private fun MessageImageContent.update(url: String,
                                           encryptedFileInfo: EncryptedFileInfo?): MessageImageContent {
        return copy(
                url = if (encryptedFileInfo == null) url else null,
                encryptedFileInfo = encryptedFileInfo?.copy(url = url)
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
}
