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
package im.vector.riotx.features.crypto.verification

import com.airbnb.mvrx.*
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.crypto.sas.SasVerificationService
import im.vector.matrix.android.api.session.crypto.sas.SasVerificationTransaction
import im.vector.matrix.android.api.util.MatrixItem
import im.vector.matrix.android.api.util.toMatrixItem
import im.vector.matrix.android.internal.crypto.verification.PendingVerificationRequest
import im.vector.riotx.core.di.HasScreenInjector
import im.vector.riotx.core.platform.VectorViewModel

data class VerificationRequestViewState(
        val roomId: String? = null,
        val matrixItem: MatrixItem,
        val started: Async<Boolean> = Success(false)
) : MvRxState

class VerificationRequestViewModel @AssistedInject constructor(
        @Assisted initialState: VerificationRequestViewState,
        private val session: Session
) : VectorViewModel<VerificationRequestViewState, VerificationAction>(initialState), SasVerificationService.SasVerificationListener {

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: VerificationRequestViewState): VerificationRequestViewModel
    }

    init {
        session.getSasVerificationService().addListener(this)
    }

    override fun onCleared() {
        session.getSasVerificationService().removeListener(this)
        super.onCleared()
    }

    companion object : MvRxViewModelFactory<VerificationRequestViewModel, VerificationRequestViewState> {
        override fun create(viewModelContext: ViewModelContext, state: VerificationRequestViewState): VerificationRequestViewModel? {
            val fragment: VerificationRequestFragment = (viewModelContext as FragmentViewModelContext).fragment()
            return fragment.verificationRequestViewModelFactory.create(state)
        }

        override fun initialState(viewModelContext: ViewModelContext): VerificationRequestViewState? {
            val args = viewModelContext.args<VerificationBottomSheet.VerificationArgs>()
            val session = (viewModelContext.activity as HasScreenInjector).injector().activeSessionHolder().getActiveSession()

            val pr = session.getSasVerificationService()
                    .getExistingVerificationRequest(args.otherUserId, args.verificationId)
            return session.getUser(args.otherUserId)?.let {
                VerificationRequestViewState(
                        started = Success(false).takeIf { pr == null }
                                ?: Success(true).takeIf { pr?.isReady == true }
                                ?: Loading(),
                        matrixItem = it.toMatrixItem()
                )
            }
        }
    }

    override fun handle(action: VerificationAction) {
    }

    override fun transactionCreated(tx: SasVerificationTransaction) {}

    override fun transactionUpdated(tx: SasVerificationTransaction) {}

    override fun verificationRequestCreated(pr: PendingVerificationRequest) {
        verificationRequestUpdated(pr)
    }

    override fun verificationRequestUpdated(pr: PendingVerificationRequest) = withState { state ->
        if (pr.otherUserId == state.matrixItem.id) {
            if (pr.isReady) {
                setState {
                    copy(started = Success(true))
                }
            } else {
                setState {
                    copy(started = Loading())
                }
            }
        }
    }
}
