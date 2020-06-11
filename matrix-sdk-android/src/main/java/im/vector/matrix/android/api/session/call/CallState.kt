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

package im.vector.matrix.android.api.session.call

enum class CallState {

    /** Idle, setting up objects */
    IDLE,

    /** Dialing.  Outgoing call is signaling the remote peer */
    DIALING,

    /** Answering.  Incoming call is responding to remote peer */
    ANSWERING,

    /** Remote ringing. Outgoing call, ICE negotiation is complete */
    REMOTE_RINGING,

    /** Local ringing. Incoming call, ICE negotiation is complete */
    LOCAL_RINGING,

    /** Connected. Incoming/Outgoing call, the call is connected */
    CONNECTED,

    /** Terminated.  Incoming/Outgoing call, the call is terminated */
    TERMINATED,

}
