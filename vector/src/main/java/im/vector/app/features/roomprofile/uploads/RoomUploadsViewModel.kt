/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.uploads

import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Success
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.voicebroadcast.isVoiceBroadcast
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.room.model.message.MessageType
import org.matrix.android.sdk.api.session.room.model.message.asMessageAudioEvent
import org.matrix.android.sdk.flow.flow
import org.matrix.android.sdk.flow.unwrap

class RoomUploadsViewModel @AssistedInject constructor(
        @Assisted initialState: RoomUploadsViewState,
        private val session: Session
) : VectorViewModel<RoomUploadsViewState, RoomUploadsAction, RoomUploadsViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<RoomUploadsViewModel, RoomUploadsViewState> {
        override fun create(initialState: RoomUploadsViewState): RoomUploadsViewModel
    }

    companion object : MavericksViewModelFactory<RoomUploadsViewModel, RoomUploadsViewState> by hiltMavericksViewModelFactory()

    private val room = session.getRoom(initialState.roomId)!!

    init {
        observeRoomSummary()
        // Send a first request
        handleLoadMore()
    }

    private fun observeRoomSummary() {
        room.flow().liveRoomSummary()
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
                val result = room.uploadsService().getUploads(20, token)

                token = result.nextToken

                val groupedUploadEvents = result.uploadEvents
                        // Remove voice broadcast chunks from the attachments
                        .filterNot { it.root.asMessageAudioEvent().isVoiceBroadcast() }
                        .groupBy {
                            it.contentWithAttachmentContent.msgType == MessageType.MSGTYPE_IMAGE ||
                                    it.contentWithAttachmentContent.msgType == MessageType.MSGTYPE_VIDEO
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
            is RoomUploadsAction.Share -> handleShare(action)
            RoomUploadsAction.Retry -> handleLoadMore()
            RoomUploadsAction.LoadMore -> handleLoadMore()
        }
    }

    private fun handleShare(action: RoomUploadsAction.Share) {
        viewModelScope.launch {
            val event = try {
                val file = session.fileService().downloadFile(
                        messageContent = action.uploadEvent.contentWithAttachmentContent
                )
                RoomUploadsViewEvents.FileReadyForSharing(file)
            } catch (failure: Throwable) {
                RoomUploadsViewEvents.Failure(failure)
            }
            _viewEvents.post(event)
        }
    }

    private fun handleDownload(action: RoomUploadsAction.Download) {
        viewModelScope.launch {
            val event = try {
                val file = session.fileService().downloadFile(
                        messageContent = action.uploadEvent.contentWithAttachmentContent
                )
                RoomUploadsViewEvents.FileReadyForSaving(file, action.uploadEvent.contentWithAttachmentContent.body)
            } catch (failure: Throwable) {
                RoomUploadsViewEvents.Failure(failure)
            }
            _viewEvents.post(event)
        }
    }
}
