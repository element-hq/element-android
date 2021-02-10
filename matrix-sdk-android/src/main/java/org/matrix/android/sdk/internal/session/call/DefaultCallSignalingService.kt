/*
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

package org.matrix.android.sdk.internal.session.call

import kotlinx.coroutines.Dispatchers
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.session.call.CallListener
import org.matrix.android.sdk.api.session.call.CallSignalingService
import org.matrix.android.sdk.api.session.call.MxCall
import org.matrix.android.sdk.api.session.call.TurnServerResponse
import org.matrix.android.sdk.api.util.Cancelable
import org.matrix.android.sdk.internal.session.SessionScope
import org.matrix.android.sdk.internal.task.TaskExecutor
import org.matrix.android.sdk.internal.task.launchToCallback
import timber.log.Timber
import javax.inject.Inject

@SessionScope
internal class DefaultCallSignalingService @Inject constructor(
        private val callSignalingHandler: CallSignalingHandler,
        private val mxCallFactory: MxCallFactory,
        private val activeCallHandler: ActiveCallHandler,
        private val taskExecutor: TaskExecutor,
        private val turnServerDataSource: TurnServerDataSource
) : CallSignalingService {

    override fun getTurnServer(callback: MatrixCallback<TurnServerResponse>): Cancelable {
        return taskExecutor.executorScope.launchToCallback(Dispatchers.Default, callback) {
            turnServerDataSource.getTurnServer()
        }
    }

    override fun createOutgoingCall(roomId: String, otherUserId: String, isVideoCall: Boolean): MxCall {
        return mxCallFactory.createOutgoingCall(roomId, otherUserId, isVideoCall).also {
            activeCallHandler.addCall(it)
        }
    }

    override fun addCallListener(listener: CallListener) {
        callSignalingHandler.addCallListener(listener)
    }

    override fun removeCallListener(listener: CallListener) {
        callSignalingHandler.removeCallListener(listener)
    }

    override fun getCallWithId(callId: String): MxCall? {
        Timber.v("## VOIP getCallWithId $callId all calls ${activeCallHandler.getActiveCallsLiveData().value?.map { it.callId }}")
        return activeCallHandler.getCallWithId(callId)
    }

    override fun isThereAnyActiveCall(): Boolean {
        return activeCallHandler.getActiveCallsLiveData().value?.isNotEmpty() == true
    }

    companion object {
        const val CALL_TIMEOUT_MS = 120_000
    }
}
