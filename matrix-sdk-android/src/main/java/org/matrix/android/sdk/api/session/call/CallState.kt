/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.call

import org.matrix.android.sdk.api.session.room.model.call.EndCallReason

sealed class CallState {

    /** Idle, setting up objects. */
    object Idle : CallState()

    /**
     * CreateOffer. Intermediate state between Idle and Dialing.
     */
    object CreateOffer : CallState()

    /** Dialing.  Outgoing call is signaling the remote peer */
    object Dialing : CallState()

    /** Local ringing. Incoming call offer received */
    object LocalRinging : CallState()

    /** Answering.  Incoming call is responding to remote peer */
    object Answering : CallState()

    /**
     * Connected. Incoming/Outgoing call, ice layer connecting or connected
     * Notice that the PeerState failed is not always final, if you switch network, new ice candidtates
     * could be exchanged, and the connection could go back to connected
     * */
    data class Connected(val iceConnectionState: MxPeerConnectionState) : CallState()

    /** Ended.  Incoming/Outgoing call, the call is terminated */
    data class Ended(val reason: EndCallReason? = null) : CallState()
}
