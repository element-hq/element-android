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
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.Uninitialized
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.initsync.SyncStatusService
import org.matrix.android.sdk.api.session.room.members.ChangeMembershipState
import org.matrix.android.sdk.api.session.room.model.RoomMemberSummary
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.sync.SyncState
import org.matrix.android.sdk.api.session.widgets.model.Widget
import org.matrix.android.sdk.api.session.widgets.model.WidgetType

sealed class UnreadState {
    object Unknown : UnreadState()
    object HasNoUnread : UnreadState()
    data class ReadMarkerNotLoaded(val readMarkerId: String) : UnreadState()
    data class HasUnread(val firstUnreadEventId: String) : UnreadState()
}

data class JitsiState(
        val hasJoined: Boolean = false,
        // Not null if we have an active jitsi widget on the room
        val confId: String? = null,
        val widgetId: String? = null,
        val deleteWidgetInProgress: Boolean = false
)

data class RoomDetailViewState(
        val roomId: String,
        val eventId: String?,
        val myRoomMember: Async<RoomMemberSummary> = Uninitialized,
        val asyncInviter: Async<RoomMemberSummary> = Uninitialized,
        val asyncRoomSummary: Async<RoomSummary> = Uninitialized,
        val activeRoomWidgets: Async<List<Widget>> = Uninitialized,
        val formattedTypingUsers: String? = null,
        val tombstoneEvent: Event? = null,
        val joinUpgradedRoomAsync: Async<String> = Uninitialized,
        val syncState: SyncState = SyncState.Idle,
        val incrementalSyncStatus: SyncStatusService.Status.IncrementalSyncStatus = SyncStatusService.Status.IncrementalSyncIdle,
        val pushCounter: Int = 0,
        val highlightedEventId: String? = null,
        val unreadState: UnreadState = UnreadState.Unknown,
        val canShowJumpToReadMarker: Boolean = true,
        val changeMembershipState: ChangeMembershipState = ChangeMembershipState.Unknown,
        val canInvite: Boolean = true,
        val isAllowedToManageWidgets: Boolean = false,
        val isAllowedToStartWebRTCCall: Boolean = true,
        val isAllowedToSetupEncryption: Boolean = true,
        val hasFailedSending: Boolean = false,
        val jitsiState: JitsiState = JitsiState(),
        val switchToParentSpace: Boolean = false
) : MavericksState {

    constructor(args: RoomDetailArgs) : this(
            roomId = args.roomId,
            eventId = args.eventId,
            // Also highlight the target event, if any
            highlightedEventId = args.eventId,
            switchToParentSpace = args.switchToParentSpace
    )

    fun isWebRTCCallOptionAvailable() = (asyncRoomSummary.invoke()?.joinedMembersCount ?: 0) <= 2

    // This checks directly on the active room widgets.
    // It can differs for a short period of time on the JitsiState as its computed async.
    fun hasActiveJitsiWidget() = activeRoomWidgets()?.any { it.type == WidgetType.Jitsi && it.isActive }.orFalse()

    fun isDm() = asyncRoomSummary()?.isDirect == true
}
