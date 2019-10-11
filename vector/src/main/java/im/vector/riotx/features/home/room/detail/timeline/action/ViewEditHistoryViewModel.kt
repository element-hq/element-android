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
package im.vector.riotx.features.home.room.detail.timeline.action

import com.airbnb.mvrx.*
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.crypto.MXCryptoError
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.message.MessageContent
import im.vector.matrix.android.api.session.room.model.message.isReply
import im.vector.matrix.android.internal.crypto.algorithms.olm.OlmDecryptionResult
import im.vector.riotx.core.platform.VectorViewModel
import im.vector.riotx.core.date.VectorDateFormatter
import timber.log.Timber
import java.util.*

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
) : VectorViewModel<ViewEditHistoryViewState>(initialState) {

    private val roomId = initialState.roomId
    private val eventId = initialState.eventId
    private val room = session.getRoom(roomId)
            ?: throw IllegalStateException("Shouldn't use this ViewModel without a room")

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: ViewEditHistoryViewState): ViewEditHistoryViewModel
    }

    companion object : MvRxViewModelFactory<ViewEditHistoryViewModel, ViewEditHistoryViewState> {

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
                                val result = session.decryptEvent(it, timelineID)
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
                            originalIsReply = it.getClearContent().toModel<MessageContent>().isReply()
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
}
