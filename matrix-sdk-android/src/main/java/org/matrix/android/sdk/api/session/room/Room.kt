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

package org.matrix.android.sdk.api.session.room

import androidx.lifecycle.LiveData
import org.matrix.android.sdk.api.MatrixCoroutineDispatchers
import org.matrix.android.sdk.api.session.room.accountdata.RoomAccountDataService
import org.matrix.android.sdk.api.session.room.alias.AliasService
import org.matrix.android.sdk.api.session.room.call.RoomCallService
import org.matrix.android.sdk.api.session.room.crypto.RoomCryptoService
import org.matrix.android.sdk.api.session.room.location.LocationSharingService
import org.matrix.android.sdk.api.session.room.members.MembershipService
import org.matrix.android.sdk.api.session.room.model.LocalRoomSummary
import org.matrix.android.sdk.api.session.room.model.RoomSummary
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

/**
 * This interface defines methods to interact within a room.
 */
interface Room {

    val coroutineDispatchers: MatrixCoroutineDispatchers

    /**
     * The roomId of this room.
     */
    val roomId: String

    /**
     * A live [RoomSummary] associated with the room.
     * You can observe this summary to get dynamic data from this room.
     */
    fun getRoomSummaryLive(): LiveData<Optional<RoomSummary>>

    /**
     * A live [LocalRoomSummary] associated with the room.
     * You can observe this summary to get dynamic data from this room.
     */
    fun getLocalRoomSummaryLive(): LiveData<Optional<LocalRoomSummary>>

    /**
     * A current snapshot of [RoomSummary] associated with the room.
     */
    fun roomSummary(): RoomSummary?

    /**
     * A current snapshot of [LocalRoomSummary] associated with the room.
     */
    fun localRoomSummary(): LocalRoomSummary?

    /**
     * Use this room as a Space, if the type is correct.
     */
    fun asSpace(): Space?

    /**
     * Get the TimelineService associated to this Room.
     */
    fun timelineService(): TimelineService

    /**
     * Get the ThreadsService associated to this Room.
     */
    fun threadsService(): ThreadsService

    /**
     * Get the ThreadsLocalService associated to this Room.
     */
    fun threadsLocalService(): ThreadsLocalService

    /**
     * Get the SendService associated to this Room.
     */
    fun sendService(): SendService

    /**
     * Get the DraftService associated to this Room.
     */
    fun draftService(): DraftService

    /**
     * Get the ReadService associated to this Room.
     */
    fun readService(): ReadService

    /**
     * Get the TypingService associated to this Room.
     */
    fun typingService(): TypingService

    /**
     * Get the AliasService associated to this Room.
     */
    fun aliasService(): AliasService

    /**
     * Get the TagsService associated to this Room.
     */
    fun tagsService(): TagsService

    /**
     * Get the MembershipService associated to this Room.
     */
    fun membershipService(): MembershipService

    /**
     * Get the StateService associated to this Room.
     */
    fun stateService(): StateService

    /**
     * Get the UploadsService associated to this Room.
     */
    fun uploadsService(): UploadsService

    /**
     * Get the ReportingService associated to this Room.
     */
    fun reportingService(): ReportingService

    /**
     * Get the RoomCallService associated to this Room.
     */
    fun roomCallService(): RoomCallService

    /**
     * Get the RelationService associated to this Room.
     */
    fun relationService(): RelationService

    /**
     * Get the RoomCryptoService associated to this Room.
     */
    fun roomCryptoService(): RoomCryptoService

    /**
     * Get the RoomPushRuleService associated to this Room.
     */
    fun roomPushRuleService(): RoomPushRuleService

    /**
     * Get the RoomAccountDataService associated to this Room.
     */
    fun roomAccountDataService(): RoomAccountDataService

    /**
     * Get the RoomVersionService associated to this Room.
     */
    fun roomVersionService(): RoomVersionService

    /**
     * Get the LocationSharingService associated to this Room.
     */
    fun locationSharingService(): LocationSharingService

    /**
     * Get the PollHistoryService associated to this Room.
     */
    fun pollHistoryService(): PollHistoryService
}
