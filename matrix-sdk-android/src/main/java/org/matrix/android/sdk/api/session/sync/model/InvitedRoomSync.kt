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

package org.matrix.android.sdk.api.session.sync.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// InvitedRoomSync represents a room invitation during server sync v2.
@JsonClass(generateAdapter = true)
data class InvitedRoomSync(

        /**
         * The state of a room that the user has been invited to. These state events may only have the 'sender', 'type', 'state_key'
         * and 'content' keys present. These events do not replace any state that the client already has for the room, for example if
         * the client has archived the room. Instead the client should keep two separate copies of the state: the one from the 'invite_state'
         * and one from the archived 'state'. If the client joins the room then the current state will be given as a delta against the
         * archived 'state' not the 'invite_state'.
         */
        @Json(name = "invite_state") val inviteState: RoomInviteState? = null
)
