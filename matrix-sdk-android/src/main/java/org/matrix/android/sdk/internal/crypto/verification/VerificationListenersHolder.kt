/*
 * Copyright (c) 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.crypto.verification

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.MatrixCoroutineDispatchers
import org.matrix.android.sdk.api.session.crypto.verification.VerificationEvent
import org.matrix.android.sdk.api.session.crypto.verification.VerificationTransaction
import org.matrix.android.sdk.api.session.crypto.verification.dbgState
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.session.SessionScope
import timber.log.Timber
import javax.inject.Inject

@SessionScope
internal class VerificationListenersHolder @Inject constructor(
        coroutineDispatchers: MatrixCoroutineDispatchers,
        @UserId myUserId: String,
) {

    val myUserId = myUserId.take(5)

    val scope = CoroutineScope(SupervisorJob() + coroutineDispatchers.dmVerif)
    val eventFlow = MutableSharedFlow<VerificationEvent>(extraBufferCapacity = 20, onBufferOverflow = BufferOverflow.SUSPEND)

    fun dispatchTxAdded(tx: VerificationTransaction) {
        scope.launch {
            Timber.v("## SAS [$myUserId] dispatchTxAdded txId:${tx.transactionId} | ${tx.dbgState()}")
            eventFlow.emit(VerificationEvent.TransactionAdded(tx))
        }
    }

    fun dispatchTxUpdated(tx: VerificationTransaction) {
        scope.launch {
            Timber.v("## SAS [$myUserId] dispatchTxUpdated txId:${tx.transactionId} | ${tx.dbgState()}")
            eventFlow.emit(VerificationEvent.TransactionUpdated(tx))
        }
    }

    fun dispatchRequestAdded(verificationRequest: VerificationRequest) {
        scope.launch {
            Timber.v("## SAS [$myUserId] dispatchRequestAdded txId:${verificationRequest.flowId()} state:${verificationRequest.innerState()}")
            eventFlow.emit(VerificationEvent.RequestAdded(verificationRequest.toPendingVerificationRequest()))
        }
    }

    fun dispatchRequestUpdated(verificationRequest: VerificationRequest) {
        scope.launch {
            Timber.v("## SAS [$myUserId] dispatchRequestUpdated txId:${verificationRequest.flowId()} state:${verificationRequest.innerState()}")
            eventFlow.emit(VerificationEvent.RequestUpdated(verificationRequest.toPendingVerificationRequest()))
        }
    }
}
