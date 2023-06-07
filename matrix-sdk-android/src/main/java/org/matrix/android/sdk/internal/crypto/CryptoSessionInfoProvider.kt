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

package org.matrix.android.sdk.internal.crypto

import com.zhuinden.monarchy.Monarchy
import org.matrix.android.sdk.api.session.crypto.model.RoomEncryptionTrustLevel
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.internal.database.mapper.EventMapper
import org.matrix.android.sdk.internal.database.model.EventEntity
import org.matrix.android.sdk.internal.database.model.EventEntityFields
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntity
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntityFields
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.database.query.whereType
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.query.process
import org.matrix.android.sdk.internal.session.room.membership.RoomMemberHelper
import org.matrix.android.sdk.internal.util.fetchCopied
import timber.log.Timber
import javax.inject.Inject

/**
 * The crypto module needs some information regarding rooms that are stored
 * in the session DB, this class encapsulate this functionality.
 */
internal class CryptoSessionInfoProvider @Inject constructor(
        @SessionDatabase private val monarchy: Monarchy,
        @UserId private val myUserId: String
) {

    fun isRoomEncrypted(roomId: String): Boolean {
        // We look at the presence at any m.room.encryption state event no matter if it's
        // the latest one or if it is well formed
        val encryptionEvent = monarchy.fetchCopied { realm ->
            EventEntity.whereType(realm, roomId = roomId, type = EventType.STATE_ROOM_ENCRYPTION)
                    .isEmpty(EventEntityFields.STATE_KEY)
                    .findFirst()
        }
        return encryptionEvent != null
    }

    /**
     * @param roomId the room Id
     * @param allActive if true return joined as well as invited, if false, only joined
     */
    fun getRoomUserIds(roomId: String, allActive: Boolean): List<String> {
        var userIds: List<String> = emptyList()
        monarchy.doWithRealm { realm ->
            userIds = if (allActive) {
                RoomMemberHelper(realm, roomId).getActiveRoomMemberIds()
            } else {
                RoomMemberHelper(realm, roomId).getJoinedRoomMemberIds()
            }
        }
        return userIds
    }

    fun getUserListForShieldComputation(roomId: String): List<String> {
        var userIds: List<String> = emptyList()
        monarchy.doWithRealm { realm ->
            userIds = RoomMemberHelper(realm, roomId).getActiveRoomMemberIds()
        }
        var isDirect = false
        monarchy.doWithRealm { realm ->
            isDirect = RoomSummaryEntity.where(realm, roomId = roomId).findFirst()?.isDirect == true
        }

        return if (isDirect || userIds.size <= 2) {
            userIds.filter { it != myUserId }
        } else {
            userIds
        }
    }

    fun getRoomsWhereUsersAreParticipating(userList: List<String>): List<String> {
        if (userList.contains(myUserId)) {
            // just take all
            val roomIds: List<String>? = null
            monarchy.doWithRealm { sessionRealm ->
                RoomSummaryEntity.where(sessionRealm)
                        .process(RoomSummaryEntityFields.MEMBERSHIP_STR, Membership.activeMemberships())
                        .findAll()
                        .map { it.roomId }
            }
            return roomIds.orEmpty()
        }
        var roomIds: List<String>? = null
        monarchy.doWithRealm { sessionRealm ->
            roomIds = RoomSummaryEntity.where(sessionRealm)
                    .process(RoomSummaryEntityFields.MEMBERSHIP_STR, Membership.activeMemberships())
                    .findAll()
                    .filter { it.otherMemberIds.any { it in userList } }
                    .map { it.roomId }
//            roomIds = sessionRealm.where(RoomMemberSummaryEntity::class.java)
//                    .`in`(RoomMemberSummaryEntityFields.USER_ID, userList.toTypedArray())
//                    .distinct(RoomMemberSummaryEntityFields.ROOM_ID)
//                    .findAll()
//                    .map { it.roomId }
//                    .also { Timber.d("## CrossSigning -  ... impacted rooms ${it.logLimit()}") }
        }
        return roomIds.orEmpty()
    }

    fun markMessageVerificationStateAsDirty(userList: List<String>) {
        monarchy.writeAsync { sessionRealm ->
            sessionRealm.where(EventEntity::class.java)
                    .`in`(EventEntityFields.SENDER, userList.toTypedArray())
                    .equalTo(EventEntityFields.TYPE, EventType.ENCRYPTED)
                    .isNotNull(EventEntityFields.DECRYPTION_RESULT_JSON)
//                    // A bit annoying to have to do that like that and it could break :/
//                    .contains(EventEntityFields.DECRYPTION_RESULT_JSON, "\"verification_state\":\"UNKNOWN_DEVICE\"")
                    .findAll()
                    .onEach {
                        it.isVerificationStateDirty = true
                    }
                    .map { EventMapper.map(it) }
                    .also { Timber.v("## VerificationState refresh -  ... impacted events ${it.joinToString{ it.eventId.orEmpty() }}") }
        }
    }

    fun updateShieldForRoom(roomId: String, shield: RoomEncryptionTrustLevel?) {
        monarchy.writeAsync { realm ->
            val summary = RoomSummaryEntity.where(realm, roomId = roomId).findFirst()
            summary?.roomEncryptionTrustLevel = shield
        }
    }
}
