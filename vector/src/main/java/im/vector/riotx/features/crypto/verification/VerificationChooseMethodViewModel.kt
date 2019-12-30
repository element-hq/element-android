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

import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.internal.crypto.model.rest.KeyVerificationStart
import im.vector.riotx.core.platform.EmptyAction
import im.vector.riotx.core.platform.VectorViewModel

data class VerificationChooseMethodViewState(
        val otherUserId: String = "",
        val transactionId: String = "",
        val QRModeAvailable: Boolean = false,
        val SASMOdeAvailable: Boolean = false
) : MvRxState


class VerificationChooseMethodViewModel @AssistedInject constructor(
        @Assisted initialState: VerificationChooseMethodViewState,
        private val session: Session
) : VectorViewModel<VerificationChooseMethodViewState, EmptyAction>(initialState) {


    init {
        withState { state ->
            val pvr = session.getSasVerificationService().getExistingVerificationRequest(state.otherUserId)?.first {
                it.transactionId == state.transactionId
            }
            val qrAvailable = pvr?.readyInfo?.methods?.contains(KeyVerificationStart.VERIF_METHOD_SCAN) ?: false
            val emojiAvailable = pvr?.readyInfo?.methods?.contains(KeyVerificationStart.VERIF_METHOD_SAS) ?: false
            setState {
                copy(QRModeAvailable = qrAvailable, SASMOdeAvailable = emojiAvailable)
            }
        }
    }

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: VerificationChooseMethodViewState): VerificationChooseMethodViewModel
    }

    companion object : MvRxViewModelFactory<VerificationChooseMethodViewModel, VerificationChooseMethodViewState> {
        override fun create(viewModelContext: ViewModelContext, state: VerificationChooseMethodViewState): VerificationChooseMethodViewModel? {
            val fragment: VerificationChooseMethodFragment = (viewModelContext as FragmentViewModelContext).fragment()
            return fragment.verificationChooseMethodViewModelFactory.create(state)
        }

        override fun initialState(viewModelContext: ViewModelContext): VerificationChooseMethodViewState? {
            val args: VerificationBottomSheet.VerificationArgs = viewModelContext.args()
            return VerificationChooseMethodViewState(otherUserId = args.otherUserId, transactionId = args.verificationId ?: "")
        }
    }


    override fun handle(action: EmptyAction) {}


}
