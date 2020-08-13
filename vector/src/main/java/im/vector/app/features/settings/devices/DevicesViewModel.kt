/*
 * Copyright 2019 New Vector Ltd
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

import androidx.lifecycle.viewModelScope
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.NoOpMatrixCallback
import org.matrix.android.sdk.api.auth.data.LoginFlowTypes
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.verification.VerificationMethod
import org.matrix.android.sdk.api.session.crypto.verification.VerificationService
import org.matrix.android.sdk.api.session.crypto.verification.VerificationTransaction
import org.matrix.android.sdk.api.session.crypto.verification.VerificationTxState
import org.matrix.android.sdk.internal.crypto.crosssigning.DeviceTrustLevel
import org.matrix.android.sdk.internal.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.internal.crypto.model.rest.DeviceInfo
import org.matrix.android.sdk.internal.util.awaitCallback
import im.vector.app.core.platform.VectorViewModel
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.launch
import org.matrix.android.sdk.rx.rx
import timber.log.Timber
import java.util.concurrent.TimeUnit

data class DevicesViewState(
        val myDeviceId: String = "",
//        val devices: Async<List<DeviceInfo>> = Uninitialized,
//        val cryptoDevices: Async<List<CryptoDeviceInfo>> = Uninitialized,
        val devices: Async<List<DeviceFullInfo>> = Uninitialized,
        // TODO Replace by isLoading boolean
        val request: Async<Unit> = Uninitialized,
        val hasAccountCrossSigning: Boolean = false,
        val accountCrossSigningIsTrusted: Boolean = false
) : MvRxState

data class DeviceFullInfo(
        val deviceInfo: DeviceInfo,
        val cryptoDeviceInfo: CryptoDeviceInfo?
)

class DevicesViewModel @AssistedInject constructor(
        @Assisted initialState: DevicesViewState,
        private val session: Session
) : VectorViewModel<DevicesViewState, DevicesAction, DevicesViewEvents>(initialState), VerificationService.Listener {

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: DevicesViewState): DevicesViewModel
    }

    companion object : MvRxViewModelFactory<DevicesViewModel, DevicesViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: DevicesViewState): DevicesViewModel? {
            val fragment: VectorSettingsDevicesFragment = (viewModelContext as FragmentViewModelContext).fragment()
            return fragment.devicesViewModelFactory.create(state)
        }
    }

    // temp storage when we ask for the user password
    private var _currentDeviceId: String? = null
    private var _currentSession: String? = null

    private val refreshPublisher: PublishSubject<Unit> = PublishSubject.create()

    init {

        setState {
            copy(
                    hasAccountCrossSigning = session.cryptoService().crossSigningService().isCrossSigningInitialized(),
                    accountCrossSigningIsTrusted = session.cryptoService().crossSigningService().isCrossSigningVerified(),
                    myDeviceId = session.sessionParams.deviceId ?: ""
            )
        }

        Observable.combineLatest<List<CryptoDeviceInfo>, List<DeviceInfo>, List<DeviceFullInfo>>(
                session.rx().liveUserCryptoDevices(session.myUserId),
                session.rx().liveMyDevicesInfo(),
                BiFunction { cryptoList, infoList ->
                    infoList
                            .sortedByDescending { it.lastSeenTs }
                            .map { deviceInfo ->
                                val cryptoDeviceInfo = cryptoList.firstOrNull { it.deviceId == deviceInfo.deviceId }
                                DeviceFullInfo(deviceInfo, cryptoDeviceInfo)
                            }
                }
        )
                .distinctUntilChanged()
                .execute { async ->
                    copy(
                            devices = async
                    )
                }

        session.rx().liveCrossSigningInfo(session.myUserId)
                .execute {
                    copy(
                            hasAccountCrossSigning = it.invoke()?.getOrNull() != null,
                            accountCrossSigningIsTrusted = it.invoke()?.getOrNull()?.isTrusted() == true
                    )
                }
        session.cryptoService().verificationService().addListener(this)

//        session.rx().liveMyDeviceInfo()
//                .execute {
//                    copy(
//                            devices = it
//                    )
//                }

        session.rx().liveUserCryptoDevices(session.myUserId)
                .map { it.size }
                .distinctUntilChanged()
                .throttleLast(5_000, TimeUnit.MILLISECONDS)
                .subscribe {
                    // If we have a new crypto device change, we might want to trigger refresh of device info
                    session.cryptoService().fetchDevicesList(NoOpMatrixCallback())
                }
                .disposeOnClear()

//        session.rx().liveUserCryptoDevices(session.myUserId)
//                .execute {
//                    copy(
//                            cryptoDevices = it
//                    )
//                }

        refreshPublisher.throttleFirst(4_000, TimeUnit.MILLISECONDS)
                .subscribe {
                    session.cryptoService().fetchDevicesList(NoOpMatrixCallback())
                    session.cryptoService().downloadKeys(listOf(session.myUserId), true, NoOpMatrixCallback())
                }
                .disposeOnClear()
        // then force download
        queryRefreshDevicesList()
    }

    override fun onCleared() {
        session.cryptoService().verificationService().removeListener(this)
        super.onCleared()
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
        refreshPublisher.onNext(Unit)
    }

    override fun handle(action: DevicesAction) {
        return when (action) {
            is DevicesAction.Refresh                -> queryRefreshDevicesList()
            is DevicesAction.Delete                 -> handleDelete(action)
            is DevicesAction.Password               -> handlePassword(action)
            is DevicesAction.Rename                 -> handleRename(action)
            is DevicesAction.PromptRename           -> handlePromptRename(action)
            is DevicesAction.VerifyMyDevice         -> handleInteractiveVerification(action)
            is DevicesAction.CompleteSecurity       -> handleCompleteSecurity()
            is DevicesAction.MarkAsManuallyVerified -> handleVerifyManually(action)
            is DevicesAction.VerifyMyDeviceManually -> handleShowDeviceCryptoInfo(action)
        }
    }

    private fun handleInteractiveVerification(action: DevicesAction.VerifyMyDevice) {
        val txID = session.cryptoService()
                .verificationService()
                .beginKeyVerification(VerificationMethod.SAS, session.myUserId, action.deviceId, null)
        _viewEvents.post(DevicesViewEvents.ShowVerifyDevice(
                session.myUserId,
                txID
        ))
    }

    private fun handleShowDeviceCryptoInfo(action: DevicesAction.VerifyMyDeviceManually) = withState { state ->
        state.devices.invoke()
                ?.firstOrNull { it.cryptoDeviceInfo?.deviceId == action.deviceId }
                ?.let {
                    _viewEvents.post(DevicesViewEvents.ShowManuallyVerify(it.cryptoDeviceInfo!!))
                }
    }

    private fun handleVerifyManually(action: DevicesAction.MarkAsManuallyVerified) = withState { state ->
        viewModelScope.launch {
            if (state.hasAccountCrossSigning) {
                try {
                    awaitCallback<Unit> {
                        session.cryptoService().crossSigningService().trustDevice(action.cryptoDeviceInfo.deviceId, it)
                    }
                } catch (failure: Throwable) {
                    Timber.e("Failed to manually cross sign device ${action.cryptoDeviceInfo.deviceId} : ${failure.localizedMessage}")
                    _viewEvents.post(DevicesViewEvents.Failure(failure))
                }
            } else {
                // legacy
                session.cryptoService().setDeviceVerification(
                        DeviceTrustLevel(crossSigningVerified = false, locallyVerified = true),
                        action.cryptoDeviceInfo.userId,
                        action.cryptoDeviceInfo.deviceId)
            }
        }
    }

    private fun handleCompleteSecurity() {
        _viewEvents.post(DevicesViewEvents.SelfVerification(session))
    }

    private fun handlePromptRename(action: DevicesAction.PromptRename) = withState { state ->
        val info = state.devices.invoke()?.firstOrNull { it.deviceInfo.deviceId == action.deviceId }
        if (info != null) {
            _viewEvents.post(DevicesViewEvents.PromptRenameDevice(info.deviceInfo))
        }
    }

    private fun handleRename(action: DevicesAction.Rename) {
        session.cryptoService().setDeviceName(action.deviceId, action.newName, object : MatrixCallback<Unit> {
            override fun onSuccess(data: Unit) {
                setState {
                    copy(
                            request = Success(data)
                    )
                }
                // force settings update
                queryRefreshDevicesList()
            }

            override fun onFailure(failure: Throwable) {
                setState {
                    copy(
                            request = Fail(failure)
                    )
                }

                _viewEvents.post(DevicesViewEvents.Failure(failure))
            }
        })
    }

    /**
     * Try to delete a device.
     */
    private fun handleDelete(action: DevicesAction.Delete) {
        val deviceId = action.deviceId

        setState {
            copy(
                    request = Loading()
            )
        }

        session.cryptoService().deleteDevice(deviceId, object : MatrixCallback<Unit> {
            override fun onFailure(failure: Throwable) {
                var isPasswordRequestFound = false

                if (failure is Failure.RegistrationFlowError) {
                    // We only support LoginFlowTypes.PASSWORD
                    // Check if we can provide the user password
                    failure.registrationFlowResponse.flows?.forEach { interactiveAuthenticationFlow ->
                        isPasswordRequestFound = isPasswordRequestFound || interactiveAuthenticationFlow.stages?.any { it == LoginFlowTypes.PASSWORD } == true
                    }

                    if (isPasswordRequestFound) {
                        _currentDeviceId = deviceId
                        _currentSession = failure.registrationFlowResponse.session

                        setState {
                            copy(
                                    request = Success(Unit)
                            )
                        }

                        _viewEvents.post(DevicesViewEvents.RequestPassword)
                    }
                }

                if (!isPasswordRequestFound) {
                    // LoginFlowTypes.PASSWORD not supported, and this is the only one RiotX supports so far...
                    setState {
                        copy(
                                request = Fail(failure)
                        )
                    }

                    _viewEvents.post(DevicesViewEvents.Failure(failure))
                }
            }

            override fun onSuccess(data: Unit) {
                setState {
                    copy(
                            request = Success(data)
                    )
                }
                // force settings update
                queryRefreshDevicesList()
            }
        })
    }

    private fun handlePassword(action: DevicesAction.Password) {
        val currentDeviceId = _currentDeviceId
        if (currentDeviceId.isNullOrBlank()) {
            // Abort
            return
        }

        setState {
            copy(
                    request = Loading()
            )
        }

        session.cryptoService().deleteDeviceWithUserPassword(currentDeviceId, _currentSession, action.password, object : MatrixCallback<Unit> {
            override fun onSuccess(data: Unit) {
                _currentDeviceId = null
                _currentSession = null

                setState {
                    copy(
                            request = Success(data)
                    )
                }
                // force settings update
                queryRefreshDevicesList()
            }

            override fun onFailure(failure: Throwable) {
                _currentDeviceId = null
                _currentSession = null

                // Password is maybe not good
                setState {
                    copy(
                            request = Fail(failure)
                    )
                }

                _viewEvents.post(DevicesViewEvents.Failure(failure))
            }
        })
    }
}
