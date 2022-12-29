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

package im.vector.app.features.home.room.detail.composer.link

import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorViewModel

class SetLinkViewModel @AssistedInject constructor(
        @Assisted private val initialState: SetLinkViewState,
) : VectorViewModel<SetLinkViewState, SetLinkAction, SetLinkViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<SetLinkViewModel, SetLinkViewState> {
        override fun create(initialState: SetLinkViewState): SetLinkViewModel
    }

    companion object : MavericksViewModelFactory<SetLinkViewModel, SetLinkViewState> by hiltMavericksViewModelFactory()

    override fun handle(action: SetLinkAction) = when (action) {
        is SetLinkAction.LinkChanged -> handleLinkChanged(action.newLink)
        is SetLinkAction.Save -> handleSave(action.link, action.text)
    }

    private fun handleLinkChanged(newLink: String) = setState {
        copy(saveEnabled = newLink != initialLink.orEmpty())
    }

    private fun handleSave(
            link: String,
            text: String
    ) = if (initialState.isTextSupported) {
        _viewEvents.post(SetLinkViewEvents.SavedLinkAndText(link, text))
    } else {
        _viewEvents.post(SetLinkViewEvents.SavedLink(link))
    }
}
