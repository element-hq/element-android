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
 */

package im.vector.app.features.roomdirectory.createroom

import com.airbnb.epoxy.TypedEpoxyController
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import im.vector.app.R
import im.vector.app.core.epoxy.errorWithRetryItem
import im.vector.app.core.epoxy.loadingItem
import im.vector.app.core.error.ErrorFormatter
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.discovery.settingsSectionTitleItem
import im.vector.app.features.form.formEditTextItem
import im.vector.app.features.form.formEditableAvatarItem
import im.vector.app.features.form.formSubmitButtonItem
import im.vector.app.features.form.formSwitchItem
import javax.inject.Inject

class CreateRoomController @Inject constructor(private val stringProvider: StringProvider,
                                               private val errorFormatter: ErrorFormatter
) : TypedEpoxyController<CreateRoomViewState>() {

    var listener: Listener? = null

    var index = 0

    override fun buildModels(viewState: CreateRoomViewState) {
        when (val asyncCreateRoom = viewState.asyncCreateRoomRequest) {
            is Success       -> {
                // Nothing to display, the screen will be closed
            }
            is Loading       -> {
                // display the form
                buildForm(viewState, false)
                loadingItem {
                    id("loading")
                }
            }
            is Uninitialized -> {
                // display the form
                buildForm(viewState, true)
            }
            is Fail          -> {
                // display the form
                buildForm(viewState, true)
                errorWithRetryItem {
                    id("error")
                    text(errorFormatter.toHumanReadable(asyncCreateRoom.error))
                    listener { listener?.retry() }
                }
            }
        }
    }

    private fun buildForm(viewState: CreateRoomViewState, enableFormElement: Boolean) {
        formEditableAvatarItem {
            id("avatar")
            enabled(enableFormElement)
            imageUri(viewState.avatarUri)
            clickListener { listener?.onAvatarChange() }
            deleteListener { listener?.onAvatarDelete() }
        }
        settingsSectionTitleItem {
            id("nameSection")
            titleResId(R.string.create_room_name_section)
        }
        formEditTextItem {
            id("name")
            enabled(enableFormElement)
            value(viewState.roomName)
            hint(stringProvider.getString(R.string.create_room_name_hint))

            onTextChange { text ->
                listener?.onNameChange(text)
            }
        }
        settingsSectionTitleItem {
            id("topicSection")
            titleResId(R.string.create_room_topic_section)
        }
        formEditTextItem {
            id("topic")
            enabled(enableFormElement)
            value(viewState.roomTopic)
            hint(stringProvider.getString(R.string.create_room_topic_hint))

            onTextChange { text ->
                listener?.onTopicChange(text)
            }
        }
        settingsSectionTitleItem {
            id("settingsSection")
            titleResId(R.string.create_room_settings_section)
        }
        formSwitchItem {
            id("public")
            enabled(enableFormElement)
            title(stringProvider.getString(R.string.create_room_public_title))
            summary(stringProvider.getString(R.string.create_room_public_description))
            switchChecked(viewState.isPublic)

            listener { value ->
                listener?.setIsPublic(value)
            }
        }
        formSwitchItem {
            id("directory")
            enabled(enableFormElement)
            title(stringProvider.getString(R.string.create_room_directory_title))
            summary(stringProvider.getString(R.string.create_room_directory_description))
            switchChecked(viewState.isInRoomDirectory)

            listener { value ->
                listener?.setIsInRoomDirectory(value)
            }
        }
        formSwitchItem {
            id("encryption")
            enabled(enableFormElement)
            title(stringProvider.getString(R.string.create_room_encryption_title))
            summary(
                    if (viewState.hsAdminHasDisabledE2E) {
                        stringProvider.getString(R.string.settings_hs_admin_e2e_disabled)
                    } else {
                        stringProvider.getString(R.string.create_room_encryption_description)
                    }
            )
            switchChecked(viewState.isEncrypted)

            listener { value ->
                listener?.setIsEncrypted(value)
            }
        }
        formSubmitButtonItem {
            id("submit")
            enabled(enableFormElement)
            buttonTitleId(R.string.create_room_action_create)
            buttonClickListener { listener?.submit() }
        }
    }

    interface Listener {
        fun onAvatarDelete()
        fun onAvatarChange()
        fun onNameChange(newName: String)
        fun onTopicChange(newTopic: String)
        fun setIsPublic(isPublic: Boolean)
        fun setIsInRoomDirectory(isInRoomDirectory: Boolean)
        fun setIsEncrypted(isEncrypted: Boolean)
        fun retry()
        fun submit()
    }
}
