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

import android.graphics.Typeface
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.airbnb.epoxy.EpoxyAttribute
import im.vector.app.R
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.onClick
import im.vector.app.core.ui.views.SendStateImageView
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.detail.timeline.MessageColorProvider
import im.vector.app.features.home.room.detail.timeline.TimelineEventController
import org.matrix.android.sdk.api.session.threads.ThreadDetails
import org.matrix.android.sdk.api.util.MatrixItem

/**
 * Base timeline item that adds an optional information bar with the sender avatar, name, time, send state
 * Adds associated click listeners (on avatar, displayname)
 */
abstract class AbsMessageItem<H : AbsMessageItem.Holder> : AbsBaseMessageItem<H>() {

    override val baseAttributes: AbsBaseMessageItem.Attributes
        get() = attributes

    override fun isCacheable(): Boolean {
        return attributes.informationData.sendStateDecoration != SendStateDecoration.SENT
    }

    @EpoxyAttribute
    lateinit var attributes: Attributes

    private val _avatarClickListener = object : ClickListener {
        override fun invoke(p1: View) {
            attributes.avatarCallback?.onAvatarClicked(attributes.informationData)
        }
    }

    private val _memberNameClickListener = object : ClickListener {
        override fun invoke(p1: View) {
            attributes.avatarCallback?.onMemberNameClicked(attributes.informationData)
        }
    }

    private val _threadClickListener = object : ClickListener {
        override fun invoke(p1: View) {
            attributes.threadCallback?.onThreadSummaryClicked(attributes.informationData.eventId, attributes.threadDetails?.isRootThread ?: false)
        }
    }

    override fun bind(holder: H) {
        super.bind(holder)
        if (attributes.informationData.messageLayout.showAvatar) {
            holder.avatarImageView.layoutParams = holder.avatarImageView.layoutParams?.apply {
                height = attributes.avatarSize
                width = attributes.avatarSize
            }
            attributes.avatarRenderer.render(attributes.informationData.matrixItem, holder.avatarImageView)
            holder.avatarImageView.setOnLongClickListener(attributes.itemLongClickListener)
            holder.avatarImageView.isVisible = true
            holder.avatarImageView.onClick(_avatarClickListener)
        } else {
            holder.avatarImageView.setOnClickListener(null)
            holder.avatarImageView.setOnLongClickListener(null)
            holder.avatarImageView.isVisible = false
        }
        if (attributes.informationData.messageLayout.showDisplayName) {
            holder.memberNameView.isVisible = true
            holder.memberNameView.text = attributes.informationData.memberName
            holder.memberNameView.setTextColor(attributes.getMemberNameColor())
            holder.memberNameView.onClick(_memberNameClickListener)
            holder.memberNameView.setOnLongClickListener(attributes.itemLongClickListener)
        } else {
            holder.memberNameView.setOnClickListener(null)
            holder.memberNameView.setOnLongClickListener(null)
            holder.memberNameView.isVisible = false
        }
        if (attributes.informationData.messageLayout.showTimestamp) {
            holder.timeView.isVisible = true
            holder.timeView.text = attributes.informationData.time
        } else {
            holder.timeView.isVisible = false
        }

        // Render send state indicator
        holder.sendStateImageView.render(attributes.informationData.sendStateDecoration)
        holder.eventSendingIndicator.isVisible = attributes.informationData.sendStateDecoration == SendStateDecoration.SENDING_MEDIA

        // Threads
        if (attributes.areThreadMessagesEnabled) {
            holder.threadSummaryConstraintLayout.onClick(_threadClickListener)
            attributes.threadDetails?.let { threadDetails ->
                holder.threadSummaryConstraintLayout.isVisible = threadDetails.isRootThread
                holder.threadSummaryCounterTextView.text = threadDetails.numberOfThreads.toString()
                holder.threadSummaryInfoTextView.text = attributes.threadSummaryFormatted ?: attributes.decryptionErrorMessage

                val userId = threadDetails.threadSummarySenderInfo?.userId ?: return@let
                val displayName = threadDetails.threadSummarySenderInfo?.displayName
                val avatarUrl = threadDetails.threadSummarySenderInfo?.avatarUrl
                attributes.avatarRenderer.render(MatrixItem.UserItem(userId, displayName, avatarUrl), holder.threadSummaryAvatarImageView)
                updateHighlightedMessageHeight(holder, true)
            } ?: run {
                holder.threadSummaryConstraintLayout.isVisible = false
                updateHighlightedMessageHeight(holder, false)
            }
        }
    }

    private fun updateHighlightedMessageHeight(holder: Holder, isExpanded: Boolean) {
        holder.checkableBackground.updateLayoutParams<RelativeLayout.LayoutParams> {
            if (isExpanded) {
                addRule(RelativeLayout.ALIGN_BOTTOM, holder.threadSummaryConstraintLayout.id)
            } else {
                addRule(RelativeLayout.ALIGN_BOTTOM, holder.informationBottom.id)
            }
        }
    }

    override fun unbind(holder: H) {
        attributes.avatarRenderer.clear(holder.avatarImageView)
        holder.avatarImageView.setOnClickListener(null)
        holder.avatarImageView.setOnLongClickListener(null)
        holder.memberNameView.setOnClickListener(null)
        holder.memberNameView.setOnLongClickListener(null)
        attributes.avatarRenderer.clear(holder.threadSummaryAvatarImageView)
        holder.threadSummaryConstraintLayout.setOnClickListener(null)
        super.unbind(holder)
    }

    private fun Attributes.getMemberNameColor() = messageColorProvider.getMemberNameTextColor(informationData.matrixItem)

    abstract class Holder(@IdRes stubId: Int) : AbsBaseMessageItem.Holder(stubId) {

        val avatarImageView by bind<ImageView>(R.id.messageAvatarImageView)
        val memberNameView by bind<TextView>(R.id.messageMemberNameView)
        val timeView by bind<TextView>(R.id.messageTimeView)
        val sendStateImageView by bind<SendStateImageView>(R.id.messageSendStateImageView)
        val eventSendingIndicator by bind<ProgressBar>(R.id.eventSendingIndicator)
        val informationBottom by bind<LinearLayout>(R.id.informationBottom)
        val threadSummaryConstraintLayout by bind<ConstraintLayout>(R.id.messageThreadSummaryConstraintLayout)
        val threadSummaryCounterTextView by bind<TextView>(R.id.messageThreadSummaryCounterTextView)
        val threadSummaryAvatarImageView by bind<ImageView>(R.id.messageThreadSummaryAvatarImageView)
        val threadSummaryInfoTextView by bind<TextView>(R.id.messageThreadSummaryInfoTextView)
    }

    /**
     * This class holds all the common attributes for timeline items.
     */
    data class Attributes(
            val avatarSize: Int,
            override val informationData: MessageInformationData,
            override val avatarRenderer: AvatarRenderer,
            override val messageColorProvider: MessageColorProvider,
            override val itemLongClickListener: View.OnLongClickListener? = null,
            override val itemClickListener: ClickListener? = null,
            val memberClickListener: ClickListener? = null,
            override val reactionPillCallback: TimelineEventController.ReactionPillCallback? = null,
            val avatarCallback: TimelineEventController.AvatarCallback? = null,
            val threadCallback: TimelineEventController.ThreadCallback? = null,
            override val readReceiptsCallback: TimelineEventController.ReadReceiptsCallback? = null,
            val emojiTypeFace: Typeface? = null,
            val decryptionErrorMessage: String? = null,
            val threadSummaryFormatted: String? = null,
            val threadDetails: ThreadDetails? = null,
            val areThreadMessagesEnabled: Boolean = false,
            override val reactionsSummaryEvents: ReactionsSummaryEvents? = null,
    ) : AbsBaseMessageItem.Attributes {

        // Have to override as it's used to diff epoxy items
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Attributes

            if (avatarSize != other.avatarSize) return false
            if (informationData != other.informationData) return false
            if (threadDetails != other.threadDetails) return false

            return true
        }

        override fun hashCode(): Int {
            var result = avatarSize
            result = 31 * result + informationData.hashCode()
            result = 31 * result + threadDetails.hashCode()

            return result
        }
    }
}
