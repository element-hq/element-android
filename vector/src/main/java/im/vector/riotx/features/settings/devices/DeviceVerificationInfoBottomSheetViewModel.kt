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
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.internal.crypto.model.CryptoDeviceInfo
import im.vector.riotx.core.di.HasScreenInjector
import im.vector.riotx.core.platform.EmptyAction
import im.vector.riotx.core.platform.VectorViewModel

data class DeviceVerificationInfoBottomSheetViewState(
        val cryptoDeviceInfo: Async<CryptoDeviceInfo> = Uninitialized
) : MvRxState

class DeviceVerificationInfoBottomSheetViewModel @AssistedInject constructor(@Assisted initialState: DeviceVerificationInfoBottomSheetViewState,
                                                                             val session: Session
) : VectorViewModel<DeviceVerificationInfoBottomSheetViewState, EmptyAction>(initialState) {

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: DeviceVerificationInfoBottomSheetViewState): DeviceVerificationInfoBottomSheetViewModel
    }

    companion object : MvRxViewModelFactory<DeviceVerificationInfoBottomSheetViewModel, DeviceVerificationInfoBottomSheetViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: DeviceVerificationInfoBottomSheetViewState): DeviceVerificationInfoBottomSheetViewModel? {
            val fragment: DeviceVerificationInfoBottomSheet = (viewModelContext as FragmentViewModelContext).fragment()
            return fragment.deviceVerificationInfoViewModelFactory.create(state)
        }

        override fun initialState(viewModelContext: ViewModelContext): DeviceVerificationInfoBottomSheetViewState? {
            val session = (viewModelContext.activity as HasScreenInjector).injector().activeSessionHolder().getActiveSession()
            val args = viewModelContext.args<DeviceVerificationInfoArgs>()
            session.getDeviceInfo(args.userId, args.deviceId)?.let {
                return DeviceVerificationInfoBottomSheetViewState(cryptoDeviceInfo = Success(it))
            }
            return super.initialState(viewModelContext)
        }
    }

    override fun handle(action: EmptyAction) {
    }
}
