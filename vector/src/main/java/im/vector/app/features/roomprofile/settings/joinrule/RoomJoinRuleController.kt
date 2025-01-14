/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.settings.joinrule

import im.vector.app.core.resources.DrawableProvider
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.ui.bottomsheet.BottomSheetGenericController
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.session.room.model.RoomJoinRules
import javax.inject.Inject

class RoomJoinRuleController @Inject constructor(
        private val stringProvider: StringProvider,
        private val drawableProvider: DrawableProvider
) : BottomSheetGenericController<RoomJoinRuleState, RoomJoinRuleRadioAction>() {

    override fun getTitle() =
            stringProvider.getString(
                    // generic title for both room and space
                    CommonStrings.room_settings_access_rules_pref_dialog_title
            )

    override fun getActions(state: RoomJoinRuleState): List<RoomJoinRuleRadioAction> {
        return listOf(
                RoomJoinRuleRadioAction(
                        roomJoinRule = RoomJoinRules.INVITE,
                        description = stringProvider.getString(CommonStrings.room_settings_room_access_private_description),
                        title = stringProvider.getString(CommonStrings.room_settings_room_access_private_title),
                        isSelected = state.currentRoomJoinRule == RoomJoinRules.INVITE
                ),
                RoomJoinRuleRadioAction(
                        roomJoinRule = RoomJoinRules.PUBLIC,
                        description = stringProvider.getString(
                                if (state.isSpace) CommonStrings.room_settings_space_access_public_description
                                else CommonStrings.room_settings_room_access_public_description
                        ),
                        title = stringProvider.getString(CommonStrings.room_settings_room_access_public_title),
                        isSelected = state.currentRoomJoinRule == RoomJoinRules.PUBLIC
                ),
                RoomJoinRuleRadioAction(
                        roomJoinRule = RoomJoinRules.RESTRICTED,
                        description = if (state.parentSpaceName != null) {
                            stringProvider.getString(CommonStrings.room_create_member_of_space_name_can_join, state.parentSpaceName)
                        } else {
                            stringProvider.getString(CommonStrings.room_settings_room_access_restricted_description)
                        },
                        title = stringProvider.getString(CommonStrings.room_settings_room_access_restricted_title),
                        isSelected = state.currentRoomJoinRule == RoomJoinRules.RESTRICTED
                )
        ).filter { state.allowedJoinedRules.map { it.rule }.contains(it.roomJoinRule) }
    }
}
