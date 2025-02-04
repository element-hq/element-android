/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomdirectory.createroom

import com.airbnb.epoxy.TypedEpoxyController
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import im.vector.app.core.epoxy.dividerItem
import im.vector.app.core.epoxy.profiles.buildProfileAction
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.discovery.settingsSectionTitleItem
import im.vector.app.features.form.formAdvancedToggleItem
import im.vector.app.features.form.formEditTextItem
import im.vector.app.features.form.formEditableAvatarItem
import im.vector.app.features.form.formSubmitButtonItem
import im.vector.app.features.form.formSwitchItem
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.MatrixConstants
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.room.failure.CreateRoomFailure
import org.matrix.android.sdk.api.session.room.model.RoomJoinRules
import javax.inject.Inject

class CreateRoomController @Inject constructor(
        private val stringProvider: StringProvider,
        private val roomAliasErrorFormatter: RoomAliasErrorFormatter
) : TypedEpoxyController<CreateRoomViewState>() {

    var listener: Listener? = null

    var index = 0

    override fun buildModels(viewState: CreateRoomViewState) {
        // display the form
        buildForm(viewState, viewState.asyncCreateRoomRequest !is Loading)
    }

    private fun buildForm(viewState: CreateRoomViewState, enableFormElement: Boolean) {
        val host = this
        formEditableAvatarItem {
            id("avatar")
            enabled(enableFormElement)
            imageUri(viewState.avatarUri)
            clickListener { host.listener?.onAvatarChange() }
            deleteListener { host.listener?.onAvatarDelete() }
        }
        settingsSectionTitleItem {
            id("nameSection")
            titleResId(CommonStrings.create_room_name_section)
        }
        formEditTextItem {
            id("name")
            enabled(enableFormElement)
            value(viewState.roomName)
            hint(host.stringProvider.getString(CommonStrings.create_room_name_hint))
            autoCapitalize(true)

            onTextChange { text ->
                host.listener?.onNameChange(text)
            }
        }
        settingsSectionTitleItem {
            id("topicSection")
            titleResId(CommonStrings.create_room_topic_section)
        }
        formEditTextItem {
            id("topic")
            enabled(enableFormElement)
            value(viewState.roomTopic)
            singleLine(false)
            hint(host.stringProvider.getString(CommonStrings.create_room_topic_hint))

            onTextChange { text ->
                host.listener?.onTopicChange(text)
            }
        }

        settingsSectionTitleItem {
            id("visibility")
            titleResId(CommonStrings.room_settings_room_access_title)
        }

        when (viewState.roomJoinRules) {
            RoomJoinRules.INVITE -> {
                buildProfileAction(
                        id = "joinRule",
                        title = stringProvider.getString(CommonStrings.room_settings_room_access_private_title),
                        subtitle = stringProvider.getString(CommonStrings.room_settings_room_access_private_description),
                        divider = false,
                        editable = true,
                        action = { host.listener?.selectVisibility() }
                )
            }
            RoomJoinRules.PUBLIC -> {
                buildProfileAction(
                        id = "joinRule",
                        title = stringProvider.getString(CommonStrings.room_settings_room_access_public_title),
                        subtitle = stringProvider.getString(CommonStrings.room_settings_room_access_public_description),
                        divider = false,
                        editable = true,
                        action = { host.listener?.selectVisibility() }
                )
            }
            RoomJoinRules.RESTRICTED -> {
                buildProfileAction(
                        id = "joinRule",
                        title = stringProvider.getString(CommonStrings.room_settings_room_access_restricted_title),
                        subtitle = stringProvider.getString(CommonStrings.room_create_member_of_space_name_can_join, viewState.parentSpaceSummary?.displayName),
                        divider = false,
                        editable = true,
                        action = { host.listener?.selectVisibility() }
                )
            }
            else -> {
                // not yet supported
            }
        }

        settingsSectionTitleItem {
            id("settingsSection")
            titleResId(CommonStrings.create_room_settings_section)
        }

        if (viewState.roomJoinRules == RoomJoinRules.PUBLIC) {
            // Room alias for public room
            formEditTextItem {
                id("alias")
                enabled(enableFormElement)
                value(viewState.aliasLocalPart)
                suffixText(":" + viewState.homeServerName)
                prefixText("#")
                maxLength(MatrixConstants.maxAliasLocalPartLength(viewState.homeServerName))
                hint(host.stringProvider.getString(CommonStrings.room_alias_address_hint))
                errorMessage(
                        host.roomAliasErrorFormatter.format(
                                (((viewState.asyncCreateRoomRequest as? Fail)?.error) as? CreateRoomFailure.AliasError)?.aliasError
                        )
                )
                onTextChange { value ->
                    host.listener?.setAliasLocalPart(value)
                }
            }
        } else {
            dividerItem {
                id("divider0")
            }
            // Room encryption for private room
            formSwitchItem {
                id("encryption")
                enabled(enableFormElement)
                title(host.stringProvider.getString(CommonStrings.create_room_encryption_title))
                summary(
                        if (viewState.hsAdminHasDisabledE2E) {
                            host.stringProvider.getString(CommonStrings.settings_hs_admin_e2e_disabled)
                        } else {
                            host.stringProvider.getString(CommonStrings.create_room_encryption_description)
                        }
                )

                switchChecked(viewState.isEncrypted ?: viewState.defaultEncrypted[viewState.roomJoinRules].orFalse())

                listener { value ->
                    host.listener?.setIsEncrypted(value)
                }
            }
        }

//        dividerItem {
//            id("divider1")
//        }
        formAdvancedToggleItem {
            id("showAdvanced")
            title(host.stringProvider.getString(if (viewState.showAdvanced) CommonStrings.hide_advanced else CommonStrings.show_advanced))
            expanded(!viewState.showAdvanced)
            listener { host.listener?.toggleShowAdvanced() }
        }
        if (viewState.showAdvanced) {
            formSwitchItem {
                id("federation")
                enabled(enableFormElement)
                title(host.stringProvider.getString(CommonStrings.create_room_disable_federation_title, viewState.homeServerName))
                summary(host.stringProvider.getString(CommonStrings.create_room_disable_federation_description))
                switchChecked(viewState.disableFederation)
                listener { value -> host.listener?.setDisableFederation(value) }
            }
        }
        formSubmitButtonItem {
            id("submit")
            enabled(enableFormElement)
            buttonTitleId(CommonStrings.create_room_action_create)
            buttonClickListener { host.listener?.submit() }
        }
    }

    interface Listener {
        fun onAvatarDelete()
        fun onAvatarChange()
        fun onNameChange(newName: String)
        fun onTopicChange(newTopic: String)
        fun selectVisibility()
        fun setAliasLocalPart(aliasLocalPart: String)
        fun setIsEncrypted(isEncrypted: Boolean)
        fun toggleShowAdvanced()
        fun setDisableFederation(disableFederation: Boolean)
        fun submit()
    }
}
