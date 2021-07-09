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

package fr.gouv.tchap.features.home.roomdirectory.createroom

import com.airbnb.epoxy.TypedEpoxyController
import com.airbnb.mvrx.Loading
import im.vector.app.R
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.form.formSubmitButtonItem
import im.vector.app.features.roomdirectory.createroom.CreateRoomViewState
import im.vector.app.features.roomdirectory.createroom.tchapRoomAvatarWithNameItem
import javax.inject.Inject

class TchapCreateRoomController @Inject constructor(
        private val stringProvider: StringProvider
) : TypedEpoxyController<CreateRoomViewState>() {

    var listener: Listener? = null

    var index = 0

    override fun buildModels(viewState: CreateRoomViewState) {
        // display the form
        buildForm(viewState, viewState.asyncCreateRoomRequest !is Loading)
    }

    private fun buildForm(viewState: CreateRoomViewState, enableFormElement: Boolean) {
        val host = this

        tchapRoomAvatarWithNameItem {
            id("avatarWithName")
            enabled(enableFormElement)

            imageUri(viewState.avatarUri)
            clickListener { host.listener?.onAvatarChange() }
            deleteListener { host.listener?.onAvatarDelete() }

            value(viewState.roomName)
            hint(host.stringProvider.getString(R.string.create_room_name_hint))
            onTextChange { host.listener?.onNameChange(it) }
        }

        formSubmitButtonItem {
            id("submit")
            enabled(enableFormElement)
            buttonTitleId(R.string.create_room_action_create)
            buttonClickListener { host.listener?.submit() }
        }
    }

    interface Listener {
        fun onAvatarDelete()
        fun onAvatarChange()
        fun onNameChange(newName: String)
        fun onTopicChange(newTopic: String)
        fun setIsPublic(isPublic: Boolean)
        fun setAliasLocalPart(aliasLocalPart: String)
        fun setIsEncrypted(isEncrypted: Boolean)
        fun toggleShowAdvanced()
        fun setDisableFederation(disableFederation: Boolean)
        fun submit()
    }
}
