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

package im.vector.app.features.home.room.detail.e2einfo

import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Success
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorViewEvents
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.platform.VectorViewModelAction
import im.vector.lib.core.utils.flow.tickerFlow
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.internal.crypto.IncomingRoomKeyRequest
import org.matrix.android.sdk.internal.crypto.crosssigning.fromBase64
import org.matrix.android.sdk.internal.crypto.model.event.EncryptedEventContent
import org.matrix.android.sdk.internal.crypto.model.forEach
import timber.log.Timber

sealed class CryptoInfoAction : VectorViewModelAction {
    data class FilterContent(val searchTerm: String) : CryptoInfoAction()
    object EnableFilter : CryptoInfoAction()
    data class ForceShare(val request: IncomingRoomKeyRequest) : CryptoInfoAction()
    data class ReviewRequest(val request: IncomingRoomKeyRequest) : CryptoInfoAction()
    object RefreshRequests : CryptoInfoAction()
    object ReRequestKey : CryptoInfoAction()
}

sealed class CryptoInfoEvents : VectorViewEvents {
    data class NavigateToRequestReview(val incomingRoomKeyRequest: IncomingRoomKeyRequest) : CryptoInfoEvents()
    object NavigateToFilter : CryptoInfoEvents()
}

class CryptoInfoViewModel @AssistedInject constructor(
        @Assisted private val initialState: CryptoInfoViewState,
        private val session: Session
) : VectorViewModel<CryptoInfoViewState, CryptoInfoAction, CryptoInfoEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<CryptoInfoViewModel, CryptoInfoViewState> {
        override fun create(initialState: CryptoInfoViewState): CryptoInfoViewModel
    }

    init {
        viewModelScope.launch {
            session.getRoom(initialState.roomId)?.getTimeLineEvent(initialState.eventId)?.let { timelineEvent ->

                if (!timelineEvent.isEncrypted()) {
                    // no crypto info can be provide
                    // TODO
                    setState {
                        copy(timelineEvent = Fail(IllegalArgumentException("Not Encrypted")))
                    }
                } else {

                    val encryptedContent = timelineEvent.root.content.toModel<EncryptedEventContent>()
                    if (encryptedContent == null) {
                        setState {
                            copy(timelineEvent = Fail(IllegalArgumentException("Malformed event")))
                        }
                    } else {
                        val algorithm = encryptedContent.algorithm
                        val sentByMe = timelineEvent.senderInfo.userId == session.myUserId

                        // I want the message index
                        val messageIndex = encryptedContent.ciphertext?.fromBase64()?.inputStream()?.reader()?.let {
                            tryOrNull {
                                val megolmVersion = it.read()
                                if (megolmVersion == 3) {
                                    if (it.read() == 8
                                    /** Int tag */
                                    ) {
                                        return@let it.read()
                                    }
                                }
                                return@let null
                            }
                        }

                        val sharedWithList = if (sentByMe) {
                            // This is only when sent by me?
                            mutableListOf<UserDeviceInfo>().apply {
                                session.cryptoService()
                                        .getSharedWithInfo(initialState.roomId, encryptedContent.sessionId ?: "")
                                        .forEach { userId, deviceId, index ->
                                            if (messageIndex != null && index <= messageIndex) {
                                                this.add(UserDeviceInfo(userId, deviceId))
                                            }
                                        }
                            }
                        } else {
                            emptyList()
                        }
                        val locallyKnownIndex = session.cryptoService().isMegolmSessionKnownLocally(initialState.roomId, encryptedContent.sessionId, encryptedContent.senderKey)

                        tickerFlow(viewModelScope, 5_000, 0)
                                .execute {
                                    val incomingRequest = session.cryptoService().getIncomingRoomKeyRequests()
                                            .filter {
                                                it.requestBody?.roomId == initialState.roomId && it.requestBody?.sessionId == encryptedContent.sessionId
                                            }

                                    val info = EncryptionInfo(
                                            sentByMe = sentByMe,
                                            sentByThisDevice = sentByMe && encryptedContent.deviceId == session.cryptoService().getMyDevice().deviceId,
                                            algorithm = algorithm,
                                            sharedWithUsers = sharedWithList,
                                            encryptedEventContent = encryptedContent,
                                            sentByUser = UserDeviceInfo(timelineEvent.root.senderId ?: "", encryptedContent.deviceId ?: ""),
                                            incomingRoomKeyRequest = incomingRequest,
                                            messageIndex = messageIndex ?: -1,
                                            locallyKnownIndex = locallyKnownIndex
                                    )
                                    copy(
                                            timelineEvent = Success(timelineEvent),
                                            e2eInfo = Success(info)
                                    )
                                }
                    }
                }
            }
        }
    }

    companion object : MavericksViewModelFactory<CryptoInfoViewModel, CryptoInfoViewState> by hiltMavericksViewModelFactory()

    override fun handle(action: CryptoInfoAction) {
        when (action) {
            is CryptoInfoAction.FilterContent -> {
                setState {
                    copy(searchFilter = action.searchTerm)
                }
            }
            is CryptoInfoAction.ForceShare    -> {
                viewModelScope.launch {
                    try {
                        session.cryptoService().replyToForwardKeyRequest(action.request)
                    } catch (failure: Throwable) {
                        Timber.w("## VALR: failed to share $failure")
                    }
                }
            }
            is CryptoInfoAction.ReviewRequest -> {
                _viewEvents.post(CryptoInfoEvents.NavigateToRequestReview(action.request))
            }
            CryptoInfoAction.EnableFilter     -> {
                setState {
                    copy(
                            searchFilter = ""
                    )
                }
                _viewEvents.post(CryptoInfoEvents.NavigateToFilter)
            }
            CryptoInfoAction.RefreshRequests  -> {
                withState { state ->
                    state.e2eInfo.invoke()?.let { e2eInfo ->
                        val sessionId = e2eInfo.encryptedEventContent.sessionId
                        viewModelScope.launch {
                            val incomingRequest = session.cryptoService().getIncomingRoomKeyRequests()
                                    .filter {
                                        it.requestBody?.roomId == initialState.roomId && it.requestBody?.sessionId == sessionId
                                    }

                            setState {
                                copy(
                                        e2eInfo = Success(
                                                e2eInfo.copy(
                                                        incomingRoomKeyRequest = incomingRequest
                                                )
                                        )
                                )
                            }
                        }
                    }
                }
            }
            CryptoInfoAction.ReRequestKey     -> {
                withState { state ->
                    state.timelineEvent.invoke()?.let {
                        session.cryptoService().reRequestRoomKeyForEvent(it.root)
                    }
                }
            }
        }
    }
}
