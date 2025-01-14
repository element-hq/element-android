/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.homeserver

import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Success
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.EmptyViewEvents
import im.vector.app.core.platform.VectorViewModel
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.Session

class HomeserverSettingsViewModel @AssistedInject constructor(
        @Assisted initialState: HomeServerSettingsViewState,
        private val session: Session
) : VectorViewModel<HomeServerSettingsViewState, HomeserverSettingsAction, EmptyViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<HomeserverSettingsViewModel, HomeServerSettingsViewState> {
        override fun create(initialState: HomeServerSettingsViewState): HomeserverSettingsViewModel
    }

    companion object : MavericksViewModelFactory<HomeserverSettingsViewModel, HomeServerSettingsViewState> by hiltMavericksViewModelFactory()

    init {
        setState {
            copy(
                    homeserverUrl = session.sessionParams.homeServerUrl,
                    homeserverClientServerApiUrl = session.sessionParams.homeServerUrlBase,
                    homeServerCapabilities = session.homeServerCapabilitiesService().getHomeServerCapabilities()
            )
        }
        fetchHomeserverVersion()
        refreshHomeServerCapabilities()
    }

    private fun refreshHomeServerCapabilities() {
        viewModelScope.launch {
            runCatching {
                session.homeServerCapabilitiesService().refreshHomeServerCapabilities()
            }

            setState {
                copy(
                        homeServerCapabilities = session.homeServerCapabilitiesService().getHomeServerCapabilities()
                )
            }
        }
    }

    private fun fetchHomeserverVersion() {
        setState {
            copy(
                    federationVersion = Loading()
            )
        }

        viewModelScope.launch {
            try {
                val federationVersion = session.federationService().getFederationVersion()
                setState {
                    copy(
                            federationVersion = Success(federationVersion)
                    )
                }
            } catch (failure: Throwable) {
                setState {
                    copy(
                            federationVersion = Fail(failure)
                    )
                }
            }
        }
    }

    override fun handle(action: HomeserverSettingsAction) {
        when (action) {
            HomeserverSettingsAction.Refresh -> fetchHomeserverVersion()
        }
    }
}
