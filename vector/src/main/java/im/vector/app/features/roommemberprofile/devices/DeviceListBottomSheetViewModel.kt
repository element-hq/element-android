/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.crosssigning.MXCrossSigningInfo
import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.api.session.getUserOrDefault
import org.matrix.android.sdk.api.util.MatrixItem
import org.matrix.android.sdk.api.util.toMatrixItem
import org.matrix.android.sdk.flow.flow

data class DeviceListViewState(
        val userId: String,
        val myUserId: String,
        val userItem: MatrixItem? = null,
        val memberCrossSigningKey: MXCrossSigningInfo? = null,
        val myDeviceId: String = "",
        val cryptoDevices: Async<List<CryptoDeviceInfo>> = Loading(),
        val selectedDevice: CryptoDeviceInfo? = null
) : MavericksState

class DeviceListBottomSheetViewModel @AssistedInject constructor(
        @Assisted private val initialState: DeviceListViewState,
        private val session: Session
) :
        VectorViewModel<DeviceListViewState, DeviceListAction, DeviceListBottomSheetViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<DeviceListBottomSheetViewModel, DeviceListViewState> {
        override fun create(initialState: DeviceListViewState): DeviceListBottomSheetViewModel
    }

    companion object : MavericksViewModelFactory<DeviceListBottomSheetViewModel, DeviceListViewState> by hiltMavericksViewModelFactory() {

        override fun initialState(viewModelContext: ViewModelContext): DeviceListViewState {
            val args = viewModelContext.args<DeviceListBottomSheet.Args>()
            val userId = args.userId
            val session = EntryPoints.get(viewModelContext.app(), SingletonEntryPoint::class.java).activeSessionHolder().getActiveSession()
            return DeviceListViewState(
                    userId = userId,
                    myUserId = session.myUserId,
                    userItem = session.getUserOrDefault(userId).toMatrixItem(),
                    myDeviceId = session.sessionParams.deviceId,
            )
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

        updateMatrixItem()
    }

    private fun updateMatrixItem() {
        viewModelScope.launch {
            tryOrNull { session.userService().resolveUser(initialState.userId) }
                    ?.toMatrixItem()
                    ?.let { setState { copy(userItem = it) } }
        }
    }

    override fun handle(action: DeviceListAction) {
        when (action) {
            is DeviceListAction.SelectDevice -> selectDevice(action)
            is DeviceListAction.DeselectDevice -> deselectDevice()
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
        setState {
            copy(selectedDevice = action.device)
        }
    }

    private fun deselectDevice() {
        setState {
            copy(selectedDevice = null)
        }
    }
}
