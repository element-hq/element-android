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

package org.matrix.android.sdk.api.session.room

import androidx.lifecycle.LiveData
import org.matrix.android.sdk.api.session.room.call.RoomCallService
import org.matrix.android.sdk.api.session.room.crypto.RoomCryptoService
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

/**
 * This interface defines methods to interact within a room.
 */
interface Room :
        TimelineService,
        SendService,
        DraftService,
        ReadService,
        TypingService,
        TagsService,
        MembershipService,
        StateService,
        UploadsService,
        ReportingService,
        RoomCallService,
        RelationService,
        RoomCryptoService,
        RoomPushRuleService {

    /**
     * The roomId of this room
     */
    val roomId: String

    /**
     * A live [RoomSummary] associated with the room
     * You can observe this summary to get dynamic data from this room.
     */
    fun getRoomSummaryLive(): LiveData<Optional<RoomSummary>>

    /**
     * A current snapshot of [RoomSummary] associated with the room
     */
    fun roomSummary(): RoomSummary?
}
