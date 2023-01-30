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

package org.matrix.android.sdk.internal.session.room

import androidx.lifecycle.LiveData
import org.matrix.android.sdk.api.MatrixCoroutineDispatchers
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.accountdata.RoomAccountDataService
import org.matrix.android.sdk.api.session.room.alias.AliasService
import org.matrix.android.sdk.api.session.room.call.RoomCallService
import org.matrix.android.sdk.api.session.room.crypto.RoomCryptoService
import org.matrix.android.sdk.api.session.room.location.LocationSharingService
import org.matrix.android.sdk.api.session.room.members.MembershipService
import org.matrix.android.sdk.api.session.room.model.LocalRoomSummary
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.model.RoomType
import org.matrix.android.sdk.api.session.room.model.relation.RelationService
import org.matrix.android.sdk.api.session.room.notification.RoomPushRuleService
import org.matrix.android.sdk.api.session.room.poll.PollHistoryService
import org.matrix.android.sdk.api.session.room.read.ReadService
import org.matrix.android.sdk.api.session.room.reporting.ReportingService
import org.matrix.android.sdk.api.session.room.send.DraftService
import org.matrix.android.sdk.api.session.room.send.SendService
import org.matrix.android.sdk.api.session.room.state.StateService
import org.matrix.android.sdk.api.session.room.tags.TagsService
import org.matrix.android.sdk.api.session.room.threads.ThreadsService
import org.matrix.android.sdk.api.session.room.threads.local.ThreadsLocalService
import org.matrix.android.sdk.api.session.room.timeline.TimelineService
import org.matrix.android.sdk.api.session.room.typing.TypingService
import org.matrix.android.sdk.api.session.room.uploads.UploadsService
import org.matrix.android.sdk.api.session.room.version.RoomVersionService
import org.matrix.android.sdk.api.session.space.Space
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.internal.session.permalinks.ViaParameterFinder
import org.matrix.android.sdk.internal.session.room.summary.RoomSummaryDataSource
import org.matrix.android.sdk.internal.session.space.DefaultSpace

internal class DefaultRoom(
        override val roomId: String,
        private val roomSummaryDataSource: RoomSummaryDataSource,
        private val roomCryptoService: RoomCryptoService,
        private val timelineService: TimelineService,
        private val threadsService: ThreadsService,
        private val threadsLocalService: ThreadsLocalService,
        private val sendService: SendService,
        private val draftService: DraftService,
        private val stateService: StateService,
        private val uploadsService: UploadsService,
        private val reportingService: ReportingService,
        private val roomCallService: RoomCallService,
        private val readService: ReadService,
        private val typingService: TypingService,
        private val aliasService: AliasService,
        private val tagsService: TagsService,
        private val relationService: RelationService,
        private val roomMembersService: MembershipService,
        private val roomPushRuleService: RoomPushRuleService,
        private val roomAccountDataService: RoomAccountDataService,
        private val roomVersionService: RoomVersionService,
        private val viaParameterFinder: ViaParameterFinder,
        private val locationSharingService: LocationSharingService,
        private val pollHistoryService: PollHistoryService,
        override val coroutineDispatchers: MatrixCoroutineDispatchers
) : Room {

    override fun getRoomSummaryLive(): LiveData<Optional<RoomSummary>> {
        return roomSummaryDataSource.getRoomSummaryLive(roomId)
    }

    override fun roomSummary(): RoomSummary? {
        return roomSummaryDataSource.getRoomSummary(roomId)
    }

    override fun getLocalRoomSummaryLive(): LiveData<Optional<LocalRoomSummary>> {
        return roomSummaryDataSource.getLocalRoomSummaryLive(roomId)
    }

    override fun localRoomSummary(): LocalRoomSummary? {
        return roomSummaryDataSource.getLocalRoomSummary(roomId)
    }

    override fun asSpace(): Space? {
        if (roomSummary()?.roomType != RoomType.SPACE) return null
        return DefaultSpace(this, roomSummaryDataSource, viaParameterFinder)
    }

    override fun timelineService() = timelineService
    override fun threadsService() = threadsService
    override fun threadsLocalService() = threadsLocalService
    override fun sendService() = sendService
    override fun draftService() = draftService
    override fun stateService() = stateService
    override fun uploadsService() = uploadsService
    override fun reportingService() = reportingService
    override fun roomCallService() = roomCallService
    override fun readService() = readService
    override fun typingService() = typingService
    override fun aliasService() = aliasService
    override fun tagsService() = tagsService
    override fun relationService() = relationService
    override fun roomCryptoService() = roomCryptoService
    override fun membershipService() = roomMembersService
    override fun roomPushRuleService() = roomPushRuleService
    override fun roomAccountDataService() = roomAccountDataService
    override fun roomVersionService() = roomVersionService
    override fun locationSharingService() = locationSharingService
    override fun pollHistoryService() = pollHistoryService
}
