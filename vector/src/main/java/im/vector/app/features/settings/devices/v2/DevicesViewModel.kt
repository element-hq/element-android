/*
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
                        val unverifiedSessionsCount = deviceFullInfoList.count { !it.cryptoDeviceInfo?.isVerified.orFalse() }
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
            is DevicesAction.MarkAsManuallyVerified -> handleMarkAsManuallyVerifiedAction()
        }
    }

    private fun handleMarkAsManuallyVerifiedAction() {
        // TODO implement when needed
    }
}
