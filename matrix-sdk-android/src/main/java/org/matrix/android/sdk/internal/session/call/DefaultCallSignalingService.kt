/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.call

import org.matrix.android.sdk.api.session.call.CallListener
import org.matrix.android.sdk.api.session.call.CallSignalingService
import org.matrix.android.sdk.api.session.call.MxCall
import org.matrix.android.sdk.api.session.call.TurnServerResponse
import org.matrix.android.sdk.internal.session.SessionScope
import javax.inject.Inject

@SessionScope
internal class DefaultCallSignalingService @Inject constructor(
        private val callSignalingHandler: CallSignalingHandler,
        private val mxCallFactory: MxCallFactory,
        private val activeCallHandler: ActiveCallHandler,
        private val turnServerDataSource: TurnServerDataSource
) : CallSignalingService {

    override suspend fun getTurnServer(): TurnServerResponse {
        return turnServerDataSource.getTurnServer()
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
        return activeCallHandler.getCallWithId(callId)
    }

    override fun isThereAnyActiveCall(): Boolean {
        return activeCallHandler.getActiveCallsLiveData().value?.isNotEmpty() == true
    }

    companion object {
        const val CALL_TIMEOUT_MS = 120_000
    }
}
