/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.permalinks

import org.matrix.android.sdk.api.MatrixPatterns.getServerName
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.members.roomMemberQueryParams
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.PowerLevelsContent
import org.matrix.android.sdk.api.session.room.powerlevels.PowerLevelsHelper
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.session.room.RoomGetter
import org.matrix.android.sdk.internal.session.room.state.StateEventDataSource
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Provider

internal class ViaParameterFinder @Inject constructor(
        @UserId private val userId: String,
        private val roomGetterProvider: Provider<RoomGetter>,
        private val stateEventDataSource: StateEventDataSource
) {

    fun computeViaParams(roomId: String, max: Int): List<String> {
        return computeViaParams(userId, roomId, max)
    }

    /**
     * Compute the via parameters.
     * Take up to 3 homeserver domains, taking the most representative one regarding room members and including the
     * current user one.
     */
    fun computeViaParams(userId: String, roomId: String): String {
        return asUrlViaParameters(computeViaParams(userId, roomId, 3))
    }

    fun asUrlViaParameters(viaList: List<String>): String {
        return viaList.joinToString(prefix = "?via=", separator = "&via=") { URLEncoder.encode(it, "utf-8") }
    }

    fun computeViaParams(userId: String, roomId: String, max: Int): List<String> {
        val userHomeserver = userId.getServerName()
        return getUserIdsOfJoinedMembers(roomId)
                .map { it.getServerName() }
                .groupBy { it }
                .mapValues { it.value.size }
                .toMutableMap()
                // Ensure the user homeserver will be included
                .apply { this[userHomeserver] = Int.MAX_VALUE }
                .let { map -> map.keys.sortedByDescending { map[it] } }
                .take(max)
    }

    /**
     * Get a set of userIds of joined members of a room.
     */
    private fun getUserIdsOfJoinedMembers(roomId: String): Set<String> {
        return roomGetterProvider.get().getRoom(roomId)
                ?.membershipService()
                ?.getRoomMembers(roomMemberQueryParams { memberships = listOf(Membership.JOIN) })
                ?.map { it.userId }
                .orEmpty()
                .toSet()
    }

    // not used much for now but as per MSC1772
    // the via parameter of m.space.child must contain a via key which gives a list of candidate servers that can be used to join the room.
    // It is possible for the list of candidate servers and the list of authorised servers to diverge.
    // It may not be possible for a user to join a room if there's no overlap between these
    fun computeViaParamsForRestricted(roomId: String, max: Int): List<String> {
        val userThatCanInvite = roomGetterProvider.get().getRoom(roomId)
                ?.membershipService()
                ?.getRoomMembers(roomMemberQueryParams { memberships = listOf(Membership.JOIN) })
                ?.map { it.userId }
                ?.filter { userCanInvite(userId, roomId) }
                .orEmpty()
                .toSet()

        return userThatCanInvite.map { it.getServerName() }
                .groupBy { it }
                .mapValues { it.value.size }
                .toMutableMap()
                .let { map -> map.keys.sortedByDescending { map[it] } }
                .take(max)
    }

    fun userCanInvite(userId: String, roomId: String): Boolean {
        val powerLevelsHelper = stateEventDataSource.getStateEvent(roomId, EventType.STATE_ROOM_POWER_LEVELS, QueryStringValue.IsEmpty)
                ?.content?.toModel<PowerLevelsContent>()
                ?.let { PowerLevelsHelper(it) }

        return powerLevelsHelper?.isUserAbleToInvite(userId) ?: false
    }
}
