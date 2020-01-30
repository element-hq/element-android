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
package im.vector.riotx.features.settings.crosssigning

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.ViewModelContext
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.failure.Failure
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.crypto.crosssigning.MXCrossSigningInfo
import im.vector.matrix.android.internal.auth.data.LoginFlowTypes
import im.vector.matrix.android.internal.auth.registration.RegistrationFlowResponse
import im.vector.matrix.android.internal.crypto.crosssigning.isVerified
import im.vector.matrix.android.internal.crypto.model.rest.UserPasswordAuth
import im.vector.matrix.android.internal.di.MoshiProvider
import im.vector.matrix.rx.rx
import im.vector.riotx.core.platform.EmptyViewEvents
import im.vector.riotx.core.platform.VectorViewModel
import im.vector.riotx.core.platform.VectorViewModelAction
import im.vector.riotx.core.resources.StringProvider
import im.vector.riotx.core.utils.LiveEvent

data class CrossSigningSettingsViewState(
        val crossSigningInfo: MXCrossSigningInfo? = null,
        val xSigningIsEnableInAccount: Boolean = false,
        val xSigningKeysAreTrusted: Boolean = false,
        val xSigningKeyCanSign: Boolean = true,
        val isUploadingKeys: Boolean = false
) : MvRxState

sealed class CrossSigningAction : VectorViewModelAction {
    data class InitializeCrossSigning(val auth: UserPasswordAuth? = null) : CrossSigningAction()
    data class RequestPasswordAuth(val sessionId: String) : CrossSigningAction()
}

class CrossSigningSettingsViewModel @AssistedInject constructor(@Assisted private val initialState: CrossSigningSettingsViewState,
                                                                private val stringProvider: StringProvider,
                                                                private val session: Session)
    : VectorViewModel<CrossSigningSettingsViewState, CrossSigningAction, EmptyViewEvents>(initialState) {

    // Can be used for several actions, for a one shot result
    private val _requestLiveData = MutableLiveData<LiveEvent<Async<CrossSigningAction>>>()
    val requestLiveData: LiveData<LiveEvent<Async<CrossSigningAction>>>
        get() = _requestLiveData

    init {
        session.rx().liveCrossSigningInfo(session.myUserId)
                .execute {
                    val crossSigningKeys = it.invoke()?.getOrNull()
                    val xSigningIsEnableInAccount = crossSigningKeys != null
                    val xSigningKeysAreTrusted = session.getCrossSigningService().checkUserTrust(session.myUserId).isVerified()
                    val xSigningKeyCanSign = session.getCrossSigningService().canCrossSign()
                    copy(
                            crossSigningInfo = crossSigningKeys,
                            xSigningIsEnableInAccount = xSigningIsEnableInAccount,
                            xSigningKeysAreTrusted = xSigningKeysAreTrusted,
                            xSigningKeyCanSign = xSigningKeyCanSign
                    )
                }
    }

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: CrossSigningSettingsViewState): CrossSigningSettingsViewModel
    }

    override fun handle(action: CrossSigningAction) {
        when (action) {
            is CrossSigningAction.InitializeCrossSigning -> {
                initializeCrossSigning(action.auth?.copy(user = session.myUserId))
            }
        }
    }

    private fun initializeCrossSigning(auth: UserPasswordAuth?) {
        setState {
            copy(isUploadingKeys = true)
        }
        session.getCrossSigningService().initializeCrossSigning(auth, object : MatrixCallback<Unit> {
            override fun onSuccess(data: Unit) {
                setState {
                    copy(isUploadingKeys = false)
                }
            }

            override fun onFailure(failure: Throwable) {
                if (failure is Failure.OtherServerError
                        && failure.httpCode == 401
                ) {
                    try {
                        MoshiProvider.providesMoshi()
                                .adapter(RegistrationFlowResponse::class.java)
                                .fromJson(failure.errorBody)
                    } catch (e: Exception) {
                        null
                    }?.let { flowResponse ->
                        // Retry with authentication
                        if (flowResponse.flows?.any { it.stages?.contains(LoginFlowTypes.PASSWORD) == true } == true) {
                            _requestLiveData.postValue(LiveEvent(Success(CrossSigningAction.RequestPasswordAuth(flowResponse.session ?: ""))))
                            return
                        } else {
                            _requestLiveData.postValue(LiveEvent(Fail(Throwable("You cannot do that from mobile"))))
                            // can't do this from here
                            return
                        }
                    }
                }
                when (failure) {
                    is Failure.ServerError -> {
                        _requestLiveData.postValue(LiveEvent(Fail(Throwable(failure.error.message))))
                    }
                    else                   -> {
                        _requestLiveData.postValue(LiveEvent(Fail(failure)))
                    }
                }
                setState {
                    copy(isUploadingKeys = false)
                }
            }
        })
    }

    companion object : MvRxViewModelFactory<CrossSigningSettingsViewModel, CrossSigningSettingsViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: CrossSigningSettingsViewState): CrossSigningSettingsViewModel? {
            val fragment: CrossSigningSettingsFragment = (viewModelContext as FragmentViewModelContext).fragment()
            return fragment.viewModelFactory.create(state)
        }
    }
}
