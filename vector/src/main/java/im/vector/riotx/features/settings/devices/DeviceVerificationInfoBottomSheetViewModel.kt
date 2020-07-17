/*
 * Copyright 2020 New Vector Ltd
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
package im.vector.riotx.features.settings.devices

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.internal.crypto.model.CryptoDeviceInfo
import im.vector.matrix.android.internal.crypto.model.rest.DeviceInfo
import im.vector.matrix.rx.rx
import im.vector.riotx.core.platform.EmptyAction
import im.vector.riotx.core.platform.EmptyViewEvents
import im.vector.riotx.core.platform.VectorViewModel

data class DeviceVerificationInfoBottomSheetViewState(
        val cryptoDeviceInfo: Async<CryptoDeviceInfo?> = Uninitialized,
        val deviceInfo: Async<DeviceInfo> = Uninitialized,
        val hasAccountCrossSigning: Boolean = false,
        val accountCrossSigningIsTrusted: Boolean = false,
        val isMine: Boolean = false
) : MvRxState

class DeviceVerificationInfoBottomSheetViewModel @AssistedInject constructor(@Assisted initialState: DeviceVerificationInfoBottomSheetViewState,
                                                                             @Assisted val deviceId: String,
                                                                             val session: Session
) : VectorViewModel<DeviceVerificationInfoBottomSheetViewState, EmptyAction, EmptyViewEvents>(initialState) {

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: DeviceVerificationInfoBottomSheetViewState, deviceId: String): DeviceVerificationInfoBottomSheetViewModel
    }

    init {

        setState {
            copy(
                    hasAccountCrossSigning = session.cryptoService().crossSigningService().isCrossSigningInitialized(),
                    accountCrossSigningIsTrusted = session.cryptoService().crossSigningService().isCrossSigningVerified()
            )
        }
        session.rx().liveCrossSigningInfo(session.myUserId)
                .execute {
                    copy(
                            hasAccountCrossSigning = it.invoke()?.getOrNull() != null,
                            accountCrossSigningIsTrusted = it.invoke()?.getOrNull()?.isTrusted() == true
                    )
                }

        session.rx().liveUserCryptoDevices(session.myUserId)
                .map { list ->
                    list.firstOrNull { it.deviceId == deviceId }
                }
                .execute {
                    copy(
                            cryptoDeviceInfo = it,
                            isMine = it.invoke()?.deviceId == session.sessionParams.deviceId
                    )
                }

        setState {
            copy(deviceInfo = Loading())
        }

        session.rx().liveMyDeviceInfo()
                .map { devices ->
                    devices.firstOrNull { it.deviceId == deviceId } ?: DeviceInfo(deviceId = deviceId)
                }
                .execute {
                    copy(deviceInfo = it)
                }
    }

    companion object : MvRxViewModelFactory<DeviceVerificationInfoBottomSheetViewModel, DeviceVerificationInfoBottomSheetViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: DeviceVerificationInfoBottomSheetViewState)
                : DeviceVerificationInfoBottomSheetViewModel? {
            val fragment: DeviceVerificationInfoBottomSheet = (viewModelContext as FragmentViewModelContext).fragment()
            val args = viewModelContext.args<DeviceVerificationInfoArgs>()
            return fragment.deviceVerificationInfoViewModelFactory.create(state, args.deviceId)
        }
    }

    override fun handle(action: EmptyAction) {
    }
}
