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
 */
package im.vector.app.features.settings.crosssigning

import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.R
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.auth.PendingAuthHandler
import im.vector.app.features.login.ReAuthHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
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
import org.matrix.android.sdk.api.util.awaitCallback
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

    init {
        combine(
                session.flow().liveMyDevicesInfo(),
                session.flow().liveCrossSigningInfo(session.myUserId)
        ) { myDevicesInfo, mxCrossSigningInfo ->
            myDevicesInfo to mxCrossSigningInfo
        }
                .execute { data ->
                    val crossSigningKeys = data.invoke()?.second?.getOrNull()
                    val xSigningIsEnableInAccount = crossSigningKeys != null
                    val xSigningKeysAreTrusted = session.cryptoService().crossSigningService().checkUserTrust(session.myUserId).isVerified()
                    val xSigningKeyCanSign = session.cryptoService().crossSigningService().canCrossSign()

                    copy(
                            crossSigningInfo = crossSigningKeys,
                            xSigningIsEnableInAccount = xSigningIsEnableInAccount,
                            xSigningKeysAreTrusted = xSigningKeysAreTrusted,
                            xSigningKeyCanSign = xSigningKeyCanSign
                    )
                }
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
                        awaitCallback<Unit> {
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
                                    }, it
                            )
                        }
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

    private fun handleInitializeXSigningError(failure: Throwable) {
        Timber.e(failure, "## CrossSigning - Failed to initialize cross signing")
        _viewEvents.post(CrossSigningSettingsViewEvents.Failure(Exception(stringProvider.getString(R.string.failed_to_initialize_cross_signing))))
    }

    companion object : MavericksViewModelFactory<CrossSigningSettingsViewModel, CrossSigningSettingsViewState> by hiltMavericksViewModelFactory()
}
