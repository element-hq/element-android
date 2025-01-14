/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.polls.list.ui

import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.epoxy.onClick
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.features.home.room.detail.timeline.item.PollOptionView
import im.vector.app.features.home.room.detail.timeline.item.PollOptionViewState

@EpoxyModelClass
abstract class RoomPollItem : VectorEpoxyModel<RoomPollItem.Holder>(R.layout.item_poll) {

    @EpoxyAttribute
    lateinit var formattedDate: String

    @EpoxyAttribute
    lateinit var title: String

    @EpoxyAttribute
    var winnerOptions: List<PollOptionViewState.PollEnded> = emptyList()

    @EpoxyAttribute
    var totalVotesStatus: String? = null

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var clickListener: ClickListener? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.view.onClick(clickListener)
        holder.date.text = formattedDate
        holder.title.text = title
        holder.winnerOptions.removeAllViews()
        holder.winnerOptions.isVisible = winnerOptions.isNotEmpty()
        for (winnerOption in winnerOptions) {
            val optionView = PollOptionView(holder.view.context)
            holder.winnerOptions.addView(optionView)
            optionView.render(winnerOption)
        }
        holder.totalVotes.setTextOrHide(totalVotesStatus)
    }

    class Holder : VectorEpoxyHolder() {
        val date by bind<TextView>(R.id.pollDate)
        val title by bind<TextView>(R.id.pollTitle)
        val winnerOptions by bind<LinearLayout>(R.id.pollWinnerOptionsContainer)
        val totalVotes by bind<TextView>(R.id.pollTotalVotes)
    }
}
