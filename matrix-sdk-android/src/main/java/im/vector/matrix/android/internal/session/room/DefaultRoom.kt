/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.internal.session.room

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.session.crypto.CryptoService
import im.vector.matrix.android.api.session.room.Room
import im.vector.matrix.android.api.session.room.members.MembershipService
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.matrix.android.api.session.room.model.relation.RelationService
import im.vector.matrix.android.api.session.room.read.ReadService
import im.vector.matrix.android.api.session.room.send.SendService
import im.vector.matrix.android.api.session.room.state.StateService
import im.vector.matrix.android.api.session.room.timeline.TimelineService
import im.vector.matrix.android.internal.database.RealmLiveData
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.database.model.RoomSummaryEntity
import im.vector.matrix.android.internal.database.model.RoomSummaryEntityFields
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.util.fetchCopied

internal class DefaultRoom(
        override val roomId: String,
        private val monarchy: Monarchy,
        private val timelineService: TimelineService,
        private val sendService: SendService,
        private val stateService: StateService,
        private val readService: ReadService,
        private val cryptoService: CryptoService,
        private val relationService: RelationService,
        private val roomMembersService: MembershipService
) : Room,
        TimelineService by timelineService,
        SendService by sendService,
        StateService by stateService,
        ReadService by readService,
        RelationService by relationService,
        MembershipService by roomMembersService {

    override val liveRoomSummary: LiveData<RoomSummary> by lazy {
        val liveRealmData = RealmLiveData<RoomSummaryEntity>(monarchy.realmConfiguration) { realm ->
            RoomSummaryEntity.where(realm, roomId).isNotEmpty(RoomSummaryEntityFields.DISPLAY_NAME)
        }
        Transformations.map(liveRealmData) { results ->
            val roomSummaries = results.map { it.asDomain() }

            if (roomSummaries.isEmpty()) {
                // Create a dummy RoomSummary to avoid Crash during Sign Out or clear cache
                RoomSummary(roomId)
            } else {
                roomSummaries.first()
            }
        }
    }

    override val roomSummary: RoomSummary?
        get() {
            var sum: RoomSummaryEntity? = monarchy.fetchCopied { RoomSummaryEntity.where(it, roomId).isNotEmpty(RoomSummaryEntityFields.DISPLAY_NAME).findFirst() }
            return sum?.asDomain()
        }

    override fun isEncrypted(): Boolean {
        return cryptoService.isRoomEncrypted(roomId)
    }

    override fun encryptionAlgorithm(): String? {
        return cryptoService.getEncryptionAlgorithm(roomId)
    }

    override fun shouldEncryptForInvitedMembers(): Boolean {
        return cryptoService.shouldEncryptForInvitedMembers(roomId)
    }

}