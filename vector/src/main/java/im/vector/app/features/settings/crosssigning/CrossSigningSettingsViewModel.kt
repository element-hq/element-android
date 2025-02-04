/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.settings.crosssigning

import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.auth.PendingAuthHandler
import im.vector.app.features.login.ReAuthHelper
import im.vector.lib.strings.CommonStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.auth.UIABaseAuth
import org.matrix.android.sdk.api.auth.UserInteractiveAuthInterceptor
import org.matrix.android.sdk.api.auth.UserPasswordAuth
import org.matrix.android.sdk.api.auth.data.LoginFlowTypes
import org.matrix.android.sdk.api.auth.registration.RegistrationFlowResponse
import org.matrix.android.sdk.api.auth.registration.nextUncompletedStage
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.crosssigning.isVerified
import org.matrix.android.sdk.api.session.uia.DefaultBaseAuth
import org.matrix.android.sdk.flow.flow
import timber.log.Timber
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class CrossSigningSettingsViewModel @AssistedInject constructor(
        @Assisted private val initialState: CrossSigningSettingsViewState,
        private val session: Session,
        private val reAuthHelper: ReAuthHelper,
        private val stringProvider: StringProvider,
        private val pendingAuthHandler: PendingAuthHandler,
) : VectorViewModel<CrossSigningSettingsViewState, CrossSigningSettingsAction, CrossSigningSettingsViewEvents>(initialState) {

    private var observeCrossSigningJob: Job? = null

    init {
        observeCrossSigning()
    }

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<CrossSigningSettingsViewModel, CrossSigningSettingsViewState> {
        override fun create(initialState: CrossSigningSettingsViewState): CrossSigningSettingsViewModel
    }

    override fun handle(action: CrossSigningSettingsAction) {
        when (action) {
            CrossSigningSettingsAction.InitializeCrossSigning -> {
                _viewEvents.post(CrossSigningSettingsViewEvents.ShowModalWaitingView(null))
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        session.cryptoService().crossSigningService().initializeCrossSigning(
                                object : UserInteractiveAuthInterceptor {
                                    override fun performStage(
                                            flowResponse: RegistrationFlowResponse,
                                            errCode: String?,
                                            promise: Continuation<UIABaseAuth>
                                    ) {
                                        Timber.d("## UIA : initializeCrossSigning UIA")
                                        if (flowResponse.nextUncompletedStage() == LoginFlowTypes.PASSWORD &&
                                                reAuthHelper.data != null && errCode == null) {
                                            UserPasswordAuth(
                                                    session = null,
                                                    user = session.myUserId,
                                                    password = reAuthHelper.data
                                            ).let { promise.resume(it) }
                                        } else {
                                            Timber.d("## UIA : initializeCrossSigning UIA > start reauth activity")
                                            _viewEvents.post(CrossSigningSettingsViewEvents.RequestReAuth(flowResponse, errCode))
                                            pendingAuthHandler.pendingAuth = DefaultBaseAuth(session = flowResponse.session)
                                            pendingAuthHandler.uiaContinuation = promise
                                        }
                                    }
                                })
                        // Force a fast refresh of the data
                        observeCrossSigning()
                    } catch (failure: Throwable) {
                        handleInitializeXSigningError(failure)
                    } finally {
                        _viewEvents.post(CrossSigningSettingsViewEvents.HideModalWaitingView)
                    }
                }
                Unit
            }
            is CrossSigningSettingsAction.SsoAuthDone -> pendingAuthHandler.ssoAuthDone()
            is CrossSigningSettingsAction.PasswordAuthDone -> pendingAuthHandler.passwordAuthDone(action.password)
            CrossSigningSettingsAction.ReAuthCancelled -> {
                _viewEvents.post(CrossSigningSettingsViewEvents.HideModalWaitingView)
                pendingAuthHandler.reAuthCancelled()
            }
        }
    }

    private fun observeCrossSigning() {
//        combine(
//                session.flow().liveUserCryptoDevices(session.myUserId),
//                session.flow().liveCrossSigningInfo(session.myUserId)
//        ) { myDevicesInfo, mxCrossSigningInfo ->
//            myDevicesInfo to mxCrossSigningInfo
//        }
        observeCrossSigningJob?.cancel()
        observeCrossSigningJob = session.flow().liveCrossSigningInfo(session.myUserId)
                .onEach { data ->
                    val crossSigningKeys = data.getOrNull()
                    val xSigningIsEnableInAccount = crossSigningKeys != null
                    val xSigningKeysAreTrusted = session.cryptoService().crossSigningService().checkUserTrust(session.myUserId).isVerified()
                    val xSigningKeyCanSign = session.cryptoService().crossSigningService().canCrossSign()
                    setState {
                        copy(
                                crossSigningInfo = crossSigningKeys,
                                xSigningIsEnableInAccount = xSigningIsEnableInAccount,
                                xSigningKeysAreTrusted = xSigningKeysAreTrusted,
                                xSigningKeyCanSign = xSigningKeyCanSign
                        )
                    }
                }
                .launchIn(viewModelScope)
    }

    private fun handleInitializeXSigningError(failure: Throwable) {
        Timber.e(failure, "## CrossSigning - Failed to initialize cross signing")
        _viewEvents.post(CrossSigningSettingsViewEvents.Failure(Exception(stringProvider.getString(CommonStrings.failed_to_initialize_cross_signing))))
    }

    companion object : MavericksViewModelFactory<CrossSigningSettingsViewModel, CrossSigningSettingsViewState> by hiltMavericksViewModelFactory()
}
