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

import com.airbnb.epoxy.TypedEpoxyController
import org.matrix.android.sdk.api.crypto.RoomEncryptionTrustLevel
import im.vector.app.R
import im.vector.app.core.epoxy.profiles.buildProfileAction
import im.vector.app.core.epoxy.profiles.buildProfileSection
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.ui.list.genericFooterItem
import im.vector.app.features.settings.VectorPreferences
import javax.inject.Inject

class RoomProfileController @Inject constructor(
        private val stringProvider: StringProvider,
        private val vectorPreferences: VectorPreferences,
        colorProvider: ColorProvider
) : TypedEpoxyController<RoomProfileViewState>() {

    private val dividerColor = colorProvider.getColorFromAttribute(R.attr.vctr_list_divider_color)

    var callback: Callback? = null

    interface Callback {
        fun onLearnMoreClicked()
        fun onMemberListClicked()
        fun onBannedMemberListClicked()
        fun onNotificationsClicked()
        fun onUploadsClicked()
        fun onSettingsClicked()
        fun onLeaveRoomClicked()
        fun onRoomIdClicked()
    }

    override fun buildModels(data: RoomProfileViewState?) {
        if (data == null) {
            return
        }
        val roomSummary = data.roomSummary() ?: return
        // Security
        buildProfileSection(stringProvider.getString(R.string.room_profile_section_security))
        val learnMoreSubtitle = if (roomSummary.isEncrypted) {
            R.string.room_profile_encrypted_subtitle
        } else {
            R.string.room_profile_not_encrypted_subtitle
        }
        genericFooterItem {
            id("e2e info")
            centered(false)
            text(stringProvider.getString(learnMoreSubtitle))
        }

        // More
        buildProfileSection(stringProvider.getString(R.string.room_profile_section_more))
        buildProfileAction(
                id = "settings",
                title = stringProvider.getString(R.string.room_profile_section_more_settings),
                dividerColor = dividerColor,
                icon = R.drawable.ic_room_profile_settings,
                action = { callback?.onSettingsClicked() }
        )
        buildProfileAction(
                id = "notifications",
                title = stringProvider.getString(R.string.room_profile_section_more_notifications),
                dividerColor = dividerColor,
                icon = R.drawable.ic_room_profile_notification,
                action = { callback?.onNotificationsClicked() }
        )
        val numberOfMembers = roomSummary.joinedMembersCount ?: 0
        val hasWarning = roomSummary.isEncrypted && roomSummary.roomEncryptionTrustLevel == RoomEncryptionTrustLevel.Warning
        buildProfileAction(
                id = "member_list",
                title = stringProvider.getQuantityString(R.plurals.room_profile_section_more_member_list, numberOfMembers, numberOfMembers),
                dividerColor = dividerColor,
                icon = R.drawable.ic_room_profile_member_list,
                accessory = R.drawable.ic_shield_warning.takeIf { hasWarning } ?: 0,
                action = { callback?.onMemberListClicked() }
        )

        if (data.bannedMembership.invoke()?.isNotEmpty() == true) {
            buildProfileAction(
                    id = "banned_list",
                    title = stringProvider.getString(R.string.room_settings_banned_users_title),
                    dividerColor = dividerColor,
                    icon = R.drawable.ic_settings_root_labs,
                    action = { callback?.onBannedMemberListClicked() }
            )
        }
        buildProfileAction(
                id = "uploads",
                title = stringProvider.getString(R.string.room_profile_section_more_uploads),
                dividerColor = dividerColor,
                icon = R.drawable.ic_room_profile_uploads,
                action = { callback?.onUploadsClicked() }
        )
        buildProfileAction(
                id = "leave",
                title = stringProvider.getString(R.string.room_profile_section_more_leave),
                dividerColor = dividerColor,
                divider = false,
                destructive = true,
                editable = false,
                action = { callback?.onLeaveRoomClicked() }
        )

        // Advanced
        if (vectorPreferences.developerMode()) {
            buildProfileSection(stringProvider.getString(R.string.room_settings_category_advanced_title))
            buildProfileAction(
                    id = "roomId",
                    title = stringProvider.getString(R.string.room_settings_room_internal_id),
                    subtitle = roomSummary.roomId,
                    dividerColor = dividerColor,
                    divider = false,
                    editable = false,
                    action = { callback?.onRoomIdClicked() }
            )
        }
    }
}
