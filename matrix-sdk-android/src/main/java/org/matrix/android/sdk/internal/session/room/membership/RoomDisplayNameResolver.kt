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

package org.matrix.android.sdk.internal.session.room.membership

import io.realm.Realm
import org.matrix.android.sdk.api.MatrixConfiguration
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomCanonicalAliasContent
import org.matrix.android.sdk.api.session.room.model.RoomNameContent
import org.matrix.android.sdk.internal.database.mapper.ContentMapper
import org.matrix.android.sdk.internal.database.model.CurrentStateEventEntity
import org.matrix.android.sdk.internal.database.model.RoomEntity
import org.matrix.android.sdk.internal.database.model.RoomMemberSummaryEntity
import org.matrix.android.sdk.internal.database.model.RoomMemberSummaryEntityFields
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntity
import org.matrix.android.sdk.internal.database.query.getOrNull
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.session.displayname.DisplayNameResolver
import org.matrix.android.sdk.internal.util.Normalizer
import javax.inject.Inject

/**
 * This class computes room display name
 */
internal class RoomDisplayNameResolver @Inject constructor(
        matrixConfiguration: MatrixConfiguration,
        private val displayNameResolver: DisplayNameResolver,
        private val normalizer: Normalizer,
        @UserId private val userId: String
) {

    private val roomDisplayNameFallbackProvider = matrixConfiguration.roomDisplayNameFallbackProvider

    /**
     * Compute the room display name
     *
     * @param realm: the current instance of realm
     * @param roomId: the roomId to resolve the name of.
     * @return the room display name
     */
    fun resolve(realm: Realm, roomId: String): RoomName {
        // this algorithm is the one defined in
        // https://github.com/matrix-org/matrix-js-sdk/blob/develop/lib/models/room.js#L617
        // calculateRoomName(room, userId)

        // For Lazy Loaded room, see algorithm here:
        // https://docs.google.com/document/d/11i14UI1cUz-OJ0knD5BFu7fmT6Fo327zvMYqfSAR7xs/edit#heading=h.qif6pkqyjgzn
        var name: String?
        val roomEntity = RoomEntity.where(realm, roomId = roomId).findFirst()
        val roomName = CurrentStateEventEntity.getOrNull(realm, roomId, type = EventType.STATE_ROOM_NAME, stateKey = "")?.root
        name = ContentMapper.map(roomName?.content).toModel<RoomNameContent>()?.name
        if (!name.isNullOrEmpty()) {
            return name.toRoomName()
        }
        val canonicalAlias = CurrentStateEventEntity.getOrNull(realm, roomId, type = EventType.STATE_ROOM_CANONICAL_ALIAS, stateKey = "")?.root
        name = ContentMapper.map(canonicalAlias?.content).toModel<RoomCanonicalAliasContent>()?.canonicalAlias
        if (!name.isNullOrEmpty()) {
            return name.toRoomName()
        }

        val roomMembers = RoomMemberHelper(realm, roomId)
        val activeMembers = roomMembers.queryActiveRoomMembersEvent().findAll()

        if (roomEntity?.membership == Membership.INVITE) {
            val inviteMeEvent = roomMembers.getLastStateEvent(userId)
            val inviterId = inviteMeEvent?.sender
            name = inviterId
                    ?.let {
                        activeMembers.where()
                                .equalTo(RoomMemberSummaryEntityFields.USER_ID, it)
                                .findFirst()
                                ?.toMatrixItem()
                                ?.let { matrixItem -> displayNameResolver.getBestName(matrixItem) }
                    }
                    ?: roomDisplayNameFallbackProvider.getNameForRoomInvite()
        } else if (roomEntity?.membership == Membership.JOIN) {
            val roomSummary = RoomSummaryEntity.where(realm, roomId).findFirst()
            val invitedCount = roomSummary?.invitedMembersCount ?: 0
            val joinedCount = roomSummary?.joinedMembersCount ?: 0
            val otherMembersSubset: List<RoomMemberSummaryEntity> = if (roomSummary?.heroes?.isNotEmpty() == true) {
                roomSummary.heroes.mapNotNull { userId ->
                    roomMembers.getLastRoomMember(userId)?.takeIf {
                        it.membership == Membership.INVITE || it.membership == Membership.JOIN
                    }
                }
            } else {
                activeMembers.where()
                        .notEqualTo(RoomMemberSummaryEntityFields.USER_ID, userId)
                        .limit(5)
                        .findAll()
                        .createSnapshot()
            }
            val otherMembersCount = otherMembersSubset.count()
            name = when (otherMembersCount) {
                0    -> {
                    // Get left members if any
                    val leftMembersNames = roomMembers.queryLeftRoomMembersEvent()
                            .findAll()
                            .map { displayNameResolver.getBestName(it.toMatrixItem()) }
                    roomDisplayNameFallbackProvider.getNameForEmptyRoom(roomSummary?.isDirect.orFalse(), leftMembersNames)
                }
                1    -> {
                    roomDisplayNameFallbackProvider.getNameFor1member(
                            resolveRoomMemberName(otherMembersSubset[0], roomMembers)
                    )
                }
                2    -> {
                    roomDisplayNameFallbackProvider.getNameFor2members(
                            resolveRoomMemberName(otherMembersSubset[0], roomMembers),
                            resolveRoomMemberName(otherMembersSubset[1], roomMembers)
                    )
                }
                3    -> {
                    roomDisplayNameFallbackProvider.getNameFor3members(
                            resolveRoomMemberName(otherMembersSubset[0], roomMembers),
                            resolveRoomMemberName(otherMembersSubset[1], roomMembers),
                            resolveRoomMemberName(otherMembersSubset[2], roomMembers)
                    )
                }
                4    -> {
                    roomDisplayNameFallbackProvider.getNameFor4members(
                            resolveRoomMemberName(otherMembersSubset[0], roomMembers),
                            resolveRoomMemberName(otherMembersSubset[1], roomMembers),
                            resolveRoomMemberName(otherMembersSubset[2], roomMembers),
                            resolveRoomMemberName(otherMembersSubset[3], roomMembers)
                    )
                }
                else -> {
                    val remainingCount = invitedCount + joinedCount - otherMembersCount + 1
                    roomDisplayNameFallbackProvider.getNameFor4membersAndMore(
                            resolveRoomMemberName(otherMembersSubset[0], roomMembers),
                            resolveRoomMemberName(otherMembersSubset[1], roomMembers),
                            resolveRoomMemberName(otherMembersSubset[2], roomMembers),
                            remainingCount
                    )
                }
            }
        }
        return (name ?: roomId).toRoomName()
    }

    /** See [org.matrix.android.sdk.api.session.room.sender.SenderInfo.disambiguatedDisplayName] */
    private fun resolveRoomMemberName(roomMemberSummary: RoomMemberSummaryEntity,
                                      roomMemberHelper: RoomMemberHelper): String {
        val isUnique = roomMemberHelper.isUniqueDisplayName(roomMemberSummary.displayName)
        return if (isUnique) {
            displayNameResolver.getBestName(roomMemberSummary.toMatrixItem())
        } else {
            "${roomMemberSummary.displayName} (${roomMemberSummary.userId})"
        }
    }

    private fun String.toRoomName() = RoomName(this, normalizedName = normalizer.normalize(this))
}

internal data class RoomName(val name: String, val normalizedName: String)
