/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.call

import org.matrix.android.sdk.api.session.room.model.call.CallAnswerContent
import org.matrix.android.sdk.api.session.room.model.call.CallAssertedIdentityContent
import org.matrix.android.sdk.api.session.room.model.call.CallCandidatesContent
import org.matrix.android.sdk.api.session.room.model.call.CallHangupContent
import org.matrix.android.sdk.api.session.room.model.call.CallInviteContent
import org.matrix.android.sdk.api.session.room.model.call.CallNegotiateContent
import org.matrix.android.sdk.api.session.room.model.call.CallRejectContent
import org.matrix.android.sdk.api.session.room.model.call.CallSelectAnswerContent

interface CallListener {
    /**
     * Called when there is an incoming call within the room.
     */
    fun onCallInviteReceived(mxCall: MxCall, callInviteContent: CallInviteContent)

    fun onCallIceCandidateReceived(mxCall: MxCall, iceCandidatesContent: CallCandidatesContent)

    /**
     * An outgoing call is started.
     */
    fun onCallAnswerReceived(callAnswerContent: CallAnswerContent)

    /**
     * Called when a called has been hung up.
     */
    fun onCallHangupReceived(callHangupContent: CallHangupContent)

    /**
     * Called when a called has been rejected.
     */
    fun onCallRejectReceived(callRejectContent: CallRejectContent)

    /**
     * Called when an answer has been selected.
     */
    fun onCallSelectAnswerReceived(callSelectAnswerContent: CallSelectAnswerContent)

    /**
     * Called when a negotiation is sent.
     */
    fun onCallNegotiateReceived(callNegotiateContent: CallNegotiateContent)

    /**
     * Called when the call has been managed by an other session.
     */
    fun onCallManagedByOtherSession(callId: String)

    /**
     * Called when an asserted identity event is received.
     */
    fun onCallAssertedIdentityReceived(callAssertedIdentityContent: CallAssertedIdentityContent)
}
