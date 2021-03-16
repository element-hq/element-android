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

import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.space.Space
import org.matrix.android.sdk.api.session.space.model.SpaceChildContent
import java.lang.IllegalArgumentException

class DefaultSpace(private val room: Room) : Space {

    override fun asRoom(): Room {
        return room
    }

    override suspend fun addChildren(roomId: String, viaServers: List<String>, order: String?, autoJoin: Boolean) {
        asRoom().sendStateEvent(
                eventType = EventType.STATE_SPACE_CHILD,
                stateKey = roomId,
                body = SpaceChildContent(
                        via = viaServers,
                        autoJoin = autoJoin,
                        order = order
                ).toContent()
        )
    }

    override suspend fun removeRoom(roomId: String) {
        val existing = asRoom().getStateEvents(setOf(EventType.STATE_SPACE_CHILD), QueryStringValue.Equals(roomId))
                .firstOrNull()
                ?.content.toModel<SpaceChildContent>()
                ?: // should we throw here?
                return

        // edit state event and set via to null
        asRoom().sendStateEvent(
                eventType = EventType.STATE_SPACE_CHILD,
                stateKey = roomId,
                body = SpaceChildContent(
                        order = existing.order,
                        via = null,
                        autoJoin = existing.autoJoin
                ).toContent()
        )
    }

    override suspend fun setChildrenOrder(roomId: String, order: String?) {
        val existing = asRoom().getStateEvents(setOf(EventType.STATE_SPACE_CHILD), QueryStringValue.Equals(roomId))
                .firstOrNull()
                ?.content.toModel<SpaceChildContent>()
                ?: throw IllegalArgumentException("$roomId is not a child of this space")

        // edit state event and set via to null
        asRoom().sendStateEvent(
                eventType = EventType.STATE_SPACE_CHILD,
                stateKey = roomId,
                body = SpaceChildContent(
                        order = order,
                        via = existing.via,
                        autoJoin = existing.autoJoin
                ).toContent()
        )
    }

    override suspend fun setChildrenAutoJoin(roomId: String, autoJoin: Boolean) {
        val existing = asRoom().getStateEvents(setOf(EventType.STATE_SPACE_CHILD), QueryStringValue.Equals(roomId))
                .firstOrNull()
                ?.content.toModel<SpaceChildContent>()
                ?: throw IllegalArgumentException("$roomId is not a child of this space")

        // edit state event and set via to null
        asRoom().sendStateEvent(
                eventType = EventType.STATE_SPACE_CHILD,
                stateKey = roomId,
                body = SpaceChildContent(
                        order = existing.order,
                        via = existing.via,
                        autoJoin = autoJoin
                ).toContent()
        )
    }
}
