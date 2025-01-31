/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
                .mapNotNull {
                    ContentMapper.map(it.root?.content).toModel<SpaceChildContent>()?.let { scc ->
                        // Children where via is not present are ignored.
                        scc.via?.let { via ->
                            SpaceChildInfo(
                                    roomId = it.stateKey,
                                    order = scc.validOrder(),
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
                .mapNotNull {
                    ContentMapper.map(it.root?.content).toModel<SpaceParentContent>()?.let { spaceParentContent ->
                        // Parent where via is not present are ignored.
                        spaceParentContent.via?.let { via ->
                            SpaceParentInfo(
                                    roomId = it.stateKey,
                                    canonical = spaceParentContent.canonical ?: false,
                                    viaServers = via,
                                    stateEventSender = it.root?.sender ?: ""
                            )
                        }
                    }
                }
    }
}
