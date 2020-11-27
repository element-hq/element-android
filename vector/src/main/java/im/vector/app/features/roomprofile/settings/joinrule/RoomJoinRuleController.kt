/*
 * Copyright (c) 2020 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.roomprofile.settings.joinrule

import im.vector.app.R
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.ui.bottomsheet.BottomSheetGenericController
import org.matrix.android.sdk.api.session.room.model.GuestAccess
import org.matrix.android.sdk.api.session.room.model.RoomJoinRules
import javax.inject.Inject

class RoomJoinRuleController @Inject constructor(
        private val stringProvider: StringProvider
) : BottomSheetGenericController<RoomJoinRuleState, RoomJoinRuleAction>() {

    override fun getTitle() = stringProvider.getString(R.string.room_settings_room_access_rules_pref_dialog_title)

    override fun getActions(state: RoomJoinRuleState): List<RoomJoinRuleAction> {
        return listOf(
                RoomJoinRuleAction(
                        roomJoinRule = RoomJoinRules.INVITE,
                        roomGuestAccess = null,
                        title = stringProvider.getString(R.string.room_settings_room_access_entry_only_invited),
                        iconResId = 0,
                        isSelected = state.currentRoomJoinRule == RoomJoinRules.INVITE
                ),
                RoomJoinRuleAction(
                        roomJoinRule = RoomJoinRules.PUBLIC,
                        roomGuestAccess = GuestAccess.Forbidden,
                        title = stringProvider.getString(R.string.room_settings_room_access_entry_anyone_with_link_apart_guest),
                        iconResId = 0,
                        isSelected = state.currentRoomJoinRule == RoomJoinRules.PUBLIC && state.currentGuestAccess == GuestAccess.Forbidden
                ),
                RoomJoinRuleAction(
                        roomJoinRule = RoomJoinRules.PUBLIC,
                        roomGuestAccess = GuestAccess.CanJoin,
                        title = stringProvider.getString(R.string.room_settings_room_access_entry_anyone_with_link_including_guest),
                        iconResId = 0,
                        isSelected = state.currentRoomJoinRule == RoomJoinRules.PUBLIC && state.currentGuestAccess == GuestAccess.CanJoin
                )
        )
    }
}
