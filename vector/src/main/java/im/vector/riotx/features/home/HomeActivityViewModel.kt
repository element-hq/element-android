/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.riotx.features.home

import com.airbnb.mvrx.MvRx
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.NoOpMatrixCallback
import im.vector.matrix.android.api.session.InitialSyncProgressService
import im.vector.matrix.android.api.util.toMatrixItem
import im.vector.matrix.android.internal.crypto.model.CryptoDeviceInfo
import im.vector.matrix.android.internal.crypto.model.MXUsersDevicesMap
import im.vector.matrix.android.internal.crypto.model.rest.UserPasswordAuth
import im.vector.matrix.rx.asObservable
import im.vector.riotx.core.di.ActiveSessionHolder
import im.vector.riotx.core.platform.EmptyAction
import im.vector.riotx.core.platform.VectorViewModel
import im.vector.riotx.features.login.ReAuthHelper
import timber.log.Timber

class HomeActivityViewModel @AssistedInject constructor(
        @Assisted initialState: HomeActivityViewState,
        @Assisted private val args: HomeActivityArgs,
        private val activeSessionHolder: ActiveSessionHolder,
        private val reAuthHelper: ReAuthHelper
) : VectorViewModel<HomeActivityViewState, EmptyAction, HomeActivityViewEvents>(initialState) {

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: HomeActivityViewState, args: HomeActivityArgs): HomeActivityViewModel
    }

    companion object : MvRxViewModelFactory<HomeActivityViewModel, HomeActivityViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: HomeActivityViewState): HomeActivityViewModel? {
            val activity: HomeActivity = viewModelContext.activity()
            val args: HomeActivityArgs? = activity.intent.getParcelableExtra(MvRx.KEY_ARG)
            return args?.let { activity.viewModelFactory.create(state, it) }
        }
    }

    private var checkBootstrap = false

    init {
        observeInitialSync()
        mayBeInitializeCrossSigning()
    }

    private fun observeInitialSync() {
        val session = activeSessionHolder.getSafeActiveSession() ?: return

        session.getInitialSyncProgressStatus()
                .asObservable()
                .subscribe { status ->
                    when (status) {
                        is InitialSyncProgressService.Status.Progressing -> {
                            // Schedule a check of the bootstrap when the init sync will be finished
                            checkBootstrap = true
                        }
                        is InitialSyncProgressService.Status.Idle        -> {
                            if (checkBootstrap) {
                                checkBootstrap = false
                                maybeBootstrapCrossSigning()
                            }
                        }
                    }

                    setState {
                        copy(
                                initialSyncProgressServiceStatus = status
                        )
                    }
                }
                .disposeOnClear()
    }

    private fun mayBeInitializeCrossSigning() {
        if (args.accountCreation) {
            val password = reAuthHelper.data ?: return Unit.also {
                Timber.w("No password to init cross signing")
            }

            val session = activeSessionHolder.getSafeActiveSession() ?: return Unit.also {
                Timber.w("No session to init cross signing")
            }

            // We do not use the viewModel context because we do not want to cancel this action
            Timber.d("Initialize cross signing")
            session.cryptoService().crossSigningService().initializeCrossSigning(
                    authParams = UserPasswordAuth(
                            session = null,
                            user = session.myUserId,
                            password = password
                    ),
                    callback = NoOpMatrixCallback()
            )
        }
    }

    private fun maybeBootstrapCrossSigning() {
        // In case of account creation, it is already done before
        if (args.accountCreation) return

        val session = activeSessionHolder.getSafeActiveSession() ?: return

        // Ensure keys of the user are downloaded
        session.cryptoService().downloadKeys(listOf(session.myUserId), true, object : MatrixCallback<MXUsersDevicesMap<CryptoDeviceInfo>> {
            override fun onSuccess(data: MXUsersDevicesMap<CryptoDeviceInfo>) {
                // Is there already cross signing keys here?
                val mxCrossSigningInfo = session.cryptoService().crossSigningService().getMyCrossSigningKeys()
                if (mxCrossSigningInfo != null) {
                    // Cross-signing is already set up for this user, is it trusted?
                    if (!mxCrossSigningInfo.isTrusted()) {
                        // New session
                        _viewEvents.post(HomeActivityViewEvents.OnNewSession(session.getUser(session.myUserId)?.toMatrixItem()))
                    }
                } else {
                    // Initialize cross-signing
                    val password = reAuthHelper.data

                    if (password == null) {
                        // Check this is not an SSO account
                        if (session.getHomeServerCapabilities().canChangePassword) {
                            // Ask password to the user: Upgrade security
                            _viewEvents.post(HomeActivityViewEvents.AskPasswordToInitCrossSigning(session.getUser(session.myUserId)?.toMatrixItem()))
                        }
                        // Else (SSO) just ignore for the moment
                    } else {
                        // We do not use the viewModel context because we do not want to cancel this action
                        Timber.d("Initialize cross signing")
                        session.cryptoService().crossSigningService().initializeCrossSigning(
                                authParams = UserPasswordAuth(
                                        session = null,
                                        user = session.myUserId,
                                        password = password
                                ),
                                callback = NoOpMatrixCallback()
                        )
                    }
                }
            }
        })
    }

    override fun handle(action: EmptyAction) {
        // NA
    }
}
