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
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyAttribute
import kotlin.math.max
import kotlin.math.round
import im.vector.app.R
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.onClick
import im.vector.app.core.ui.views.SendStateImageView
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.detail.timeline.MessageColorProvider
import im.vector.app.features.home.room.detail.timeline.TimelineEventController
import im.vector.app.features.themes.BubbleThemeUtils
import im.vector.app.features.themes.ThemeUtils
import kotlin.math.ceil

/**
 * Base timeline item that adds an optional information bar with the sender avatar, name, time, send state
 * Adds associated click listeners (on avatar, displayname)
 */
abstract class AbsMessageItem<H : AbsMessageItem.Holder> : AbsBaseMessageItem<H>() {

    override val baseAttributes: AbsBaseMessageItem.Attributes
        get() = attributes

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

    override fun bind(holder: H) {
        super.bind(holder)
        val contentInBubble = infoInBubbles(holder.memberNameView.context)
        val senderInBubble = senderNameInBubble(holder.memberNameView.context)

        val avatarImageView: ImageView?
        var memberNameView: TextView?
        var timeView: TextView?
        val hiddenViews = ArrayList<View>()
        val invisibleViews = ArrayList<View>()

        val canHideAvatar = canHideAvatars()
        val canHideSender = canHideSender()

        // Select which views are visible, based on bubble style and other criteria
        if (attributes.informationData.showInformation) {
            if (senderInBubble) {
                memberNameView = holder.bubbleMemberNameView
                hiddenViews.add(holder.memberNameView)
            } else {
                memberNameView = holder.memberNameView
                hiddenViews.add(holder.bubbleMemberNameView)
            }
            if (contentInBubble) {
                timeView = holder.bubbleTimeView
                hiddenViews.add(holder.timeView)
            } else {
                timeView = holder.timeView
                hiddenViews.add(holder.bubbleTimeView)
            }
        } else if (attributes.informationData.forceShowTimestamp) {
            memberNameView = null
            //hiddenViews.add(holder.memberNameView) // this one get's some special hiding treatment below
            hiddenViews.add(holder.bubbleMemberNameView)
            if (contentInBubble) {
                timeView = holder.bubbleTimeView
                hiddenViews.add(holder.timeView)

                hiddenViews.add(holder.memberNameView)
            } else {
                timeView = holder.timeView
                hiddenViews.add(holder.bubbleTimeView)

                // Set to INVISIBLE instead of adding to hiddenViews, which are set to GONE
                // (upstream sets memberNameView.isInvisible = true here, which is effectively the same)
                invisibleViews.add(holder.memberNameView)
            }
        } else {
            memberNameView = null
            hiddenViews.add(holder.memberNameView)
            hiddenViews.add(holder.bubbleMemberNameView)
            timeView = null
            hiddenViews.add(holder.timeView)
            hiddenViews.add(holder.bubbleTimeView)
        }

        if (timeView === holder.bubbleTimeView) {
            // We have two possible bubble time view locations
            // For code readability, we don't inline this setting in the above cases
            if (BubbleThemeUtils.getBubbleTimeLocation(holder.bubbleTimeView.context) == BubbleThemeUtils.BUBBLE_TIME_BOTTOM) {
                timeView = holder.bubbleFooterTimeView
                if (attributes.informationData.showInformation) {
                    if (canHideSender) {
                        // In the case of footer time, we can also hide the names without making it look awkward
                        if (memberNameView != null) {
                            hiddenViews.add(memberNameView)
                            memberNameView = null
                        }
                        hiddenViews.add(holder.bubbleTimeView)
                    } else if (!senderInBubble) {
                        // We don't need to reserve space here
                        hiddenViews.add(holder.bubbleTimeView)
                    } else {
                        // Don't completely remove, just hide, so our relative layout rules still work
                        invisibleViews.add(holder.bubbleTimeView)
                    }
                } else {
                    // Do hide, or we accidentally reserve space
                    hiddenViews.add(holder.bubbleTimeView)
                }
            } else {
                hiddenViews.add(holder.bubbleFooterTimeView)
            }
        }

        // Dual-side bubbles: hide own avatar, and all in direct chats
        if ((!attributes.informationData.showInformation) ||
                (contentInBubble && canHideAvatar)) {
            avatarImageView = null
            hiddenViews.add(holder.avatarImageView)
        } else {
            avatarImageView = holder.avatarImageView
        }

        // Views available in upstream Element
        avatarImageView?.layoutParams = avatarImageView?.layoutParams?.apply {
            height = attributes.avatarSize
            width = attributes.avatarSize
        }
        avatarImageView?.visibility = View.VISIBLE
        avatarImageView?.onClick(_avatarClickListener)
        memberNameView?.visibility = View.VISIBLE
        memberNameView?.onClick(_memberNameClickListener)
        timeView?.visibility = View.VISIBLE
        timeView?.text = attributes.informationData.time
        memberNameView?.text = attributes.informationData.memberName
        memberNameView?.setTextColor(attributes.getMemberNameColor())
        if (avatarImageView != null) attributes.avatarRenderer.render(attributes.informationData.matrixItem, avatarImageView)
        avatarImageView?.setOnLongClickListener(attributes.itemLongClickListener)
        memberNameView?.setOnLongClickListener(attributes.itemLongClickListener)

        // More extra views added by Schildi
        if (senderInBubble) {
            holder.viewStubContainer.minimumWidth = getViewStubMinimumWidth(holder, contentInBubble, attributes.informationData.showInformation)
        } else {
            holder.viewStubContainer.minimumWidth = 0
        }
        if (contentInBubble) {
            holder.bubbleFootView.visibility = View.VISIBLE
        } else {
            hiddenViews.add(holder.bubbleFootView)
        }

        // Actually hide all unnecessary views
        hiddenViews.forEach {
            // Same as it.isVisible = false
            it.visibility = View.GONE
        }
        invisibleViews.forEach {
            // Same as it.isInvisible = true
            it.visibility = View.INVISIBLE
        }

        // Render send state indicator
        if (contentInBubble) {
            // Bubbles have their own decoration in the anonymous read receipt (in the message footer)
            holder.sendStateImageView.render(SendStateDecoration.NONE)
            holder.eventSendingIndicator.isVisible = false
        } else {
            holder.sendStateImageView.render(attributes.informationData.sendStateDecoration)
            holder.eventSendingIndicator.isVisible = attributes.informationData.sendStateDecoration == SendStateDecoration.SENDING_MEDIA
        }
    }

    override fun unbind(holder: H) {
        attributes.avatarRenderer.clear(holder.avatarImageView)
        holder.avatarImageView.setOnClickListener(null)
        holder.avatarImageView.setOnLongClickListener(null)
        holder.memberNameView.setOnClickListener(null)
        holder.memberNameView.setOnLongClickListener(null)
        super.unbind(holder)
    }

    private fun Attributes.getMemberNameColor() = messageColorProvider.getMemberNameTextColor(informationData.matrixItem)

    abstract class Holder(@IdRes stubId: Int) : AbsBaseMessageItem.Holder(stubId) {
        val avatarImageView by bind<ImageView>(R.id.messageAvatarImageView)
        val memberNameView by bind<TextView>(R.id.messageMemberNameView)
        val timeView by bind<TextView>(R.id.messageTimeView)
        val eventBaseView by bind<RelativeLayout>(R.id.eventBaseView)
        val bubbleView by bind<ViewGroup>(R.id.bubbleView)
        val bubbleMemberNameView by bind<TextView>(R.id.bubbleMessageMemberNameView)
        val bubbleTimeView by bind<TextView>(R.id.bubbleMessageTimeView)
        val bubbleFootView by bind<LinearLayout>(R.id.bubbleFootView)
        val bubbleFooterTimeView by bind<TextView>(R.id.bubbleFooterMessageTimeView)
        val bubbleFooterReadReceipt by bind<ImageView>(R.id.bubbleFooterReadReceipt)
        val viewStubContainer by bind<FrameLayout>(R.id.viewStubContainer)
        val sendStateImageView by bind<SendStateImageView>(R.id.messageSendStateImageView)
        val eventSendingIndicator by bind<ProgressBar>(R.id.eventSendingIndicator)
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
        return infoInBubbles(context) && canHideAvatars()
    }

    open fun getViewStubMinimumWidth(holder: H, contentInBubble: Boolean, showInformation: Boolean): Int {
        val memberName = attributes.informationData.memberName.toString()
        val time = attributes.informationData.time.toString()
        return if (contentInBubble) {
            if (BubbleThemeUtils.getBubbleTimeLocation(holder.bubbleTimeView.context) == BubbleThemeUtils.BUBBLE_TIME_BOTTOM) {
                if (attributes.informationData.showInformation && !canHideSender()) {
                    // Since timeView automatically gets enough space, either within or outside the viewStub, we just need to ensure the member name view has enough space
                    // Somehow not enough without extra space...
                    ceil(BubbleThemeUtils.guessTextWidth(holder.bubbleMemberNameView, "$memberName ")).toInt()
                } else {
                    // wrap_content works!
                    0
                }
            } else if (attributes.informationData.showInformation || attributes.informationData.forceShowTimestamp) {
                // Guess text width for name and time next to each other
                val text = if (attributes.informationData.showInformation) {
                    "$memberName $time"
                } else {
                    time
                }
                val textSize = if (attributes.informationData.showInformation) {
                    max(holder.bubbleMemberNameView.textSize, holder.bubbleTimeView.textSize)
                } else {
                    holder.bubbleTimeView.textSize
                }
                ceil(BubbleThemeUtils.guessTextWidth(textSize, text)).toInt()
            } else {
                // Not showing any header, use wrap_content of content only
                0
            }
        } else {
            0
        }
    }

    private fun infoInBubbles(context: Context): Boolean {
        return BubbleThemeUtils.getBubbleStyle(context) == BubbleThemeUtils.BUBBLE_STYLE_BOTH &&
                (messageBubbleAllowed(context) || pseudoBubbleAllowed())
    }

    private fun senderNameInBubble(context: Context): Boolean {
        return infoInBubbles(context) && !pseudoBubbleAllowed()
    }

    override fun shouldReverseBubble(): Boolean {
        return attributes.informationData.sentByMe
    }

    open fun getBubbleMargin(resources: Resources, bubbleStyle: String, reverseBubble: Boolean): Int {
        return when (bubbleStyle) {
            BubbleThemeUtils.BUBBLE_STYLE_START,
            BubbleThemeUtils.BUBBLE_STYLE_START_HIDDEN -> 0
            // else: dual-side bubbles (getBubbleMargin should not get called for other bubbleStyles)
            else -> {
                when {
                    // Direct chats usually have avatars hidden on both sides
                    attributes.informationData.isDirect -> resources.getDimensionPixelSize(R.dimen.dual_bubble_both_sides_without_avatar_margin)
                    // No direct chat, but sent by me: other side has an avatar
                    attributes.informationData.sentByMe -> {
                        resources.getDimensionPixelSize(R.dimen.dual_bubble_one_side_without_avatar_margin) +
                                resources.getDimensionPixelSize(R.dimen.dual_bubble_one_side_avatar_offset) +
                                attributes.avatarSize
                    }
                    // No direct chat, sent by other: my side has hidden avatar
                    else -> resources.getDimensionPixelSize(R.dimen.dual_bubble_one_side_without_avatar_margin)
                }
            }
        }
    }

    /**
     * Whether to show the footer in front of the viewStub
     */
    open fun allowFooterOverlay(holder: H): Boolean {
        return false
    }

    /**
     * Whether to show the footer aligned below the viewStub - requires enough width!
     */
    open fun allowFooterBelow(holder: H): Boolean {
        return true
    }

    open fun needsFooterReservation(holder: H): Boolean {
        return false
    }

    open fun reserveFooterSpace(holder: H, width: Int, height: Int) {
    }

    private fun canHideAvatars(): Boolean {
        return attributes.informationData.sentByMe || attributes.informationData.isDirect
    }

    private fun canHideSender(): Boolean {
        return attributes.informationData.sentByMe ||
                (attributes.informationData.isDirect && attributes.informationData.senderId == attributes.informationData.dmChatPartnerId)
    }

    protected fun getFooterMeasures(holder: H): Array<Int> {
        val anonymousReadReceipt = BubbleThemeUtils.getVisibleAnonymousReadReceipts(holder.bubbleFootView.context,
                attributes.informationData.readReceiptAnonymous, attributes.informationData.sentByMe)
        return getFooterMeasures(holder, anonymousReadReceipt)
    }

    private fun getFooterMeasures(holder: H, anonymousReadReceipt: AnonymousReadReceipt): Array<Int> {
        val timeWidth: Int
        val timeHeight: Int
        if (BubbleThemeUtils.getBubbleTimeLocation(holder.bubbleTimeView.context) == BubbleThemeUtils.BUBBLE_TIME_BOTTOM) {
            // Guess text width for name and time
            timeWidth = ceil(BubbleThemeUtils.guessTextWidth(holder.bubbleFooterTimeView, attributes.informationData.time.toString())).toInt() + holder.bubbleFooterTimeView.paddingLeft + holder.bubbleFooterTimeView.paddingRight
            timeHeight = ceil(holder.bubbleFooterTimeView.textSize).toInt() + holder.bubbleFooterTimeView.paddingTop + holder.bubbleFooterTimeView.paddingBottom
        } else {
            timeWidth = 0
            timeHeight = 0
        }
        val readReceiptWidth: Int
        val readReceiptHeight: Int
        if (anonymousReadReceipt == AnonymousReadReceipt.NONE) {
            readReceiptWidth = 0
            readReceiptHeight = 0
        } else {
            readReceiptWidth = holder.bubbleFooterReadReceipt.maxWidth + holder.bubbleFooterReadReceipt.paddingLeft + holder.bubbleFooterReadReceipt.paddingRight
            readReceiptHeight = holder.bubbleFooterReadReceipt.maxHeight + holder.bubbleFooterReadReceipt.paddingTop + holder.bubbleFooterReadReceipt.paddingBottom
        }

        var footerWidth = timeWidth + readReceiptWidth
        var footerHeight = max(timeHeight, readReceiptHeight)
        // Reserve extra padding, if we do have actual content
        if (footerWidth > 0) {
            footerWidth += holder.bubbleFootView.paddingLeft + holder.bubbleFootView.paddingRight
        }
        if (footerHeight > 0) {
            footerHeight += holder.bubbleFootView.paddingTop + holder.bubbleFootView.paddingBottom
        }
        return arrayOf(footerWidth, footerHeight)
    }

    @SuppressLint("RtlHardcoded")
    override fun setBubbleLayout(holder: H, bubbleStyle: String, bubbleStyleSetting: String, reverseBubble: Boolean) {
        super.setBubbleLayout(holder, bubbleStyle, bubbleStyleSetting, reverseBubble)

        //val bubbleView = holder.eventBaseView
        val bubbleView = holder.bubbleView
        val contentInBubble = infoInBubbles(holder.memberNameView.context)

        val defaultDirection = holder.eventBaseView.resources.configuration.layoutDirection;
        val defaultRtl = defaultDirection == View.LAYOUT_DIRECTION_RTL
        val reverseDirection = if (defaultRtl) View.LAYOUT_DIRECTION_LTR else View.LAYOUT_DIRECTION_RTL

        when (bubbleStyle) {
            BubbleThemeUtils.BUBBLE_STYLE_START,
            BubbleThemeUtils.BUBBLE_STYLE_BOTH,
            BubbleThemeUtils.BUBBLE_STYLE_BOTH_HIDDEN,
            BubbleThemeUtils.BUBBLE_STYLE_START_HIDDEN -> {
                // Padding for bubble content: long for side with tail, short for other sides
                val longPadding: Int
                val shortPadding: Int
                if (BubbleThemeUtils.drawsActualBubbles(bubbleStyle)) {
                    if (attributes.informationData.showInformation) {
                        bubbleView.setBackgroundResource(if (reverseBubble) R.drawable.msg_bubble_outgoing else R.drawable.msg_bubble_incoming)
                    } else {
                        bubbleView.setBackgroundResource(if (reverseBubble) R.drawable.msg_bubble2_outgoing else R.drawable.msg_bubble2_incoming)
                    }
                    val tintColor = ColorStateList(
                            arrayOf(intArrayOf(0)),
                            intArrayOf(ThemeUtils.getColor(bubbleView.context,
                                    if (attributes.informationData.sentByMe) R.attr.sc_message_bg_outgoing else R.attr.sc_message_bg_incoming)
                            )
                    )
                    bubbleView.backgroundTintList = tintColor
                    longPadding = 20
                    shortPadding = 8
                } else {
                    longPadding = 10
                    shortPadding = 0//if (attributes.informationData.showInformation && !hideSenderInformation()) { 8 } else { 0 }
                }
                val density = bubbleView.resources.displayMetrics.density
                if (reverseBubble != defaultRtl) {
                    // Use left/right instead of start/end: bubbleView is always LTR
                    (bubbleView.layoutParams as ViewGroup.MarginLayoutParams).leftMargin = getBubbleMargin(bubbleView.resources, bubbleStyle, reverseBubble)
                    (bubbleView.layoutParams as ViewGroup.MarginLayoutParams).rightMargin = 0
                } else {
                    (bubbleView.layoutParams as ViewGroup.MarginLayoutParams).leftMargin = 0
                    (bubbleView.layoutParams as ViewGroup.MarginLayoutParams).rightMargin = getBubbleMargin(bubbleView.resources, bubbleStyle, reverseBubble)
                }
                /*
                (bubbleView.layoutParams as RelativeLayout.LayoutParams).marginStart = round(20*density).toInt()
                (bubbleView.layoutParams as RelativeLayout.LayoutParams).topMargin = round(8*density).toInt()
                (bubbleView.layoutParams as RelativeLayout.LayoutParams).bottomMargin = round(8*density).toInt()
                 */
                val shortPaddingDp = round(shortPadding * density).toInt()
                val longPaddingDp = round(longPadding * density).toInt()
                if (reverseBubble != defaultRtl) {
                    // Use left/right instead of start/end: bubbleView is always LTR
                    bubbleView.setPadding(
                            shortPaddingDp,
                            shortPaddingDp,
                            longPaddingDp,
                            shortPaddingDp
                    )
                } else {
                    bubbleView.setPadding(
                            longPaddingDp,
                            shortPaddingDp,
                            shortPaddingDp,
                            shortPaddingDp
                    )
                }

                if (contentInBubble) {
                    val anonymousReadReceipt = BubbleThemeUtils.getVisibleAnonymousReadReceipts(holder.bubbleFootView.context,
                            attributes.informationData.readReceiptAnonymous, attributes.informationData.sentByMe)

                    when (anonymousReadReceipt) {
                        AnonymousReadReceipt.PROCESSING -> {
                            holder.bubbleFooterReadReceipt.visibility = View.VISIBLE
                            holder.bubbleFooterReadReceipt.setImageResource(R.drawable.ic_processing_msg)
                        }
                        else                            -> {
                            holder.bubbleFooterReadReceipt.visibility = View.GONE
                        }
                    }

                    // We can't use end and start because of our weird layout RTL tricks
                    val alignEnd = if(defaultRtl) RelativeLayout.ALIGN_LEFT else RelativeLayout.ALIGN_RIGHT
                    val alignStart = if(defaultRtl) RelativeLayout.ALIGN_RIGHT else RelativeLayout.ALIGN_LEFT
                    val startOf = if(defaultRtl) RelativeLayout.RIGHT_OF else RelativeLayout.LEFT_OF
                    val endOf = if(defaultRtl) RelativeLayout.LEFT_OF else RelativeLayout.RIGHT_OF

                    val footerLayoutParams = holder.bubbleFootView.layoutParams as RelativeLayout.LayoutParams
                    var footerMarginStartDp = 4
                    var footerMarginEndDp = 1
                    if (allowFooterOverlay(holder)) {
                        footerLayoutParams.addRule(RelativeLayout.ALIGN_BOTTOM, R.id.viewStubContainer)
                        footerLayoutParams.addRule(alignEnd, R.id.viewStubContainer)
                        footerLayoutParams.removeRule(alignStart)
                        footerLayoutParams.removeRule(RelativeLayout.BELOW)
                        footerLayoutParams.removeRule(endOf)
                        footerLayoutParams.removeRule(startOf)
                        if (needsFooterReservation(holder)) {
                            // Remove style used when not having reserved space
                            removeFooterOverlayStyle(holder, density)

                            // Calculate required footer space
                            val footerMeasures = getFooterMeasures(holder, anonymousReadReceipt)
                            val footerWidth = footerMeasures[0]
                            val footerHeight = footerMeasures[1]

                            reserveFooterSpace(holder, footerWidth, footerHeight)
                        } else {
                            // We have no reserved space -> style it to ensure readability on arbitrary backgrounds
                            styleFooterOverlay(holder, density)
                        }
                    } else {
                        when {
                            allowFooterBelow(holder) -> {
                                footerLayoutParams.addRule(RelativeLayout.BELOW, R.id.viewStubContainer)
                                footerLayoutParams.addRule(alignEnd, R.id.viewStubContainer)
                                footerLayoutParams.removeRule(alignStart)
                                footerLayoutParams.removeRule(RelativeLayout.ALIGN_BOTTOM)
                                footerLayoutParams.removeRule(endOf)
                                footerLayoutParams.removeRule(startOf)
                                footerLayoutParams.removeRule(RelativeLayout.START_OF)
                            }
                            reverseBubble            -> /* force footer on the left / at the start */ {
                                footerLayoutParams.addRule(RelativeLayout.START_OF, R.id.viewStubContainer)
                                footerLayoutParams.addRule(RelativeLayout.ALIGN_BOTTOM, R.id.viewStubContainer)
                                footerLayoutParams.removeRule(alignEnd)
                                footerLayoutParams.removeRule(alignStart)
                                footerLayoutParams.removeRule(endOf)
                                footerLayoutParams.removeRule(startOf)
                                footerLayoutParams.removeRule(RelativeLayout.BELOW)
                                // Reverse margins
                                footerMarginStartDp = 1
                                // 4 as previously the start margin, +4 to compensate the missing inner padding for the textView which we have on the other side
                                footerMarginEndDp = 8
                            }
                            else                     -> /* footer on the right / at the end */ {
                                footerLayoutParams.addRule(endOf, R.id.viewStubContainer)
                                footerLayoutParams.addRule(RelativeLayout.ALIGN_BOTTOM, R.id.viewStubContainer)
                                footerLayoutParams.removeRule(startOf)
                                footerLayoutParams.removeRule(alignEnd)
                                footerLayoutParams.removeRule(alignStart)
                                footerLayoutParams.removeRule(RelativeLayout.BELOW)
                                footerLayoutParams.removeRule(RelativeLayout.START_OF)
                            }
                        }
                        removeFooterOverlayStyle(holder, density)
                    }
                    if (defaultRtl) {
                        footerLayoutParams.rightMargin = round(footerMarginStartDp * density).toInt()
                        footerLayoutParams.leftMargin = round(footerMarginEndDp * density).toInt()
                        holder.bubbleMemberNameView.gravity = Gravity.RIGHT
                    } else {
                        footerLayoutParams.leftMargin = round(footerMarginStartDp * density).toInt()
                        footerLayoutParams.rightMargin = round(footerMarginEndDp * density).toInt()
                        holder.bubbleMemberNameView.gravity = Gravity.LEFT
                    }
                }
                if (bubbleStyle == BubbleThemeUtils.BUBBLE_STYLE_BOTH_HIDDEN) {
                    // We need to align the non-bubble member name view to pseudo bubbles
                    if (reverseBubble) {
                        holder.memberNameView.setPaddingRelative(
                                shortPaddingDp,
                                0,
                                longPaddingDp,
                                0
                        )
                    } else {
                        holder.memberNameView.setPaddingRelative(
                                longPaddingDp,
                                0,
                                shortPaddingDp,
                                0
                        )
                    }
                }
            }
            //BubbleThemeUtils.BUBBLE_STYLE_NONE,
            else -> {
                bubbleView.background = null
                (bubbleView.layoutParams as ViewGroup.MarginLayoutParams).marginStart = 0
                (bubbleView.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = 0
                /*
                (bubbleView.layoutParams as RelativeLayout.LayoutParams).marginStart = 0
                (bubbleView.layoutParams as RelativeLayout.LayoutParams).topMargin = 0
                (bubbleView.layoutParams as RelativeLayout.LayoutParams).bottomMargin = 0
                 */
                bubbleView.setPadding(0, 0, 0, 0)
                holder.memberNameView.setPadding(0, 0, 0, 0)
            }
        }

        /*
        holder.eventBaseView.layoutDirection = if (shouldRtl) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
        setRtl(shouldRtl)
         */
        (holder.bubbleView.layoutParams as FrameLayout.LayoutParams).gravity = if (reverseBubble) Gravity.END else Gravity.START
        //holder.informationBottom.layoutDirection = if (shouldRtl) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
        setFlatRtl(holder.reactionsContainer, if (reverseBubble) reverseDirection else defaultDirection,
                holder.eventBaseView.resources.configuration.layoutDirection)
        // Layout is broken if bubbleView is RTL (for some reason, Android uses left/end padding for right/start as well...)
        setFlatRtl(holder.bubbleView, View.LAYOUT_DIRECTION_LTR,
                holder.eventBaseView.resources.configuration.layoutDirection)
    }

    private fun tintFooter(holder: H, color: Int) {
        val tintList = ColorStateList(arrayOf(intArrayOf(0)), intArrayOf(color))
        holder.bubbleFooterReadReceipt.imageTintList = tintList
        holder.bubbleFooterTimeView.setTextColor(tintList)
    }

    private fun styleFooterOverlay(holder: H, density: Float) {
        holder.bubbleFootView.setBackgroundResource(R.drawable.timestamp_overlay)
        tintFooter(holder, ThemeUtils.getColor(holder.bubbleFootView.context, R.attr.timestamp_overlay_fg))
        val padding = round(2*density).toInt()
        holder.bubbleFootView.setPaddingRelative(
                padding,
                padding,
                padding + round(4*density).toInt(), // compensate from inner view padding on the other side
                padding
        )
    }

    private fun removeFooterOverlayStyle(holder: H, density: Float) {
        holder.bubbleFootView.background = null
        tintFooter(holder, ThemeUtils.getColor(holder.bubbleFootView.context, R.attr.vctr_content_secondary))
        holder.bubbleFootView.setPaddingRelative(
                0,
                round(4*density).toInt(),
                0,
                -round(1.5*density).toInt()
        )
    }
}
