/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
