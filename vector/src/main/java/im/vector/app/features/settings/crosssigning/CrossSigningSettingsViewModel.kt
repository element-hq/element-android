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

import androidx.lifecycle.viewModelScope
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.R
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.auth.ReAuthActivity
import im.vector.app.features.login.ReAuthHelper
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.auth.UIABaseAuth
import org.matrix.android.sdk.api.auth.UserInteractiveAuthInterceptor
import org.matrix.android.sdk.api.auth.UserPasswordAuth
import org.matrix.android.sdk.api.auth.data.LoginFlowTypes
import org.matrix.android.sdk.api.auth.registration.RegistrationFlowResponse
import org.matrix.android.sdk.api.auth.registration.nextUncompletedStage
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.crosssigning.MXCrossSigningInfo
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.internal.crypto.crosssigning.fromBase64
import org.matrix.android.sdk.internal.crypto.crosssigning.isVerified
import org.matrix.android.sdk.internal.crypto.model.rest.DefaultBaseAuth
import org.matrix.android.sdk.internal.crypto.model.rest.DeviceInfo
import org.matrix.android.sdk.internal.util.awaitCallback
import org.matrix.android.sdk.rx.rx
import timber.log.Timber
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class CrossSigningSettingsViewModel @AssistedInject constructor(
        @Assisted private val initialState: CrossSigningSettingsViewState,
        private val session: Session,
        private val reAuthHelper: ReAuthHelper,
        private val stringProvider: StringProvider
) : VectorViewModel<CrossSigningSettingsViewState, CrossSigningSettingsAction, CrossSigningSettingsViewEvents>(initialState) {

    init {
        Observable.combineLatest<List<DeviceInfo>, Optional<MXCrossSigningInfo>, Pair<List<DeviceInfo>, Optional<MXCrossSigningInfo>>>(
                session.rx().liveMyDevicesInfo(),
                session.rx().liveCrossSigningInfo(session.myUserId),
                BiFunction { myDevicesInfo, mxCrossSigningInfo ->
                    myDevicesInfo to mxCrossSigningInfo
                }
        )
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

    var uiaContinuation: Continuation<UIABaseAuth>? = null
    var pendingAuth: UIABaseAuth? = null

    @AssistedFactory
    interface Factory {
        fun create(initialState: CrossSigningSettingsViewState): CrossSigningSettingsViewModel
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
                                        override fun performStage(flowResponse: RegistrationFlowResponse,
                                                                  errCode: String?,
                                                                  promise: Continuation<UIABaseAuth>) {
                                            Timber.d("## UIA : initializeCrossSigning UIA")
                                            if (flowResponse.nextUncompletedStage() == LoginFlowTypes.PASSWORD
                                                    && reAuthHelper.data != null && errCode == null) {
                                                UserPasswordAuth(
                                                        session = null,
                                                        user = session.myUserId,
                                                        password = reAuthHelper.data
                                                ).let { promise.resume(it) }
                                            } else {
                                                Timber.d("## UIA : initializeCrossSigning UIA > start reauth activity")
                                                _viewEvents.post(CrossSigningSettingsViewEvents.RequestReAuth(flowResponse, errCode))
                                                pendingAuth = DefaultBaseAuth(session = flowResponse.session)
                                                uiaContinuation = promise
                                            }
                                        }
                                    }, it)
                        }
                    } catch (failure: Throwable) {
                        handleInitializeXSigningError(failure)
                    } finally {
                        _viewEvents.post(CrossSigningSettingsViewEvents.HideModalWaitingView)
                    }
                }
                Unit
            }
            is CrossSigningSettingsAction.SsoAuthDone -> {
                Timber.d("## UIA - FallBack success")
                if (pendingAuth != null) {
                    uiaContinuation?.resume(pendingAuth!!)
                } else {
                    uiaContinuation?.resumeWith(Result.failure((IllegalArgumentException())))
                }
            }
            is CrossSigningSettingsAction.PasswordAuthDone -> {
                val decryptedPass = session.loadSecureSecret<String>(action.password.fromBase64().inputStream(), ReAuthActivity.DEFAULT_RESULT_KEYSTORE_ALIAS)
                uiaContinuation?.resume(
                        UserPasswordAuth(
                                session = pendingAuth?.session,
                                password = decryptedPass,
                                user = session.myUserId
                        )
                )
            }
            CrossSigningSettingsAction.ReAuthCancelled -> {
                Timber.d("## UIA - Reauth cancelled")
                _viewEvents.post(CrossSigningSettingsViewEvents.HideModalWaitingView)
                uiaContinuation?.resumeWith(Result.failure((Exception())))
                uiaContinuation = null
                pendingAuth = null
            }
        }.exhaustive
    }

    private fun handleInitializeXSigningError(failure: Throwable) {
        Timber.e(failure, "## CrossSigning - Failed to initialize cross signing")
        _viewEvents.post(CrossSigningSettingsViewEvents.Failure(Exception(stringProvider.getString(R.string.failed_to_initialize_cross_signing))))
    }

    companion object : MvRxViewModelFactory<CrossSigningSettingsViewModel, CrossSigningSettingsViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: CrossSigningSettingsViewState): CrossSigningSettingsViewModel? {
            val fragment: CrossSigningSettingsFragment = (viewModelContext as FragmentViewModelContext).fragment()
            return fragment.viewModelFactory.create(state)
        }
    }
}
