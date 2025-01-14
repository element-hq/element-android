/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.details

import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Success
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.utils.CopyToClipboardUseCase
import im.vector.app.features.settings.devices.v2.overview.GetDeviceFullInfoUseCase
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class SessionDetailsViewModel @AssistedInject constructor(
        @Assisted val initialState: SessionDetailsViewState,
        private val getDeviceFullInfoUseCase: GetDeviceFullInfoUseCase,
        private val copyToClipboardUseCase: CopyToClipboardUseCase,
) : VectorViewModel<SessionDetailsViewState, SessionDetailsAction, SessionDetailsViewEvent>(initialState) {

    companion object : MavericksViewModelFactory<SessionDetailsViewModel, SessionDetailsViewState> by hiltMavericksViewModelFactory()

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<SessionDetailsViewModel, SessionDetailsViewState> {
        override fun create(initialState: SessionDetailsViewState): SessionDetailsViewModel
    }

    init {
        observeSessionInfo(initialState.deviceId)
    }

    private fun observeSessionInfo(deviceId: String) {
        getDeviceFullInfoUseCase.execute(deviceId)
                .onEach { setState { copy(deviceFullInfo = Success(it)) } }
                .launchIn(viewModelScope)
    }

    override fun handle(action: SessionDetailsAction) {
        return when (action) {
            is SessionDetailsAction.CopyToClipboard -> handleCopyToClipboard(action)
        }
    }

    private fun handleCopyToClipboard(copyToClipboard: SessionDetailsAction.CopyToClipboard) {
        copyToClipboardUseCase.execute(copyToClipboard.content)
        _viewEvents.post(SessionDetailsViewEvent.ContentCopiedToClipboard)
    }
}
