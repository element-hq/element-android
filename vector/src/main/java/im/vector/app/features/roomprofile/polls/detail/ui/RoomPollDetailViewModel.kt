/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.polls.detail.ui

import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.event.GetTimelineEventUseCase
import im.vector.app.core.platform.EmptyViewEvents
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.home.room.detail.poll.VoteToPollUseCase
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class RoomPollDetailViewModel @AssistedInject constructor(
        @Assisted initialState: RoomPollDetailViewState,
        private val getTimelineEventUseCase: GetTimelineEventUseCase,
        private val roomPollDetailMapper: RoomPollDetailMapper,
        private val voteToPollUseCase: VoteToPollUseCase,
) : VectorViewModel<RoomPollDetailViewState, RoomPollDetailAction, EmptyViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<RoomPollDetailViewModel, RoomPollDetailViewState> {
        override fun create(initialState: RoomPollDetailViewState): RoomPollDetailViewModel
    }

    companion object : MavericksViewModelFactory<RoomPollDetailViewModel, RoomPollDetailViewState> by hiltMavericksViewModelFactory()

    init {
        observePollDetails(
                pollId = initialState.pollId,
                roomId = initialState.roomId,
        )
    }

    private fun observePollDetails(pollId: String, roomId: String) {
        getTimelineEventUseCase.execute(roomId = roomId, eventId = pollId)
                .map { roomPollDetailMapper.map(it) }
                .onEach { setState { copy(pollDetail = it) } }
                .launchIn(viewModelScope)
    }

    override fun handle(action: RoomPollDetailAction) {
        when (action) {
            is RoomPollDetailAction.Vote -> handleVote(action)
        }
    }

    private fun handleVote(vote: RoomPollDetailAction.Vote) = withState { state ->
        voteToPollUseCase.execute(
                roomId = state.roomId,
                pollEventId = vote.pollEventId,
                optionId = vote.optionId,
        )
    }
}
