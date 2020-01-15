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
package im.vector.riotx.features.crypto.verification.conclusion

import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import im.vector.matrix.android.api.session.crypto.sas.CancelCode
import im.vector.matrix.android.api.session.crypto.sas.safeValueOf
import im.vector.riotx.core.platform.EmptyAction
import im.vector.riotx.core.platform.VectorViewModel

data class SASVerificationConclusionViewState(
        val conclusionState: ConclusionState = ConclusionState.CANCELLED
) : MvRxState

enum class ConclusionState {
    SUCCESS,
    WARNING,
    CANCELLED
}

class VerificationConclusionViewModel(initialState: SASVerificationConclusionViewState)
    : VectorViewModel<SASVerificationConclusionViewState, EmptyAction>(initialState) {

    companion object : MvRxViewModelFactory<VerificationConclusionViewModel, SASVerificationConclusionViewState> {

        override fun initialState(viewModelContext: ViewModelContext): SASVerificationConclusionViewState? {
            val args = viewModelContext.args<VerificationConclusionFragment.Args>()

            return when (safeValueOf(args.cancelReason)) {
                CancelCode.MismatchedSas,
                CancelCode.MismatchedCommitment,
                CancelCode.MismatchedKeys -> {
                    SASVerificationConclusionViewState(ConclusionState.WARNING)
                }
                else                      -> {
                    SASVerificationConclusionViewState(
                            if (args.isSuccessFull) ConclusionState.SUCCESS
                            else ConclusionState.CANCELLED
                    )
                }
            }
        }
    }

    override fun handle(action: EmptyAction) {}
}
