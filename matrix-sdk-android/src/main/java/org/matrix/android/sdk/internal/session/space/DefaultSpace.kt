/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.space

import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.space.Space
import org.matrix.android.sdk.api.session.space.model.SpaceChildContent

class DefaultSpace(private val room: Room) : Space {

    override fun asRoom(): Room {
        return room
    }

    override suspend fun addRoom(roomId: String) {
        asRoom().sendStateEvent(
                eventType = EventType.STATE_SPACE_CHILD,
                stateKey = roomId,
                body = SpaceChildContent(present = true).toContent()
        )
    }

//    override fun getChildren(): List<IRoomSummary> {
// //        asRoom().getStateEvents(setOf(EventType.STATE_SPACE_CHILD)).mapNotNull {
// //            // statekeys are the roomIds
// //
// //        }
//        return emptyList<IRoomSummary>()
//    }
}
