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
package im.vector.riotx.features.crypto.verification.choose

import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.crypto.sas.SasVerificationService
import im.vector.matrix.android.api.session.crypto.sas.SasVerificationTransaction
import im.vector.matrix.android.internal.crypto.model.rest.KeyVerificationStart
import im.vector.matrix.android.internal.crypto.verification.PendingVerificationRequest
import im.vector.riotx.core.di.HasScreenInjector
import im.vector.riotx.core.platform.EmptyAction
import im.vector.riotx.core.platform.VectorViewModel
import im.vector.riotx.features.crypto.verification.VerificationBottomSheet

data class VerificationChooseMethodViewState(
        val otherUserId: String = "",
        val transactionId: String = "",
        val QRModeAvailable: Boolean = false,
        val SASModeAvailable: Boolean = false
) : MvRxState

class VerificationChooseMethodViewModel @AssistedInject constructor(
        @Assisted initialState: VerificationChooseMethodViewState,
        private val session: Session
) : VectorViewModel<VerificationChooseMethodViewState, EmptyAction>(initialState), SasVerificationService.SasVerificationListener {

    override fun transactionCreated(tx: SasVerificationTransaction) {}

    override fun transactionUpdated(tx: SasVerificationTransaction) {}

    override fun verificationRequestUpdated(pr: PendingVerificationRequest) = withState { state ->
        val pvr = session.getSasVerificationService().getExistingVerificationRequest(state.otherUserId, state.transactionId)
        val qrAvailable = pvr?.readyInfo?.methods?.contains(KeyVerificationStart.VERIF_METHOD_SCAN)
                ?: false
        val emojiAvailable = pvr?.readyInfo?.methods?.contains(KeyVerificationStart.VERIF_METHOD_SAS)
                ?: false

        setState {
            copy(
                    QRModeAvailable = qrAvailable,
                    SASModeAvailable = emojiAvailable
            )
        }
    }

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: VerificationChooseMethodViewState): VerificationChooseMethodViewModel
    }

    init {
        session.getSasVerificationService().addListener(this)
    }

    override fun onCleared() {
        super.onCleared()
        session.getSasVerificationService().removeListener(this)
    }

    companion object : MvRxViewModelFactory<VerificationChooseMethodViewModel, VerificationChooseMethodViewState> {
        override fun create(viewModelContext: ViewModelContext, state: VerificationChooseMethodViewState): VerificationChooseMethodViewModel? {
            val fragment: VerificationChooseMethodFragment = (viewModelContext as FragmentViewModelContext).fragment()
            return fragment.verificationChooseMethodViewModelFactory.create(state)
        }

        override fun initialState(viewModelContext: ViewModelContext): VerificationChooseMethodViewState? {
            val args: VerificationBottomSheet.VerificationArgs = viewModelContext.args()
            val session = (viewModelContext.activity as HasScreenInjector).injector().activeSessionHolder().getActiveSession()
            val pvr = session.getSasVerificationService().getExistingVerificationRequest(args.otherUserId, args.verificationId)
            val qrAvailable = pvr?.readyInfo?.methods?.contains(KeyVerificationStart.VERIF_METHOD_SCAN)
                    ?: false
            val emojiAvailable = pvr?.readyInfo?.methods?.contains(KeyVerificationStart.VERIF_METHOD_SAS)
                    ?: false

            return VerificationChooseMethodViewState(otherUserId = args.otherUserId,
                    transactionId = args.verificationId ?: "",
                    QRModeAvailable = qrAvailable,
                    SASModeAvailable = emojiAvailable
            )
        }
    }

    override fun handle(action: EmptyAction) {}
}
