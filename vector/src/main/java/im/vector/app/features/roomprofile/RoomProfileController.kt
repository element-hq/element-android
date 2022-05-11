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
import im.vector.app.R
import im.vector.app.core.epoxy.expandableTextItem
import im.vector.app.core.epoxy.profiles.buildProfileAction
import im.vector.app.core.epoxy.profiles.buildProfileSection
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.resources.DrawableProvider
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.ui.list.genericFooterItem
import im.vector.app.core.ui.list.genericPositiveButtonItem
import im.vector.app.features.home.ShortcutCreator
import im.vector.app.features.home.room.detail.timeline.TimelineEventController
import im.vector.app.features.home.room.detail.timeline.tools.createLinkMovementMethod
import im.vector.app.features.settings.VectorPreferences
import im.vector.lib.core.utils.epoxy.charsequence.toEpoxyCharSequence
import me.gujun.android.span.image
import me.gujun.android.span.span
import org.matrix.android.sdk.api.session.crypto.model.RoomEncryptionTrustLevel
import org.matrix.android.sdk.api.session.room.model.RoomEncryptionAlgorithm
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import javax.inject.Inject

class RoomProfileController @Inject constructor(
        private val stringProvider: StringProvider,
        private val colorProvider: ColorProvider,
        private val vectorPreferences: VectorPreferences,
        private val drawableProvider: DrawableProvider,
        private val shortcutCreator: ShortcutCreator
) : TypedEpoxyController<RoomProfileViewState>() {

    var callback: Callback? = null

    interface Callback {
        fun onLearnMoreClicked()
        fun onEnableEncryptionClicked()
        fun onMemberListClicked()
        fun onBannedMemberListClicked()
        fun onNotificationsClicked()
        fun onUploadsClicked()
        fun createShortcut()
        fun onSettingsClicked()
        fun onLeaveRoomClicked()
        fun onRoomAliasesClicked()
        fun onRoomPermissionsClicked()
        fun onRoomIdClicked()
        fun onRoomDevToolsClicked()
        fun onUrlInTopicLongClicked(url: String)
        fun doMigrateToVersion(newVersion: String)
        fun restoreEncryptionState()
    }

    override fun buildModels(data: RoomProfileViewState?) {
        data ?: return
        val host = this
        val roomSummary = data.roomSummary() ?: return

        // Topic
        roomSummary
                .topic
                .takeIf { it.isNotEmpty() }
                ?.let {
                    buildProfileSection(stringProvider.getString(R.string.room_settings_topic))
                    expandableTextItem {
                        id("topic")
                        content(it)
                        maxLines(2)
                        movementMethod(createLinkMovementMethod(object : TimelineEventController.UrlClickCallback {
                            override fun onUrlClicked(url: String, title: String): Boolean {
                                return false
                            }

                            override fun onUrlLongClicked(url: String): Boolean {
                                host.callback?.onUrlInTopicLongClicked(url)
                                return true
                            }
                        }))
                    }
                }

        // Security
        buildProfileSection(stringProvider.getString(R.string.room_profile_section_security))

        // Upgrade warning
        val roomVersion = data.roomCreateContent()?.roomVersion
        if (data.canUpgradeRoom &&
                !data.isTombstoned &&
                roomVersion != null &&
                data.isUsingUnstableRoomVersion &&
                data.recommendedRoomVersion != null) {
            genericFooterItem {
                id("version_warning")
                text(host.stringProvider.getString(R.string.room_using_unstable_room_version, roomVersion).toEpoxyCharSequence())
                textColor(host.colorProvider.getColorFromAttribute(R.attr.colorError))
                centered(false)
            }

            genericPositiveButtonItem {
                id("migrate_button")
                text(host.stringProvider.getString(R.string.room_upgrade_to_recommended_version))
                buttonClickAction { host.callback?.doMigrateToVersion(data.recommendedRoomVersion) }
            }
        }

        var encryptionMisconfigured = false
        val e2eInfoText = if (roomSummary.isEncrypted) {
            if (roomSummary.roomEncryptionAlgorithm is RoomEncryptionAlgorithm.SupportedAlgorithm) {
                stringProvider.getString(
                        if (roomSummary.isDirect) R.string.direct_room_profile_encrypted_subtitle
                        else R.string.room_profile_encrypted_subtitle
                )
            } else {
                encryptionMisconfigured = true
                buildString {
                    append(stringProvider.getString(R.string.encryption_has_been_misconfigured))
                    append(" ")
                    apply {
                        if (!data.canUpdateRoomState) {
                            append(stringProvider.getString(R.string.contact_admin_to_restore_encryption))
                        }
                    }
                }
            }
        } else {
            stringProvider.getString(
                    if (roomSummary.isDirect) R.string.direct_room_profile_not_encrypted_subtitle
                    else R.string.room_profile_not_encrypted_subtitle
            )
        }
        genericFooterItem {
            id("e2e info")
            centered(false)
            text(
                    span {
                        apply {
                            if (encryptionMisconfigured) {
                                host.drawableProvider.getDrawable(R.drawable.ic_warning_badge)?.let {
                                    image(it, "baseline")
                                }
                                +" "
                            }
                        }
                        +e2eInfoText
                    }.toEpoxyCharSequence()
            )
        }

        if (encryptionMisconfigured && data.canUpdateRoomState) {
            genericPositiveButtonItem {
                id("restore_encryption")
                text(host.stringProvider.getString(R.string.room_profile_section_restore_security))
                iconRes(R.drawable.ic_shield_black_no_border)
                buttonClickAction {
                    host.callback?.restoreEncryptionState()
                }
            }
        }
        buildEncryptionAction(data.actionPermissions, roomSummary)

        // More
        buildProfileSection(stringProvider.getString(R.string.room_profile_section_more))
        buildProfileAction(
                id = "settings",
                title = stringProvider.getString(
                        if (roomSummary.isDirect) {
                            R.string.direct_room_profile_section_more_settings
                        } else {
                            R.string.room_profile_section_more_settings
                        }
                ),
                icon = R.drawable.ic_room_profile_settings,
                action = { callback?.onSettingsClicked() }
        )
        buildProfileAction(
                id = "notifications",
                title = stringProvider.getString(R.string.room_profile_section_more_notifications),
                icon = R.drawable.ic_room_profile_notification,
                action = { callback?.onNotificationsClicked() }
        )
        val numberOfMembers = roomSummary.joinedMembersCount ?: 0
        val hasWarning = roomSummary.isEncrypted && roomSummary.roomEncryptionTrustLevel == RoomEncryptionTrustLevel.Warning
        buildProfileAction(
                id = "member_list",
                title = stringProvider.getQuantityString(R.plurals.room_profile_section_more_member_list, numberOfMembers, numberOfMembers),
                icon = R.drawable.ic_room_profile_member_list,
                accessory = R.drawable.ic_shield_warning.takeIf { hasWarning } ?: 0,
                action = { callback?.onMemberListClicked() }
        )

        if (data.bannedMembership.invoke()?.isNotEmpty() == true) {
            buildProfileAction(
                    id = "banned_list",
                    title = stringProvider.getString(R.string.room_settings_banned_users_title),
                    icon = R.drawable.ic_settings_root_labs,
                    action = { callback?.onBannedMemberListClicked() }
            )
        }
        buildProfileAction(
                id = "uploads",
                title = stringProvider.getString(R.string.room_profile_section_more_uploads),
                icon = R.drawable.ic_room_profile_uploads,
                action = { callback?.onUploadsClicked() }
        )
        if (shortcutCreator.canCreateShortcut()) {
            buildProfileAction(
                    id = "shortcut",
                    title = stringProvider.getString(R.string.room_settings_add_homescreen_shortcut),
                    editable = false,
                    icon = R.drawable.ic_add_to_home_screen_24dp,
                    action = { callback?.createShortcut() }
            )
        }
        buildProfileAction(
                id = "leave",
                title = stringProvider.getString(
                        if (roomSummary.isDirect) {
                            R.string.direct_room_profile_section_more_leave
                        } else {
                            R.string.room_profile_section_more_leave
                        }
                ),
                divider = false,
                destructive = true,
                icon = R.drawable.ic_room_actions_leave,
                editable = false,
                action = { callback?.onLeaveRoomClicked() }
        )

        // Advanced
        buildProfileSection(stringProvider.getString(R.string.room_settings_category_advanced_title))

        buildProfileAction(
                id = "alias",
                title = stringProvider.getString(R.string.room_settings_alias_title),
                subtitle = stringProvider.getString(R.string.room_settings_alias_subtitle),
                divider = true,
                editable = true,
                action = { callback?.onRoomAliasesClicked() }
        )

        buildProfileAction(
                id = "permissions",
                title = stringProvider.getString(R.string.room_settings_permissions_title),
                subtitle = stringProvider.getString(R.string.room_settings_permissions_subtitle),
                divider = vectorPreferences.developerMode(),
                editable = true,
                action = { callback?.onRoomPermissionsClicked() }
        )

        if (vectorPreferences.developerMode()) {
            buildProfileAction(
                    id = "roomId",
                    title = stringProvider.getString(R.string.room_settings_room_internal_id),
                    subtitle = roomSummary.roomId,
                    divider = true,
                    editable = false,
                    action = { callback?.onRoomIdClicked() }
            )
            roomVersion?.let {
                buildProfileAction(
                        id = "roomVersion",
                        title = stringProvider.getString(R.string.room_settings_room_version_title),
                        subtitle = it,
                        divider = true,
                        editable = false
                )
            }
            buildProfileAction(
                    id = "devTools",
                    title = stringProvider.getString(R.string.dev_tools_menu_name),
                    divider = false,
                    editable = true,
                    action = { callback?.onRoomDevToolsClicked() }
            )
        }
    }

    private fun buildEncryptionAction(actionPermissions: RoomProfileViewState.ActionPermissions, roomSummary: RoomSummary) {
        if (!roomSummary.isEncrypted) {
            if (actionPermissions.canEnableEncryption) {
                buildProfileAction(
                        id = "enableEncryption",
                        title = stringProvider.getString(R.string.room_settings_enable_encryption),
                        icon = R.drawable.ic_shield_black,
                        divider = false,
                        editable = false,
                        action = { callback?.onEnableEncryptionClicked() }
                )
            } else {
                buildProfileAction(
                        id = "enableEncryption",
                        title = stringProvider.getString(R.string.room_settings_enable_encryption_no_permission),
                        icon = R.drawable.ic_shield_black,
                        divider = false,
                        editable = false
                )
            }
        }
    }
}
