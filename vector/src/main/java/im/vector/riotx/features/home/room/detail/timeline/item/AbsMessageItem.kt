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

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Paint
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.core.view.children
import com.airbnb.epoxy.EpoxyAttribute
import im.vector.riotx.R
import im.vector.riotx.core.ui.views.ReadReceiptsView
import im.vector.riotx.core.utils.DebouncedClickListener
import im.vector.riotx.features.home.AvatarRenderer
import im.vector.riotx.features.home.room.detail.timeline.MessageColorProvider
import im.vector.riotx.features.home.room.detail.timeline.TimelineEventController
import im.vector.riotx.features.themes.BubbleThemeUtils
import im.vector.riotx.features.themes.ThemeUtils
import kotlin.math.max
import kotlin.math.round

/**
 * Base timeline item that adds an optional information bar with the sender avatar, name and time
 * Adds associated click listeners (on avatar, displayname)
 */
abstract class AbsMessageItem<H : AbsMessageItem.Holder> : AbsBaseMessageItem<H>() {

    override val baseAttributes: AbsBaseMessageItem.Attributes
        get() = attributes

    @EpoxyAttribute
    lateinit var attributes: Attributes

    private val _avatarClickListener = DebouncedClickListener(View.OnClickListener {
        attributes.avatarCallback?.onAvatarClicked(attributes.informationData)
    })
    private val _memberNameClickListener = DebouncedClickListener(View.OnClickListener {
        attributes.avatarCallback?.onMemberNameClicked(attributes.informationData)
    })

    override fun bind(holder: H) {
        super.bind(holder)
        val contentInBubble = infoInBubbles(holder.memberNameView.context)
        if (attributes.informationData.showInformation and (!contentInBubble || !attributes.informationData.sentByMe)) {
            holder.avatarImageView.layoutParams = holder.avatarImageView.layoutParams?.apply {
                height = attributes.avatarSize
                width = attributes.avatarSize
            }
            holder.avatarImageView.visibility = View.VISIBLE
            holder.avatarImageView.setOnClickListener(_avatarClickListener)
            //holder.memberNameView.visibility = View.VISIBLE
            holder.memberNameView.setOnClickListener(_memberNameClickListener)
            holder.bubbleMemberNameView.setOnClickListener(_memberNameClickListener)
            //holder.timeView.visibility = View.VISIBLE
            holder.timeView.text = attributes.informationData.time
            holder.bubbleTimeView.text = attributes.informationData.time
            holder.memberNameView.text = attributes.informationData.memberName
            holder.bubbleMemberNameView.text = attributes.informationData.memberName
            holder.memberNameView.setTextColor(attributes.getMemberNameColor())
            holder.bubbleMemberNameView.setTextColor(attributes.getMemberNameColor())
            attributes.avatarRenderer.render(attributes.informationData.matrixItem, holder.avatarImageView)
            holder.avatarImageView.setOnLongClickListener(attributes.itemLongClickListener)
            holder.memberNameView.setOnLongClickListener(attributes.itemLongClickListener)
            holder.bubbleMemberNameView.setOnLongClickListener(attributes.itemLongClickListener)
            if (contentInBubble) {
                holder.memberNameView.visibility = View.GONE
                holder.timeView.visibility = View.GONE
                holder.bubbleMemberNameView.visibility = View.VISIBLE
                holder.bubbleTimeView.visibility = View.VISIBLE
            } else {
                holder.memberNameView.visibility = View.VISIBLE
                holder.timeView.visibility = View.VISIBLE
                holder.bubbleMemberNameView.visibility = View.GONE
                holder.bubbleTimeView.visibility = View.GONE
            }
        } else {
            holder.avatarImageView.setOnClickListener(null)
            holder.memberNameView.setOnClickListener(null)
            holder.avatarImageView.visibility = View.GONE
            holder.memberNameView.visibility = View.GONE
            holder.timeView.visibility = View.GONE
            holder.avatarImageView.setOnLongClickListener(null)
            holder.memberNameView.setOnLongClickListener(null)
            if (attributes.informationData.showInformation /* && contentInBubble && attributes.informationData.sentByMe */) {
                holder.bubbleTimeView.visibility = View.VISIBLE
                holder.bubbleTimeView.text = attributes.informationData.time
                holder.bubbleMemberNameView.visibility = View.VISIBLE
                holder.bubbleMemberNameView.setOnClickListener(_memberNameClickListener)
                holder.bubbleMemberNameView.text = attributes.informationData.memberName
                holder.bubbleMemberNameView.setTextColor(attributes.getMemberNameColor())
                holder.bubbleMemberNameView.setOnLongClickListener(attributes.itemLongClickListener)
            } else {
                holder.bubbleTimeView.visibility = View.GONE
                holder.bubbleMemberNameView.setOnClickListener(null)
                holder.bubbleMemberNameView.visibility = View.GONE
                holder.bubbleMemberNameView.setOnLongClickListener(null)
            }
        }
        holder.viewStubContainer.minimumWidth = getViewStubMinimumWidth(holder, contentInBubble, attributes.informationData.showInformation)
    }

    override fun unbind(holder: H) {
        holder.avatarImageView.setOnClickListener(null)
        holder.avatarImageView.setOnLongClickListener(null)
        holder.memberNameView.setOnClickListener(null)
        holder.memberNameView.setOnLongClickListener(null)
        super.unbind(holder)
    }

    private fun Attributes.getMemberNameColor() = messageColorProvider.getMemberNameTextColor(informationData.senderId)

    abstract class Holder(@IdRes stubId: Int) : AbsBaseMessageItem.Holder(stubId) {
        val avatarImageView by bind<ImageView>(R.id.messageAvatarImageView)
        val memberNameView by bind<TextView>(R.id.messageMemberNameView)
        val timeView by bind<TextView>(R.id.messageTimeView)
        val eventBaseView by bind<RelativeLayout>(R.id.eventBaseView)
        val bubbleView by bind<View>(R.id.bubbleView)
        val bubbleMemberNameView by bind<TextView>(R.id.bubbleMessageMemberNameView)
        val bubbleTimeView by bind<TextView>(R.id.bubbleMessageTimeView)
        val viewStubContainer by bind<FrameLayout>(R.id.viewStubContainer)
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
            override val itemClickListener: View.OnClickListener? = null,
            val memberClickListener: View.OnClickListener? = null,
            override val reactionPillCallback: TimelineEventController.ReactionPillCallback? = null,
            val avatarCallback: TimelineEventController.AvatarCallback? = null,
            override val readReceiptsCallback: TimelineEventController.ReadReceiptsCallback? = null,
            val emojiTypeFace: Typeface? = null
    ) : AbsBaseMessageItem.Attributes {

        // Have to override as it's used to diff epoxy items
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Attributes

            if (avatarSize != other.avatarSize) return false
            if (informationData != other.informationData) return false

            return true
        }

        override fun hashCode(): Int {
            var result = avatarSize
            result = 31 * result + informationData.hashCode()
            return result
        }
    }

    override fun ignoreMessageGuideline(context: Context): Boolean {
        return infoInBubbles(context) && attributes.informationData.sentByMe
    }

    open fun getViewStubMinimumWidth(holder: H, contentInBubble: Boolean, showInformation: Boolean): Int {
        return if (contentInBubble && attributes.informationData.showInformation) {
            // Guess text width for name and time
            val text = holder.bubbleMemberNameView.text.toString() + " " + holder.bubbleTimeView.text.toString()
            val paint = Paint()
            paint.textSize = max(holder.bubbleMemberNameView.textSize, holder.bubbleTimeView.textSize)
            round(paint.measureText(text)).toInt()
        } else {
            0
        }
    }

    private fun infoInBubbles(context: Context): Boolean {
        return messageBubbleAllowed(context) && BubbleThemeUtils.getBubbleStyle(context) == BubbleThemeUtils.BUBBLE_STYLE_BOTH
    }

    override fun shouldReverseBubble(): Boolean {
        return attributes.informationData.sentByMe
    }

    open fun getBubbleMargin(density: Float, reverseBubble: Boolean): Int {
        return round(96*density).toInt()
    }

    override fun setBubbleLayout(holder: H, bubbleStyle: String, bubbleStyleSetting: String, reverseBubble: Boolean) {
        super.setBubbleLayout(holder, bubbleStyle, bubbleStyleSetting, reverseBubble)

        //val bubbleView = holder.eventBaseView
        val bubbleView = holder.bubbleView

        when (bubbleStyle) {
            BubbleThemeUtils.BUBBLE_STYLE_NONE                                      -> {
                bubbleView.background = null
                (bubbleView.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = 0
                /*
                (bubbleView.layoutParams as RelativeLayout.LayoutParams).marginStart = 0
                (bubbleView.layoutParams as RelativeLayout.LayoutParams).topMargin = 0
                (bubbleView.layoutParams as RelativeLayout.LayoutParams).bottomMargin = 0
                 */
                bubbleView.setPadding(0, 0, 0, 0)
            }
            BubbleThemeUtils.BUBBLE_STYLE_START, BubbleThemeUtils.BUBBLE_STYLE_BOTH -> {
                bubbleView.setBackgroundResource(if (reverseBubble) R.drawable.msg_bubble_outgoing else R.drawable.msg_bubble_incoming)
                var tintColor = ColorStateList(
                        arrayOf(intArrayOf(0)),
                        intArrayOf(ThemeUtils.getColor(bubbleView.context,
                                if (attributes.informationData.sentByMe) R.attr.sc_message_bg_outgoing else R.attr.sc_message_bg_incoming)
                        )
                )
                bubbleView.backgroundTintList = tintColor
                val density = bubbleView.resources.displayMetrics.density
                // TODO 96 = 2 * avatar size?
                if (reverseBubble) {
                    (bubbleView.layoutParams as ViewGroup.MarginLayoutParams).marginStart = getBubbleMargin(density, reverseBubble)
                    (bubbleView.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = 0
                } else {
                    (bubbleView.layoutParams as ViewGroup.MarginLayoutParams).marginStart = 0
                    (bubbleView.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = getBubbleMargin(density, reverseBubble)
                }
                /*
                (bubbleView.layoutParams as RelativeLayout.LayoutParams).marginStart = round(20*density).toInt()
                (bubbleView.layoutParams as RelativeLayout.LayoutParams).topMargin = round(8*density).toInt()
                (bubbleView.layoutParams as RelativeLayout.LayoutParams).bottomMargin = round(8*density).toInt()
                 */
                // TODO padding?
                if (reverseBubble) {
                    bubbleView.setPaddingRelative(
                            round(8 * density).toInt(),
                            round(8 * density).toInt(),
                            round(20 * density).toInt(),
                            round(8 * density).toInt()
                    )
                } else {
                    bubbleView.setPaddingRelative(
                            round(20 * density).toInt(),
                            round(8 * density).toInt(),
                            round(8 * density).toInt(),
                            round(8 * density).toInt()
                    )
                }
            }
        }

        val defaultDirection = holder.eventBaseView.resources.configuration.layoutDirection;
        val defaultRtl = defaultDirection == View.LAYOUT_DIRECTION_RTL
        val reverseDirection = if (defaultRtl) View.LAYOUT_DIRECTION_LTR else View.LAYOUT_DIRECTION_RTL
        /*
        holder.eventBaseView.layoutDirection = if (shouldRtl) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
        setRtl(shouldRtl)
         */
        (holder.bubbleView.layoutParams as FrameLayout.LayoutParams).gravity = if (reverseBubble) Gravity.END else Gravity.START
        //holder.informationBottom.layoutDirection = if (shouldRtl) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
        setFlatRtl(holder.reactionsContainer, if (reverseBubble) reverseDirection else defaultDirection,
                holder.eventBaseView.resources.configuration.layoutDirection)
    }
}
