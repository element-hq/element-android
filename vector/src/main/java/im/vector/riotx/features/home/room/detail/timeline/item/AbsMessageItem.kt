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

package im.vector.riotx.features.home.room.detail.timeline.item

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.constraintlayout.helper.widget.Flow
import androidx.core.view.children
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyAttribute
import im.vector.matrix.android.api.session.room.send.SendState
import im.vector.riotx.R
import im.vector.riotx.core.resources.ColorProvider
import im.vector.riotx.core.utils.DebouncedClickListener
import im.vector.riotx.core.utils.DimensionUtils.dpToPx
import im.vector.riotx.features.home.AvatarRenderer
import im.vector.riotx.features.home.room.detail.timeline.TimelineEventController
import im.vector.riotx.features.reactions.widget.ReactionButton
import im.vector.riotx.features.ui.getMessageTextColor

private const val MAX_RECEIPT_DISPLAYED = 5

abstract class AbsMessageItem<H : AbsMessageItem.Holder> : BaseEventItem<H>() {

    @EpoxyAttribute
    lateinit var informationData: MessageInformationData

    @EpoxyAttribute
    lateinit var avatarRenderer: AvatarRenderer

    @EpoxyAttribute
    lateinit var colorProvider: ColorProvider

    @EpoxyAttribute
    var longClickListener: View.OnLongClickListener? = null

    @EpoxyAttribute
    var cellClickListener: View.OnClickListener? = null

    @EpoxyAttribute
    var memberClickListener: View.OnClickListener? = null

    @EpoxyAttribute
    var emojiTypeFace: Typeface? = null

    @EpoxyAttribute
    var reactionPillCallback: TimelineEventController.ReactionPillCallback? = null

    @EpoxyAttribute
    var avatarCallback: TimelineEventController.AvatarCallback? = null

    private val _avatarClickListener = DebouncedClickListener(View.OnClickListener {
        avatarCallback?.onAvatarClicked(informationData)
    })
    private val _memberNameClickListener = DebouncedClickListener(View.OnClickListener {
        avatarCallback?.onMemberNameClicked(informationData)
    })


    var reactionClickListener: ReactionButton.ReactedListener = object : ReactionButton.ReactedListener {
        override fun onReacted(reactionButton: ReactionButton) {
            reactionPillCallback?.onClickOnReactionPill(informationData, reactionButton.reactionString, true)
        }

        override fun onUnReacted(reactionButton: ReactionButton) {
            reactionPillCallback?.onClickOnReactionPill(informationData, reactionButton.reactionString, false)
        }

        override fun onLongClick(reactionButton: ReactionButton) {
            reactionPillCallback?.onLongClickOnReactionPill(informationData, reactionButton.reactionString)
        }
    }

    override fun bind(holder: H) {
        super.bind(holder)
        if (informationData.showInformation) {
            holder.avatarImageView.layoutParams = holder.avatarImageView.layoutParams?.apply {
                val size = dpToPx(avatarStyle.avatarSizeDP, holder.view.context)
                height = size
                width = size
            }
            holder.avatarImageView.visibility = View.VISIBLE
            holder.avatarImageView.setOnClickListener(_avatarClickListener)
            holder.memberNameView.visibility = View.VISIBLE
            holder.memberNameView.setOnClickListener(_memberNameClickListener)
            holder.timeView.visibility = View.VISIBLE
            holder.timeView.text = informationData.time
            holder.memberNameView.text = informationData.memberName
            avatarRenderer.render(informationData.avatarUrl, informationData.senderId, informationData.memberName?.toString(), holder.avatarImageView)
            holder.view.setOnClickListener(cellClickListener)
            holder.view.setOnLongClickListener(longClickListener)
            holder.avatarImageView.setOnLongClickListener(longClickListener)
            holder.memberNameView.setOnLongClickListener(longClickListener)
        } else {
            holder.avatarImageView.setOnClickListener(null)
            holder.memberNameView.setOnClickListener(null)
            holder.avatarImageView.visibility = View.GONE
            holder.memberNameView.visibility = View.GONE
            holder.timeView.visibility = View.GONE
            holder.view.setOnClickListener(null)
            holder.view.setOnLongClickListener(null)
            holder.avatarImageView.setOnLongClickListener(null)
            holder.memberNameView.setOnLongClickListener(null)
        }

        if (informationData.readReceipts.isNotEmpty()) {
            holder.readReceiptsView.isVisible = true
            for (index in 0 until MAX_RECEIPT_DISPLAYED) {
                val receiptData = informationData.readReceipts.getOrNull(index)
                if (receiptData == null) {
                    holder.receiptAvatars[index].isVisible = false
                } else {
                    holder.receiptAvatars[index].isVisible = true
                    avatarRenderer.render(receiptData.avatarUrl, receiptData.userId, receiptData.displayName, holder.receiptAvatars[index])
                }
            }
            if (informationData.readReceipts.size > MAX_RECEIPT_DISPLAYED) {
                holder.receiptMoreView.isVisible = true
                holder.receiptMoreView.text = holder.view.context.getString(
                        R.string.x_plus, informationData.readReceipts.size - MAX_RECEIPT_DISPLAYED
                )
            } else {
                holder.receiptMoreView.isVisible = false
            }
        } else {
            holder.readReceiptsView.isVisible = false
        }

        if (!shouldShowReactionAtBottom() || informationData.orderedReactionList.isNullOrEmpty()) {
            holder.reactionWrapper?.isVisible = false
        } else {
            //inflate if needed
            if (holder.reactionFlowHelper == null) {
                holder.reactionWrapper = holder.view.findViewById<ViewStub>(R.id.messageBottomInfo).inflate() as? ViewGroup
                holder.reactionFlowHelper = holder.view.findViewById(R.id.reactionsFlowHelper)
            }
            holder.reactionWrapper?.isVisible = true
            //clear all reaction buttons (but not the Flow helper!)
            holder.reactionWrapper?.children?.forEach { (it as? ReactionButton)?.isGone = true }
            val idToRefInFlow = ArrayList<Int>()
            informationData.orderedReactionList?.chunked(8)?.firstOrNull()?.forEachIndexed { index, reaction ->
                (holder.reactionWrapper?.children?.elementAtOrNull(index) as? ReactionButton)?.let { reactionButton ->
                    reactionButton.isVisible = true
                    reactionButton.reactedListener = reactionClickListener
                    reactionButton.setTag(R.id.messageBottomInfo, reaction.key)
                    idToRefInFlow.add(reactionButton.id)
                    reactionButton.reactionString = reaction.key
                    reactionButton.reactionCount = reaction.count
                    reactionButton.emojiTypeFace = emojiTypeFace
                    reactionButton.setChecked(reaction.addedByMe)
                    reactionButton.isEnabled = reaction.synced
                }
            }
            // Just setting the view as gone will break the FlowHelper (and invisible will take too much space),
            // so have to update ref ids
            holder.reactionFlowHelper?.referencedIds = idToRefInFlow.toIntArray()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 && !holder.view.isInLayout) {
                holder.reactionFlowHelper?.requestLayout()
            }
            holder.reactionWrapper?.setOnLongClickListener(longClickListener)
        }
    }

    open fun shouldShowReactionAtBottom(): Boolean {
        return true
    }

    protected open fun renderSendState(root: View, textView: TextView?, failureIndicator: ImageView? = null) {
        root.isClickable = informationData.sendState.isSent()
        val state = if (informationData.hasPendingEdits) SendState.UNSENT else informationData.sendState
        textView?.setTextColor(colorProvider.getMessageTextColor(state))
        failureIndicator?.isVisible = informationData.sendState.hasFailed()
    }

    abstract class Holder(@IdRes stubId: Int) : BaseHolder(stubId) {
        val avatarImageView by bind<ImageView>(R.id.messageAvatarImageView)
        val memberNameView by bind<TextView>(R.id.messageMemberNameView)
        val timeView by bind<TextView>(R.id.messageTimeView)
        val readReceiptsView by bind<ViewGroup>(R.id.readReceiptsView)
        val receiptAvatar1 by bind<ImageView>(R.id.message_avatar_receipt_1)
        val receiptAvatar2 by bind<ImageView>(R.id.message_avatar_receipt_2)
        val receiptAvatar3 by bind<ImageView>(R.id.message_avatar_receipt_3)
        val receiptAvatar4 by bind<ImageView>(R.id.message_avatar_receipt_4)
        val receiptAvatar5 by bind<ImageView>(R.id.message_avatar_receipt_5)
        val receiptMoreView by bind<TextView>(R.id.message_more_than_expected)
        val receiptAvatars: List<ImageView> by lazy {
            listOf(receiptAvatar1, receiptAvatar2, receiptAvatar3, receiptAvatar4, receiptAvatar5)
        }

        var reactionWrapper: ViewGroup? = null
        var reactionFlowHelper: Flow? = null
    }

}