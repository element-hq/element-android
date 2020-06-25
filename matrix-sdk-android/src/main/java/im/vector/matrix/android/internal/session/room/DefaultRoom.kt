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
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.crypto.CryptoService
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.room.Room
import im.vector.matrix.android.api.session.room.call.RoomCallService
import im.vector.matrix.android.api.session.room.members.MembershipService
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.matrix.android.api.session.room.model.relation.RelationService
import im.vector.matrix.android.api.session.room.notification.RoomPushRuleService
import im.vector.matrix.android.api.session.room.read.ReadService
import im.vector.matrix.android.api.session.room.reporting.ReportingService
import im.vector.matrix.android.api.session.room.send.DraftService
import im.vector.matrix.android.api.session.room.send.SendService
import im.vector.matrix.android.api.session.room.state.StateService
import im.vector.matrix.android.api.session.room.tags.TagsService
import im.vector.matrix.android.api.session.room.timeline.TimelineService
import im.vector.matrix.android.api.session.room.typing.TypingService
import im.vector.matrix.android.api.session.room.uploads.UploadsService
import im.vector.matrix.android.api.util.Optional
import im.vector.matrix.android.internal.crypto.MXCRYPTO_ALGORITHM_MEGOLM
import im.vector.matrix.android.internal.session.room.state.SendStateTask
import im.vector.matrix.android.internal.session.room.summary.RoomSummaryDataSource
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.configureWith
import java.security.InvalidParameterException
import javax.inject.Inject

internal class DefaultRoom @Inject constructor(override val roomId: String,
                                               private val roomSummaryDataSource: RoomSummaryDataSource,
                                               private val timelineService: TimelineService,
                                               private val sendService: SendService,
                                               private val draftService: DraftService,
                                               private val stateService: StateService,
                                               private val uploadsService: UploadsService,
                                               private val reportingService: ReportingService,
                                               private val roomCallService: RoomCallService,
                                               private val readService: ReadService,
                                               private val typingService: TypingService,
                                               private val tagsService: TagsService,
                                               private val cryptoService: CryptoService,
                                               private val relationService: RelationService,
                                               private val roomMembersService: MembershipService,
                                               private val roomPushRuleService: RoomPushRuleService,
                                               private val taskExecutor: TaskExecutor,
                                               private val sendStateTask: SendStateTask) :
        Room,
        TimelineService by timelineService,
        SendService by sendService,
        DraftService by draftService,
        StateService by stateService,
        UploadsService by uploadsService,
        ReportingService by reportingService,
        RoomCallService by roomCallService,
        ReadService by readService,
        TypingService by typingService,
        TagsService by tagsService,
        RelationService by relationService,
        MembershipService by roomMembersService,
        RoomPushRuleService by roomPushRuleService {

    override fun getRoomSummaryLive(): LiveData<Optional<RoomSummary>> {
        return roomSummaryDataSource.getRoomSummaryLive(roomId)
    }

    override fun roomSummary(): RoomSummary? {
        return roomSummaryDataSource.getRoomSummary(roomId)
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

    override fun enableEncryption(algorithm: String, callback: MatrixCallback<Unit>) {
        when {
            isEncrypted()                          -> {
                callback.onFailure(IllegalStateException("Encryption is already enabled for this room"))
            }
            algorithm != MXCRYPTO_ALGORITHM_MEGOLM -> {
                callback.onFailure(InvalidParameterException("Only MXCRYPTO_ALGORITHM_MEGOLM algorithm is supported"))
            }
            else                                   -> {
                val params = SendStateTask.Params(
                        roomId = roomId,
                        stateKey = null,
                        eventType = EventType.STATE_ROOM_ENCRYPTION,
                        body = mapOf(
                                "algorithm" to algorithm
                        ))

                sendStateTask
                        .configureWith(params) {
                            this.callback = callback
                        }
                        .executeBy(taskExecutor)
            }
        }
    }
}
