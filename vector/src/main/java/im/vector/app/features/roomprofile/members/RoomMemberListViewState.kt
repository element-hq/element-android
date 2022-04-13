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

package im.vector.app.features.roomprofile.members

import androidx.annotation.StringRes
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.Uninitialized
import im.vector.app.R
import im.vector.app.core.platform.GenericIdArgs
import im.vector.app.features.roomprofile.RoomProfileArgs
import org.matrix.android.sdk.api.session.crypto.model.RoomEncryptionTrustLevel
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.room.model.RoomMemberSummary
import org.matrix.android.sdk.api.session.room.model.RoomSummary

data class RoomMemberListViewState(
        val roomId: String,
        val roomSummary: Async<RoomSummary> = Uninitialized,
        val roomMemberSummaries: Async<RoomMemberSummaries> = Uninitialized,
        val filter: String = "",
        val threePidInvites: Async<List<Event>> = Uninitialized,
        val trustLevelMap: Async<Map<String, RoomEncryptionTrustLevel?>> = Uninitialized,
        val actionsPermissions: ActionPermissions = ActionPermissions()
) : MavericksState {

    constructor(args: RoomProfileArgs) : this(roomId = args.roomId)

    constructor(args: GenericIdArgs) : this(roomId = args.id)
}

data class ActionPermissions(
        val canInvite: Boolean = false,
        val canRevokeThreePidInvite: Boolean = false
)

typealias RoomMemberSummaries = List<Pair<RoomMemberListCategories, List<RoomMemberSummary>>>

enum class RoomMemberListCategories(@StringRes val titleRes: Int) {
    ADMIN(R.string.room_member_power_level_admins),
    MODERATOR(R.string.room_member_power_level_moderators),
    CUSTOM(R.string.room_member_power_level_custom),
    INVITE(R.string.room_member_power_level_invites),
    USER(R.string.room_member_power_level_users)
}
