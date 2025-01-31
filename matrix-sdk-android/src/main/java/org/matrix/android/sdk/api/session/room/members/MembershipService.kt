/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.members

import androidx.lifecycle.LiveData
import org.matrix.android.sdk.api.session.identity.ThreePid
import org.matrix.android.sdk.api.session.room.model.RoomMemberSummary

/**
 * This interface defines methods to handling membership. It's implemented at the room level.
 */
interface MembershipService {

    /**
     * This methods load all room members if it was done yet.
     */
    suspend fun loadRoomMembersIfNeeded()

    /**
     * All the room members can be not loaded, for instance after an initial sync.
     * All the members will be loaded when calling [loadRoomMembersIfNeeded], or when sending an encrypted
     * event to the room.
     * The fun let the app know if all the members have been loaded for this room.
     * @return true if all the members are loaded, or false elsewhere.
     */
    suspend fun areAllMembersLoaded(): Boolean

    /**
     * Live version for [areAllMembersLoaded].
     */
    fun areAllMembersLoadedLive(): LiveData<Boolean>

    /**
     * Return the roomMember with userId or null.
     * @param userId the userId param to look for
     *
     * @return the roomMember with userId or null
     */
    fun getRoomMember(userId: String): RoomMemberSummary?

    /**
     * Return all the roomMembers of the room with params.
     * @param queryParams the params to query for
     * @return a roomMember list.
     */
    fun getRoomMembers(queryParams: RoomMemberQueryParams): List<RoomMemberSummary>

    /**
     * Return all the roomMembers of the room filtered by memberships.
     * @param queryParams the params to query for
     * @return a [LiveData] of roomMember list.
     */
    fun getRoomMembersLive(queryParams: RoomMemberQueryParams): LiveData<List<RoomMemberSummary>>

    fun getNumberOfJoinedMembers(): Int

    /**
     * Invite a user in the room.
     */
    suspend fun invite(userId: String, reason: String? = null)

    /**
     * Invite a user with email or phone number in the room.
     */
    suspend fun invite3pid(threePid: ThreePid)

    /**
     * Ban a user from the room.
     */
    suspend fun ban(userId: String, reason: String? = null)

    /**
     * Unban a user from the room.
     */
    suspend fun unban(userId: String, reason: String? = null)

    /**
     * Remove a user from the room.
     */
    suspend fun remove(userId: String, reason: String? = null)

    @Deprecated("Use remove instead", ReplaceWith("remove(userId, reason)"))
    suspend fun kick(userId: String, reason: String? = null) = remove(userId, reason)
}
