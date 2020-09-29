/*
 * Copyright (c) 2020 New Vector Ltd
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.room.send

import androidx.work.BackoffPolicy
import androidx.work.OneTimeWorkRequest
import org.matrix.android.sdk.api.session.crypto.CryptoService
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.util.Cancelable
import org.matrix.android.sdk.internal.di.SessionId
import org.matrix.android.sdk.internal.di.WorkManagerProvider
import org.matrix.android.sdk.internal.session.room.timeline.TimelineSendEventWorkCommon
import org.matrix.android.sdk.internal.worker.WorkerParamsFactory
import org.matrix.android.sdk.internal.worker.startChain
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
        return if (cryptoService.isRoomEncrypted(event.roomId ?: "")
                && !event.isEncrypted() // In case of resend where it's already encrypted so skip to send
        ) {
            Timber.v("## SendEvent: [${System.currentTimeMillis()}] Schedule encrypt and send event ${event.eventId}")
            val encryptWork = createEncryptEventWork(event, true)
            // Note that event will be replaced by the result of the previous work
            val sendWork = createSendEventWork(event, false)
            timelineSendEventWorkCommon.postSequentialWorks(event.roomId ?: "", encryptWork, sendWork)
        } else {
            Timber.v("## SendEvent: [${System.currentTimeMillis()}] Schedule send event ${event.eventId}")
            val sendWork = createSendEventWork(event, true)
            timelineSendEventWorkCommon.postWork(event.roomId ?: "", sendWork)
        }
    }

    private fun createEncryptEventWork(event: Event, startChain: Boolean): OneTimeWorkRequest {
        // Same parameter
        val params = EncryptEventWorker.Params(sessionId, event.eventId!!)
        val sendWorkData = WorkerParamsFactory.toData(params)

        return workManagerProvider.matrixOneTimeWorkRequestBuilder<EncryptEventWorker>()
                .setConstraints(WorkManagerProvider.workConstraints)
                .setInputData(sendWorkData)
                .startChain(startChain)
                .setBackoffCriteria(BackoffPolicy.LINEAR, WorkManagerProvider.BACKOFF_DELAY, TimeUnit.MILLISECONDS)
                .build()
    }

    private fun createSendEventWork(event: Event, startChain: Boolean): OneTimeWorkRequest {
        val sendContentWorkerParams = SendEventWorker.Params(sessionId = sessionId, eventId = event.eventId!!)
        val sendWorkData = WorkerParamsFactory.toData(sendContentWorkerParams)

        return timelineSendEventWorkCommon.createWork<SendEventWorker>(sendWorkData, startChain)
    }
}
