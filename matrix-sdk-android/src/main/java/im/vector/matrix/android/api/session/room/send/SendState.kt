/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.api.session.room.send

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

    fun isSent(): Boolean {
        return this == SENT || this == SYNCED
    }

    fun hasFailed(): Boolean {
        return this == UNDELIVERED || this == FAILED_UNKNOWN_DEVICES
    }

    fun isSending(): Boolean {
        return this == UNSENT || this == ENCRYPTING || this == SENDING
    }

}
