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
            override val userId: String,
            val roomId: String,
            val event: Event,
            val attachment: ContentAttachmentData,
            val isRoomEncrypted: Boolean
    ) : SessionWorkerParams

    @Inject lateinit var fileUploader: FileUploader
    @Inject lateinit var contentUploadStateTracker: DefaultContentUploadStateTracker

    override suspend fun doWork(): Result {
        val params = WorkerParamsFactory.fromData<Params>(inputData)
                ?: return Result.success()

        val sessionComponent = getSessionComponent(params.userId) ?: return Result.success()
        sessionComponent.inject(this)

        val eventId = params.event.eventId ?: return Result.success()
        val attachment = params.attachment

        val isRoomEncrypted = params.isRoomEncrypted


        val thumbnailData = ThumbnailExtractor.extractThumbnail(params.attachment)
        val attachmentFile = createAttachmentFile(attachment) ?: return Result.failure()
        var uploadedThumbnailUrl: String? = null
        var uploadedThumbnailEncryptedFileInfo: EncryptedFileInfo? = null

        if (thumbnailData != null) {
            if (isRoomEncrypted) {
                Timber.v("Encrypt thumbnail")
                val encryptionResult = MXEncryptedAttachments.encryptAttachment(ByteArrayInputStream(thumbnailData.bytes), thumbnailData.mimeType)
                        ?: return Result.failure()

                uploadedThumbnailEncryptedFileInfo = encryptionResult.encryptedFileInfo

                fileUploader
                        .uploadByteArray(encryptionResult.encryptedByteArray, "thumb_${attachment.name}", thumbnailData.mimeType)
            } else {
                fileUploader
                        .uploadByteArray(thumbnailData.bytes, "thumb_${attachment.name}", thumbnailData.mimeType)
            }
                    .fold(
                            { Timber.e(it) },
                            { uploadedThumbnailUrl = it.contentUri }
                    )

        }

        val progressListener = object : ProgressRequestBody.Listener {
            override fun onProgress(current: Long, total: Long) {
                contentUploadStateTracker.setProgress(eventId, current, total)
            }
        }

        var uploadedFileEncryptedFileInfo: EncryptedFileInfo? = null

        return if (isRoomEncrypted) {
            Timber.v("Encrypt file")

            val encryptionResult = MXEncryptedAttachments.encryptAttachment(FileInputStream(attachmentFile), attachment.mimeType)
                    ?: return Result.failure()

            uploadedFileEncryptedFileInfo = encryptionResult.encryptedFileInfo

            fileUploader
                    .uploadByteArray(encryptionResult.encryptedByteArray, attachment.name, attachment.mimeType, progressListener)
        } else {
            fileUploader
                    .uploadFile(attachmentFile, attachment.name, attachment.mimeType, progressListener)
        }
                .fold(
                        { handleFailure(params) },
                        { handleSuccess(params, it.contentUri, uploadedFileEncryptedFileInfo, uploadedThumbnailUrl, uploadedThumbnailEncryptedFileInfo) }
                )

    }

    private fun createAttachmentFile(attachment: ContentAttachmentData): File? {
        return try {
            File(attachment.path)
        } catch (e: Exception) {
            Timber.e(e)
            null
        }
    }

    private fun handleFailure(params: Params): Result {
        contentUploadStateTracker.setFailure(params.event.eventId!!)
        return Result.success()
    }

    private fun handleSuccess(params: Params,
                              attachmentUrl: String,
                              encryptedFileInfo: EncryptedFileInfo?,
                              thumbnailUrl: String?,
                              thumbnailEncryptedFileInfo: EncryptedFileInfo?): Result {
        contentUploadStateTracker.setSuccess(params.event.eventId!!)
        val event = updateEvent(params.event, attachmentUrl, encryptedFileInfo, thumbnailUrl, thumbnailEncryptedFileInfo)
        val sendParams = SendEventWorker.Params(params.userId, params.roomId, event)
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
                url = url,
                encryptedFileInfo = encryptedFileInfo
        )
    }

    private fun MessageVideoContent.update(url: String,
                                           encryptedFileInfo: EncryptedFileInfo?,
                                           thumbnailUrl: String?,
                                           thumbnailEncryptedFileInfo: EncryptedFileInfo?): MessageVideoContent {
        return copy(
                url = url,
                encryptedFileInfo = encryptedFileInfo,
                videoInfo = videoInfo?.copy(
                        thumbnailUrl = thumbnailUrl,
                        thumbnailFile = thumbnailEncryptedFileInfo
                )
        )
    }

    private fun MessageFileContent.update(url: String,
                                          encryptedFileInfo: EncryptedFileInfo?): MessageFileContent {
        return copy(
                url = url,
                encryptedFileInfo = encryptedFileInfo
        )
    }

    private fun MessageAudioContent.update(url: String,
                                           encryptedFileInfo: EncryptedFileInfo?): MessageAudioContent {
        return copy(
                url = url,
                encryptedFileInfo = encryptedFileInfo
        )
    }

}

