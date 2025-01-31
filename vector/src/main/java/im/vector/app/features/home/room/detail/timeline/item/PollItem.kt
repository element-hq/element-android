/*
 * Copyright 2020-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
    }

    companion object {
        private val STUB_ID = R.id.messageContentPollStub
    }
}
