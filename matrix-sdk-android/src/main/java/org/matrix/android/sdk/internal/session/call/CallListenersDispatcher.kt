/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.call

import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.call.CallListener
import org.matrix.android.sdk.api.session.call.MxCall
import org.matrix.android.sdk.api.session.room.model.call.CallAnswerContent
import org.matrix.android.sdk.api.session.room.model.call.CallAssertedIdentityContent
import org.matrix.android.sdk.api.session.room.model.call.CallCandidatesContent
import org.matrix.android.sdk.api.session.room.model.call.CallHangupContent
import org.matrix.android.sdk.api.session.room.model.call.CallInviteContent
import org.matrix.android.sdk.api.session.room.model.call.CallNegotiateContent
import org.matrix.android.sdk.api.session.room.model.call.CallRejectContent
import org.matrix.android.sdk.api.session.room.model.call.CallSelectAnswerContent

/**
 * Dispatch each method safely to all listeners.
 */
internal class CallListenersDispatcher(private val listeners: Set<CallListener>) : CallListener {

    override fun onCallInviteReceived(mxCall: MxCall, callInviteContent: CallInviteContent) = dispatch {
        it.onCallInviteReceived(mxCall, callInviteContent)
    }

    override fun onCallIceCandidateReceived(mxCall: MxCall, iceCandidatesContent: CallCandidatesContent) = dispatch {
        it.onCallIceCandidateReceived(mxCall, iceCandidatesContent)
    }

    override fun onCallAnswerReceived(callAnswerContent: CallAnswerContent) = dispatch {
        it.onCallAnswerReceived(callAnswerContent)
    }

    override fun onCallHangupReceived(callHangupContent: CallHangupContent) = dispatch {
        it.onCallHangupReceived(callHangupContent)
    }

    override fun onCallRejectReceived(callRejectContent: CallRejectContent) = dispatch {
        it.onCallRejectReceived(callRejectContent)
    }

    override fun onCallManagedByOtherSession(callId: String) = dispatch {
        it.onCallManagedByOtherSession(callId)
    }

    override fun onCallSelectAnswerReceived(callSelectAnswerContent: CallSelectAnswerContent) = dispatch {
        it.onCallSelectAnswerReceived(callSelectAnswerContent)
    }

    override fun onCallNegotiateReceived(callNegotiateContent: CallNegotiateContent) = dispatch {
        it.onCallNegotiateReceived(callNegotiateContent)
    }

    override fun onCallAssertedIdentityReceived(callAssertedIdentityContent: CallAssertedIdentityContent) = dispatch {
        it.onCallAssertedIdentityReceived(callAssertedIdentityContent)
    }

    private fun dispatch(lambda: (CallListener) -> Unit) {
        listeners.toList().forEach {
            tryOrNull {
                lambda(it)
            }
        }
    }
}
