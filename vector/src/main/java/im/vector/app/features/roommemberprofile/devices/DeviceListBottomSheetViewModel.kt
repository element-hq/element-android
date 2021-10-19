/*
 * Copyright 2020 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package im.vector.app.features.roommemberprofile.devices

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.HasScreenInjector
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.platform.VectorViewModel
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.crosssigning.MXCrossSigningInfo
import org.matrix.android.sdk.api.session.crypto.verification.VerificationMethod
import org.matrix.android.sdk.api.util.MatrixItem
import org.matrix.android.sdk.api.util.toMatrixItem
import org.matrix.android.sdk.flow.flow
import org.matrix.android.sdk.internal.crypto.model.CryptoDeviceInfo

data class DeviceListViewState(
        val userItem: MatrixItem? = null,
        val isMine: Boolean = false,
        val memberCrossSigningKey: MXCrossSigningInfo? = null,
        val cryptoDevices: Async<List<CryptoDeviceInfo>> = Loading(),
        val selectedDevice: CryptoDeviceInfo? = null
) : MavericksState

class DeviceListBottomSheetViewModel @AssistedInject constructor(@Assisted private val initialState: DeviceListViewState,
                                                                 @Assisted private val args: DeviceListBottomSheet.Args,
                                                                 private val session: Session) :
    VectorViewModel<DeviceListViewState, DeviceListAction, DeviceListBottomSheetViewEvents>(initialState) {

    @AssistedFactory
    interface Factory {
        fun create(initialState: DeviceListViewState, args: DeviceListBottomSheet.Args): DeviceListBottomSheetViewModel
    }

    init {
        session.flow().liveUserCryptoDevices(args.userId)
                .execute {
                    copy(cryptoDevices = it).also {
                        refreshSelectedId()
                    }
                }

        session.flow().liveCrossSigningInfo(args.userId)
                .execute {
                    copy(memberCrossSigningKey = it.invoke()?.getOrNull())
                }
    }

    override fun handle(action: DeviceListAction) {
        when (action) {
            is DeviceListAction.SelectDevice   -> selectDevice(action)
            is DeviceListAction.DeselectDevice -> deselectDevice()
            is DeviceListAction.ManuallyVerify -> manuallyVerify(action)
        }.exhaustive
    }

    private fun refreshSelectedId() = withState { state ->
        if (state.selectedDevice != null) {
            state.cryptoDevices.invoke()?.firstOrNull { state.selectedDevice.deviceId == it.deviceId }?.let {
                setState {
                    copy(
                            selectedDevice = it
                    )
                }
            }
        }
    }

    private fun selectDevice(action: DeviceListAction.SelectDevice) {
        if (!args.allowDeviceAction) return
        setState {
            copy(selectedDevice = action.device)
        }
    }

    private fun deselectDevice() {
        setState {
            copy(selectedDevice = null)
        }
    }

    private fun manuallyVerify(action: DeviceListAction.ManuallyVerify) {
        if (!args.allowDeviceAction) return
        session.cryptoService().verificationService().beginKeyVerification(VerificationMethod.SAS, args.userId, action.deviceId, null)?.let { txID ->
            _viewEvents.post(DeviceListBottomSheetViewEvents.Verify(args.userId, txID))
        }
    }

    companion object : MavericksViewModelFactory<DeviceListBottomSheetViewModel, DeviceListViewState> {
        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: DeviceListViewState): DeviceListBottomSheetViewModel? {
            val fragment: DeviceListBottomSheet = (viewModelContext as FragmentViewModelContext).fragment()
            val args = viewModelContext.args<DeviceListBottomSheet.Args>()
            return fragment.viewModelFactory.create(state, args)
        }

        override fun initialState(viewModelContext: ViewModelContext): DeviceListViewState? {
            val userId = viewModelContext.args<DeviceListBottomSheet.Args>().userId
            val session = (viewModelContext.activity as HasScreenInjector).injector().activeSessionHolder().getActiveSession()
            return session.getUser(userId)?.toMatrixItem()?.let {
                DeviceListViewState(
                        userItem = it,
                        isMine = userId == session.myUserId
                )
            } ?: return super.initialState(viewModelContext)
        }
    }
}
