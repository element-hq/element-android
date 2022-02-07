/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.features.crypto.verification.cancel

import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.EntryPoints
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.SingletonEntryPoint
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.EmptyAction
import im.vector.app.core.platform.EmptyViewEvents
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.crypto.verification.VerificationBottomSheet
import org.matrix.android.sdk.api.crypto.RoomEncryptionTrustLevel
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.verification.CancelCode
import org.matrix.android.sdk.api.session.crypto.verification.VerificationService
import org.matrix.android.sdk.api.util.MatrixItem
import org.matrix.android.sdk.api.util.toMatrixItem

data class VerificationCancelViewState(
        val userMxItem: MatrixItem? = null,
        val otherUserId: String,
        val transactionId: String? = null,
        val roomId: String? = null,
        val userTrustLevel: RoomEncryptionTrustLevel? = null,
        val isMe: Boolean = false,
        val currentDeviceCanCrossSign: Boolean = false,
) : MavericksState

class VerificationCancelViewModel @AssistedInject constructor(
        @Assisted initialState: VerificationCancelViewState,
        private val session: Session
) : VectorViewModel<VerificationCancelViewState, EmptyAction, EmptyViewEvents>(initialState), VerificationService.Listener {

    init {
        session.cryptoService().verificationService().addListener(this)
    }

    override fun onCleared() {
        session.cryptoService().verificationService().removeListener(this)
        super.onCleared()
    }

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<VerificationCancelViewModel, VerificationCancelViewState> {
        override fun create(initialState: VerificationCancelViewState): VerificationCancelViewModel
    }

    companion object : MavericksViewModelFactory<VerificationCancelViewModel, VerificationCancelViewState> by hiltMavericksViewModelFactory() {

        override fun initialState(viewModelContext: ViewModelContext): VerificationCancelViewState {
            val args = viewModelContext.args<VerificationBottomSheet.VerificationArgs>()
            val session = EntryPoints.get(viewModelContext.app(), SingletonEntryPoint::class.java).activeSessionHolder().getActiveSession()
            val matrixItem = session.getUser(args.otherUserId)?.toMatrixItem()

            return VerificationCancelViewState(
                    userMxItem = matrixItem,
                    otherUserId = args.otherUserId,
                    roomId = args.roomId,
                    transactionId = args.verificationId,
                    userTrustLevel = args.userTrustLevel,
                    isMe = args.otherUserId == session.myUserId,
                    currentDeviceCanCrossSign = session.cryptoService().crossSigningService().canCrossSign()
            )
        }
    }

    override fun handle(action: EmptyAction) {}

    private fun cancelAllPendingVerifications(state: VerificationCancelViewState) {
        session.cryptoService()
                .verificationService().getExistingVerificationRequest(state.userMxItem?.id ?: "", state.transactionId)?.let {
                    session.cryptoService().verificationService().cancelVerificationRequest(it)
                }
        session.cryptoService()
                .verificationService()
                .getExistingTransaction(state.userMxItem?.id ?: "", state.transactionId ?: "")
                ?.cancel(CancelCode.User)
    }

    fun confirmCancel() = withState { state ->
        cancelAllPendingVerifications(state)
    }
}
