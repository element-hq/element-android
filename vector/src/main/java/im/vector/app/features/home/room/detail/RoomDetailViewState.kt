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

package im.vector.app.features.home.room.detail

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.Uninitialized
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.room.members.ChangeMembershipState
import org.matrix.android.sdk.api.session.room.model.RoomMemberSummary
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.sync.SyncState
import org.matrix.android.sdk.api.session.widgets.model.Widget

/**
 * Describes the current send mode:
 * REGULAR: sends the text as a regular message
 * QUOTE: User is currently quoting a message
 * EDIT: User is currently editing an existing message
 *
 * Depending on the state the bottom toolbar will change (icons/preview/actions...)
 */
sealed class SendMode(open val text: String) {
    data class REGULAR(
            override val text: String,
            val fromSharing: Boolean,
            // This is necessary for forcing refresh on selectSubscribe
            private val ts: Long = System.currentTimeMillis()
    ) : SendMode(text)

    data class QUOTE(val timelineEvent: TimelineEvent, override val text: String) : SendMode(text)
    data class EDIT(val timelineEvent: TimelineEvent, override val text: String) : SendMode(text)
    data class REPLY(val timelineEvent: TimelineEvent, override val text: String) : SendMode(text)
}

sealed class UnreadState {
    object Unknown : UnreadState()
    object HasNoUnread : UnreadState()
    data class ReadMarkerNotLoaded(val readMarkerId: String) : UnreadState()
    data class HasUnread(val firstUnreadEventId: String) : UnreadState()
}

data class RoomDetailViewState(
        val roomId: String,
        val eventId: String?,
        val myRoomMember: Async<RoomMemberSummary> = Uninitialized,
        val asyncInviter: Async<RoomMemberSummary> = Uninitialized,
        val asyncRoomSummary: Async<RoomSummary> = Uninitialized,
        val activeRoomWidgets: Async<List<Widget>> = Uninitialized,
        val typingMessage: String? = null,
        val sendMode: SendMode = SendMode.REGULAR("", false),
        val tombstoneEvent: Event? = null,
        val joinUpgradedRoomAsync: Async<String> = Uninitialized,
        val syncState: SyncState = SyncState.Idle,
        val highlightedEventId: String? = null,
        val unreadState: UnreadState = UnreadState.Unknown,
        val canShowJumpToReadMarker: Boolean = true,
        val changeMembershipState: ChangeMembershipState = ChangeMembershipState.Unknown,
        val canSendMessage: Boolean = true,
        val canInvite: Boolean = true,
        val isAllowedToManageWidgets: Boolean = false,
        val isAllowedToStartWebRTCCall: Boolean = true,
        val hasFailedSending: Boolean = false
) : MvRxState {

    constructor(args: RoomDetailArgs) : this(
            roomId = args.roomId,
            eventId = args.eventId,
            // Also highlight the target event, if any
            highlightedEventId = args.eventId
    )

    fun isDm() = asyncRoomSummary()?.isDirect == true
}
