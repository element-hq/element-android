/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.features.start

import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.ActiveSessionSetter
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorViewModel
import im.vector.lib.core.utils.flow.tickerFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class StartAppViewModel @AssistedInject constructor(
        @Assisted val initialState: StartAppViewState,
        private val activeSessionSetter: ActiveSessionSetter,
) : VectorViewModel<StartAppViewState, StartAppAction, StartAppViewEvent>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<StartAppViewModel, StartAppViewState> {
        override fun create(initialState: StartAppViewState): StartAppViewModel
    }

    companion object : MavericksViewModelFactory<StartAppViewModel, StartAppViewState> by hiltMavericksViewModelFactory()

    fun shouldStartApp(): Boolean {
        return activeSessionSetter.shouldSetActionSession()
    }

    override fun handle(action: StartAppAction) {
        when (action) {
            StartAppAction.StartApp -> handleStartApp()
        }
    }

    private fun handleStartApp() {
        startTimer()
        viewModelScope.launch(Dispatchers.IO) {
            // This can take time because of DB migration(s), so do it in a background task.
            activeSessionSetter.tryToSetActiveSession(startSync = true)
            _viewEvents.post(StartAppViewEvent.AppStarted)
        }
    }

    private fun startTimer() {
        setState { copy(duration = 0) }
        tickerFlow(viewModelScope, 1.seconds.inWholeMilliseconds)
                .onEach {
                    setState { copy(duration = duration + 1) }
                }
                .launchIn(viewModelScope)
    }
}
