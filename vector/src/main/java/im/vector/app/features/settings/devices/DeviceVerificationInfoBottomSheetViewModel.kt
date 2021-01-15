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
package im.vector.app.features.settings.devices

import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.assisted.AssistedFactory
import im.vector.app.core.platform.EmptyAction
import im.vector.app.core.platform.EmptyViewEvents
import im.vector.app.core.platform.VectorViewModel
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.internal.crypto.model.rest.DeviceInfo
import org.matrix.android.sdk.rx.rx

class DeviceVerificationInfoBottomSheetViewModel @AssistedInject constructor(@Assisted initialState: DeviceVerificationInfoBottomSheetViewState,
                                                                             @Assisted val deviceId: String,
                                                                             val session: Session
) : VectorViewModel<DeviceVerificationInfoBottomSheetViewState, EmptyAction, EmptyViewEvents>(initialState) {

    @AssistedFactory
    interface Factory {
        fun create(initialState: DeviceVerificationInfoBottomSheetViewState, deviceId: String): DeviceVerificationInfoBottomSheetViewModel
    }

    init {

        setState {
            copy(
                    hasAccountCrossSigning = session.cryptoService().crossSigningService().isCrossSigningInitialized(),
                    accountCrossSigningIsTrusted = session.cryptoService().crossSigningService().isCrossSigningVerified(),
                    isRecoverySetup = session.sharedSecretStorageService.isRecoverySetup()
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

        session.rx().liveUserCryptoDevices(session.myUserId)
                .map { it.size }
                .execute {
                    copy(
                            hasOtherSessions = it.invoke() ?: 0 > 1
                    )
                }

        setState {
            copy(deviceInfo = Loading())
        }

        session.rx().liveMyDevicesInfo()
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
