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

package im.vector.riotx.features.settings.devices

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
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.extensions.tryThis
import im.vector.matrix.android.api.failure.Failure
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.crypto.verification.VerificationMethod
import im.vector.matrix.android.api.session.crypto.verification.VerificationService
import im.vector.matrix.android.api.session.crypto.verification.VerificationTransaction
import im.vector.matrix.android.api.session.crypto.verification.VerificationTxState
import im.vector.matrix.android.internal.auth.data.LoginFlowTypes
import im.vector.matrix.android.internal.crypto.crosssigning.DeviceTrustLevel
import im.vector.matrix.android.internal.crypto.model.CryptoDeviceInfo
import im.vector.matrix.android.internal.crypto.model.MXUsersDevicesMap
import im.vector.matrix.android.internal.crypto.model.rest.DeviceInfo
import im.vector.matrix.android.internal.crypto.model.rest.DevicesListResponse
import im.vector.matrix.android.internal.util.awaitCallback
import im.vector.matrix.rx.rx
import im.vector.riotx.core.platform.VectorViewModel
import im.vector.riotx.features.crypto.verification.SupportedVerificationMethodsProvider
import kotlinx.coroutines.launch

data class DevicesViewState(
        val myDeviceId: String = "",
        val devices: Async<List<DeviceInfo>> = Uninitialized,
        val cryptoDevices: Async<List<CryptoDeviceInfo>> = Uninitialized,
        // TODO Replace by isLoading boolean
        val request: Async<Unit> = Uninitialized,
        val hasAccountCrossSigning: Boolean = false,
        val accountCrossSigningIsTrusted: Boolean = false
) : MvRxState

class DevicesViewModel @AssistedInject constructor(
        @Assisted initialState: DevicesViewState,
        private val session: Session,
        private val supportedVerificationMethodsProvider: SupportedVerificationMethodsProvider)
    : VectorViewModel<DevicesViewState, DevicesAction, DevicesViewEvents>(initialState), VerificationService.Listener {

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

    init {

        setState {
            copy(
                    hasAccountCrossSigning = session.cryptoService().crossSigningService().getMyCrossSigningKeys() != null,
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

        refreshDevicesList()
        session.cryptoService().verificationService().addListener(this)

        session.rx().liveUserCryptoDevices(session.myUserId)
                .execute {
                    copy(
                            cryptoDevices = it
                    )
                }
    }

    override fun onCleared() {
        session.cryptoService().verificationService().removeListener(this)
        super.onCleared()
    }

    override fun transactionUpdated(tx: VerificationTransaction) {
        if (tx.state == VerificationTxState.Verified) {
            refreshDevicesList()
        }
    }

    /**
     * Force the refresh of the devices list.
     * The devices list is the list of the devices where the user is logged in.
     * It can be any mobile devices, and any browsers.
     */
    private fun refreshDevicesList() {
        if (!session.sessionParams.credentials.deviceId.isNullOrEmpty()) {
            // display something asap
            val localKnown = session.cryptoService().getUserDevices(session.myUserId).map {
                DeviceInfo(
                        user_id = session.myUserId,
                        deviceId = it.deviceId,
                        displayName = it.displayName()
                )
            }

            setState {
                copy(
                        // Keep known list if we have it, and let refresh go in backgroung
                        devices = this.devices.takeIf { it is Success } ?: Success(localKnown)
                )
            }

            session.cryptoService().getDevicesList(object : MatrixCallback<DevicesListResponse> {
                override fun onSuccess(data: DevicesListResponse) {
                    setState {
                        copy(
                                myDeviceId = session.sessionParams.credentials.deviceId ?: "",
                                devices = Success(data.devices.orEmpty())
                        )
                    }
                }

                override fun onFailure(failure: Throwable) {
                    setState {
                        copy(
                                devices = Fail(failure)
                        )
                    }
                }
            })

            // Put cached state
            setState {
                copy(
                        myDeviceId = session.sessionParams.credentials.deviceId ?: "",
                        cryptoDevices = Success(session.cryptoService().getUserDevices(session.myUserId))
                )
            }

            // then force download
            session.cryptoService().downloadKeys(listOf(session.myUserId), true, object : MatrixCallback<MXUsersDevicesMap<CryptoDeviceInfo>> {
                override fun onSuccess(data: MXUsersDevicesMap<CryptoDeviceInfo>) {
                    setState {
                        copy(
                                cryptoDevices = Success(session.cryptoService().getUserDevices(session.myUserId))
                        )
                    }
                }
            })
        } else {
            // Should not happen
        }
    }

    override fun handle(action: DevicesAction) {
        return when (action) {
            is DevicesAction.Retry                  -> refreshDevicesList()
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
        state.cryptoDevices.invoke()
                ?.firstOrNull { it.deviceId == action.deviceId }
                ?.let {
                    _viewEvents.post(DevicesViewEvents.ShowManuallyVerify(it))
                }
    }

    private fun handleVerifyManually(action: DevicesAction.MarkAsManuallyVerified) = withState { state ->
        viewModelScope.launch {
            if (state.hasAccountCrossSigning) {
                awaitCallback<Unit> {
                    tryThis { session.cryptoService().crossSigningService().trustDevice(action.cryptoDeviceInfo.deviceId, it) }
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
        val info = state.devices.invoke()?.firstOrNull { it.deviceId == action.deviceId }
        if (info != null) {
            _viewEvents.post(DevicesViewEvents.PromptRenameDevice(info))
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
                refreshDevicesList()
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
                refreshDevicesList()
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
                refreshDevicesList()
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
