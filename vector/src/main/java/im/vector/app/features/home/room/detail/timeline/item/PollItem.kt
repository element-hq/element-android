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
import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.features.home.room.detail.RoomDetailAction
import im.vector.app.features.home.room.detail.timeline.TimelineEventController
import im.vector.lib.core.utils.epoxy.charsequence.EpoxyCharSequence

@EpoxyModelClass
abstract class PollItem : AbsMessageItem<PollItem.Holder>() {

    @EpoxyAttribute
    var pollQuestion: EpoxyCharSequence? = null

    @EpoxyAttribute
    var callback: TimelineEventController.Callback? = null

    @EpoxyAttribute
    var eventId: String? = null

    @EpoxyAttribute
    var canVote: Boolean = false

    @EpoxyAttribute
    var votesStatus: String? = null

    @EpoxyAttribute
    var edited: Boolean = false

    @EpoxyAttribute
    lateinit var optionViewStates: List<PollOptionViewState>

    @EpoxyAttribute
    var ended: Boolean = false

    override fun getViewStubId() = STUB_ID

    override fun bind(holder: Holder) {
        super.bind(holder)

        renderSendState(holder.view, holder.questionTextView)

        holder.questionTextView.text = pollQuestion?.charSequence
        holder.votesStatusTextView.text = votesStatus

        while (holder.optionsContainer.childCount < optionViewStates.size) {
            holder.optionsContainer.addView(PollOptionView(holder.view.context))
        }
        while (holder.optionsContainer.childCount > optionViewStates.size) {
            holder.optionsContainer.removeViewAt(0)
        }

        val views = holder.optionsContainer.children.toList().filterIsInstance<PollOptionView>()

        optionViewStates.forEachIndexed { index, optionViewState ->
            views.getOrNull(index)?.let {
                it.render(optionViewState)
                it.setOnClickListener { onPollItemClick(optionViewState) }
            }
        }

        holder.endedPollTextView.isVisible = ended
    }

    private fun onPollItemClick(optionViewState: PollOptionViewState) {
        val relatedEventId = eventId

        if (canVote && relatedEventId != null) {
            callback?.onTimelineItemAction(RoomDetailAction.VoteToPoll(relatedEventId, optionViewState.optionId))
        }
    }

    class Holder : AbsMessageItem.Holder(STUB_ID) {
        val questionTextView by bind<TextView>(R.id.questionTextView)
        val optionsContainer by bind<LinearLayout>(R.id.optionsContainer)
        val votesStatusTextView by bind<TextView>(R.id.optionsVotesStatusTextView)
        val endedPollTextView by bind<TextView>(R.id.endedPollTextView)
    }

    companion object {
        private val STUB_ID = R.id.messageContentPollStub
    }
}
