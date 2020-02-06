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

package im.vector.riotx.features.roommemberprofile

import com.airbnb.epoxy.TypedEpoxyController
import im.vector.riotx.R
import im.vector.riotx.core.epoxy.profiles.buildProfileAction
import im.vector.riotx.core.epoxy.profiles.buildProfileSection
import im.vector.riotx.core.resources.ColorProvider
import im.vector.riotx.core.resources.StringProvider
import im.vector.riotx.core.ui.list.genericFooterItem
import javax.inject.Inject

class RoomMemberProfileController @Inject constructor(
        private val stringProvider: StringProvider,
        colorProvider: ColorProvider
) : TypedEpoxyController<RoomMemberProfileViewState>() {

    private val dividerColor = colorProvider.getColorFromAttribute(R.attr.vctr_list_divider_color)

    var callback: Callback? = null

    interface Callback {
        fun onIgnoreClicked()
        fun onTapVerify()
        fun onShowDeviceList()
        fun onShowDeviceListNoCrossSigning()
        fun onJumpToReadReceiptClicked()
        fun onMentionClicked()
    }

    override fun buildModels(data: RoomMemberProfileViewState?) {
        if (data?.userMatrixItem?.invoke() == null) {
            return
        }
        if (data.showAsMember) {
            buildRoomMemberActions(data)
        } else {
            buildUserActions(data)
        }
    }

    private fun buildUserActions(state: RoomMemberProfileViewState) {
        val ignoreActionTitle = state.buildIgnoreActionTitle() ?: return
        // More
        buildProfileSection(stringProvider.getString(R.string.room_profile_section_more))
        buildProfileAction(
                id = "ignore",
                title = ignoreActionTitle,
                dividerColor = dividerColor,
                destructive = true,
                editable = false,
                divider = false,
                action = { callback?.onIgnoreClicked() }
        )
    }

    private fun buildRoomMemberActions(state: RoomMemberProfileViewState) {
        // Security
        buildProfileSection(stringProvider.getString(R.string.room_profile_section_security))

        if (state.isRoomEncrypted) {
            if (state.userMXCrossSigningInfo != null) {
                // Cross signing is enabled for this user
                if (state.userMXCrossSigningInfo.isTrusted()) {
                    // User is trusted
                    val icon = if (state.allDevicesAreTrusted) {
                        R.drawable.ic_shield_trusted
                    } else {
                        R.drawable.ic_shield_warning
                    }

                    val titleRes = if (state.allDevicesAreTrusted) {
                        R.string.verification_profile_verified
                    } else {
                        R.string.verification_profile_warning
                    }

                    buildProfileAction(
                            id = "learn_more",
                            title = stringProvider.getString(titleRes),
                            dividerColor = dividerColor,
                            editable = true,
                            icon = icon,
                            tintIcon = false,
                            divider = false,
                            action = { callback?.onShowDeviceList() }
                    )
                } else {
                    // Not trusted, propose to verify
                    if (!state.isMine) {
                        buildProfileAction(
                                id = "learn_more",
                                title = stringProvider.getString(R.string.verification_profile_verify),
                                dividerColor = dividerColor,
                                editable = true,
                                icon = R.drawable.ic_shield_black,
                                divider = false,
                                action = { callback?.onTapVerify() }
                        )
                    } else {
                        buildProfileAction(
                                id = "learn_more",
                                title = stringProvider.getString(R.string.room_profile_section_security_learn_more),
                                dividerColor = dividerColor,
                                editable = false,
                                divider = false,
                                action = { callback?.onShowDeviceListNoCrossSigning() }
                        )
                    }

                    genericFooterItem {
                        id("verify_footer")
                        text(stringProvider.getString(R.string.room_profile_encrypted_subtitle))
                        centered(false)
                    }
                }
            } else {
                buildProfileAction(
                        id = "learn_more",
                        title = stringProvider.getString(R.string.room_profile_section_security_learn_more),
                        dividerColor = dividerColor,
                        editable = false,
                        divider = false,
                        subtitle = stringProvider.getString(R.string.room_profile_encrypted_subtitle),
                        action = { callback?.onShowDeviceListNoCrossSigning() }
                )
            }
        } else {
            genericFooterItem {
                id("verify_footer_not_encrypted")
                text(stringProvider.getString(R.string.room_profile_not_encrypted_subtitle))
                centered(false)
            }
        }

        // More
        if (!state.isMine) {
            buildProfileSection(stringProvider.getString(R.string.room_profile_section_more))
            buildProfileAction(
                    id = "read_receipt",
                    editable = false,
                    title = stringProvider.getString(R.string.room_member_jump_to_read_receipt),
                    dividerColor = dividerColor,
                    action = { callback?.onJumpToReadReceiptClicked() }
            )

            val ignoreActionTitle = state.buildIgnoreActionTitle()

            buildProfileAction(
                    id = "mention",
                    title = stringProvider.getString(R.string.room_participants_action_mention),
                    dividerColor = dividerColor,
                    editable = false,
                    divider = ignoreActionTitle != null,
                    action = { callback?.onMentionClicked() }
            )
            if (ignoreActionTitle != null) {
                buildProfileAction(
                        id = "ignore",
                        title = ignoreActionTitle,
                        dividerColor = dividerColor,
                        destructive = true,
                        editable = false,
                        divider = false,
                        action = { callback?.onIgnoreClicked() }
                )
            }
        }
    }

    private fun RoomMemberProfileViewState.buildIgnoreActionTitle(): String? {
        val isIgnored = isIgnored() ?: return null
        return if (isIgnored) {
            stringProvider.getString(R.string.unignore)
        } else {
            stringProvider.getString(R.string.ignore)
        }
    }
}
