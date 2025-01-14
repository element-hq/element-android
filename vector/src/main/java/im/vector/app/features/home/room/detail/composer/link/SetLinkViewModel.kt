/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
