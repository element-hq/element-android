/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package im.vector.app.features.home.room.detail.timeline.edithistory

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.assisted.AssistedFactory
import im.vector.app.core.date.VectorDateFormatter
import im.vector.app.core.platform.EmptyAction
import im.vector.app.core.platform.EmptyViewEvents
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.home.room.detail.timeline.action.TimelineEventFragmentArgs
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.MXCryptoError
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.isReply
import org.matrix.android.sdk.internal.crypto.algorithms.olm.OlmDecryptionResult
import timber.log.Timber
import java.util.UUID

data class ViewEditHistoryViewState(
        val eventId: String,
        val roomId: String,
        val isOriginalAReply: Boolean = false,
        val editList: Async<List<Event>> = Uninitialized)
    : MvRxState {

    constructor(args: TimelineEventFragmentArgs) : this(roomId = args.roomId, eventId = args.eventId)
}

class ViewEditHistoryViewModel @AssistedInject constructor(@Assisted
                                                           initialState: ViewEditHistoryViewState,
                                                           val session: Session,
                                                           val dateFormatter: VectorDateFormatter
) : VectorViewModel<ViewEditHistoryViewState, EmptyAction, EmptyViewEvents>(initialState) {

    private val roomId = initialState.roomId
    private val eventId = initialState.eventId
    private val room = session.getRoom(roomId)
            ?: throw IllegalStateException("Shouldn't use this ViewModel without a room")

    @AssistedFactory
    interface Factory {
        fun create(initialState: ViewEditHistoryViewState): ViewEditHistoryViewModel
    }

    companion object : MvRxViewModelFactory<ViewEditHistoryViewModel, ViewEditHistoryViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: ViewEditHistoryViewState): ViewEditHistoryViewModel? {
            val fragment: ViewEditHistoryBottomSheet = (viewModelContext as FragmentViewModelContext).fragment()
            return fragment.viewEditHistoryViewModelFactory.create(state)
        }
    }

    init {
        loadHistory()
    }

    private fun loadHistory() {
        setState { copy(editList = Loading()) }
        room.fetchEditHistory(eventId, object : MatrixCallback<List<Event>> {
            override fun onFailure(failure: Throwable) {
                setState {
                    copy(editList = Fail(failure))
                }
            }

            override fun onSuccess(data: List<Event>) {
                var originalIsReply = false

                val events = data.map { event ->
                    val timelineID = event.roomId + UUID.randomUUID().toString()
                    event.also {
                        // We need to check encryption
                        if (it.isEncrypted() && it.mxDecryptionResult == null) {
                            // for now decrypt sync
                            try {
                                val result = session.cryptoService().decryptEvent(it, timelineID)
                                it.mxDecryptionResult = OlmDecryptionResult(
                                        payload = result.clearEvent,
                                        senderKey = result.senderCurve25519Key,
                                        keysClaimed = result.claimedEd25519Key?.let { k -> mapOf("ed25519" to k) },
                                        forwardingCurve25519KeyChain = result.forwardingCurve25519KeyChain
                                )
                            } catch (e: MXCryptoError) {
                                Timber.w("Failed to decrypt event in history")
                            }
                        }

                        if (event.eventId == it.eventId) {
                            originalIsReply = it.isReply()
                        }
                    }
                }
                setState {
                    copy(
                            editList = Success(events),
                            isOriginalAReply = originalIsReply
                    )
                }
            }
        })
    }

    override fun handle(action: EmptyAction) {
        // No op
    }
}
