/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.home.room.detail.timeline.edithistory

import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Success
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.EmptyAction
import im.vector.app.core.platform.EmptyViewEvents
import im.vector.app.core.platform.VectorViewModel
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.MXCryptoError
import org.matrix.android.sdk.api.session.crypto.model.OlmDecryptionResult
import org.matrix.android.sdk.api.session.events.model.isReply
import org.matrix.android.sdk.api.session.getRoom
import timber.log.Timber
import java.util.UUID

class ViewEditHistoryViewModel @AssistedInject constructor(
        @Assisted initialState: ViewEditHistoryViewState,
        private val session: Session
) : VectorViewModel<ViewEditHistoryViewState, EmptyAction, EmptyViewEvents>(initialState) {

    private val roomId = initialState.roomId
    private val eventId = initialState.eventId
    private val room = session.getRoom(roomId)
            ?: throw IllegalStateException("Shouldn't use this ViewModel without a room")

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<ViewEditHistoryViewModel, ViewEditHistoryViewState> {
        override fun create(initialState: ViewEditHistoryViewState): ViewEditHistoryViewModel
    }

    companion object : MavericksViewModelFactory<ViewEditHistoryViewModel, ViewEditHistoryViewState> by hiltMavericksViewModelFactory()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        setState { copy(editList = Loading()) }

        viewModelScope.launch {
            val data = try {
                room.relationService().fetchEditHistory(eventId)
            } catch (failure: Throwable) {
                setState {
                    copy(editList = Fail(failure))
                }
                return@launch
            }

            var originalIsReply = false

            data.forEach { event ->
                val timelineID = event.roomId + UUID.randomUUID().toString()
                // We need to check encryption
                if (event.isEncrypted() && event.mxDecryptionResult == null) {
                    // for now decrypt sync
                    try {
                        val result = session.cryptoService().decryptEvent(event, timelineID)
                        event.mxDecryptionResult = OlmDecryptionResult(
                                payload = result.clearEvent,
                                senderKey = result.senderCurve25519Key,
                                keysClaimed = result.claimedEd25519Key?.let { k -> mapOf("ed25519" to k) },
                                forwardingCurve25519KeyChain = result.forwardingCurve25519KeyChain,
                                verificationState = result.messageVerificationState
                        )
                    } catch (e: MXCryptoError) {
                        Timber.w("Failed to decrypt event in history")
                    }
                }

                if (event.eventId == eventId) {
                    originalIsReply = event.isReply()
                }
            }
            setState {
                copy(
                        editList = Success(data),
                        isOriginalAReply = originalIsReply
                )
            }
        }
    }

    override fun handle(action: EmptyAction) {
        // No op
    }
}
