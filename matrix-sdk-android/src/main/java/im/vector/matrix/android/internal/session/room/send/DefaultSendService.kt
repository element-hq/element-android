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

import android.content.Context
import androidx.work.*
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.api.session.content.ContentAttachmentData
import im.vector.matrix.android.api.session.crypto.CryptoService
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.room.send.SendService
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.api.util.CancelableBag
import im.vector.matrix.android.api.util.addTo
import im.vector.matrix.android.internal.session.content.UploadContentWorker
import im.vector.matrix.android.internal.session.room.timeline.TimelineSendEventWorkCommon
import im.vector.matrix.android.internal.util.CancelableWork
import im.vector.matrix.android.internal.worker.WorkerParamsFactory
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private const val UPLOAD_WORK = "UPLOAD_WORK"
private const val BACKOFF_DELAY = 10_000L

private val WORK_CONSTRAINTS = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

internal class DefaultSendService @Inject constructor(private val context: Context,
                                                      private val credentials: Credentials,
                                                      private val roomId: String,
                                                      private val localEchoEventFactory: LocalEchoEventFactory,
                                                      private val cryptoService: CryptoService,
                                                      private val monarchy: Monarchy)
    : SendService {

    override fun sendTextMessage(text: String, msgType: String, autoMarkdown: Boolean): Cancelable {
        val event = localEchoEventFactory.createTextEvent(roomId, msgType, text, autoMarkdown).also {
            saveLocalEcho(it)
        }
        // Encrypted room handling
        return if (cryptoService.isRoomEncrypted(roomId)) {
            Timber.v("Send event in encrypted room")
            val encryptWork = createEncryptEventWork(event)
            val sendWork = createSendEventWork(event)
            TimelineSendEventWorkCommon.postSequentialWorks(context, roomId, encryptWork, sendWork)
            CancelableWork(context, encryptWork.id)
        } else {
            sendEvent(event)
        }
    }

    private fun sendEvent(event: Event): Cancelable {
        val sendWork = createSendEventWork(event)
        TimelineSendEventWorkCommon.postWork(context, roomId, sendWork)
        return CancelableWork(context, sendWork.id)
    }

    override fun sendFormattedTextMessage(text: String, formattedText: String): Cancelable {
        val event = localEchoEventFactory.createFormattedTextEvent(roomId, text, formattedText).also {
            saveLocalEcho(it)
        }
        val sendWork = createSendEventWork(event)
        TimelineSendEventWorkCommon.postWork(context, roomId, sendWork)
        return CancelableWork(context, sendWork.id)
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
        TimelineSendEventWorkCommon.postWork(context, roomId, redactWork)
        return CancelableWork(context, redactWork.id)
    }

    override fun sendMedia(attachment: ContentAttachmentData): Cancelable {
        // Create an event with the media file path
        val event = localEchoEventFactory.createMediaEvent(roomId, attachment).also {
            saveLocalEcho(it)
        }

        val isRoomEncrypted = cryptoService.isRoomEncrypted(roomId)

        val uploadWork = createUploadMediaWork(event, attachment, isRoomEncrypted)
        val sendWork = createSendEventWork(event)

        if (isRoomEncrypted) {
            val encryptWork = createEncryptEventWork(event)

            WorkManager.getInstance(context)
                    .beginUniqueWork(buildWorkIdentifier(UPLOAD_WORK), ExistingWorkPolicy.APPEND, uploadWork)
                    .then(encryptWork)
                    .then(sendWork)
                    .enqueue()
        } else {
            WorkManager.getInstance(context)
                    .beginUniqueWork(buildWorkIdentifier(UPLOAD_WORK), ExistingWorkPolicy.APPEND, uploadWork)
                    .then(sendWork)
                    .enqueue()
        }

        return CancelableWork(context, sendWork.id)
    }

    private fun saveLocalEcho(event: Event) {
        localEchoEventFactory.saveLocalEcho(monarchy, event)
    }

    private fun buildWorkIdentifier(identifier: String): String {
        return "${roomId}_$identifier"
    }

    private fun createEncryptEventWork(event: Event): OneTimeWorkRequest {
        // Same parameter
        val params = EncryptEventWorker.Params(credentials.userId, roomId, event)
        val sendWorkData = WorkerParamsFactory.toData(params)

        return OneTimeWorkRequestBuilder<EncryptEventWorker>()
                .setConstraints(WORK_CONSTRAINTS)
                .setInputData(sendWorkData)
                .setBackoffCriteria(BackoffPolicy.LINEAR, BACKOFF_DELAY, TimeUnit.MILLISECONDS)
                .build()
    }

    private fun createSendEventWork(event: Event): OneTimeWorkRequest {
        val sendContentWorkerParams = SendEventWorker.Params(credentials.userId, roomId, event)
        val sendWorkData = WorkerParamsFactory.toData(sendContentWorkerParams)

        return TimelineSendEventWorkCommon.createWork<SendEventWorker>(sendWorkData)
    }

    private fun createRedactEventWork(event: Event, reason: String?): OneTimeWorkRequest {
        val redactEvent = localEchoEventFactory.createRedactEvent(roomId, event.eventId!!, reason).also {
            saveLocalEcho(it)
        }
        val sendContentWorkerParams = RedactEventWorker.Params(credentials.userId, redactEvent.eventId!!, roomId, event.eventId, reason)
        val redactWorkData = WorkerParamsFactory.toData(sendContentWorkerParams)
        return TimelineSendEventWorkCommon.createWork<RedactEventWorker>(redactWorkData)
    }

    private fun createUploadMediaWork(event: Event, attachment: ContentAttachmentData, isRoomEncrypted: Boolean): OneTimeWorkRequest {
        val uploadMediaWorkerParams = UploadContentWorker.Params(credentials.userId, roomId, event, attachment, isRoomEncrypted)
        val uploadWorkData = WorkerParamsFactory.toData(uploadMediaWorkerParams)

        return OneTimeWorkRequestBuilder<UploadContentWorker>()
                .setConstraints(WORK_CONSTRAINTS)
                .setInputData(uploadWorkData)
                .setBackoffCriteria(BackoffPolicy.LINEAR, BACKOFF_DELAY, TimeUnit.MILLISECONDS)
                .build()
    }

}
