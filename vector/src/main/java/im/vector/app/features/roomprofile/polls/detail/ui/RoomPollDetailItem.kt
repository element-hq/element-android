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

package im.vector.app.features.roomprofile.polls.detail.ui

import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.features.home.room.detail.timeline.item.PollOptionView
import im.vector.app.features.home.room.detail.timeline.item.PollOptionViewState

@EpoxyModelClass
abstract class RoomPollDetailItem : VectorEpoxyModel<RoomPollDetailItem.Holder>(R.layout.item_poll_detail) {

    @EpoxyAttribute
    lateinit var formattedDate: String

    @EpoxyAttribute
    var question: String? = null

    @EpoxyAttribute
    var callback: RoomPollDetailController.Callback? = null

    @EpoxyAttribute
    var eventId: String? = null

    @EpoxyAttribute
    var canVote: Boolean = false

    @EpoxyAttribute
    var votesStatus: String? = null

    @EpoxyAttribute
    lateinit var optionViewStates: List<PollOptionViewState>

    @EpoxyAttribute
    var ended: Boolean = false

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.date.text = formattedDate
        holder.questionTextView.text = question
        holder.votesStatusTextView.text = votesStatus
        holder.optionsContainer.removeAllViews()
        holder.optionsContainer.isVisible = optionViewStates.isNotEmpty()
        for (option in optionViewStates) {
            val optionView = PollOptionView(holder.view.context)
            holder.optionsContainer.addView(optionView)
            optionView.render(option)
            optionView.setOnClickListener { onOptionClicked(option) }
        }

        holder.endedPollTextView.isVisible = false
    }

    private fun onOptionClicked(optionViewState: PollOptionViewState) {
        val relatedEventId = eventId

        if (canVote && relatedEventId != null) {
            callback?.vote(pollEventId = relatedEventId, optionId = optionViewState.optionId)
        }
    }

    class Holder : VectorEpoxyHolder() {
        val date by bind<TextView>(R.id.pollDetailDate)
        val questionTextView by bind<TextView>(R.id.questionTextView)
        val optionsContainer by bind<LinearLayout>(R.id.optionsContainer)
        val votesStatusTextView by bind<TextView>(R.id.optionsVotesStatusTextView)
        val endedPollTextView by bind<TextView>(R.id.endedPollTextView)
    }
}
