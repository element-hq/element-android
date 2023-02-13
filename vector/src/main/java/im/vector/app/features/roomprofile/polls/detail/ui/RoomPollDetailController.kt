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

package im.vector.app.features.roomprofile.polls.detail.ui

import com.airbnb.epoxy.TypedEpoxyController
import im.vector.app.core.date.DateFormatKind
import im.vector.app.core.date.VectorDateFormatter
import java.util.UUID
import javax.inject.Inject

class RoomPollDetailController @Inject constructor(
        val dateFormatter: VectorDateFormatter,
) : TypedEpoxyController<RoomPollDetailViewState>() {

    interface Callback {
        fun vote(pollEventId: String, optionId: String)
        fun goToTimelineEvent(eventId: String)
    }

    var callback: Callback? = null

    override fun buildModels(viewState: RoomPollDetailViewState?) {
        val pollDetail = viewState?.pollDetail ?: return
        val pollItemViewState = pollDetail.pollItemViewState
        val host = this

        roomPollDetailItem {
            id(viewState.pollId)
            eventId(viewState.pollId)
            formattedDate(host.dateFormatter.format(pollDetail.creationTimestamp, DateFormatKind.TIMELINE_DAY_DIVIDER))
            question(pollItemViewState.question)
            canVote(pollItemViewState.canVote)
            votesStatus(pollItemViewState.votesStatus)
            optionViewStates(pollItemViewState.optionViewStates.orEmpty())
            callback(host.callback)
        }

        buildGoToTimelineItem(targetEventId = pollDetail.endedPollEventId ?: viewState.pollId)
    }

    private fun buildGoToTimelineItem(targetEventId: String) {
        val host = this
        roomPollGoToTimelineItem {
            id(UUID.randomUUID().toString())
            clickListener {
                host.callback?.goToTimelineEvent(targetEventId)
            }
        }
    }
}
