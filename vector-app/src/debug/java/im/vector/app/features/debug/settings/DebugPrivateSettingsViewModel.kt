/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.debug.settings

import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.EmptyViewEvents
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.debug.features.DebugVectorOverrides
import im.vector.app.features.debug.settings.DebugPrivateSettingsViewActions.SetAvatarCapabilityOverride
import im.vector.app.features.debug.settings.DebugPrivateSettingsViewActions.SetDisplayNameCapabilityOverride
import im.vector.app.features.home.room.list.home.release.ReleaseNotesPreferencesStore
import kotlinx.coroutines.launch

class DebugPrivateSettingsViewModel @AssistedInject constructor(
        @Assisted initialState: DebugPrivateSettingsViewState,
        private val debugVectorOverrides: DebugVectorOverrides,
        private val releaseNotesPreferencesStore: ReleaseNotesPreferencesStore,
) : VectorViewModel<DebugPrivateSettingsViewState, DebugPrivateSettingsViewActions, EmptyViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<DebugPrivateSettingsViewModel, DebugPrivateSettingsViewState> {
        override fun create(initialState: DebugPrivateSettingsViewState): DebugPrivateSettingsViewModel
    }

    companion object : MavericksViewModelFactory<DebugPrivateSettingsViewModel, DebugPrivateSettingsViewState> by hiltMavericksViewModelFactory()

    init {
        observeVectorOverrides()
        observeReleaseNotesPreferencesStore()
    }

    private fun observeReleaseNotesPreferencesStore() {
        releaseNotesPreferencesStore.appLayoutOnboardingShown.setOnEach {
            copy(
                    releaseNotesActivityHasBeenDisplayed = it
            )
        }
    }

    private fun observeVectorOverrides() {
        debugVectorOverrides.forceDialPad.setOnEach {
            copy(
                    dialPadVisible = it
            )
        }
        debugVectorOverrides.forceLoginFallback.setOnEach {
            copy(forceLoginFallback = it)
        }
        debugVectorOverrides.forceHomeserverCapabilities.setOnEach {
            val activeDisplayNameOption = BooleanHomeserverCapabilitiesOverride.from(it.canChangeDisplayName)
            val activeAvatarOption = BooleanHomeserverCapabilitiesOverride.from(it.canChangeAvatar)
            copy(
                    homeserverCapabilityOverrides = homeserverCapabilityOverrides.copy(
                            displayName = homeserverCapabilityOverrides.displayName.copy(activeOption = activeDisplayNameOption),
                            avatar = homeserverCapabilityOverrides.avatar.copy(activeOption = activeAvatarOption),
                    )
            )
        }
    }

    override fun handle(action: DebugPrivateSettingsViewActions) {
        when (action) {
            is DebugPrivateSettingsViewActions.SetDialPadVisibility -> handleSetDialPadVisibility(action)
            is DebugPrivateSettingsViewActions.SetForceLoginFallbackEnabled -> handleSetForceLoginFallbackEnabled(action)
            is SetDisplayNameCapabilityOverride -> handleSetDisplayNameCapabilityOverride(action)
            is SetAvatarCapabilityOverride -> handleSetAvatarCapabilityOverride(action)
            DebugPrivateSettingsViewActions.ResetReleaseNotesActivityHasBeenDisplayed -> handleResetReleaseNotesActivityHasBeenDisplayed()
        }
    }

    private fun handleResetReleaseNotesActivityHasBeenDisplayed() {
        viewModelScope.launch {
            releaseNotesPreferencesStore.setAppLayoutOnboardingShown(false)
        }
    }

    private fun handleSetDialPadVisibility(action: DebugPrivateSettingsViewActions.SetDialPadVisibility) {
        viewModelScope.launch {
            debugVectorOverrides.setForceDialPadDisplay(action.force)
        }
    }

    private fun handleSetForceLoginFallbackEnabled(action: DebugPrivateSettingsViewActions.SetForceLoginFallbackEnabled) {
        viewModelScope.launch {
            debugVectorOverrides.setForceLoginFallback(action.force)
        }
    }

    private fun handleSetDisplayNameCapabilityOverride(action: SetDisplayNameCapabilityOverride) {
        viewModelScope.launch {
            val forceDisplayName = action.option.toBoolean()
            debugVectorOverrides.setHomeserverCapabilities { copy(canChangeDisplayName = forceDisplayName) }
        }
    }

    private fun handleSetAvatarCapabilityOverride(action: SetAvatarCapabilityOverride) {
        viewModelScope.launch {
            val forceAvatar = action.option.toBoolean()
            debugVectorOverrides.setHomeserverCapabilities { copy(canChangeAvatar = forceAvatar) }
        }
    }
}
