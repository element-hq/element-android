/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.utils.PublishDataSource
import im.vector.app.features.auth.PendingAuthHandler
import im.vector.app.features.crypto.verification.SupportedVerificationMethodsProvider
import im.vector.app.features.login.ReAuthHelper
import im.vector.app.features.settings.devices.v2.list.CheckIfSessionIsInactiveUseCase
import im.vector.app.features.settings.devices.v2.verification.GetEncryptionTrustLevelForDeviceUseCase
import im.vector.lib.core.utils.flow.throttleFirst
import im.vector.lib.strings.CommonStrings
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.auth.UIABaseAuth
import org.matrix.android.sdk.api.auth.UserInteractiveAuthInterceptor
import org.matrix.android.sdk.api.auth.UserPasswordAuth
import org.matrix.android.sdk.api.auth.data.LoginFlowTypes
import org.matrix.android.sdk.api.auth.registration.RegistrationFlowResponse
import org.matrix.android.sdk.api.auth.registration.nextUncompletedStage
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.api.session.crypto.model.DeviceInfo
import org.matrix.android.sdk.api.session.crypto.model.RoomEncryptionTrustLevel
import org.matrix.android.sdk.api.session.crypto.verification.VerificationEvent
import org.matrix.android.sdk.api.session.crypto.verification.VerificationService
import org.matrix.android.sdk.api.session.crypto.verification.VerificationTransaction
import org.matrix.android.sdk.api.session.uia.DefaultBaseAuth
import org.matrix.android.sdk.flow.flow
import timber.log.Timber
import javax.net.ssl.HttpsURLConnection
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

data class DevicesViewState(
        val myDeviceId: String = "",
//        val devices: Async<List<DeviceInfo>> = Uninitialized,
//        val cryptoDevices: Async<List<CryptoDeviceInfo>> = Uninitialized,
        val devices: Async<List<DeviceFullInfo>> = Uninitialized,
        // TODO Replace by isLoading boolean
        val request: Async<Unit> = Uninitialized,
        val hasAccountCrossSigning: Boolean = false,
        val accountCrossSigningIsTrusted: Boolean = false,
        val unverifiedSessionsCount: Int = 0,
        val inactiveSessionsCount: Int = 0,
) : MavericksState

data class DeviceFullInfo(
        val deviceInfo: DeviceInfo,
        val cryptoDeviceInfo: CryptoDeviceInfo?,
        val trustLevelForShield: RoomEncryptionTrustLevel?,
        val isInactive: Boolean,
)

class DevicesViewModel @AssistedInject constructor(
        @Assisted initialState: DevicesViewState,
        private val session: Session,
        private val reAuthHelper: ReAuthHelper,
        private val stringProvider: StringProvider,
        private val pendingAuthHandler: PendingAuthHandler,
        private val checkIfSessionIsInactiveUseCase: CheckIfSessionIsInactiveUseCase,
        getCurrentSessionCrossSigningInfoUseCase: GetCurrentSessionCrossSigningInfoUseCase,
        private val getEncryptionTrustLevelForDeviceUseCase: GetEncryptionTrustLevelForDeviceUseCase,
        private val supportedVerificationMethodsProvider: SupportedVerificationMethodsProvider,
) : VectorViewModel<DevicesViewState, DevicesAction, DevicesViewEvents>(initialState), VerificationService.Listener {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<DevicesViewModel, DevicesViewState> {
        override fun create(initialState: DevicesViewState): DevicesViewModel
    }

    companion object : MavericksViewModelFactory<DevicesViewModel, DevicesViewState> by hiltMavericksViewModelFactory()

    private val refreshSource = PublishDataSource<Unit>()

    init {
        initState()
        viewModelScope.launch {
            val currentSessionCrossSigningInfo = getCurrentSessionCrossSigningInfoUseCase.execute()
            val hasAccountCrossSigning = currentSessionCrossSigningInfo.isCrossSigningInitialized
            val accountCrossSigningIsTrusted = currentSessionCrossSigningInfo.isCrossSigningVerified

            setState {
                copy(
                        hasAccountCrossSigning = hasAccountCrossSigning,
                        accountCrossSigningIsTrusted = accountCrossSigningIsTrusted,
                        myDeviceId = session.sessionParams.deviceId
                )
            }

            session.cryptoService().verificationService().requestEventFlow()
                    .onEach {
                        when (it) {
                            is VerificationEvent.RequestUpdated -> {
                                if (it.request.isFinished) {
                                    queryRefreshDevicesList()
                                }
                            }
                            else -> {
                                // nop
                            }
                        }
                    }
                    .launchIn(viewModelScope)
        }

        combine(
                session.flow().liveUserCryptoDevices(session.myUserId),
                session.flow().liveMyDevicesInfo()
        ) { cryptoList, infoList ->
            val unverifiedSessionsCount = cryptoList.count { !it.trustLevel?.isVerified().orFalse() }
            val inactiveSessionsCount = infoList.count { checkIfSessionIsInactiveUseCase.execute(it.date) }
            setState {
                copy(
                        unverifiedSessionsCount = unverifiedSessionsCount,
                        inactiveSessionsCount = inactiveSessionsCount
                )
            }
            infoList
                    .sortedByDescending { it.lastSeenTs }
                    .map { deviceInfo ->
                        val cryptoDeviceInfo = cryptoList.firstOrNull { it.deviceId == deviceInfo.deviceId }
                        val currentSessionCrossSigningInfo = getCurrentSessionCrossSigningInfoUseCase.execute()
                        val trustLevelForShield = getEncryptionTrustLevelForDeviceUseCase.execute(currentSessionCrossSigningInfo, cryptoDeviceInfo)
                        val isInactive = checkIfSessionIsInactiveUseCase.execute(deviceInfo.lastSeenTs)
                        DeviceFullInfo(deviceInfo, cryptoDeviceInfo, trustLevelForShield, isInactive)
                    }
        }
//                .distinctUntilChanged()
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
//        session.cryptoService().verificationService().addListener(this)

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
                    tryOrNull { session.cryptoService().fetchDevicesList() }
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
                    tryOrNull { session.cryptoService().fetchDevicesList() }
                    session.cryptoService().downloadKeysIfNeeded(listOf(session.myUserId), true)
                }
                .launchIn(viewModelScope)
        // then force download
        queryRefreshDevicesList()
    }

    private fun initState() {
        viewModelScope.launch {
            val hasAccountCrossSigning = session.cryptoService().crossSigningService().isCrossSigningInitialized()
            val accountCrossSigningIsTrusted = session.cryptoService().crossSigningService().isCrossSigningVerified()
            val myDeviceId = session.sessionParams.deviceId
            setState {
                copy(
                        hasAccountCrossSigning = hasAccountCrossSigning,
                        accountCrossSigningIsTrusted = accountCrossSigningIsTrusted,
                        myDeviceId = myDeviceId
                )
            }
        }
    }

    override fun onCleared() {
        // session.cryptoService().verificationService().removeListener(this)
        super.onCleared()
    }

    override fun transactionUpdated(tx: VerificationTransaction) {
        if (tx.isSuccessful()) {
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
            is DevicesAction.Refresh -> queryRefreshDevicesList()
            is DevicesAction.Delete -> handleDelete(action)
            is DevicesAction.Rename -> handleRename(action)
            is DevicesAction.PromptRename -> handlePromptRename(action)
            is DevicesAction.VerifyMyDevice -> handleInteractiveVerification(action)
            is DevicesAction.CompleteSecurity -> handleCompleteSecurity()
            is DevicesAction.MarkAsManuallyVerified -> handleVerifyManually(action)
            is DevicesAction.VerifyMyDeviceManually -> handleShowDeviceCryptoInfo(action)
            is DevicesAction.SsoAuthDone -> pendingAuthHandler.ssoAuthDone()
            is DevicesAction.PasswordAuthDone -> pendingAuthHandler.passwordAuthDone(action.password)
            DevicesAction.ReAuthCancelled -> pendingAuthHandler.reAuthCancelled()
            DevicesAction.ResetSecurity -> _viewEvents.post(DevicesViewEvents.PromptResetSecrets)
        }
    }

    private fun handleInteractiveVerification(action: DevicesAction.VerifyMyDevice) {
        viewModelScope.launch {
            session.cryptoService()
                    .verificationService()
                    .requestDeviceVerification(
                            supportedVerificationMethodsProvider.provide(),
                            session.myUserId,
                            action.deviceId
                    ).transactionId
                    .let {
                        _viewEvents.post(
                                DevicesViewEvents.ShowVerifyDevice(
                                        session.myUserId,
                                        it
                                )
                        )
                    }
        }
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
                    session.cryptoService().crossSigningService().trustDevice(action.cryptoDeviceInfo.deviceId)
                } catch (failure: Throwable) {
                    Timber.e("Failed to manually cross sign device ${action.cryptoDeviceInfo.deviceId} : ${failure.localizedMessage}")
                    _viewEvents.post(DevicesViewEvents.Failure(failure))
                }
            } else {
                // legacy
                session.cryptoService().verificationService().markedLocallyAsManuallyVerified(
                        action.cryptoDeviceInfo.userId,
                        action.cryptoDeviceInfo.deviceId
                )
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
        viewModelScope.launch {
            try {
                session.cryptoService().setDeviceName(action.deviceId, action.newName)
                setState {
                    copy(request = Success(Unit))
                }
                // force settings update
                queryRefreshDevicesList()
            } catch (failure: Throwable) {
                setState {
                    copy(request = Fail(failure))
                }
                _viewEvents.post(DevicesViewEvents.Failure(failure))
            }
        }
    }

    /**
     * Try to delete a device.
     */
    private fun handleDelete(action: DevicesAction.Delete) {
        val deviceId = action.deviceId

        val accountManagementUrl = session.homeServerCapabilitiesService().getHomeServerCapabilities().externalAccountManagementUrl
        if (accountManagementUrl != null) {
            // Open external browser to delete this session
            _viewEvents.post(
                    DevicesViewEvents.OpenBrowser(
                            url = accountManagementUrl.removeSuffix("/") + "?action=session_end&device_id=$deviceId"
                    )
            )
        } else {
            doDelete(deviceId)
        }
    }

    private fun doDelete(deviceId: String) {
        setState {
            copy(
                    request = Loading()
            )
        }

        viewModelScope.launch {
            try {
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
                            pendingAuthHandler.pendingAuth = DefaultBaseAuth(session = flowResponse.session)
                            pendingAuthHandler.uiaContinuation = promise
                        }
                    }
                })
                setState {
                    copy(request = Success(Unit))
                }
                queryRefreshDevicesList()
            } catch (failure: Throwable) {
                setState {
                    copy(request = Fail(failure))
                }
                if (failure is Failure.OtherServerError && failure.httpCode == HttpsURLConnection.HTTP_UNAUTHORIZED) {
                    _viewEvents.post(DevicesViewEvents.Failure(Exception(stringProvider.getString(CommonStrings.authentication_error))))
                } else {
                    _viewEvents.post(DevicesViewEvents.Failure(Exception(stringProvider.getString(CommonStrings.matrix_error))))
                }
                // ...
                Timber.e(failure, "failed to delete session")
            }
        }
    }
}
