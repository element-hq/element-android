/*
 * Copyright 2019 New Vector Ltd
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

package org.matrix.android.sdk.internal.session.room

import androidx.lifecycle.LiveData
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.session.crypto.CryptoService
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.call.RoomCallService
import org.matrix.android.sdk.api.session.room.members.MembershipService
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.model.relation.RelationService
import org.matrix.android.sdk.api.session.room.notification.RoomPushRuleService
import org.matrix.android.sdk.api.session.room.read.ReadService
import org.matrix.android.sdk.api.session.room.reporting.ReportingService
import org.matrix.android.sdk.api.session.room.send.DraftService
import org.matrix.android.sdk.api.session.room.send.SendService
import org.matrix.android.sdk.api.session.room.state.StateService
import org.matrix.android.sdk.api.session.room.tags.TagsService
import org.matrix.android.sdk.api.session.room.timeline.TimelineService
import org.matrix.android.sdk.api.session.room.typing.TypingService
import org.matrix.android.sdk.api.session.room.uploads.UploadsService
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.internal.crypto.MXCRYPTO_ALGORITHM_MEGOLM
import org.matrix.android.sdk.internal.session.room.state.SendStateTask
import org.matrix.android.sdk.internal.session.room.summary.RoomSummaryDataSource
import org.matrix.android.sdk.internal.task.TaskExecutor
import org.matrix.android.sdk.internal.task.configureWith
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
