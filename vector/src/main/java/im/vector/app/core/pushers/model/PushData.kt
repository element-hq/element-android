/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.core.pushers.model

import im.vector.app.core.pushers.PushersManager
import org.matrix.android.sdk.api.MatrixPatterns

sealed interface PushData {

    /**
     * Represent parsed data that the app has received from a Push content.
     *
     * @property eventId The Event ID. If not null, it will not be empty, and will have a valid format.
     * @property roomId The Room ID. If not null, it will not be empty, and will have a valid format.
     * @property unread Number of unread message.
     */
    data class Event(
            val eventId: String?,
            val roomId: String?,
            val unread: Int?,
    ) : PushData

    data class RemoteWipe(
            val nonce: String
    ) : PushData

    object Diagnostic : PushData

    companion object Factory {
        fun create(
                eventId: String?,
                roomId: String?,
                unread: Int?,
                remoteWipeNonce: String?,
        ): PushData =
                if (eventId == PushersManager.TEST_EVENT_ID) {
                    Diagnostic
                } else if (remoteWipeNonce != null) {
                    RemoteWipe(nonce = remoteWipeNonce)
                } else {
                    Event(
                            eventId = eventId?.takeIf { MatrixPatterns.isEventId(it) },
                            roomId = roomId?.takeIf { MatrixPatterns.isRoomId(it) },
                            unread = unread,
                    )
                }
    }
}
