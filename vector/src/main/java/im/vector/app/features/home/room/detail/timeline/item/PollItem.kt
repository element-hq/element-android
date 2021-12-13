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
import androidx.core.view.children
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.features.home.room.detail.RoomDetailAction
import im.vector.app.features.home.room.detail.timeline.TimelineEventController
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

    @EpoxyAttribute
    var totalVotesText: String? = null

    @EpoxyAttribute
    var optionViewStates: List<PollOptionViewState>? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        val relatedEventId = eventId ?: return

        renderSendState(holder.view, holder.questionTextView)

        holder.questionTextView.text = pollContent?.pollCreationInfo?.question?.question
        holder.totalVotesTextView.text = totalVotesText

        val cachedViews = mutableMapOf<String, PollOptionItem>()
        holder.optionsContainer.children.filterIsInstance<PollOptionItem>().forEach { existingPollItemView ->
            cachedViews[existingPollItemView.getTag(STUB_ID)?.toString() ?: ""] = existingPollItemView
        }

        holder.optionsContainer.removeAllViews()

        pollContent?.pollCreationInfo?.answers?.forEachIndexed { index, option ->
            val optionName = option.answer ?: ""
            val tag = relatedEventId + option.id

            val pollOptionItem: PollOptionItem = (cachedViews[tag] ?: PollOptionItem(holder.view.context))
                    .apply {
                        setTag(STUB_ID, tag)
                        render(
                                state = optionViewStates?.getOrNull(index) ?: PollOptionViewState.DisabledOptionWithInvisibleVotes(optionName)
                        )
                    }
            pollOptionItem.setOnClickListener {
                callback?.onTimelineItemAction(RoomDetailAction.VoteToPoll(relatedEventId, option.id ?: ""))
            }

            holder.optionsContainer.addView(pollOptionItem)
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
