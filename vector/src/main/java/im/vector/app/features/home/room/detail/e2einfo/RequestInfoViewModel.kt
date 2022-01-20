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
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Success
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.R
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.error.ErrorFormatter
import im.vector.app.core.platform.VectorViewEvents
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.platform.VectorViewModelAction
import im.vector.app.core.resources.StringProvider
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.util.toMatrixItem
import org.matrix.android.sdk.internal.crypto.model.forEach
import timber.log.Timber

sealed class RequestInfoAction : VectorViewModelAction {
    //    data class InitWithRequest(val request: IncomingRoomKeyRequest) : RequestInfoAction()
    object TryToForward : RequestInfoAction()
}

sealed class RequestInfoEvent : VectorViewEvents {
    data class DisplayConfirmAlert(val message: String, val isError: Boolean) : RequestInfoEvent()
}

class RequestInfoViewModel @AssistedInject constructor(
        @Assisted private val initialState: RequestState,
        private val session: Session,
        private val errorFormatter: ErrorFormatter,
        private val stringProvider: StringProvider
) : VectorViewModel<RequestState, RequestInfoAction, RequestInfoEvent>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<RequestInfoViewModel, RequestState> {
        override fun create(initialState: RequestState): RequestInfoViewModel
    }

    companion object : MavericksViewModelFactory<RequestInfoViewModel, RequestState> by hiltMavericksViewModelFactory()

    override fun handle(action: RequestInfoAction) {
        when (action) {
            is RequestInfoAction.TryToForward -> {
                withState { state ->
                    state.request?.let {
                        setState {
                            copy(sharingStatus = Loading())
                        }
                        viewModelScope.launch {
                            try {
                                session.cryptoService().replyToForwardKeyRequest(it)
                                setState {
                                    copy(sharingStatus = Success(Unit))
                                }
                                _viewEvents.post(
                                        RequestInfoEvent.DisplayConfirmAlert(
                                                stringProvider.getString(R.string.encryption_information_key_was_forwarded), false
                                        )
                                )
                            } catch (failure: Throwable) {
                                _viewEvents.post(RequestInfoEvent.DisplayConfirmAlert(errorFormatter.toHumanReadable(failure), true))
                                setState {
                                    copy(sharingStatus = Fail(failure))
                                }
                            } finally {
                                // we can refresh the status of the request?
                                session.cryptoService()
                                        .getIncomingRoomKeyRequests()
                                        .firstOrNull { it.requestId == initialState.requestId }
                                        ?.let {
                                            setState {
                                                copy(
                                                        request = request
                                                )
                                            }
                                        }
                            }
                        }
                    }
                }
            }
        }
    }

    init {
        viewModelScope.launch {
            val request = session.cryptoService().getIncomingRoomKeyRequests().firstOrNull { it.requestId == initialState.requestId }
                    ?: return@launch

            val members = request.requestBody?.roomId?.let { session.getRoomSummary(it) }?.otherMemberIds.orEmpty().plus(listOf(session.myUserId))
            val isCurrentlyInRoom = members.contains(request.userId)

            val device = session.cryptoService().getDeviceInfo(request.userId ?: "", request.deviceId ?: "")

            val knownIndex = session.cryptoService().isMegolmSessionKnownLocally(
                    request.requestBody?.roomId,
                    request.requestBody?.sessionId,
                    request.requestBody?.senderKey
            )

            val sharedIndex = session.cryptoService()
                    .getSharedWithInfo(request.requestBody?.roomId ?: "", request.requestBody?.sessionId ?: "")
                    .also {
                        it.forEach { userId, deviceId, index ->
                            Timber.w("## VALR: the session was shared with $userId|$deviceId starting Index:$index")
                        }
                    }
                    .getObject(request.userId, request.deviceId)

            val requesterItem = request.userId?.let {
                session.getUser(it) ?: session.resolveUser(it)
            }

            setState {
                copy(
                        request = request,
                        requesterItem = requesterItem?.toMatrixItem(),
                        requesterDeviceInfo = device,
                        isUserInRoom = isCurrentlyInRoom,
                        wasInitiallyShared = sharedIndex != null,
                        knownIndex = knownIndex,
                        isFromOneOfMyDevices = session.myUserId == request.userId
                )
            }
        }
    }
}
