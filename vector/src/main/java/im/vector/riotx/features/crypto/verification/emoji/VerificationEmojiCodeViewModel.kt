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
package im.vector.riotx.features.crypto.verification.emoji

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
import im.vector.riotx.core.di.HasScreenInjector
import im.vector.riotx.core.platform.EmptyAction
import im.vector.riotx.core.platform.EmptyViewEvents
import im.vector.riotx.core.platform.VectorViewModel
import im.vector.riotx.features.crypto.verification.VerificationBottomSheet

data class VerificationEmojiCodeViewState(
        val transactionId: String?,
        val otherUser: MatrixItem? = null,
        val supportsEmoji: Boolean = true,
        val emojiDescription: Async<List<EmojiRepresentation>> = Uninitialized,
        val decimalDescription: Async<String> = Uninitialized,
        val isWaitingFromOther: Boolean = false
) : MvRxState

class VerificationEmojiCodeViewModel @AssistedInject constructor(
        @Assisted initialState: VerificationEmojiCodeViewState,
        private val session: Session
) : VectorViewModel<VerificationEmojiCodeViewState, EmptyAction, EmptyViewEvents>(initialState), SasVerificationService.SasVerificationListener {

    init {
        withState { state ->
            refreshStateFromTx(session.getSasVerificationService()
                    .getExistingTransaction(state.otherUser?.id ?: "", state.transactionId
                            ?: ""))
        }

        session.getSasVerificationService().addListener(this)
    }

    override fun onCleared() {
        session.getSasVerificationService().removeListener(this)
        super.onCleared()
    }

    private fun refreshStateFromTx(sasTx: SasVerificationTransaction?) {
        when (sasTx?.state) {
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
            null                                  -> {
                setState {
                    copy(
                            isWaitingFromOther = false,
                            emojiDescription = Fail(Throwable("Unknown Transaction")),
                            decimalDescription = Fail(Throwable("Unknown Transaction"))
                    )
                }
            }
        }
    }

    override fun transactionCreated(tx: SasVerificationTransaction) {
        transactionUpdated(tx)
    }

    override fun transactionUpdated(tx: SasVerificationTransaction) = withState { state ->
        if (tx.transactionId == state.transactionId) {
            refreshStateFromTx(tx)
        }
    }

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: VerificationEmojiCodeViewState): VerificationEmojiCodeViewModel
    }

    companion object : MvRxViewModelFactory<VerificationEmojiCodeViewModel, VerificationEmojiCodeViewState> {

        override fun create(viewModelContext: ViewModelContext, state: VerificationEmojiCodeViewState): VerificationEmojiCodeViewModel? {
            val factory = (viewModelContext as FragmentViewModelContext).fragment<VerificationEmojiCodeFragment>().viewModelFactory
            return factory.create(state)
        }

        override fun initialState(viewModelContext: ViewModelContext): VerificationEmojiCodeViewState? {
            val args = viewModelContext.args<VerificationBottomSheet.VerificationArgs>()
            val session = (viewModelContext.activity as HasScreenInjector).injector().activeSessionHolder().getActiveSession()
            val matrixItem = session.getUser(args.otherUserId)?.toMatrixItem()

            return VerificationEmojiCodeViewState(
                    transactionId = args.verificationId,
                    otherUser = matrixItem
            )
        }
    }

    override fun handle(action: EmptyAction) {
    }
}
