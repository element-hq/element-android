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
import im.vector.matrix.android.api.session.content.ContentUploadStateTracker
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.toContent
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.message.MessageAudioContent
import im.vector.matrix.android.api.session.room.model.message.MessageContent
import im.vector.matrix.android.api.session.room.model.message.MessageFileContent
import im.vector.matrix.android.api.session.room.model.message.MessageImageContent
import im.vector.matrix.android.api.session.room.model.message.MessageVideoContent
import im.vector.matrix.android.internal.di.MatrixKoinComponent
import im.vector.matrix.android.internal.network.ProgressRequestBody
import im.vector.matrix.android.internal.session.room.send.SendEventWorker
import im.vector.matrix.android.internal.util.WorkerParamsFactory
import org.koin.standalone.inject
import timber.log.Timber
import java.io.File


internal class UploadContentWorker(context: Context, params: WorkerParameters)
    : CoroutineWorker(context, params), MatrixKoinComponent {

    private val fileUploader by inject<FileUploader>()
    private val contentUploadProgressTracker by inject<ContentUploadStateTracker>()

    @JsonClass(generateAdapter = true)
    internal data class Params(
            val roomId: String,
            val event: Event,
            val attachment: ContentAttachmentData
    )

    override suspend fun doWork(): Result {
        val params = WorkerParamsFactory.fromData<Params>(inputData)
                     ?: return Result.success()

        val eventId = params.event.eventId ?: return Result.success()
        val attachment = params.attachment

        val thumbnailData = ThumbnailExtractor.extractThumbnail(params.attachment)
        val attachmentFile = createAttachmentFile(attachment) ?: return Result.failure()
        var uploadedThumbnailUrl: String? = null

        if (thumbnailData != null) {
            fileUploader
                    .uploadByteArray(thumbnailData.bytes, "thumb_${attachment.name}", thumbnailData.mimeType)
                    .fold(
                            { Timber.e(it) },
                            { uploadedThumbnailUrl = it.contentUri }
                    )
        }

        val progressListener = object : ProgressRequestBody.Listener {
            override fun onProgress(current: Long, total: Long) {
                contentUploadProgressTracker.setProgress(eventId, current, total)
            }
        }
        return fileUploader
                .uploadFile(attachmentFile, attachment.name, attachment.mimeType, progressListener)
                .fold(
                        { handleFailure(params) },
                        { handleSuccess(params, it.contentUri, uploadedThumbnailUrl) }
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
        contentUploadProgressTracker.setFailure(params.event.eventId!!)
        return Result.success()
    }

    private fun handleSuccess(params: Params,
                              attachmentUrl: String,
                              thumbnailUrl: String?): Result {
        contentUploadProgressTracker.setFailure(params.event.eventId!!)
        val event = updateEvent(params.event, attachmentUrl, thumbnailUrl)
        val sendParams = SendEventWorker.Params(params.roomId, event)
        return Result.success(WorkerParamsFactory.toData(sendParams))
    }

    private fun updateEvent(event: Event, url: String, thumbnailUrl: String? = null): Event {
        val messageContent: MessageContent = event.content.toModel() ?: return event
        val updatedContent = when (messageContent) {
            is MessageImageContent -> messageContent.update(url)
            is MessageVideoContent -> messageContent.update(url, thumbnailUrl)
            is MessageFileContent  -> messageContent.update(url)
            is MessageAudioContent -> messageContent.update(url)
            else                   -> messageContent
        }
        return event.copy(content = updatedContent.toContent())
    }

    private fun MessageImageContent.update(url: String): MessageImageContent {
        return copy(url = url)
    }

    private fun MessageVideoContent.update(url: String, thumbnailUrl: String?): MessageVideoContent {
        return copy(url = url, info = info?.copy(thumbnailUrl = thumbnailUrl))
    }

    private fun MessageFileContent.update(url: String): MessageFileContent {
        return copy(url = url)
    }

    private fun MessageAudioContent.update(url: String): MessageAudioContent {
        return copy(url = url)
    }


}

