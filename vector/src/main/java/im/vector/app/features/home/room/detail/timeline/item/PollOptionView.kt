/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.timeline.item

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import im.vector.app.R
import im.vector.app.core.extensions.setAttributeTintedImageResource
import im.vector.app.databinding.ItemPollOptionBinding
import im.vector.app.features.themes.ThemeUtils
import im.vector.lib.strings.CommonPlurals

class PollOptionView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val views: ItemPollOptionBinding

    init {
        inflate(context, R.layout.item_poll_option, this)
        views = ItemPollOptionBinding.bind(this)
    }

    fun render(state: PollOptionViewState) {
        views.optionNameTextView.text = state.optionAnswer

        when (state) {
            is PollOptionViewState.PollSending -> renderPollSending()
            is PollOptionViewState.PollEnded -> renderPollEnded(state)
            is PollOptionViewState.PollReady -> renderPollReady()
            is PollOptionViewState.PollVoted -> renderPollVoted(state)
            is PollOptionViewState.PollUndisclosed -> renderPollUndisclosed(state)
        }
    }

    private fun renderPollSending() {
        views.optionCheckImageView.isVisible = false
        views.optionVoteCountTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
        hideVotes()
        renderVoteSelection(false)
    }

    private fun renderPollEnded(state: PollOptionViewState.PollEnded) {
        views.optionCheckImageView.isVisible = false
        val drawableStart = if (state.isWinner) R.drawable.ic_poll_winner else 0
        views.optionVoteCountTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(drawableStart, 0, 0, 0)
        views.optionVoteCountTextView.setTextColor(
                if (state.isWinner) ThemeUtils.getColor(context, com.google.android.material.R.attr.colorPrimary)
                else ThemeUtils.getColor(context, im.vector.lib.ui.styles.R.attr.vctr_content_secondary)
        )
        showVotes(state.voteCount, state.votePercentage)
        renderVoteSelection(state.isWinner)
    }

    private fun renderPollReady() {
        views.optionCheckImageView.isVisible = true
        views.optionVoteCountTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
        hideVotes()
        renderVoteSelection(false)
    }

    private fun renderPollVoted(state: PollOptionViewState.PollVoted) {
        views.optionCheckImageView.isVisible = true
        views.optionVoteCountTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
        showVotes(state.voteCount, state.votePercentage)
        renderVoteSelection(state.isSelected)
    }

    private fun renderPollUndisclosed(state: PollOptionViewState.PollUndisclosed) {
        views.optionCheckImageView.isVisible = true
        views.optionVoteCountTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
        hideVotes()
        renderVoteSelection(state.isSelected)
    }

    private fun showVotes(voteCount: Int, votePercentage: Double) {
        views.optionVoteCountTextView.apply {
            isVisible = true
            text = resources.getQuantityString(CommonPlurals.poll_option_vote_count, voteCount, voteCount)
        }
        views.optionVoteProgress.apply {
            val progressValue = (votePercentage * 100).toInt()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                setProgress(progressValue, true)
            } else {
                progress = progressValue
            }
        }
    }

    private fun hideVotes() {
        views.optionVoteCountTextView.isVisible = false
        views.optionVoteProgress.progress = 0
    }

    private fun renderVoteSelection(isSelected: Boolean) {
        if (isSelected) {
            views.optionBorderImageView.setAttributeTintedImageResource(R.drawable.bg_poll_option, com.google.android.material.R.attr.colorPrimary)
            views.optionVoteProgress.progressDrawable = AppCompatResources.getDrawable(context, R.drawable.poll_option_progressbar_checked)
            views.optionCheckImageView.setImageResource(R.drawable.poll_option_checked)
        } else {
            views.optionBorderImageView.setAttributeTintedImageResource(R.drawable.bg_poll_option, im.vector.lib.ui.styles.R.attr.vctr_content_quinary)
            views.optionVoteProgress.progressDrawable = AppCompatResources.getDrawable(context, R.drawable.poll_option_progressbar_unchecked)
            views.optionCheckImageView.setImageResource(R.drawable.poll_option_unchecked)
        }
    }
}
