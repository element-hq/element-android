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

package im.vector.matrix.android.internal.crypto

/**
 * possible states for a room key request
 *
 *
 * The state machine looks like:
 * <pre>
 *
 *      |
 *      V
 *    UNSENT  -----------------------------+
 *      |                                  |
 *      | (send successful)                | (cancellation requested)
 *      V                                  |
 *     SENT                                |
 *      |--------------------------------  |  --------------+
 *      |                                  |                |
 *      |                                  |                | (cancellation requested with intent
 *      |                                  |                | to resend a new request)
 *      | (cancellation requested)         |                |
 *      V                                  |                V
 *  CANCELLATION_PENDING                   | CANCELLATION_PENDING_AND_WILL_RESEND
 *      |                                  |                |
 *      | (cancellation sent)              |                | (cancellation sent. Create new request
 *      |                                  |                |  in the UNSENT state)
 *      V                                  |                |
 *  (deleted)  <---------------------------+----------------+
 *  </pre>
 */

enum class ShareRequestState {
    /**
     * request not yet sent
     */
    UNSENT,
    /**
     * request sent, awaiting reply
     */
    SENT,
    /**
     * reply received, cancellation not yet sent
     */
    CANCELLATION_PENDING,
    /**
     * Cancellation not yet sent, once sent, a new request will be done
     */
    CANCELLATION_PENDING_AND_WILL_RESEND,
    /**
     * sending failed
     */
    FAILED;

    companion object {
        fun from(state: Int) = when (state) {
            0          -> UNSENT
            1          -> SENT
            2          -> CANCELLATION_PENDING
            3          -> CANCELLATION_PENDING_AND_WILL_RESEND
            else /*4*/ -> FAILED
        }
    }
}
