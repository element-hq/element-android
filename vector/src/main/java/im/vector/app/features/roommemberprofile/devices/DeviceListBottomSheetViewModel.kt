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
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.EntryPoints
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.SingletonEntryPoint
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorViewModel
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.crosssigning.MXCrossSigningInfo
import org.matrix.android.sdk.api.session.crypto.verification.VerificationMethod
import org.matrix.android.sdk.api.util.MatrixItem
import org.matrix.android.sdk.api.util.toMatrixItem
import org.matrix.android.sdk.flow.flow
import org.matrix.android.sdk.internal.crypto.model.CryptoDeviceInfo

data class DeviceListViewState(
        val userId: String,
        val allowDeviceAction: Boolean,
        val userItem: MatrixItem? = null,
        val isMine: Boolean = false,
        val memberCrossSigningKey: MXCrossSigningInfo? = null,
        val cryptoDevices: Async<List<CryptoDeviceInfo>> = Loading(),
        val selectedDevice: CryptoDeviceInfo? = null
) : MavericksState

class DeviceListBottomSheetViewModel @AssistedInject constructor(@Assisted private val initialState: DeviceListViewState,
                                                                 private val session: Session) :
        VectorViewModel<DeviceListViewState, DeviceListAction, DeviceListBottomSheetViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<DeviceListBottomSheetViewModel, DeviceListViewState> {
        override fun create(initialState: DeviceListViewState): DeviceListBottomSheetViewModel
    }

    companion object : MavericksViewModelFactory<DeviceListBottomSheetViewModel, DeviceListViewState> by hiltMavericksViewModelFactory() {

        override fun initialState(viewModelContext: ViewModelContext): DeviceListViewState? {
            val args = viewModelContext.args<DeviceListBottomSheet.Args>()
            val userId = args.userId
            val session = EntryPoints.get(viewModelContext.app(), SingletonEntryPoint::class.java).activeSessionHolder().getActiveSession()
            return session.getUser(userId)?.toMatrixItem()?.let {
                DeviceListViewState(
                        userId = userId,
                        allowDeviceAction = args.allowDeviceAction,
                        userItem = it,
                        isMine = userId == session.myUserId
                )
            } ?: return super.initialState(viewModelContext)
        }
    }

    init {

        session.flow().liveUserCryptoDevices(initialState.userId)
                .execute {
                    copy(cryptoDevices = it).also {
                        refreshSelectedId()
                    }
                }

        session.flow().liveCrossSigningInfo(initialState.userId)
                .execute {
                    copy(memberCrossSigningKey = it.invoke()?.getOrNull())
                }
    }

    override fun handle(action: DeviceListAction) {
        when (action) {
            is DeviceListAction.SelectDevice   -> selectDevice(action)
            is DeviceListAction.DeselectDevice -> deselectDevice()
            is DeviceListAction.ManuallyVerify -> manuallyVerify(action)
        }
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
        if (!initialState.allowDeviceAction) return
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
        if (!initialState.allowDeviceAction) return
        session.cryptoService().verificationService().beginKeyVerification(VerificationMethod.SAS, initialState.userId, action.deviceId, null)?.let { txID ->
            _viewEvents.post(DeviceListBottomSheetViewEvents.Verify(initialState.userId, txID))
        }
    }
}
