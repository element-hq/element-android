/*
 * Copyright (c) 2020 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.matrix.android.internal.session.room.send

import androidx.work.BackoffPolicy
import androidx.work.OneTimeWorkRequest
import im.vector.matrix.android.api.session.crypto.CryptoService
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.internal.di.SessionId
import im.vector.matrix.android.internal.di.WorkManagerProvider
import im.vector.matrix.android.internal.session.room.timeline.TimelineSendEventWorkCommon
import im.vector.matrix.android.internal.worker.WorkerParamsFactory
import im.vector.matrix.android.internal.worker.startChain
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

internal class RoomEventSender @Inject constructor(
        private val workManagerProvider: WorkManagerProvider,
        private val timelineSendEventWorkCommon: TimelineSendEventWorkCommon,
        @SessionId private val sessionId: String,
        private val cryptoService: CryptoService
) {
    fun sendEvent(event: Event): Cancelable {
        // Encrypted room handling
        return if (cryptoService.isRoomEncrypted(event.roomId ?: "")) {
            Timber.v("Send event in encrypted room")
            val encryptWork = createEncryptEventWork(event, true)
            // Note that event will be replaced by the result of the previous work
            val sendWork = createSendEventWork(event, false)
            timelineSendEventWorkCommon.postSequentialWorks(event.roomId ?: "", encryptWork, sendWork)
        } else {
            val sendWork = createSendEventWork(event, true)
            timelineSendEventWorkCommon.postWork(event.roomId ?: "", sendWork)
        }
    }

    private fun createEncryptEventWork(event: Event, startChain: Boolean): OneTimeWorkRequest {
        // Same parameter
        val params = EncryptEventWorker.Params(sessionId, event)
        val sendWorkData = WorkerParamsFactory.toData(params)

        return workManagerProvider.matrixOneTimeWorkRequestBuilder<EncryptEventWorker>()
                .setConstraints(WorkManagerProvider.workConstraints)
                .setInputData(sendWorkData)
                .startChain(startChain)
                .setBackoffCriteria(BackoffPolicy.LINEAR, WorkManagerProvider.BACKOFF_DELAY, TimeUnit.MILLISECONDS)
                .build()
    }

    private fun createSendEventWork(event: Event, startChain: Boolean): OneTimeWorkRequest {
        val sendContentWorkerParams = SendEventWorker.Params(sessionId, event)
        val sendWorkData = WorkerParamsFactory.toData(sendContentWorkerParams)

        return timelineSendEventWorkCommon.createWork<SendEventWorker>(sendWorkData, startChain)
    }
}
