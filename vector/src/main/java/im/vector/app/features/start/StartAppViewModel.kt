/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.start

import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.dispatchers.CoroutineDispatchers
import im.vector.app.core.platform.VectorViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class StartAppViewModel @AssistedInject constructor(
        @Assisted val initialState: StartAppViewState,
        private val sessionHolder: ActiveSessionHolder,
        private val dispatchers: CoroutineDispatchers,
) : VectorViewModel<StartAppViewState, StartAppAction, StartAppViewEvent>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<StartAppViewModel, StartAppViewState> {
        override fun create(initialState: StartAppViewState): StartAppViewModel
    }

    companion object : MavericksViewModelFactory<StartAppViewModel, StartAppViewState> by hiltMavericksViewModelFactory()

    fun shouldStartApp(): Boolean {
        return sessionHolder.isWaitingForSessionInitialization()
    }

    override fun handle(action: StartAppAction) {
        when (action) {
            StartAppAction.StartApp -> handleStartApp()
        }
    }

    private fun handleStartApp() {
        handleLongProcessing()
        viewModelScope.launch(dispatchers.io) {
            // This can take time because of DB migration(s), so do it in a background task.
            eagerlyInitializeSession()
            _viewEvents.post(StartAppViewEvent.AppStarted)
        }
    }

    private suspend fun eagerlyInitializeSession() {
        sessionHolder.getOrInitializeSession()
    }

    private fun handleLongProcessing() {
        viewModelScope.launch(Dispatchers.Default) {
            delay(1.seconds.inWholeMilliseconds)
            setState { copy(mayBeLongToProcess = true) }
            _viewEvents.post(StartAppViewEvent.StartForegroundService)
        }
    }
}
