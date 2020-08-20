/*
 * Copyright (c) 2020 New Vector Ltd
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

package org.matrix.android.sdk.internal.session.permalinks

import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.permalinks.PermalinkService.Companion.MATRIX_TO_URL_BASE
import org.matrix.android.sdk.api.session.room.members.roomMemberQueryParams
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.session.room.RoomGetter
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Provider

internal class PermalinkFactory @Inject constructor(
        @UserId
        private val userId: String,
        // Use a provider to fix circular Dagger dependency
        private val roomGetterProvider: Provider<RoomGetter>
) {

    fun createPermalink(event: Event): String? {
        if (event.roomId.isNullOrEmpty() || event.eventId.isNullOrEmpty()) {
            return null
        }
        return createPermalink(event.roomId, event.eventId)
    }

    fun createPermalink(id: String): String? {
        return if (id.isEmpty()) {
            null
        } else MATRIX_TO_URL_BASE + escape(id)
    }

    fun createRoomPermalink(roomId: String): String? {
        return if (roomId.isEmpty()) {
            null
        } else {
            MATRIX_TO_URL_BASE + escape(roomId) + computeViaParams(userId, roomId)
        }
    }

    fun createPermalink(roomId: String, eventId: String): String {
        return MATRIX_TO_URL_BASE + escape(roomId) + "/" + escape(eventId) + computeViaParams(userId, roomId)
    }

    fun getLinkedId(url: String): String? {
        val isSupported = url.startsWith(MATRIX_TO_URL_BASE)

        return if (isSupported) {
            url.substring(MATRIX_TO_URL_BASE.length)
        } else null
    }

    /**
     * Compute the via parameters.
     * Take up to 3 homeserver domains, taking the most representative one regarding room members and including the
     * current user one.
     */
    private fun computeViaParams(userId: String, roomId: String): String {
        val userHomeserver = userId.substringAfter(":")
        return getUserIdsOfJoinedMembers(roomId)
                .map { it.substringAfter(":") }
                .groupBy { it }
                .mapValues { it.value.size }
                .toMutableMap()
                // Ensure the user homeserver will be included
                .apply { this[userHomeserver] = Int.MAX_VALUE }
                .let { map -> map.keys.sortedByDescending { map[it] } }
                .take(3)
                .joinToString(prefix = "?via=", separator = "&via=") { URLEncoder.encode(it, "utf-8") }
    }

    /**
     * Escape '/' in id, because it is used as a separator
     *
     * @param id the id to escape
     * @return the escaped id
     */
    private fun escape(id: String): String {
        return id.replace("/", "%2F")
    }

    /**
     * Unescape '/' in id
     *
     * @param id the id to escape
     * @return the escaped id
     */
    private fun unescape(id: String): String {
        return id.replace("%2F", "/")
    }

    /**
     * Get a set of userIds of joined members of a room
     */
    private fun getUserIdsOfJoinedMembers(roomId: String): Set<String> {
        return roomGetterProvider.get().getRoom(roomId)
                ?.getRoomMembers(roomMemberQueryParams { memberships = listOf(Membership.JOIN) })
                ?.map { it.userId }
                .orEmpty()
                .toSet()
    }
}
