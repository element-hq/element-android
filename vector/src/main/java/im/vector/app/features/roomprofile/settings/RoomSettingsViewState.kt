/*
 * Copyright 2020 New Vector Ltd
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

package im.vector.app.features.roomprofile.settings

import android.net.Uri
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.Uninitialized
import im.vector.app.features.roomprofile.RoomProfileArgs
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.room.model.RoomHistoryVisibility
import org.matrix.android.sdk.api.session.room.model.RoomSummary

data class RoomSettingsViewState(
        val roomId: String,
        val historyVisibilityEvent: Event? = null,
        val roomSummary: Async<RoomSummary> = Uninitialized,
        val isLoading: Boolean = false,
        val currentRoomAvatarUrl: String? = null,
        val avatarAction: AvatarAction = AvatarAction.None,
        val newName: String? = null,
        val newTopic: String? = null,
        val newHistoryVisibility: RoomHistoryVisibility? = null,
        val newCanonicalAlias: String? = null,
        val showSaveAction: Boolean = false,
        val actionPermissions: ActionPermissions = ActionPermissions()
) : MvRxState {

    constructor(args: RoomProfileArgs) : this(roomId = args.roomId)

    data class ActionPermissions(
            val canChangeAvatar: Boolean = false,
            val canChangeName: Boolean = false,
            val canChangeTopic: Boolean = false,
            val canChangeCanonicalAlias: Boolean = false,
            val canChangeHistoryReadability: Boolean = false,
            val canEnableEncryption: Boolean = false
    )

    sealed class AvatarAction {
        object None : AvatarAction()
        object DeleteAvatar : AvatarAction()
        data class UpdateAvatar(val newAvatarUri: Uri,
                                val newAvatarFileName: String) : AvatarAction()
    }
}
