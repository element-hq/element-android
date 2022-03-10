/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.roomdirectory.createroom

import com.airbnb.epoxy.TypedEpoxyController
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import im.vector.app.R
import im.vector.app.core.epoxy.profiles.buildProfileAction
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.ui.list.genericPillItem
import im.vector.app.features.discovery.settingsSectionTitleItem
import im.vector.app.features.form.formEditTextItem
import im.vector.app.features.form.formEditableSquareAvatarItem
import im.vector.app.features.form.formMultiLineEditTextItem
import im.vector.app.features.form.formSubmitButtonItem
import im.vector.lib.core.utils.epoxy.charsequence.toEpoxyCharSequence
import org.matrix.android.sdk.api.MatrixConstants
import org.matrix.android.sdk.api.session.room.failure.CreateRoomFailure
import org.matrix.android.sdk.api.session.room.model.RoomJoinRules
import javax.inject.Inject

class CreateSubSpaceController @Inject constructor(
        private val stringProvider: StringProvider,
        private val roomAliasErrorFormatter: RoomAliasErrorFormatter
) : TypedEpoxyController<CreateRoomViewState>() {

    var listener: CreateRoomController.Listener? = null

    override fun buildModels(viewState: CreateRoomViewState) {
        // display the form
        buildForm(viewState, viewState.asyncCreateRoomRequest !is Loading)
    }

    private fun buildForm(data: CreateRoomViewState, enableFormElement: Boolean) {
        val host = this

        genericPillItem {
            id("beta")
            imageRes(R.drawable.ic_beta_pill)
            tintIcon(false)
            text(host.stringProvider.getString(R.string.space_add_space_to_any_space_you_manage).toEpoxyCharSequence())
        }

        formEditableSquareAvatarItem {
            id("avatar")
            enabled(enableFormElement)
            imageUri(data.avatarUri)
            clickListener { host.listener?.onAvatarChange() }
            deleteListener { host.listener?.onAvatarDelete() }
        }

        formEditTextItem {
            id("name")
            enabled(enableFormElement)
            enabled(true)
            value(data.roomName)
            hint(host.stringProvider.getString(R.string.create_room_name_hint))
            onTextChange { text ->
                host.listener?.onNameChange(text)
            }
        }

        if (data.roomJoinRules == RoomJoinRules.PUBLIC) {
            formEditTextItem {
                id("alias")
                enabled(enableFormElement)
                value(data.aliasLocalPart)
                hint(host.stringProvider.getString(R.string.create_space_alias_hint))
                suffixText(":" + data.homeServerName)
                prefixText("#")
                maxLength(MatrixConstants.maxAliasLocalPartLength(data.homeServerName))
                errorMessage(
                        host.roomAliasErrorFormatter.format(
                                (((data.asyncCreateRoomRequest as? Fail)?.error) as? CreateRoomFailure.AliasError)?.aliasError)
                )
                onTextChange { value ->
                    host.listener?.setAliasLocalPart(value)
                }
            }
        }

        formMultiLineEditTextItem {
            id("topic")
            enabled(enableFormElement)
            value(data.roomTopic)
            hint(host.stringProvider.getString(R.string.create_space_topic_hint))
            textSizeSp(16)
            onTextChange { text ->
                host.listener?.onTopicChange(text)
            }
        }

        settingsSectionTitleItem {
            id("visibility")
            titleResId(R.string.room_settings_space_access_title)
        }

        when (data.roomJoinRules) {
            RoomJoinRules.INVITE     -> {
                buildProfileAction(
                        id = "joinRule",
                        title = stringProvider.getString(R.string.room_settings_room_access_private_title),
                        subtitle = stringProvider.getString(R.string.room_settings_room_access_private_description),
                        divider = false,
                        editable = true,
                        action = { host.listener?.selectVisibility() }
                )
            }
            RoomJoinRules.PUBLIC     -> {
                buildProfileAction(
                        id = "joinRule",
                        title = stringProvider.getString(R.string.room_settings_room_access_public_title),
                        subtitle = stringProvider.getString(R.string.room_settings_room_access_public_description),
                        divider = false,
                        editable = true,
                        action = { host.listener?.selectVisibility() }
                )
            }
            RoomJoinRules.RESTRICTED -> {
                buildProfileAction(
                        id = "joinRule",
                        title = stringProvider.getString(R.string.room_settings_room_access_restricted_title),
                        subtitle = stringProvider.getString(R.string.room_create_member_of_space_name_can_join, data.parentSpaceSummary?.displayName),
                        divider = false,
                        editable = true,
                        action = { host.listener?.selectVisibility() }
                )
            }
            else                     -> {
                // not yet supported
            }
        }

        formSubmitButtonItem {
            id("submit")
            enabled(enableFormElement && data.roomName.isNullOrBlank().not())
            buttonTitleId(R.string.create_room_action_create)
            buttonClickListener { host.listener?.submit() }
        }
    }
}
