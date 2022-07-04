/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.auth

import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorViewModel
import org.matrix.android.sdk.api.Matrix
import org.matrix.android.sdk.api.auth.data.LoginFlowTypes
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.util.toBase64NoPadding
import java.io.ByteArrayOutputStream

class ReAuthViewModel @AssistedInject constructor(
        @Assisted val initialState: ReAuthState,
        private val session: Session,
        private val matrix: Matrix,
) : VectorViewModel<ReAuthState, ReAuthActions, ReAuthEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<ReAuthViewModel, ReAuthState> {
        override fun create(initialState: ReAuthState): ReAuthViewModel
    }

    companion object : MavericksViewModelFactory<ReAuthViewModel, ReAuthState> by hiltMavericksViewModelFactory()

    override fun handle(action: ReAuthActions) = withState { state ->
        when (action) {
            ReAuthActions.StartSSOFallback -> {
                if (state.flowType == LoginFlowTypes.SSO) {
                    setState { copy(ssoFallbackPageWasShown = true) }
                    val ssoURL = session.getUiaSsoFallbackUrl(initialState.session ?: "")
                    _viewEvents.post(ReAuthEvents.OpenSsoURl(ssoURL))
                }
            }
            ReAuthActions.FallBackPageLoaded -> {
                setState { copy(ssoFallbackPageWasShown = true) }
            }
            ReAuthActions.FallBackPageClosed -> {
                // Should we do something here?
            }
            is ReAuthActions.ReAuthWithPass -> {
                val safeForIntentCypher = ByteArrayOutputStream().also {
                    it.use {
                        matrix.secureStorageService().securelyStoreObject(action.password, initialState.resultKeyStoreAlias, it)
                    }
                }.toByteArray().toBase64NoPadding()
                _viewEvents.post(ReAuthEvents.PasswordFinishSuccess(safeForIntentCypher))
            }
        }
    }
}
