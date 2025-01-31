/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.room.membership

import io.realm.Realm
import org.matrix.android.sdk.api.session.events.model.Content
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.room.model.RoomMemberContent
import org.matrix.android.sdk.internal.database.model.RoomMemberSummaryEntity
import org.matrix.android.sdk.internal.database.model.presence.UserPresenceEntity
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.session.events.getFixedRoomMemberContent
import org.matrix.android.sdk.internal.session.sync.SyncResponsePostTreatmentAggregator
import org.matrix.android.sdk.internal.session.user.UserEntityFactory
import javax.inject.Inject

internal class RoomMemberEventHandler @Inject constructor(
        @UserId private val myUserId: String
) {

    fun handle(
            realm: Realm,
            roomId: String,
            event: Event,
            isInitialSync: Boolean,
            aggregator: SyncResponsePostTreatmentAggregator? = null
    ): Boolean {
        if (event.type != EventType.STATE_ROOM_MEMBER) {
            return false
        }
        val eventUserId = event.stateKey ?: return false
        val roomMember = event.getFixedRoomMemberContent() ?: return false

        return if (isInitialSync) {
            handleInitialSync(realm, roomId, myUserId, eventUserId, roomMember, aggregator)
        } else {
            handleIncrementalSync(
                    realm,
                    roomId,
                    eventUserId,
                    roomMember,
                    event.resolvedPrevContent(),
                    aggregator
            )
        }
    }

    private fun handleInitialSync(
            realm: Realm,
            roomId: String,
            currentUserId: String,
            eventUserId: String,
            roomMember: RoomMemberContent,
            aggregator: SyncResponsePostTreatmentAggregator?
    ): Boolean {
        if (currentUserId != eventUserId) {
            saveUserEntityLocallyIfNecessary(realm, eventUserId, roomMember)
        }
        saveRoomMemberEntityLocally(realm, roomId, eventUserId, roomMember)
        updateDirectChatsIfNecessary(roomId, roomMember, aggregator)
        return true
    }

    private fun saveRoomMemberEntityLocally(
            realm: Realm,
            roomId: String,
            userId: String,
            roomMember: RoomMemberContent
    ) {
        val existingRoomMemberSummary = RoomMemberSummaryEntity.where(realm, roomId, userId).findFirst()
        if (existingRoomMemberSummary != null) {
            existingRoomMemberSummary.displayName = roomMember.displayName
            existingRoomMemberSummary.avatarUrl = roomMember.avatarUrl
            existingRoomMemberSummary.membership = roomMember.membership
        } else {
            val presenceEntity = UserPresenceEntity.where(realm, userId).findFirst()
            val roomMemberEntity = RoomMemberEntityFactory.create(
                    roomId,
                    userId,
                    roomMember,
                    presenceEntity
            )
            realm.insert(roomMemberEntity)
        }
    }

    private fun saveUserEntityLocallyIfNecessary(
            realm: Realm,
            userId: String,
            roomMember: RoomMemberContent
    ) {
        if (roomMember.membership.isActive()) {
            saveUserLocally(realm, userId, roomMember)
        }
    }

    private fun saveUserLocally(realm: Realm, userId: String, roomMember: RoomMemberContent) {
        val userEntity = UserEntityFactory.create(userId, roomMember)
        realm.insertOrUpdate(userEntity)
    }

    private fun updateDirectChatsIfNecessary(
            roomId: String,
            roomMember: RoomMemberContent,
            aggregator: SyncResponsePostTreatmentAggregator?
    ) {
        // check whether this new room member event may be used to update the directs dictionary in account data
        // this is required to handle correctly invite by email in DM
        val mxId = roomMember.thirdPartyInvite?.signed?.mxid
        if (mxId != null && mxId != myUserId) {
            aggregator?.directChatsToCheck?.put(roomId, mxId)
        }
    }

    private fun handleIncrementalSync(
            realm: Realm,
            roomId: String,
            eventUserId: String,
            roomMember: RoomMemberContent,
            prevContent: Content?,
            aggregator: SyncResponsePostTreatmentAggregator?
    ): Boolean {
        if (aggregator != null) {
            val previousDisplayName = prevContent?.get("displayname") as? String
            val previousAvatar = prevContent?.get("avatar_url") as? String

            if ((previousDisplayName != null && previousDisplayName != roomMember.displayName) ||
                    (previousAvatar != null && previousAvatar != roomMember.avatarUrl)) {
                aggregator.userIdsToFetch.add(eventUserId)
            }
        }

        saveRoomMemberEntityLocally(realm, roomId, eventUserId, roomMember)
        // At the end of the sync, fetch all the profiles from the aggregator
        updateDirectChatsIfNecessary(roomId, roomMember, aggregator)
        return true
    }
}
