/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.app.features.roomprofile.uploads

import androidx.lifecycle.viewModelScope
import com.airbnb.mvrx.ActivityViewModelContext
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.ViewModelContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.assisted.AssistedFactory
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.platform.VectorViewModel
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.room.model.message.MessageType
import org.matrix.android.sdk.internal.util.awaitCallback
import org.matrix.android.sdk.rx.rx
import org.matrix.android.sdk.rx.unwrap
import java.io.File

class RoomUploadsViewModel @AssistedInject constructor(
        @Assisted initialState: RoomUploadsViewState,
        private val session: Session
) : VectorViewModel<RoomUploadsViewState, RoomUploadsAction, RoomUploadsViewEvents>(initialState) {

    @AssistedFactory
    interface Factory {
        fun create(initialState: RoomUploadsViewState): RoomUploadsViewModel
    }

    companion object : MvRxViewModelFactory<RoomUploadsViewModel, RoomUploadsViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: RoomUploadsViewState): RoomUploadsViewModel? {
            val factory = when (viewModelContext) {
                is FragmentViewModelContext -> viewModelContext.fragment as? Factory
                is ActivityViewModelContext -> viewModelContext.activity as? Factory
            }
            return factory?.create(state) ?: error("You should let your activity/fragment implements Factory interface")
        }
    }

    private val room = session.getRoom(initialState.roomId)!!

    init {
        observeRoomSummary()
        // Send a first request
        handleLoadMore()
    }

    private fun observeRoomSummary() {
        room.rx().liveRoomSummary()
                .unwrap()
                .execute { async ->
                    copy(roomSummary = async)
                }
    }

    private fun handleLoadMore() = withState { state ->
        if (state.asyncEventsRequest is Loading) return@withState
        if (!state.hasMore) return@withState

        setState {
            copy(
                    asyncEventsRequest = Loading()
            )
        }

        viewModelScope.launch {
            try {
                val result = room.getUploads(20, token)

                token = result.nextToken

                val groupedUploadEvents = result.uploadEvents
                        .groupBy {
                            it.contentWithAttachmentContent.msgType == MessageType.MSGTYPE_IMAGE
                                    || it.contentWithAttachmentContent.msgType == MessageType.MSGTYPE_VIDEO
                        }

                setState {
                    copy(
                            asyncEventsRequest = Success(Unit),
                            mediaEvents = this.mediaEvents + groupedUploadEvents[true].orEmpty(),
                            fileEvents = this.fileEvents + groupedUploadEvents[false].orEmpty(),
                            hasMore = result.hasMore
                    )
                }
            } catch (failure: Throwable) {
                _viewEvents.post(RoomUploadsViewEvents.Failure(failure))
                setState {
                    copy(
                            asyncEventsRequest = Fail(failure)
                    )
                }
            }
        }
    }

    private var token: String? = null

    override fun handle(action: RoomUploadsAction) {
        when (action) {
            is RoomUploadsAction.Download -> handleDownload(action)
            is RoomUploadsAction.Share    -> handleShare(action)
            RoomUploadsAction.Retry       -> handleLoadMore()
            RoomUploadsAction.LoadMore    -> handleLoadMore()
        }.exhaustive
    }

    private fun handleShare(action: RoomUploadsAction.Share) {
        viewModelScope.launch {
            try {
                val file = awaitCallback<File> {
                    session.fileService().downloadFile(
                            messageContent = action.uploadEvent.contentWithAttachmentContent,
                            callback = it
                    )
                }
                _viewEvents.post(RoomUploadsViewEvents.FileReadyForSharing(file))
            } catch (failure: Throwable) {
                _viewEvents.post(RoomUploadsViewEvents.Failure(failure))
            }
        }
    }

    private fun handleDownload(action: RoomUploadsAction.Download) {
        viewModelScope.launch {
            try {
                val file = awaitCallback<File> {
                    session.fileService().downloadFile(
                            messageContent = action.uploadEvent.contentWithAttachmentContent,
                            callback = it)
                }
                _viewEvents.post(RoomUploadsViewEvents.FileReadyForSaving(file, action.uploadEvent.contentWithAttachmentContent.body))
            } catch (failure: Throwable) {
                _viewEvents.post(RoomUploadsViewEvents.Failure(failure))
            }
        }
    }
}
