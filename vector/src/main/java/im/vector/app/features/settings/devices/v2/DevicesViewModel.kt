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
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.utils.PublishDataSource
import im.vector.lib.core.utils.flow.throttleFirst
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.crypto.verification.VerificationService
import org.matrix.android.sdk.api.session.crypto.verification.VerificationTransaction
import org.matrix.android.sdk.api.session.crypto.verification.VerificationTxState
import kotlin.time.Duration.Companion.seconds

class DevicesViewModel @AssistedInject constructor(
        @Assisted initialState: DevicesViewState,
        private val activeSessionHolder: ActiveSessionHolder,
        private val getCurrentSessionCrossSigningInfoUseCase: GetCurrentSessionCrossSigningInfoUseCase,
        private val getDeviceFullInfoListUseCase: GetDeviceFullInfoListUseCase,
        private val refreshDevicesUseCase: RefreshDevicesUseCase,
        private val refreshDevicesOnCryptoDevicesChangeUseCase: RefreshDevicesOnCryptoDevicesChangeUseCase,
) : VectorViewModel<DevicesViewState, DevicesAction, DevicesViewEvent>(initialState), VerificationService.Listener {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<DevicesViewModel, DevicesViewState> {
        override fun create(initialState: DevicesViewState): DevicesViewModel
    }

    companion object : MavericksViewModelFactory<DevicesViewModel, DevicesViewState> by hiltMavericksViewModelFactory()

    private val refreshSource = PublishDataSource<Unit>()
    private val refreshThrottleDelayMs = 4.seconds.inWholeMilliseconds

    init {
        addVerificationListener()
        observeCurrentSessionCrossSigningInfo()
        observeDevices()
        observeRefreshSource()
        refreshDevicesOnCryptoDevicesChange()
        queryRefreshDevicesList()
    }

    override fun onCleared() {
        removeVerificationListener()
        super.onCleared()
    }

    private fun addVerificationListener() {
        activeSessionHolder.getSafeActiveSession()
                ?.cryptoService()
                ?.verificationService()
                ?.addListener(this)
    }

    private fun removeVerificationListener() {
        activeSessionHolder.getSafeActiveSession()
                ?.cryptoService()
                ?.verificationService()
                ?.removeListener(this)
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
        getDeviceFullInfoListUseCase.execute()
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

    private fun observeRefreshSource() {
        refreshSource.stream()
                .throttleFirst(refreshThrottleDelayMs)
                .onEach { refreshDevicesUseCase.execute() }
                .launchIn(viewModelScope)
    }

    override fun transactionUpdated(tx: VerificationTransaction) {
        if (tx.state == VerificationTxState.Verified) {
            queryRefreshDevicesList()
        }
    }

    /**
     * Force the refresh of the devices list.
     * The devices list is the list of the devices where the user is logged in.
     * It can be any mobile devices, and any browsers.
     */
    private fun queryRefreshDevicesList() {
        refreshSource.post(Unit)
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
