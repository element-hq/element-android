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

package org.matrix.android.sdk.internal.session.room.relationship

import io.realm.Realm
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.space.model.SpaceChildContent
import org.matrix.android.sdk.api.session.space.model.SpaceParentContent
import org.matrix.android.sdk.internal.database.mapper.ContentMapper
import org.matrix.android.sdk.internal.database.model.CurrentStateEventEntity
import org.matrix.android.sdk.internal.database.query.whereType

/**
 * Relationship between rooms and spaces
 * The intention is that rooms and spaces form a hierarchy, which clients can use to structure the user's room list into a tree view.
 * The parent/child relationship can be expressed in one of two ways:
 *  - The admins of a space can advertise rooms and subspaces for their space by setting m.space.child state events.
 *  The state_key is the ID of a child room or space, and the content should contain a via key which gives
 *  a list of candidate servers that can be used to join the room. present: true key is included to distinguish from a deleted state event.
 *
 *  - Separately, rooms can claim parents via the m.room.parent state event.
 */
internal class RoomChildRelationInfo(
        private val realm: Realm,
        private val roomId: String
) {

    data class SpaceChildInfo(
            val roomId: String,
            val order: String?,
//            val autoJoin: Boolean,
            val viaServers: List<String>
    )

    data class SpaceParentInfo(
            val roomId: String,
            val canonical: Boolean,
            val viaServers: List<String>,
            val stateEventSender: String
    )

    /**
     * Gets the ordered list of valid child description.
     */
    fun getDirectChildrenDescriptions(): List<SpaceChildInfo> {
        return CurrentStateEventEntity.whereType(realm, roomId, EventType.STATE_SPACE_CHILD)
                .findAll()
//                .also {
//                    Timber.v("## Space: Found ${it.count()} m.space.child state events for $roomId")
//                }
                .mapNotNull {
                    ContentMapper.map(it.root?.content).toModel<SpaceChildContent>()?.let { scc ->
//                        Timber.v("## Space child desc state event $scc")
                        // Children where via is not present are ignored.
                        scc.via?.let { via ->
                            SpaceChildInfo(
                                    roomId = it.stateKey,
                                    order = scc.validOrder(),
//                                    autoJoin = scc.autoJoin ?: false,
                                    viaServers = via
                            )
                        }
                    }
                }
                .sortedBy { it.order }
    }

    fun getParentDescriptions(): List<SpaceParentInfo> {
        return CurrentStateEventEntity.whereType(realm, roomId, EventType.STATE_SPACE_PARENT)
                .findAll()
//                .also {
//                    Timber.v("## Space: Found ${it.count()} m.space.parent state events for $roomId")
//                }
                .mapNotNull {
                    ContentMapper.map(it.root?.content).toModel<SpaceParentContent>()?.let { scc ->
//                        Timber.v("## Space parent desc state event $scc")
                        // Parent where via is not present are ignored.
                        scc.via?.let { via ->
                            SpaceParentInfo(
                                    roomId = it.stateKey,
                                    canonical = scc.canonical ?: false,
                                    viaServers = via,
                                    stateEventSender = it.root?.sender ?: ""
                            )
                        }
                    }
                }
    }
}
