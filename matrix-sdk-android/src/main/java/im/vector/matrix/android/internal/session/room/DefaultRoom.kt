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
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.crypto.CryptoService
import im.vector.matrix.android.api.session.room.Room
import im.vector.matrix.android.api.session.room.members.MembershipService
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.matrix.android.api.session.room.model.relation.RelationService
import im.vector.matrix.android.api.session.room.notification.RoomPushRuleService
import im.vector.matrix.android.api.session.room.read.ReadService
import im.vector.matrix.android.api.session.room.reporting.ReportingService
import im.vector.matrix.android.api.session.room.send.DraftService
import im.vector.matrix.android.api.session.room.send.SendService
import im.vector.matrix.android.api.session.room.state.StateService
import im.vector.matrix.android.api.session.room.timeline.TimelineService
import im.vector.matrix.android.api.session.room.typing.TypingService
import im.vector.matrix.android.api.util.Optional
import im.vector.matrix.android.api.util.toOptional
import im.vector.matrix.android.internal.database.mapper.RoomSummaryMapper
import im.vector.matrix.android.internal.database.model.RoomSummaryEntity
import im.vector.matrix.android.internal.database.model.RoomSummaryEntityFields
import im.vector.matrix.android.internal.database.query.where
import javax.inject.Inject

internal class DefaultRoom @Inject constructor(override val roomId: String,
                                               private val monarchy: Monarchy,
                                               private val roomSummaryMapper: RoomSummaryMapper,
                                               private val timelineService: TimelineService,
                                               private val sendService: SendService,
                                               private val draftService: DraftService,
                                               private val stateService: StateService,
                                               private val reportingService: ReportingService,
                                               private val readService: ReadService,
                                               private val typingService: TypingService,
                                               private val cryptoService: CryptoService,
                                               private val relationService: RelationService,
                                               private val roomMembersService: MembershipService,
                                               private val roomPushRuleService: RoomPushRuleService) :
        Room,
        TimelineService by timelineService,
        SendService by sendService,
        DraftService by draftService,
        StateService by stateService,
        ReportingService by reportingService,
        ReadService by readService,
        TypingService by typingService,
        RelationService by relationService,
        MembershipService by roomMembersService,
        RoomPushRuleService by roomPushRuleService {

    override fun getRoomSummaryLive(): LiveData<Optional<RoomSummary>> {
        val liveData = monarchy.findAllMappedWithChanges(
                { realm -> RoomSummaryEntity.where(realm, roomId).isNotEmpty(RoomSummaryEntityFields.DISPLAY_NAME) },
                { roomSummaryMapper.map(it) }
        )
        return Transformations.map(liveData) { results ->
            results.firstOrNull().toOptional()
        }
    }

    override fun roomSummary(): RoomSummary? {
        return monarchy.fetchAllMappedSync(
                { realm -> RoomSummaryEntity.where(realm, roomId).isNotEmpty(RoomSummaryEntityFields.DISPLAY_NAME) },
                { roomSummaryMapper.map(it) }
        ).firstOrNull()
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

    override fun enableEncryptionWithAlgorithm(algorithm: String, callback: MatrixCallback<Unit>) {
        if (isEncrypted()) {
            callback.onFailure(IllegalStateException("Encryption is already enabled for this room"))
        } else {
            stateService.enableEncryption(algorithm, callback)
        }
    }
}
