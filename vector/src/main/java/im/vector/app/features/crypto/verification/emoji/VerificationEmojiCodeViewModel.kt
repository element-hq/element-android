/*
 * Copyright 2019-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.crypto.verification.emoji

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
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
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.verification.EmojiRepresentation
import org.matrix.android.sdk.api.session.crypto.verification.SasVerificationTransaction
import org.matrix.android.sdk.api.session.crypto.verification.VerificationService
import org.matrix.android.sdk.api.session.crypto.verification.VerificationTransaction
import org.matrix.android.sdk.api.session.crypto.verification.VerificationTxState
import org.matrix.android.sdk.api.session.getUserOrDefault
import org.matrix.android.sdk.api.util.MatrixItem
import org.matrix.android.sdk.api.util.toMatrixItem

data class VerificationEmojiCodeViewState(
        val transactionId: String?,
        val otherUser: MatrixItem,
        val supportsEmoji: Boolean = true,
        val emojiDescription: Async<List<EmojiRepresentation>> = Uninitialized,
        val decimalDescription: Async<String> = Uninitialized,
        val isWaitingFromOther: Boolean = false
) : MavericksState

class VerificationEmojiCodeViewModel @AssistedInject constructor(
        @Assisted initialState: VerificationEmojiCodeViewState,
        private val session: Session
) : VectorViewModel<VerificationEmojiCodeViewState, EmptyAction, EmptyViewEvents>(initialState), VerificationService.Listener {

    init {
        refreshStateFromTx(
                session.cryptoService().verificationService()
                        .getExistingTransaction(
                                otherUserId = initialState.otherUser.id,
                                tid = initialState.transactionId ?: ""
                        ) as? SasVerificationTransaction
        )

        session.cryptoService().verificationService().addListener(this)
    }

    override fun onCleared() {
        session.cryptoService().verificationService().removeListener(this)
        super.onCleared()
    }

    private fun refreshStateFromTx(sasTx: SasVerificationTransaction?) {
        when (sasTx?.state) {
            is VerificationTxState.None,
            is VerificationTxState.SendingStart,
            is VerificationTxState.Started,
            is VerificationTxState.OnStarted,
            is VerificationTxState.SendingAccept,
            is VerificationTxState.Accepted,
            is VerificationTxState.OnAccepted,
            is VerificationTxState.SendingKey,
            is VerificationTxState.KeySent,
            is VerificationTxState.OnKeyReceived -> {
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
            is VerificationTxState.ShortCodeReady -> {
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
            is VerificationTxState.ShortCodeAccepted,
            is VerificationTxState.SendingMac,
            is VerificationTxState.MacSent,
            is VerificationTxState.Verifying,
            is VerificationTxState.Verified -> {
                setState {
                    copy(isWaitingFromOther = true)
                }
            }
            is VerificationTxState.Cancelled -> {
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
            null -> {
                setState {
                    copy(
                            isWaitingFromOther = false,
                            emojiDescription = Fail(Throwable("Unknown Transaction")),
                            decimalDescription = Fail(Throwable("Unknown Transaction"))
                    )
                }
            }
            else -> Unit
        }
    }

    override fun transactionCreated(tx: VerificationTransaction) {
        transactionUpdated(tx)
    }

    override fun transactionUpdated(tx: VerificationTransaction) = withState { state ->
        if (tx.transactionId == state.transactionId && tx is SasVerificationTransaction) {
            refreshStateFromTx(tx)
        }
    }

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<VerificationEmojiCodeViewModel, VerificationEmojiCodeViewState> {
        override fun create(initialState: VerificationEmojiCodeViewState): VerificationEmojiCodeViewModel
    }

    companion object : MavericksViewModelFactory<VerificationEmojiCodeViewModel, VerificationEmojiCodeViewState> by hiltMavericksViewModelFactory() {

        override fun initialState(viewModelContext: ViewModelContext): VerificationEmojiCodeViewState {
            val args = viewModelContext.args<VerificationBottomSheet.VerificationArgs>()
            val session = EntryPoints.get(viewModelContext.app(), SingletonEntryPoint::class.java).activeSessionHolder().getActiveSession()
            val matrixItem = session.getUserOrDefault(args.otherUserId).toMatrixItem()

            return VerificationEmojiCodeViewState(
                    transactionId = args.verificationId,
                    otherUser = matrixItem
            )
        }
    }

    override fun handle(action: EmptyAction) {
    }
}
