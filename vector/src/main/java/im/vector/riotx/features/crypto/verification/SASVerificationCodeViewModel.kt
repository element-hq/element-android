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
import im.vector.matrix.android.api.session.crypto.sas.EmojiRepresentation
import im.vector.matrix.android.api.session.crypto.sas.SasVerificationService
import im.vector.matrix.android.api.session.crypto.sas.SasVerificationTransaction
import im.vector.matrix.android.api.session.crypto.sas.SasVerificationTxState
import im.vector.matrix.android.api.util.MatrixItem
import im.vector.matrix.android.api.util.toMatrixItem
import im.vector.riotx.core.platform.EmptyAction
import im.vector.riotx.core.platform.VectorViewModel

data class SASVerificationCodeViewState(
        val transactionId: String,
        val otherUserId: String,
        val otherUser: MatrixItem? = null,
        val supportsEmoji: Boolean = true,
        val emojiDescription: Async<List<EmojiRepresentation>> = Uninitialized,
        val decimalDescription: Async<String> = Uninitialized,
        val isWaitingFromOther: Boolean = false
) : MvRxState

class SASVerificationCodeViewModel @AssistedInject constructor(
        @Assisted initialState: SASVerificationCodeViewState,
        private val session: Session
) : VectorViewModel<SASVerificationCodeViewState, EmptyAction>(initialState)
        , SasVerificationService.SasVerificationListener {

    init {
        withState { state ->
            val matrixItem = session.getUser(state.otherUserId)?.toMatrixItem()
            setState {
                copy(otherUser = matrixItem)
            }
            val sasTx = session.getSasVerificationService()
                    .getExistingTransaction(state.otherUserId, state.transactionId)
            if (sasTx == null) {
                setState {
                    copy(
                            isWaitingFromOther = false,
                            emojiDescription = Fail(Throwable("Unknown Transaction")),
                            decimalDescription = Fail(Throwable("Unknown Transaction"))
                    )
                }
            } else {
                refreshStateFromTx(sasTx)
            }
        }

        session.getSasVerificationService().addListener(this)
    }

    override fun onCleared() {
        session.getSasVerificationService().removeListener(this)
        super.onCleared()
    }

    private fun refreshStateFromTx(sasTx: SasVerificationTransaction) {
        when (sasTx.state) {
            SasVerificationTxState.None,
            SasVerificationTxState.SendingStart,
            SasVerificationTxState.Started,
            SasVerificationTxState.OnStarted,
            SasVerificationTxState.SendingAccept,
            SasVerificationTxState.Accepted,
            SasVerificationTxState.OnAccepted,
            SasVerificationTxState.SendingKey,
            SasVerificationTxState.KeySent,
            SasVerificationTxState.OnKeyReceived  -> {
                setState {
                    copy(
                            isWaitingFromOther = false,
                            supportsEmoji = sasTx.supportsEmoji(),
                            emojiDescription = Loading<List<EmojiRepresentation>>()
                                    .takeIf { sasTx.supportsEmoji() }
                                    ?: Uninitialized,
                            decimalDescription = Loading<String>()
                                    .takeIf { sasTx.supportsEmoji().not() }
                                    ?: Uninitialized
                    )
                }
            }
            SasVerificationTxState.ShortCodeReady -> {
                setState {
                    copy(
                            isWaitingFromOther = false,
                            supportsEmoji = sasTx.supportsEmoji(),
                            emojiDescription = if (sasTx.supportsEmoji()) Success(sasTx.getEmojiCodeRepresentation())
                            else Uninitialized,
                            decimalDescription = if (!sasTx.supportsEmoji()) Success(sasTx.getDecimalCodeRepresentation())
                            else Uninitialized
                    )
                }
            }
            SasVerificationTxState.ShortCodeAccepted,
            SasVerificationTxState.SendingMac,
            SasVerificationTxState.MacSent,
            SasVerificationTxState.Verifying,
            SasVerificationTxState.Verified       -> {
                setState {
                    copy(isWaitingFromOther = true)
                }
            }
            SasVerificationTxState.Cancelled,
            SasVerificationTxState.OnCancelled    -> {
                // The fragment should not be rendered in this state,
                // it should have been replaced by a conclusion fragment
                setState {
                    copy(
                            isWaitingFromOther = false,
                            supportsEmoji = sasTx.supportsEmoji(),
                            emojiDescription = Fail(Throwable("Transaction Cancelled")),
                            decimalDescription = Fail(Throwable("Transaction Cancelled"))
                    )
                }
            }
        }
    }

    override fun transactionCreated(tx: SasVerificationTransaction) {
        transactionUpdated(tx)
    }

    override fun transactionUpdated(tx: SasVerificationTransaction) {
        refreshStateFromTx(tx)
    }

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: SASVerificationCodeViewState): SASVerificationCodeViewModel
    }

    companion object : MvRxViewModelFactory<SASVerificationCodeViewModel, SASVerificationCodeViewState> {

        override fun create(viewModelContext: ViewModelContext, state: SASVerificationCodeViewState): SASVerificationCodeViewModel? {
            val factory = (viewModelContext as FragmentViewModelContext).fragment<SASVerificationCodeFragment>().viewModelFactory
            return factory.create(state)
        }

        override fun initialState(viewModelContext: ViewModelContext): SASVerificationCodeViewState? {
            val args = viewModelContext.args<VerificationBottomSheet.VerificationArgs>()
            return SASVerificationCodeViewState(
                    transactionId = args.verificationId ?: "",
                    otherUserId = args.otherUserId
            )
        }
    }

    override fun handle(action: EmptyAction) {

    }
}
