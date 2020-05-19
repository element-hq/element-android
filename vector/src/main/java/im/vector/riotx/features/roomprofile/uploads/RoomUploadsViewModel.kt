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

package im.vector.riotx.features.roomprofile.uploads

import androidx.lifecycle.viewModelScope
import com.airbnb.mvrx.ActivityViewModelContext
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.isPreviewableMessage
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.message.MessageContent
import im.vector.matrix.android.api.session.room.uploads.GetUploadsResult
import im.vector.matrix.android.internal.util.awaitCallback
import im.vector.matrix.rx.rx
import im.vector.matrix.rx.unwrap
import im.vector.riotx.core.platform.EmptyViewEvents
import im.vector.riotx.core.platform.VectorViewModel
import kotlinx.coroutines.launch

class RoomUploadsViewModel @AssistedInject constructor(
        @Assisted initialState: RoomUploadsViewState,
        private val session: Session
) : VectorViewModel<RoomUploadsViewState, RoomUploadsAction, EmptyViewEvents>(initialState) {

    @AssistedInject.Factory
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

        setState {
            copy(
                    asyncEventsRequest = Loading()
            )
        }

        viewModelScope.launch {
            try {
                val result = awaitCallback<GetUploadsResult> {
                    room.getUploads(20, token, it)
                }

                token = result.nextToken

                val groupedEvents = result.events
                        .filter { it.getClearType() == EventType.MESSAGE && it.getClearContent()?.toModel<MessageContent>() != null }
                        .groupBy { it.isPreviewableMessage() }

                setState {
                    copy(
                            asyncEventsRequest = Uninitialized,
                            mediaEvents = this.mediaEvents + groupedEvents[true].orEmpty(),
                            fileEvents = this.fileEvents + groupedEvents[false].orEmpty(),
                            hasMore = result.nextToken != null
                    )
                }
            } catch (failure: Throwable) {
                // TODO Post fail
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
        //   when (action) {
//
        //   }.exhaustive
    }
}
