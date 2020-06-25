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

import im.vector.matrix.android.api.session.crypto.CryptoService
import im.vector.matrix.android.api.session.room.Room
import im.vector.matrix.android.internal.session.SessionScope
import im.vector.matrix.android.internal.session.room.call.DefaultRoomCallService
import im.vector.matrix.android.internal.session.room.draft.DefaultDraftService
import im.vector.matrix.android.internal.session.room.membership.DefaultMembershipService
import im.vector.matrix.android.internal.session.room.notification.DefaultRoomPushRuleService
import im.vector.matrix.android.internal.session.room.read.DefaultReadService
import im.vector.matrix.android.internal.session.room.relation.DefaultRelationService
import im.vector.matrix.android.internal.session.room.reporting.DefaultReportingService
import im.vector.matrix.android.internal.session.room.send.DefaultSendService
import im.vector.matrix.android.internal.session.room.state.DefaultStateService
import im.vector.matrix.android.internal.session.room.state.SendStateTask
import im.vector.matrix.android.internal.session.room.summary.RoomSummaryDataSource
import im.vector.matrix.android.internal.session.room.tags.DefaultTagsService
import im.vector.matrix.android.internal.session.room.timeline.DefaultTimelineService
import im.vector.matrix.android.internal.session.room.typing.DefaultTypingService
import im.vector.matrix.android.internal.session.room.uploads.DefaultUploadsService
import im.vector.matrix.android.internal.task.TaskExecutor
import javax.inject.Inject

internal interface RoomFactory {
    fun create(roomId: String): Room
}

@SessionScope
internal class DefaultRoomFactory @Inject constructor(private val cryptoService: CryptoService,
                                                      private val roomSummaryDataSource: RoomSummaryDataSource,
                                                      private val timelineServiceFactory: DefaultTimelineService.Factory,
                                                      private val sendServiceFactory: DefaultSendService.Factory,
                                                      private val draftServiceFactory: DefaultDraftService.Factory,
                                                      private val stateServiceFactory: DefaultStateService.Factory,
                                                      private val uploadsServiceFactory: DefaultUploadsService.Factory,
                                                      private val reportingServiceFactory: DefaultReportingService.Factory,
                                                      private val roomCallServiceFactory: DefaultRoomCallService.Factory,
                                                      private val readServiceFactory: DefaultReadService.Factory,
                                                      private val typingServiceFactory: DefaultTypingService.Factory,
                                                      private val tagsServiceFactory: DefaultTagsService.Factory,
                                                      private val relationServiceFactory: DefaultRelationService.Factory,
                                                      private val membershipServiceFactory: DefaultMembershipService.Factory,
                                                      private val roomPushRuleServiceFactory: DefaultRoomPushRuleService.Factory,
                                                      private val taskExecutor: TaskExecutor,
                                                      private val sendStateTask: SendStateTask) :
        RoomFactory {

    override fun create(roomId: String): Room {
        return DefaultRoom(
                roomId = roomId,
                roomSummaryDataSource = roomSummaryDataSource,
                timelineService = timelineServiceFactory.create(roomId),
                sendService = sendServiceFactory.create(roomId),
                draftService = draftServiceFactory.create(roomId),
                stateService = stateServiceFactory.create(roomId),
                uploadsService = uploadsServiceFactory.create(roomId),
                reportingService = reportingServiceFactory.create(roomId),
                roomCallService = roomCallServiceFactory.create(roomId),
                readService = readServiceFactory.create(roomId),
                typingService = typingServiceFactory.create(roomId),
                tagsService = tagsServiceFactory.create(roomId),
                cryptoService = cryptoService,
                relationService = relationServiceFactory.create(roomId),
                roomMembersService = membershipServiceFactory.create(roomId),
                roomPushRuleService = roomPushRuleServiceFactory.create(roomId),
                taskExecutor = taskExecutor,
                sendStateTask = sendStateTask
        )
    }
}
