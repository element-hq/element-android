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

import android.content.res.ColorStateList
import android.text.method.MovementMethod
import android.view.Gravity
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.text.PrecomputedTextCompat
import androidx.core.widget.TextViewCompat
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.riotx.R
import im.vector.riotx.features.home.room.detail.timeline.tools.findPillsAndProcess
import im.vector.riotx.features.themes.BubbleThemeUtils
import im.vector.riotx.features.themes.BubbleThemeUtils.BUBBLE_STYLE_BOTH
import im.vector.riotx.features.themes.BubbleThemeUtils.BUBBLE_STYLE_NONE
import im.vector.riotx.features.themes.BubbleThemeUtils.BUBBLE_STYLE_START
import im.vector.riotx.features.themes.ThemeUtils
import kotlin.math.round

@EpoxyModelClass(layout = R.layout.item_timeline_event_base)
abstract class MessageTextItem : AbsMessageItem<MessageTextItem.Holder>() {

    @EpoxyAttribute
    var searchForPills: Boolean = false
    @EpoxyAttribute
    var message: CharSequence? = null
    @EpoxyAttribute
    var useBigFont: Boolean = false
    @EpoxyAttribute
    var movementMethod: MovementMethod? = null
    @EpoxyAttribute
    var incomingMessage: Boolean = false
    @EpoxyAttribute
    var outgoingMessage: Boolean = false

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.messageView.movementMethod = movementMethod
        if (useBigFont) {
            holder.messageView.textSize = 44F
        } else {
            holder.messageView.textSize = 14F
        }
        renderSendState(holder.messageView, holder.messageView)
        holder.messageView.setOnClickListener(attributes.itemClickListener)
        holder.messageView.setOnLongClickListener(attributes.itemLongClickListener)
        if (searchForPills) {
            message?.findPillsAndProcess(coroutineScope) { it.bind(holder.messageView) }
        }
        val textFuture = PrecomputedTextCompat.getTextFuture(
                message ?: "",
                TextViewCompat.getTextMetricsParams(holder.messageView),
                null)
        holder.messageView.setTextFuture(textFuture)

        var bubbleStyle = if (incomingMessage || outgoingMessage) BubbleThemeUtils.getBubbleStyle(holder.messageView.context) else BUBBLE_STYLE_NONE
        when (bubbleStyle) {
            BUBBLE_STYLE_NONE -> {
                holder.messageView.background = null
                holder.messageView.setPadding(0, 0, 0, 0)
            }
            BUBBLE_STYLE_START, BUBBLE_STYLE_BOTH -> {
                holder.messageView.setBackgroundResource(R.drawable.msg_bubble_incoming)
                var tintColor = ColorStateList(
                        arrayOf(intArrayOf(0)),
                        intArrayOf(ThemeUtils.getColor(holder.messageView.context,
                                if (outgoingMessage) R.attr.sc_message_bg_outgoing else R.attr.sc_message_bg_incoming)
                        )
                )
                holder.messageView.backgroundTintList = tintColor
                val density = holder.messageView.resources.displayMetrics.density
                holder.messageView.setPaddingRelative(
                        round(20*density).toInt(),
                        round(8*density).toInt(),
                        round(8*density).toInt(),
                        round(8*density).toInt()
                )
            }
        }
        if (holder.messageView.layoutParams is FrameLayout.LayoutParams) {
            (holder.messageView.layoutParams as FrameLayout.LayoutParams).gravity =
                    if (outgoingMessage && bubbleStyle == BUBBLE_STYLE_BOTH) Gravity.END else Gravity.START
        }
    }

    override fun getViewType() = STUB_ID

    class Holder : AbsMessageItem.Holder(STUB_ID) {
        val messageView by bind<AppCompatTextView>(R.id.messageTextView)
    }

    companion object {
        private const val STUB_ID = R.id.messageContentTextStub
    }
}
