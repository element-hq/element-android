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

import im.vector.app.core.platform.VectorViewModelAction
import org.matrix.android.sdk.api.session.room.model.GuestAccess
import org.matrix.android.sdk.api.session.room.model.RoomHistoryVisibility
import org.matrix.android.sdk.api.session.room.model.RoomJoinRules

sealed class RoomSettingsAction : VectorViewModelAction {
    data class SetAvatarAction(val avatarAction: RoomSettingsViewState.AvatarAction) : RoomSettingsAction()
    data class SetRoomName(val newName: String) : RoomSettingsAction()
    data class SetRoomTopic(val newTopic: String) : RoomSettingsAction()
    data class SetRoomHistoryVisibility(val visibility: RoomHistoryVisibility) : RoomSettingsAction()
    data class SetRoomJoinRule(val roomJoinRule: RoomJoinRules) : RoomSettingsAction()
    data class SetRoomGuestAccess(val guestAccess: GuestAccess) : RoomSettingsAction()

    object Save : RoomSettingsAction()
    object Cancel : RoomSettingsAction()
}
