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
import im.vector.matrix.android.api.util.MatrixItem
import im.vector.matrix.android.api.util.toMatrixItem
import im.vector.riotx.core.platform.VectorViewModel
import im.vector.riotx.core.platform.VectorViewModelAction


data class VerificationRequestViewState(
        val otherUserId: String = "",
        val matrixItem: MatrixItem? = null,
        val started: Async<Boolean> = Success(false)
) : MvRxState

sealed class VerificationAction : VectorViewModelAction {
    data class RequestVerificationByDM(val userID: String) : VerificationAction()
}

class OutgoingVerificationRequestViewModel @AssistedInject constructor(
        @Assisted initialState: VerificationRequestViewState,
        private val session: Session
) : VectorViewModel<VerificationRequestViewState, VerificationAction>(initialState) {

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: VerificationRequestViewState): OutgoingVerificationRequestViewModel
    }

    init {
        withState {
            val user = session.getUser(it.otherUserId)
            setState {
                copy(matrixItem = user?.toMatrixItem())
            }
        }
    }

    companion object : MvRxViewModelFactory<OutgoingVerificationRequestViewModel, VerificationRequestViewState> {
        override fun create(viewModelContext: ViewModelContext, state: VerificationRequestViewState): OutgoingVerificationRequestViewModel? {
            val fragment: OutgoingVerificationRequestFragment = (viewModelContext as FragmentViewModelContext).fragment()
            return fragment.outgoingVerificationRequestViewModelFactory.create(state)
        }

        override fun initialState(viewModelContext: ViewModelContext): VerificationRequestViewState? {
            val userID: String = viewModelContext.args<String>()
            return VerificationRequestViewState(otherUserId = userID)
        }
    }


    override fun handle(action: VerificationAction) {
    }

}
