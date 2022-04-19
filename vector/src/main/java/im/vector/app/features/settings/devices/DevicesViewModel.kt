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

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.R
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.utils.PublishDataSource
import im.vector.app.features.auth.ReAuthActivity
import im.vector.app.features.login.ReAuthHelper
import im.vector.lib.core.utils.flow.throttleFirst
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.NoOpMatrixCallback
import org.matrix.android.sdk.api.auth.UIABaseAuth
import org.matrix.android.sdk.api.auth.UserInteractiveAuthInterceptor
import org.matrix.android.sdk.api.auth.UserPasswordAuth
import org.matrix.android.sdk.api.auth.data.LoginFlowTypes
import org.matrix.android.sdk.api.auth.registration.RegistrationFlowResponse
import org.matrix.android.sdk.api.auth.registration.nextUncompletedStage
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.crosssigning.DeviceTrustLevel
import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.api.session.crypto.model.DeviceInfo
import org.matrix.android.sdk.api.session.crypto.verification.VerificationMethod
import org.matrix.android.sdk.api.session.crypto.verification.VerificationService
import org.matrix.android.sdk.api.session.crypto.verification.VerificationTransaction
import org.matrix.android.sdk.api.session.crypto.verification.VerificationTxState
import org.matrix.android.sdk.api.session.uia.DefaultBaseAuth
import org.matrix.android.sdk.api.util.awaitCallback
import org.matrix.android.sdk.api.util.fromBase64
import org.matrix.android.sdk.flow.flow
import timber.log.Timber
import javax.net.ssl.HttpsURLConnection
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class DevicesViewState(
        val myDeviceId: String = "",
//        val devices: Async<List<DeviceInfo>> = Uninitialized,
//        val cryptoDevices: Async<List<CryptoDeviceInfo>> = Uninitialized,
        val devices: Async<List<DeviceFullInfo>> = Uninitialized,
        // TODO Replace by isLoading boolean
        val request: Async<Unit> = Uninitialized,
        val hasAccountCrossSigning: Boolean = false,
        val accountCrossSigningIsTrusted: Boolean = false
) : MavericksState

data class DeviceFullInfo(
        val deviceInfo: DeviceInfo,
        val cryptoDeviceInfo: CryptoDeviceInfo?
)

class DevicesViewModel @AssistedInject constructor(
        @Assisted initialState: DevicesViewState,
        private val session: Session,
        private val reAuthHelper: ReAuthHelper,
        private val stringProvider: StringProvider
) : VectorViewModel<DevicesViewState, DevicesAction, DevicesViewEvents>(initialState), VerificationService.Listener {

    var uiaContinuation: Continuation<UIABaseAuth>? = null
    var pendingAuth: UIABaseAuth? = null

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<DevicesViewModel, DevicesViewState> {
        override fun create(initialState: DevicesViewState): DevicesViewModel
    }

    companion object : MavericksViewModelFactory<DevicesViewModel, DevicesViewState> by hiltMavericksViewModelFactory()

    private val refreshSource = PublishDataSource<Unit>()

    init {

        setState {
            copy(
                    hasAccountCrossSigning = session.cryptoService().crossSigningService().isCrossSigningInitialized(),
                    accountCrossSigningIsTrusted = session.cryptoService().crossSigningService().isCrossSigningVerified(),
                    myDeviceId = session.sessionParams.deviceId ?: ""
            )
        }

        combine(
                session.flow().liveUserCryptoDevices(session.myUserId),
                session.flow().liveMyDevicesInfo()
        ) { cryptoList, infoList ->
            infoList
                    .sortedByDescending { it.lastSeenTs }
                    .map { deviceInfo ->
                        val cryptoDeviceInfo = cryptoList.firstOrNull { it.deviceId == deviceInfo.deviceId }
                        DeviceFullInfo(deviceInfo, cryptoDeviceInfo)
                    }
        }
                .distinctUntilChanged()
                .execute { async ->
                    copy(
                            devices = async
                    )
                }

        session.flow().liveCrossSigningInfo(session.myUserId)
                .execute {
                    copy(
                            hasAccountCrossSigning = it.invoke()?.getOrNull() != null,
                            accountCrossSigningIsTrusted = it.invoke()?.getOrNull()?.isTrusted() == true
                    )
                }
        session.cryptoService().verificationService().addListener(this)

//        session.flow().liveMyDeviceInfo()
//                .execute {
//                    copy(
//                            devices = it
//                    )
//                }

        session.flow().liveUserCryptoDevices(session.myUserId)
                .map { it.size }
                .distinctUntilChanged()
                .sample(5_000)
                .onEach {
                    // If we have a new crypto device change, we might want to trigger refresh of device info
                    session.cryptoService().fetchDevicesList(NoOpMatrixCallback())
                }
                .launchIn(viewModelScope)

//        session.flow().liveUserCryptoDevices(session.myUserId)
//                .execute {
//                    copy(
//                            cryptoDevices = it
//                    )
//                }

        refreshSource.stream().throttleFirst(4_000)
                .onEach {
                    session.cryptoService().fetchDevicesList(NoOpMatrixCallback())
                    session.cryptoService().downloadKeys(listOf(session.myUserId), true, NoOpMatrixCallback())
                }
                .launchIn(viewModelScope)
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
        refreshSource.post(Unit)
    }

    override fun handle(action: DevicesAction) {
        return when (action) {
            is DevicesAction.Refresh                -> queryRefreshDevicesList()
            is DevicesAction.Delete                 -> handleDelete(action)
            is DevicesAction.Rename                 -> handleRename(action)
            is DevicesAction.PromptRename           -> handlePromptRename(action)
            is DevicesAction.VerifyMyDevice         -> handleInteractiveVerification(action)
            is DevicesAction.CompleteSecurity       -> handleCompleteSecurity()
            is DevicesAction.MarkAsManuallyVerified -> handleVerifyManually(action)
            is DevicesAction.VerifyMyDeviceManually -> handleShowDeviceCryptoInfo(action)
            is DevicesAction.SsoAuthDone            -> {
                // we should use token based auth
                // _viewEvents.post(CrossSigningSettingsViewEvents.ShowModalWaitingView(null))
                // will release the interactive auth interceptor
                Timber.d("## UIA - FallBack success $pendingAuth , continuation: $uiaContinuation")
                if (pendingAuth != null) {
                    uiaContinuation?.resume(pendingAuth!!)
                } else {
                    uiaContinuation?.resumeWithException(IllegalArgumentException())
                }
                Unit
            }
            is DevicesAction.PasswordAuthDone       -> {
                val decryptedPass = session.loadSecureSecret<String>(action.password.fromBase64().inputStream(), ReAuthActivity.DEFAULT_RESULT_KEYSTORE_ALIAS)
                uiaContinuation?.resume(
                        UserPasswordAuth(
                                session = pendingAuth?.session,
                                password = decryptedPass,
                                user = session.myUserId
                        )
                )
                Unit
            }
            DevicesAction.ReAuthCancelled           -> {
                Timber.d("## UIA - Reauth cancelled")
//                _viewEvents.post(DevicesViewEvents.Loading)
                uiaContinuation?.resumeWithException(Exception())
                uiaContinuation = null
                pendingAuth = null
            }
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

        viewModelScope.launch(Dispatchers.IO) {
            try {
                awaitCallback<Unit> {
                    session.cryptoService().deleteDevice(deviceId, object : UserInteractiveAuthInterceptor {
                        override fun performStage(flowResponse: RegistrationFlowResponse, errCode: String?, promise: Continuation<UIABaseAuth>) {
                            Timber.d("## UIA : deleteDevice UIA")
                            if (flowResponse.nextUncompletedStage() == LoginFlowTypes.PASSWORD && reAuthHelper.data != null && errCode == null) {
                                UserPasswordAuth(
                                        session = null,
                                        user = session.myUserId,
                                        password = reAuthHelper.data
                                ).let { promise.resume(it) }
                            } else {
                                Timber.d("## UIA : deleteDevice UIA > start reauth activity")
                                _viewEvents.post(DevicesViewEvents.RequestReAuth(flowResponse, errCode))
                                pendingAuth = DefaultBaseAuth(session = flowResponse.session)
                                uiaContinuation = promise
                            }
                        }
                    }, it)
                }
                setState {
                    copy(
                            request = Success(Unit)
                    )
                }
                // force settings update
                queryRefreshDevicesList()
            } catch (failure: Throwable) {
                setState {
                    copy(
                            request = Fail(failure)
                    )
                }
                if (failure is Failure.OtherServerError && failure.httpCode == HttpsURLConnection.HTTP_UNAUTHORIZED) {
                    _viewEvents.post(DevicesViewEvents.Failure(Exception(stringProvider.getString(R.string.authentication_error))))
                } else {
                    _viewEvents.post(DevicesViewEvents.Failure(Exception(stringProvider.getString(R.string.matrix_error))))
                }
                // ...
                Timber.e(failure, "failed to delete session")
            }
        }
    }
}
