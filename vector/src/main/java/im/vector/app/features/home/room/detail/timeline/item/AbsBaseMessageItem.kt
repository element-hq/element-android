/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.timeline.item

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat.getDrawable
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import im.vector.app.R
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.onClick
import im.vector.app.core.extensions.getDrawableAsSpannable
import im.vector.app.core.ui.views.ShieldImageView
import im.vector.app.core.utils.DimensionConverter
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.detail.timeline.MessageColorProvider
import im.vector.app.features.home.room.detail.timeline.TimelineEventController
import im.vector.app.features.home.room.detail.timeline.view.TimelineMessageLayoutRenderer
import im.vector.app.features.reactions.widget.ReactionButton
import im.vector.app.features.themes.ThemeUtils
import im.vector.lib.strings.CommonPlurals
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.session.room.send.SendState

private const val MAX_REACTIONS_TO_SHOW = 8

/**
 * Base timeline item with reactions and read receipts.
 * Manages associated click listeners and send status.
 * Should not be used as this, use a subclass.
 */
abstract class AbsBaseMessageItem<H : AbsBaseMessageItem.Holder>(@LayoutRes layoutId: Int) : BaseEventItem<H>(layoutId) {

    abstract val baseAttributes: Attributes

    private var reactionClickListener: ReactionButton.ReactedListener = object : ReactionButton.ReactedListener {
        override fun onReacted(reactionButton: ReactionButton) {
            baseAttributes.reactionPillCallback?.onClickOnReactionPill(baseAttributes.informationData, reactionButton.reactionString, true)
        }

        override fun onUnReacted(reactionButton: ReactionButton) {
            baseAttributes.reactionPillCallback?.onClickOnReactionPill(baseAttributes.informationData, reactionButton.reactionString, false)
        }

        override fun onLongClick(reactionButton: ReactionButton) {
            baseAttributes.reactionPillCallback?.onLongClickOnReactionPill(baseAttributes.informationData, reactionButton.reactionString)
        }
    }

    open fun shouldShowReactionAtBottom(): Boolean {
        return true
    }

    override fun getEventIds(): List<String> {
        return listOf(baseAttributes.informationData.eventId)
    }

    @SuppressLint("SetTextI18n")
    override fun bind(holder: H) {
        super.bind(holder)
        renderReactions(holder, baseAttributes.informationData.reactionsSummary)
        holder.e2EDecorationView.renderE2EDecoration(baseAttributes.informationData.e2eDecoration)
        holder.view.onClick(baseAttributes.itemClickListener)
        holder.view.setOnLongClickListener(baseAttributes.itemLongClickListener)
        (holder.view as? TimelineMessageLayoutRenderer)?.renderMessageLayout(baseAttributes.informationData.messageLayout)
    }

    private fun renderReactions(holder: H, reactionsSummary: ReactionsSummaryData) {
        val reactions = reactionsSummary.reactions
        if (!shouldShowReactionAtBottom() || reactions.isNullOrEmpty()) {
            holder.reactionsContainer.isVisible = false
        } else {
            holder.reactionsContainer.isVisible = true
            holder.reactionsContainer.removeAllViews()
            val reactionsToShow = if (reactionsSummary.showAll) {
                reactions
            } else {
                reactions.take(MAX_REACTIONS_TO_SHOW)
            }
            reactionsToShow.forEach { reaction ->
                val reactionButton = ReactionButton(holder.view.context)
                reactionButton.reactedListener = reactionClickListener
                reactionButton.setTag(R.id.reactionsContainer, reaction.key)
                reactionButton.reactionString = reaction.key
                reactionButton.reactionCount = reaction.count
                reactionButton.setChecked(reaction.addedByMe)
                reactionButton.isEnabled = reaction.synced
                holder.reactionsContainer.addView(reactionButton)
            }
            if (reactions.count() > MAX_REACTIONS_TO_SHOW) {
                val showReactionsTextView = createReactionTextView(holder)
                if (reactionsSummary.showAll) {
                    showReactionsTextView.setText(CommonStrings.message_reaction_show_less)
                    showReactionsTextView.onClick {
                        baseAttributes.reactionsSummaryEvents?.onShowLessClicked?.invoke()
                    }
                } else {
                    val moreCount = reactions.count() - MAX_REACTIONS_TO_SHOW
                    showReactionsTextView.text = holder.view.resources.getQuantityString(CommonPlurals.message_reaction_show_more, moreCount, moreCount)
                    showReactionsTextView.onClick {
                        baseAttributes.reactionsSummaryEvents?.onShowMoreClicked?.invoke()
                    }
                }
                holder.reactionsContainer.addView(showReactionsTextView)
            }
            val addMoreReactionsTextView = createReactionTextView(holder)

            addMoreReactionsTextView.text = holder.view.context.getDrawableAsSpannable(R.drawable.ic_add_reaction_small)
            addMoreReactionsTextView.onClick {
                baseAttributes.reactionsSummaryEvents?.onAddMoreClicked?.invoke()
            }
            holder.reactionsContainer.addView(addMoreReactionsTextView)
            holder.reactionsContainer.setOnLongClickListener(baseAttributes.itemLongClickListener)
        }
    }

    private fun createReactionTextView(holder: H): TextView {
        return TextView(ContextThemeWrapper(holder.view.context, im.vector.lib.ui.styles.R.style.TimelineReactionView)).apply {
            background = getDrawable(context, R.drawable.reaction_rounded_rect_shape_off)
            TextViewCompat.setTextAppearance(this, im.vector.lib.ui.styles.R.style.TextAppearance_Vector_Micro)
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(ThemeUtils.getColor(context, im.vector.lib.ui.styles.R.attr.vctr_content_secondary))
        }
    }

    override fun unbind(holder: H) {
        holder.reactionsContainer.setOnLongClickListener(null)
        super.unbind(holder)
    }

    protected open fun renderSendState(root: View, textView: TextView?, failureIndicator: ImageView? = null) {
        root.isClickable = baseAttributes.informationData.sendState.isSent()
        val state = if (baseAttributes.informationData.hasPendingEdits) SendState.UNSENT else baseAttributes.informationData.sendState
        textView?.setTextColor(baseAttributes.messageColorProvider.getMessageTextColor(state))
        failureIndicator?.isVisible = baseAttributes.informationData.sendState.hasFailed()
    }

    abstract class Holder(@IdRes stubId: Int) : BaseEventItem.BaseHolder(stubId) {
        val dimensionConverter by lazy {
            DimensionConverter(view.resources)
        }
        val reactionsContainer by bind<ViewGroup>(R.id.reactionsContainer)
        val e2EDecorationView by bind<ShieldImageView>(R.id.messageE2EDecoration)
    }

    /**
     * This class holds all the common attributes for timeline items.
     */
    interface Attributes {
        //            val avatarSize: Int,
        val informationData: MessageInformationData
        val avatarRenderer: AvatarRenderer
        val messageColorProvider: MessageColorProvider
        val itemLongClickListener: View.OnLongClickListener?
        val itemClickListener: ClickListener?

        //        val memberClickListener: ClickListener?
        val reactionPillCallback: TimelineEventController.ReactionPillCallback?
        val reactionsSummaryEvents: ReactionsSummaryEvents?

        //        val avatarCallback: TimelineEventController.AvatarCallback?
        val readReceiptsCallback: TimelineEventController.ReadReceiptsCallback?
//        val emojiTypeFace: Typeface?
    }

//    data class AbsAttributes(
//            override val informationData: MessageInformationData,
//            override val avatarRenderer: AvatarRenderer,
//            override val colorProvider: ColorProvider,
//            override val itemLongClickListener: View.OnLongClickListener? = null,
//            override val itemClickListener: ClickListener? = null,
//            override val reactionPillCallback: TimelineEventController.ReactionPillCallback? = null,
//            override val readReceiptsCallback: TimelineEventController.ReadReceiptsCallback? = null
//    ) : Attributes
}
