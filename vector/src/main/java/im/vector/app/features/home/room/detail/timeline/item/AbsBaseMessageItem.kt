/*
 * Copyright 2019 New Vector Ltd
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

import android.annotation.SuppressLint
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.core.content.ContextCompat.getDrawable
import androidx.core.view.isVisible
import im.vector.app.R
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.onClick
import im.vector.app.core.ui.views.ShieldImageView
import im.vector.app.core.utils.DimensionConverter
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.detail.timeline.MessageColorProvider
import im.vector.app.features.home.room.detail.timeline.TimelineEventController
import im.vector.app.features.home.room.detail.timeline.view.TimelineMessageLayoutRenderer
import im.vector.app.features.reactions.widget.ReactionButton
import org.matrix.android.sdk.api.crypto.RoomEncryptionTrustLevel
import org.matrix.android.sdk.api.session.room.send.SendState


private const val MAX_REACTIONS_TO_SHOW = 8

/**
 * Base timeline item with reactions and read receipts.
 * Manages associated click listeners and send status.
 * Should not be used as this, use a subclass.
 */
abstract class AbsBaseMessageItem<H : AbsBaseMessageItem.Holder> : BaseEventItem<H>() {

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
        when (baseAttributes.informationData.e2eDecoration) {
            E2EDecoration.NONE                 -> {
                holder.e2EDecorationView.render(null)
            }
            E2EDecoration.WARN_IN_CLEAR,
            E2EDecoration.WARN_SENT_BY_UNVERIFIED,
            E2EDecoration.WARN_SENT_BY_UNKNOWN -> {
                holder.e2EDecorationView.render(RoomEncryptionTrustLevel.Warning)
            }
        }

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
                val showReactionsTextView = createReactionTextView(holder, 6)
                if (reactionsSummary.showAll) {
                    showReactionsTextView.setText(R.string.message_reaction_show_less)
                    showReactionsTextView.onClick { reactionsSummary.onShowLessClicked() }
                } else {
                    val moreCount = reactions.count() - MAX_REACTIONS_TO_SHOW
                    showReactionsTextView.text = holder.view.resources.getString(R.string.message_reaction_show_more, moreCount)
                    showReactionsTextView.onClick { reactionsSummary.onShowMoreClicked() }
                }
                holder.reactionsContainer.addView(showReactionsTextView)
            }
            val addMoreReactionsTextView = createReactionTextView(holder, 14)
            addMoreReactionsTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_add_reaction_small, 0, 0, 0)
            addMoreReactionsTextView.onClick { reactionsSummary.onAddMoreClicked() }
            holder.reactionsContainer.addView(addMoreReactionsTextView)
            holder.reactionsContainer.setOnLongClickListener(baseAttributes.itemLongClickListener)
        }
    }

    private fun createReactionTextView(holder: H, horizontalPaddingDp: Int): TextView {
        return TextView(holder.view.context).apply {
            textSize = 10f
            gravity = Gravity.CENTER
            minimumHeight = resources.getDimensionPixelSize(R.dimen.chat_reaction_min_height)
            minimumWidth = resources.getDimensionPixelSize(R.dimen.chat_reaction_min_width)
            background = getDrawable(context, R.drawable.reaction_rounded_rect_shape_off)
            val padding = holder.dimensionConverter.dpToPx(horizontalPaddingDp)
            setPadding(padding, 0, padding, 0)
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
