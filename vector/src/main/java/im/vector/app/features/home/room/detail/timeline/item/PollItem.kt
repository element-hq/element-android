/*
 * Copyright 2020 New Vector Ltd
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

package im.vector.app.features.home.room.detail.timeline.item

import android.widget.LinearLayout
import android.widget.TextView
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.features.home.room.detail.RoomDetailAction
import im.vector.app.features.home.room.detail.timeline.TimelineEventController
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.room.model.message.MessagePollContent

@EpoxyModelClass(layout = R.layout.item_timeline_event_base)
abstract class PollItem : AbsMessageItem<PollItem.Holder>() {

    @EpoxyAttribute
    var pollContent: MessagePollContent? = null

    @EpoxyAttribute
    var pollResponseSummary: PollResponseData? = null

    @EpoxyAttribute
    var callback: TimelineEventController.Callback? = null

    @EpoxyAttribute
    var eventId: String? = null

    @EpoxyAttribute
    var pollSent: Boolean = false

    override fun bind(holder: Holder) {
        super.bind(holder)
        val relatedEventId = eventId ?: return

        renderSendState(holder.view, holder.questionTextView)

        holder.questionTextView.text = pollContent?.pollCreationInfo?.question?.question

        holder.optionsContainer.removeAllViews()

        val isEnded = pollResponseSummary?.isClosed.orFalse()
        val didUserVoted = pollResponseSummary?.myVote?.isNotEmpty().orFalse()
        val totalVotes = pollResponseSummary?.totalVotes ?: 0
        val winnerVoteCount = pollResponseSummary?.winnerVoteCount

        pollContent?.pollCreationInfo?.answers?.forEach { option ->
            val voteSummary = pollResponseSummary?.votes?.get(option.id)
            val isMyVote = pollResponseSummary?.myVote == option.id
            val voteCount = voteSummary?.total ?: 0
            val votePercentage = voteSummary?.percentage ?: 0.0
            val optionName = option.answer ?: ""

            holder.optionsContainer.addView(
                    PollOptionItem(holder.view.context).apply {
                        val callback = object : PollOptionItem.Callback {
                            override fun onOptionClicked() {
                                callback?.onTimelineItemAction(RoomDetailAction.VoteToPoll(relatedEventId, option.id ?: ""))
                            }
                        }

                        if (!pollSent) {
                            // Poll event is not send yet. Disable option.
                            render(PollOptionViewState.DisabledOptionWithInvisibleVotes(optionName), callback)
                        } else if (isEnded) {
                            // Poll is ended. Disable option, show votes and mark the winner.
                            val isWinner = winnerVoteCount != 0 && voteCount == winnerVoteCount
                            render(PollOptionViewState.DisabledOptionWithVisibleVotes(optionName, voteCount, votePercentage, isWinner), callback)
                        } else if (didUserVoted) {
                            // User voted to the poll, but poll is not ended. Enable option, show votes and mark the user's selection.
                            render(PollOptionViewState.EnabledOptionWithVisibleVotes(optionName, voteCount, votePercentage, isMyVote), callback)
                        } else {
                            // User didn't voted yet and poll is not ended yet. Enable options, hide votes.
                            render(PollOptionViewState.EnabledOptionWithInvisibleVotes(optionName), callback)
                        }
                    }
            )
        }

        holder.totalVotesTextView.apply {
            text = when {
                isEnded      -> resources.getQuantityString(R.plurals.poll_total_vote_count_after_ended, totalVotes, totalVotes)
                didUserVoted -> resources.getQuantityString(R.plurals.poll_total_vote_count_before_ended_and_voted, totalVotes, totalVotes)
                else         -> resources.getQuantityString(R.plurals.poll_total_vote_count_before_ended_and_not_voted, totalVotes, totalVotes)
            }
        }
    }

    class Holder : AbsMessageItem.Holder(STUB_ID) {
        val questionTextView by bind<TextView>(R.id.questionTextView)
        val optionsContainer by bind<LinearLayout>(R.id.optionsContainer)
        val totalVotesTextView by bind<TextView>(R.id.optionsTotalVotesTextView)
    }

    companion object {
        private const val STUB_ID = R.id.messageContentPollStub
    }
}
