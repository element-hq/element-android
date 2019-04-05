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
import im.vector.matrix.android.api.session.room.model.message.MessageContent
import im.vector.matrix.android.api.session.room.model.message.MessageImageContent
import im.vector.matrix.android.internal.di.MatrixKoinComponent
import im.vector.matrix.android.internal.session.room.send.SendEventWorker
import im.vector.matrix.android.internal.util.WorkerParamsFactory
import org.koin.standalone.inject

internal class UploadContentWorker(context: Context, params: WorkerParameters)
    : CoroutineWorker(context, params), MatrixKoinComponent {

    private val mediaUploader by inject<ContentUploader>()

    @JsonClass(generateAdapter = true)
    internal data class Params(
            val roomId: String,
            val event: Event,
            val attachment: ContentAttachmentData
    )

    override suspend fun doWork(): Result {
        val params = WorkerParamsFactory.fromData<Params>(inputData)
                     ?: return Result.failure()

        if (params.event.eventId == null) {
            return Result.failure()
        }
        return mediaUploader
                .uploadFile(params.event.eventId, params.attachment)
                .fold({ handleFailure() }, { handleSuccess(params, it) })
    }

    private fun handleFailure(): Result {
        return Result.retry()
    }

    private fun handleSuccess(params: Params, contentUploadResponse: ContentUploadResponse): Result {
        val event = updateEvent(params.event, contentUploadResponse.contentUri)
        val sendParams = SendEventWorker.Params(params.roomId, event)
        return Result.success(WorkerParamsFactory.toData(sendParams))
    }

    private fun updateEvent(event: Event, url: String): Event {
        val messageContent: MessageContent = event.content.toModel() ?: return event
        val updatedContent = when (messageContent) {
            is MessageImageContent -> messageContent.update(url)
            else                   -> messageContent
        }
        return event.copy(content = updatedContent.toContent())
    }

    private fun MessageImageContent.update(url: String): MessageImageContent {
        return copy(url = url)
    }


}

