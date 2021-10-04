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
 *
 */

package im.vector.app.features.roomprofile

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.Uninitialized
import org.matrix.android.sdk.api.session.room.model.RoomMemberSummary
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.model.create.RoomCreateContent

data class RoomProfileViewState(
        val roomId: String,
        val roomSummary: Async<RoomSummary> = Uninitialized,
        val roomCreateContent: Async<RoomCreateContent> = Uninitialized,
        val bannedMembership: Async<List<RoomMemberSummary>> = Uninitialized,
        val actionPermissions: ActionPermissions = ActionPermissions(),
        val isLoading: Boolean = false,
        val isUsingUnstableRoomVersion: Boolean = false,
        val recommendedRoomVersion: String? = null,
        val canUpgradeRoom: Boolean = false,
        val isTombstoned: Boolean = false
) : MvRxState {

    constructor(args: RoomProfileArgs) : this(roomId = args.roomId)

    data class ActionPermissions(
            val canEnableEncryption: Boolean = false
    )
}
