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
import im.vector.matrix.android.internal.crypto.model.rest.UserPasswordAuth
import im.vector.matrix.rx.asObservable
import im.vector.riotx.core.di.ActiveSessionHolder
import im.vector.riotx.core.platform.EmptyAction
import im.vector.riotx.core.platform.EmptyViewEvents
import im.vector.riotx.core.platform.VectorViewModel
import im.vector.riotx.features.login.ReAuthHelper
import timber.log.Timber

class HomeActivityViewModel @AssistedInject constructor(
        @Assisted initialState: HomeActivityViewState,
        @Assisted private val args: HomeActivityArgs,
        private val activeSessionHolder: ActiveSessionHolder,
        private val reAuthHelper: ReAuthHelper
) : VectorViewModel<HomeActivityViewState, EmptyAction, EmptyViewEvents>(initialState) {

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

    // TODO Remove?
    var hasDisplayedCompleteSecurityPrompt: Boolean = false

    init {
        observeInitialSync()
        mayBeInitializeCrossSigning()
    }

    private fun observeInitialSync() {
        val session = activeSessionHolder.getSafeActiveSession() ?: return

        session.getInitialSyncProgressStatus()
                .asObservable()
                .subscribe { status ->
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
                    )
            )
            // TODO Download keys?
        }
    }

    override fun handle(action: EmptyAction) {
        // NA
    }
}
