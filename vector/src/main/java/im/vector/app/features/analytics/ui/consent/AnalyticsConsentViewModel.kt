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

package im.vector.app.features.analytics.ui.consent

import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.analytics.VectorAnalytics
import kotlinx.coroutines.launch

class AnalyticsConsentViewModel @AssistedInject constructor(
        @Assisted initialState: AnalyticsConsentViewState,
        private val analytics: VectorAnalytics
) : VectorViewModel<AnalyticsConsentViewState, AnalyticsConsentViewActions, AnalyticsOptInViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<AnalyticsConsentViewModel, AnalyticsConsentViewState> {
        override fun create(initialState: AnalyticsConsentViewState): AnalyticsConsentViewModel
    }

    companion object : MavericksViewModelFactory<AnalyticsConsentViewModel, AnalyticsConsentViewState> by hiltMavericksViewModelFactory()

    init {
        observeAnalytics()
    }

    private fun observeAnalytics() {
        analytics.didAskUserConsent().setOnEach {
            copy(didAskUserConsent = it)
        }
        analytics.getUserConsent().setOnEach {
            copy(userConsent = it)
        }
    }

    override fun handle(action: AnalyticsConsentViewActions) {
        when (action) {
            is AnalyticsConsentViewActions.SetUserConsent -> handleSetUserConsent(action)
        }
    }

    private fun handleSetUserConsent(action: AnalyticsConsentViewActions.SetUserConsent) {
        viewModelScope.launch {
            analytics.setUserConsent(action.userConsent)
            analytics.setDidAskUserConsent()
            _viewEvents.post(AnalyticsOptInViewEvents.OnDataSaved)
        }
    }
}
