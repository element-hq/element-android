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

package im.vector.app.features.settings.devices.v2

import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Success
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.features.settings.devices.v2.filter.DeviceManagerFilterType
import im.vector.app.features.settings.devices.v2.verification.CheckIfCurrentSessionCanBeVerifiedUseCase
import im.vector.app.features.settings.devices.v2.verification.GetCurrentSessionCrossSigningInfoUseCase
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.extensions.orFalse

class DevicesViewModel @AssistedInject constructor(
        @Assisted initialState: DevicesViewState,
        activeSessionHolder: ActiveSessionHolder,
        private val getCurrentSessionCrossSigningInfoUseCase: GetCurrentSessionCrossSigningInfoUseCase,
        private val getDeviceFullInfoListUseCase: GetDeviceFullInfoListUseCase,
        private val refreshDevicesOnCryptoDevicesChangeUseCase: RefreshDevicesOnCryptoDevicesChangeUseCase,
        private val checkIfCurrentSessionCanBeVerifiedUseCase: CheckIfCurrentSessionCanBeVerifiedUseCase,
        refreshDevicesUseCase: RefreshDevicesUseCase,
) : VectorSessionsListViewModel<DevicesViewState, DevicesAction, DevicesViewEvent>(initialState, activeSessionHolder, refreshDevicesUseCase) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<DevicesViewModel, DevicesViewState> {
        override fun create(initialState: DevicesViewState): DevicesViewModel
    }

    companion object : MavericksViewModelFactory<DevicesViewModel, DevicesViewState> by hiltMavericksViewModelFactory()

    init {
        observeCurrentSessionCrossSigningInfo()
        observeDevices()
        refreshDevicesOnCryptoDevicesChange()
        refreshDeviceList()
    }

    private fun observeCurrentSessionCrossSigningInfo() {
        getCurrentSessionCrossSigningInfoUseCase.execute()
                .onEach { crossSigningInfo ->
                    setState {
                        copy(currentSessionCrossSigningInfo = crossSigningInfo)
                    }
                }
                .launchIn(viewModelScope)
    }

    private fun observeDevices() {
        getDeviceFullInfoListUseCase.execute(
                filterType = DeviceManagerFilterType.ALL_SESSIONS,
                excludeCurrentDevice = false
        )
                .execute { async ->
                    if (async is Success) {
                        val deviceFullInfoList = async.invoke()
                        val unverifiedSessionsCount = deviceFullInfoList.count { !it.cryptoDeviceInfo?.trustLevel?.isCrossSigningVerified().orFalse() }
                        val inactiveSessionsCount = deviceFullInfoList.count { it.isInactive }
                        copy(
                                devices = async,
                                unverifiedSessionsCount = unverifiedSessionsCount,
                                inactiveSessionsCount = inactiveSessionsCount,
                        )
                    } else {
                        copy(
                                devices = async
                        )
                    }
                }
    }

    private fun refreshDevicesOnCryptoDevicesChange() {
        viewModelScope.launch {
            refreshDevicesOnCryptoDevicesChangeUseCase.execute()
        }
    }

    override fun handle(action: DevicesAction) {
        when (action) {
            is DevicesAction.VerifyCurrentSession -> handleVerifyCurrentSessionAction()
            is DevicesAction.MarkAsManuallyVerified -> handleMarkAsManuallyVerifiedAction()
        }
    }

    private fun handleVerifyCurrentSessionAction() {
        viewModelScope.launch {
            val currentSessionCanBeVerified = checkIfCurrentSessionCanBeVerifiedUseCase.execute()
            if (currentSessionCanBeVerified) {
                _viewEvents.post(DevicesViewEvent.SelfVerification)
            } else {
                _viewEvents.post(DevicesViewEvent.PromptResetSecrets)
            }
        }
    }

    private fun handleMarkAsManuallyVerifiedAction() {
        // TODO implement when needed
    }
}
