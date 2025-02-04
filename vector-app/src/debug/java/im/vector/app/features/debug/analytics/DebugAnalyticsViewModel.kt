/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.debug.analytics

import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.EmptyViewEvents
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.analytics.store.AnalyticsStore
import kotlinx.coroutines.launch

class DebugAnalyticsViewModel @AssistedInject constructor(
        @Assisted initialState: DebugAnalyticsViewState,
        private val analyticsStore: AnalyticsStore
) : VectorViewModel<DebugAnalyticsViewState, DebugAnalyticsViewActions, EmptyViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<DebugAnalyticsViewModel, DebugAnalyticsViewState> {
        override fun create(initialState: DebugAnalyticsViewState): DebugAnalyticsViewModel
    }

    companion object : MavericksViewModelFactory<DebugAnalyticsViewModel, DebugAnalyticsViewState> by hiltMavericksViewModelFactory()

    init {
        observerStore()
    }

    private fun observerStore() {
        analyticsStore.analyticsIdFlow.setOnEach { copy(analyticsId = it) }
        analyticsStore.userConsentFlow.setOnEach { copy(userConsent = it) }
        analyticsStore.didAskUserConsentFlow.setOnEach { copy(didAskUserConsent = it) }
    }

    override fun handle(action: DebugAnalyticsViewActions) {
        when (action) {
            DebugAnalyticsViewActions.ResetAnalyticsOptInDisplayed -> handleResetAnalyticsOptInDisplayed()
        }
    }

    private fun handleResetAnalyticsOptInDisplayed() {
        viewModelScope.launch {
            analyticsStore.setDidAskUserConsent(false)
        }
    }
}
