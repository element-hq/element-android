/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.api.session.room.send

enum class SendState {
    UNKNOWN,

    // the event has not been sent
    UNSENT,

    // the event is encrypting
    ENCRYPTING,

    // the event is currently sending
    SENDING,

    // the event has been sent
    SENT,

    // the event has been received from server
    SYNCED,

    // The event failed to be sent
    UNDELIVERED,

    // the event failed to be sent because some unknown devices have been found while encrypting it
    FAILED_UNKNOWN_DEVICES;

    internal companion object {
        val HAS_FAILED_STATES = listOf(UNDELIVERED, FAILED_UNKNOWN_DEVICES)
        val IS_SENT_STATES = listOf(SENT, SYNCED)
        val IS_PROGRESSING_STATES = listOf(ENCRYPTING, SENDING)
        val IS_SENDING_STATES = IS_PROGRESSING_STATES + UNSENT
        val PENDING_STATES = IS_SENDING_STATES + HAS_FAILED_STATES
    }

    fun isSent() = IS_SENT_STATES.contains(this)

    fun hasFailed() = HAS_FAILED_STATES.contains(this)

    fun isInProgress() = IS_PROGRESSING_STATES.contains(this)

    fun isSending() = IS_SENDING_STATES.contains(this)
}
