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

package im.vector.app.features.settings.notifications

import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorDummyViewState
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.pushers.EnsureFcmTokenIsRetrievedUseCase
import im.vector.app.core.pushers.PushersManager
import im.vector.app.core.pushers.RegisterUnifiedPushUseCase
import im.vector.app.core.pushers.UnregisterUnifiedPushUseCase
import im.vector.app.features.settings.VectorPreferences
import kotlinx.coroutines.launch

class VectorSettingsNotificationPreferenceViewModel @AssistedInject constructor(
        @Assisted initialState: VectorDummyViewState,
        private val pushersManager: PushersManager,
        private val vectorPreferences: VectorPreferences,
        private val enableNotificationsForCurrentSessionUseCase: EnableNotificationsForCurrentSessionUseCase,
        private val disableNotificationsForCurrentSessionUseCase: DisableNotificationsForCurrentSessionUseCase,
        private val unregisterUnifiedPushUseCase: UnregisterUnifiedPushUseCase,
        private val registerUnifiedPushUseCase: RegisterUnifiedPushUseCase,
        private val ensureFcmTokenIsRetrievedUseCase: EnsureFcmTokenIsRetrievedUseCase,
) : VectorViewModel<VectorDummyViewState, VectorSettingsNotificationPreferenceViewAction, VectorSettingsNotificationPreferenceViewEvent>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<VectorSettingsNotificationPreferenceViewModel, VectorDummyViewState> {
        override fun create(initialState: VectorDummyViewState): VectorSettingsNotificationPreferenceViewModel
    }

    companion object : MavericksViewModelFactory<VectorSettingsNotificationPreferenceViewModel, VectorDummyViewState> by hiltMavericksViewModelFactory()

    // TODO add unit tests
    override fun handle(action: VectorSettingsNotificationPreferenceViewAction) {
        when (action) {
            VectorSettingsNotificationPreferenceViewAction.DisableNotificationsForDevice -> handleDisableNotificationsForDevice()
            is VectorSettingsNotificationPreferenceViewAction.EnableNotificationsForDevice -> handleEnableNotificationsForDevice(action.pushDistributor)
            is VectorSettingsNotificationPreferenceViewAction.RegisterPushDistributor -> handleRegisterPushDistributor(action.pushDistributor)
        }
    }

    private fun handleDisableNotificationsForDevice() {
        viewModelScope.launch {
            disableNotificationsForCurrentSessionUseCase.execute()
            _viewEvents.post(VectorSettingsNotificationPreferenceViewEvent.NotificationsForDeviceDisabled)
        }
    }

    private fun handleEnableNotificationsForDevice(distributor: String) {
        viewModelScope.launch {
            when (enableNotificationsForCurrentSessionUseCase.execute(distributor)) {
                EnableNotificationsForCurrentSessionUseCase.EnableNotificationsResult.Failure -> {
                    _viewEvents.post(VectorSettingsNotificationPreferenceViewEvent.EnableNotificationForDeviceFailure)
                }
                is EnableNotificationsForCurrentSessionUseCase.EnableNotificationsResult.NeedToAskUserForDistributor -> {
                    _viewEvents.post(VectorSettingsNotificationPreferenceViewEvent.AskUserForPushDistributor)
                }
                EnableNotificationsForCurrentSessionUseCase.EnableNotificationsResult.Success -> {
                    _viewEvents.post(VectorSettingsNotificationPreferenceViewEvent.NotificationsForDeviceEnabled)
                }
            }
        }
    }

    private fun handleRegisterPushDistributor(distributor: String) {
        viewModelScope.launch {
            unregisterUnifiedPushUseCase.execute(pushersManager)
            when (registerUnifiedPushUseCase.execute(distributor)) {
                RegisterUnifiedPushUseCase.RegisterUnifiedPushResult.NeedToAskUserForDistributor -> {
                    _viewEvents.post(VectorSettingsNotificationPreferenceViewEvent.AskUserForPushDistributor)
                }
                RegisterUnifiedPushUseCase.RegisterUnifiedPushResult.Success -> {
                    ensureFcmTokenIsRetrievedUseCase.execute(pushersManager, registerPusher = vectorPreferences.areNotificationEnabledForDevice())
                    _viewEvents.post(VectorSettingsNotificationPreferenceViewEvent.NotificationMethodChanged)
                }
            }
        }
    }
}
