/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
