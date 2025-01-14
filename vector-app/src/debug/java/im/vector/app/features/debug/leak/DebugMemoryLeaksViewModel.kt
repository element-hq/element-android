/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.debug.leak

import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.debug.LeakDetector
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.EmptyViewEvents
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.settings.VectorPreferences
import kotlinx.coroutines.launch

class DebugMemoryLeaksViewModel @AssistedInject constructor(
        @Assisted initialState: DebugMemoryLeaksViewState,
        private val vectorPreferences: VectorPreferences,
        private val leakDetector: LeakDetector,
) : VectorViewModel<DebugMemoryLeaksViewState, DebugMemoryLeaksViewActions, EmptyViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<DebugMemoryLeaksViewModel, DebugMemoryLeaksViewState> {
        override fun create(initialState: DebugMemoryLeaksViewState): DebugMemoryLeaksViewModel
    }

    companion object : MavericksViewModelFactory<DebugMemoryLeaksViewModel, DebugMemoryLeaksViewState> by hiltMavericksViewModelFactory()

    init {
        viewModelScope.launch {
            refreshStateFromPreferences()
        }
    }

    override fun handle(action: DebugMemoryLeaksViewActions) {
        when (action) {
            is DebugMemoryLeaksViewActions.EnableMemoryLeaksAnalysis -> handleEnableMemoryLeaksAnalysis(action)
        }
    }

    private fun handleEnableMemoryLeaksAnalysis(action: DebugMemoryLeaksViewActions.EnableMemoryLeaksAnalysis) {
        viewModelScope.launch {
            vectorPreferences.enableMemoryLeakAnalysis(action.isEnabled)
            leakDetector.enable(action.isEnabled)
            refreshStateFromPreferences()
        }
    }

    private fun refreshStateFromPreferences() {
        setState { copy(isMemoryLeaksAnalysisEnabled = vectorPreferences.isMemoryLeakAnalysisEnabled()) }
    }
}
