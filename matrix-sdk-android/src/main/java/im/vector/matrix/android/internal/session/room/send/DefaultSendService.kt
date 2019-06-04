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

package im.vector.matrix.android.internal.session.room.send

import androidx.work.*
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.session.content.ContentAttachmentData
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.room.send.SendService
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.api.util.CancelableBag
import im.vector.matrix.android.api.util.addTo
import im.vector.matrix.android.internal.session.content.UploadContentWorker
import im.vector.matrix.android.internal.session.room.timeline.TimelineSendEventWorkCommon
import im.vector.matrix.android.internal.util.CancelableWork
import im.vector.matrix.android.internal.util.WorkerParamsFactory
import java.util.concurrent.TimeUnit

private const val UPLOAD_WORK = "UPLOAD_WORK"
private const val BACKOFF_DELAY = 10_000L

private val WORK_CONSTRAINTS = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

internal class DefaultSendService(private val roomId: String,
                                  private val eventFactory: LocalEchoEventFactory,
                                  private val monarchy: Monarchy)
    : SendService {


    override fun sendTextMessage(text: String, msgType: String, autoMarkdown: Boolean): Cancelable {
        val event = eventFactory.createTextEvent(roomId, msgType, text, autoMarkdown).also {
            saveLocalEcho(it)
        }
        val sendWork = createSendEventWork(event)
        TimelineSendEventWorkCommon.postWork(roomId, sendWork)
        return CancelableWork(sendWork.id)
    }

    override fun sendFormattedTextMessage(text: String, formattedText: String): Cancelable {
        val event = eventFactory.createFormattedTextEvent(roomId, text, formattedText).also {
            saveLocalEcho(it)
        }
        val sendWork = createSendEventWork(event)
        TimelineSendEventWorkCommon.postWork(roomId, sendWork)
        return CancelableWork(sendWork.id)
    }

    override fun sendMedias(attachments: List<ContentAttachmentData>): Cancelable {
        val cancelableBag = CancelableBag()
        attachments.forEach {
            sendMedia(it).addTo(cancelableBag)
        }
        return cancelableBag
    }

    override fun redactEvent(event: Event, reason: String?): Cancelable {
        //TODO manage media/attachements?
        val redactWork = createRedactEventWork(event, reason)
        TimelineSendEventWorkCommon.postWork(roomId, redactWork)
        return CancelableWork(redactWork.id)
    }

    override fun sendMedia(attachment: ContentAttachmentData): Cancelable {
        // Create an event with the media file path
        val event = eventFactory.createMediaEvent(roomId, attachment).also {
            saveLocalEcho(it)
        }
        val uploadWork = createUploadMediaWork(event, attachment)
        val sendWork = createSendEventWork(event)

        WorkManager.getInstance()
                .beginUniqueWork(buildWorkIdentifier(UPLOAD_WORK), ExistingWorkPolicy.APPEND, uploadWork)
                .then(sendWork)
                .enqueue()

        return CancelableWork(sendWork.id)
    }

    private fun saveLocalEcho(event: Event) {
        eventFactory.saveLocalEcho(monarchy, event)
    }

    private fun buildWorkIdentifier(identifier: String): String {
        return "${roomId}_$identifier"
    }

    private fun createSendEventWork(event: Event): OneTimeWorkRequest {
        val sendContentWorkerParams = SendEventWorker.Params(roomId, event)
        val sendWorkData = WorkerParamsFactory.toData(sendContentWorkerParams)

        return TimelineSendEventWorkCommon.createWork<SendEventWorker>(sendWorkData)
    }

    private fun createRedactEventWork(event: Event, reason: String?): OneTimeWorkRequest {

        val redactEvent = eventFactory.createRedactEvent(roomId, event.eventId!!, reason).also {
            saveLocalEcho(it)
        }

        val sendContentWorkerParams = RedactEventWorker.Params(redactEvent.eventId!!,
                roomId, event.eventId, reason)
        val redactWorkData = WorkerParamsFactory.toData(sendContentWorkerParams)

        return TimelineSendEventWorkCommon.createWork<RedactEventWorker>(redactWorkData)
    }

    private fun createUploadMediaWork(event: Event, attachment: ContentAttachmentData): OneTimeWorkRequest {
        val uploadMediaWorkerParams = UploadContentWorker.Params(roomId, event, attachment)
        val uploadWorkData = WorkerParamsFactory.toData(uploadMediaWorkerParams)

        return OneTimeWorkRequestBuilder<UploadContentWorker>()
                .setConstraints(WORK_CONSTRAINTS)
                .setInputData(uploadWorkData)
                .setBackoffCriteria(BackoffPolicy.LINEAR, BACKOFF_DELAY, TimeUnit.MILLISECONDS)
                .build()
    }

}
