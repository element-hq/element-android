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
 *
 */

package im.vector.app.features.roommemberprofile

import im.vector.app.core.platform.VectorViewModelAction

sealed class RoomMemberProfileAction : VectorViewModelAction {
    object RetryFetchingInfo : RoomMemberProfileAction()
    object IgnoreUser : RoomMemberProfileAction()
    data class BanOrUnbanUser(val reason: String?) : RoomMemberProfileAction()
    data class KickUser(val reason: String?) : RoomMemberProfileAction()
    object InviteUser : RoomMemberProfileAction()
    object VerifyUser : RoomMemberProfileAction()
    object ShareRoomMemberProfile : RoomMemberProfileAction()
    data class SetPowerLevel(val previousValue: Int, val newValue: Int, val askForValidation: Boolean) : RoomMemberProfileAction()
    data class SetUserColorOverride(val newColorSpec: String) : RoomMemberProfileAction()
    data class OpenOrCreateDm(val userId: String) : RoomMemberProfileAction()
}
