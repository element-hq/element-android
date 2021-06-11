/*
 * Copyright (c) 2020 The Matrix.org Foundation C.I.C.
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
