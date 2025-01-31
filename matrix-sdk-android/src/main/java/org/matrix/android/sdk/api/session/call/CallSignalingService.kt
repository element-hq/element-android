/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.call

interface CallSignalingService {

    suspend fun getTurnServer(): TurnServerResponse

    /**
     * Create an outgoing call.
     */
    fun createOutgoingCall(roomId: String, otherUserId: String, isVideoCall: Boolean): MxCall

    fun addCallListener(listener: CallListener)

    fun removeCallListener(listener: CallListener)

    fun getCallWithId(callId: String): MxCall?

    fun isThereAnyActiveCall(): Boolean
}
