/*
 * Copyright (c) 2023 New Vector Ltd
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

package im.vector.app.features.roomprofile.polls.detail

import com.airbnb.epoxy.TypedEpoxyController
import im.vector.app.features.home.room.detail.timeline.item.PollItem_
import im.vector.app.features.roomprofile.polls.RoomPollsViewState
import im.vector.lib.core.utils.epoxy.charsequence.toEpoxyCharSequence
import javax.inject.Inject

class RoomPollDetailController @Inject constructor(

) : TypedEpoxyController<RoomPollsViewState>() {

    override fun buildModels(viewState: RoomPollsViewState?) {
        viewState ?: return
        val pollSummary = viewState.getSelectedPoll() ?: return

        PollItem_()
                .eventId(pollSummary.id)
                .pollQuestion(pollSummary.title.toEpoxyCharSequence())
                .canVote(viewState.canVoteSelectedPoll())
                .optionViewStates(pollSummary.optionViewStates)
                .ended(viewState.canVoteSelectedPoll().not())
    }
}
