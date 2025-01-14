/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home

import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.EmptyAction
import im.vector.app.core.platform.EmptyViewEvents
import im.vector.app.core.platform.VectorViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class NewHomeDetailViewModel @AssistedInject constructor(
        @Assisted initialState: NewHomeDetailViewState,
        private val getSpacesNotificationBadgeStateUseCase: GetSpacesNotificationBadgeStateUseCase,
) : VectorViewModel<NewHomeDetailViewState, EmptyAction, EmptyViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<NewHomeDetailViewModel, NewHomeDetailViewState> {
        override fun create(initialState: NewHomeDetailViewState): NewHomeDetailViewModel
    }

    companion object : MavericksViewModelFactory<NewHomeDetailViewModel, NewHomeDetailViewState> by hiltMavericksViewModelFactory()

    init {
        observeSpacesNotificationBadgeState()
    }

    private fun observeSpacesNotificationBadgeState() {
        getSpacesNotificationBadgeStateUseCase.execute()
                .onEach { badgeState -> setState { copy(spacesNotificationCounterBadgeState = badgeState) } }
                .launchIn(viewModelScope)
    }

    override fun handle(action: EmptyAction) {
        // do nothing
    }
}
