/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.space

import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.model.RoomJoinRules
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.space.Space
import org.matrix.android.sdk.api.session.space.model.SpaceChildContent
import org.matrix.android.sdk.internal.session.permalinks.ViaParameterFinder
import org.matrix.android.sdk.internal.session.room.summary.RoomSummaryDataSource

internal class DefaultSpace(
        private val room: Room,
        private val spaceSummaryDataSource: RoomSummaryDataSource,
        private val viaParameterFinder: ViaParameterFinder
) : Space {

    override fun asRoom(): Room {
        return room
    }

    override val spaceId = room.roomId

    override fun spaceSummary(): RoomSummary? {
        return spaceSummaryDataSource.getSpaceSummary(room.roomId)
    }

    override suspend fun addChildren(
            roomId: String,
            viaServers: List<String>?,
            order: String?,
//                                     autoJoin: Boolean,
            suggested: Boolean?
    ) {
        // Find best via
        val bestVia = viaServers
                ?: (spaceSummaryDataSource.getRoomSummary(roomId)
                        ?.takeIf { it.joinRules == RoomJoinRules.RESTRICTED }
                        ?.let {
                            // for restricted room, best to take via from users that can invite in the
                            // child room
                            viaParameterFinder.computeViaParamsForRestricted(roomId, 3)
                        }
                        ?: viaParameterFinder.computeViaParams(roomId, 3))

        room.stateService().sendStateEvent(
                eventType = EventType.STATE_SPACE_CHILD,
                stateKey = roomId,
                body = SpaceChildContent(
                        via = bestVia,
                        order = order,
                        suggested = suggested
                ).toContent()
        )
    }

    override suspend fun removeChildren(roomId: String) {
//        val existing = room.getStateEvents(setOf(EventType.STATE_SPACE_CHILD), QueryStringValue.Equals(roomId))
//                .firstOrNull()
//                ?.content.toModel<SpaceChildContent>()
//                ?: // should we throw here?
//                return

        // edit state event and set via to null
        room.stateService().sendStateEvent(
                eventType = EventType.STATE_SPACE_CHILD,
                stateKey = roomId,
                body = SpaceChildContent(
                        order = null,
                        via = null,
                        suggested = null
                ).toContent()
        )
    }

    override fun getChildInfo(roomId: String): SpaceChildContent? {
        return room.stateService().getStateEvents(setOf(EventType.STATE_SPACE_CHILD), QueryStringValue.Equals(roomId))
                .firstOrNull()
                ?.content.toModel<SpaceChildContent>()
    }

    override suspend fun setChildrenOrder(roomId: String, order: String?) {
        val existing = room.stateService().getStateEvents(setOf(EventType.STATE_SPACE_CHILD), QueryStringValue.Equals(roomId))
                .firstOrNull()
                ?.content.toModel<SpaceChildContent>()
                ?: throw IllegalArgumentException("$roomId is not a child of this space")

        // edit state event and set via to null
        room.stateService().sendStateEvent(
                eventType = EventType.STATE_SPACE_CHILD,
                stateKey = roomId,
                body = SpaceChildContent(
                        order = order,
                        via = existing.via,
//                        autoJoin = existing.autoJoin,
                        suggested = existing.suggested
                ).toContent()
        )
    }

//    override suspend fun setChildrenAutoJoin(roomId: String, autoJoin: Boolean) {
//        val existing = room.getStateEvents(setOf(EventType.STATE_SPACE_CHILD), QueryStringValue.Equals(roomId))
//                .firstOrNull()
//                ?.content.toModel<SpaceChildContent>()
//                ?: throw IllegalArgumentException("$roomId is not a child of this space")
//
//        if (existing.autoJoin == autoJoin) {
//            // nothing to do?
//            return
//        }
//
//        // edit state event and set via to null
//        room.sendStateEvent(
//                eventType = EventType.STATE_SPACE_CHILD,
//                stateKey = roomId,
//                body = SpaceChildContent(
//                        order = existing.order,
//                        via = existing.via,
//                        autoJoin = autoJoin,
//                        suggested = existing.suggested
//                ).toContent()
//        )
//    }

    override suspend fun setChildrenSuggested(roomId: String, suggested: Boolean) {
        val existing = room.stateService().getStateEvents(setOf(EventType.STATE_SPACE_CHILD), QueryStringValue.Equals(roomId))
                .firstOrNull()
                ?.content.toModel<SpaceChildContent>()
                ?: throw IllegalArgumentException("$roomId is not a child of this space")

        if (existing.suggested == suggested) {
            // nothing to do?
            return
        }
        // edit state event and set via to null
        room.stateService().sendStateEvent(
                eventType = EventType.STATE_SPACE_CHILD,
                stateKey = roomId,
                body = SpaceChildContent(
                        order = existing.order,
                        via = existing.via,
//                        autoJoin = existing.autoJoin,
                        suggested = suggested
                ).toContent()
        )
    }
}
