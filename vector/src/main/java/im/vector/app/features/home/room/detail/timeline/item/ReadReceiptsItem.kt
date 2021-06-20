/*
 * Copyright (c) 2021 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.home.room.detail.timeline.item

import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import com.airbnb.epoxy.EpoxyModelWithHolder
import im.vector.app.R
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.onClick
import im.vector.app.core.ui.views.BubbleDependentView
import im.vector.app.core.ui.views.ReadReceiptsView
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.themes.BubbleThemeUtils
import timber.log.Timber

@EpoxyModelClass(layout = R.layout.item_timeline_event_read_receipts)
abstract class ReadReceiptsItem : EpoxyModelWithHolder<ReadReceiptsItem.Holder>(), ItemWithEvents, BubbleDependentView<ReadReceiptsItem.Holder> {

    @EpoxyAttribute lateinit var eventId: String
    @EpoxyAttribute lateinit var readReceipts: List<ReadReceiptData>
    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash) lateinit var avatarRenderer: AvatarRenderer
    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash) lateinit var clickListener: ClickListener

    override fun canAppendReadMarker(): Boolean = false

    override fun getEventIds(): List<String> = listOf(eventId)

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.readReceiptsView.onClick(clickListener)
        holder.readReceiptsView.render(readReceipts, avatarRenderer)

        updateMessageBubble(holder.readReceiptsView.context, holder)
    }

    override fun unbind(holder: Holder) {
        holder.readReceiptsView.unbind(avatarRenderer)
        super.unbind(holder)
    }

    class Holder : VectorEpoxyHolder() {
        val readReceiptsView by bind<ReadReceiptsView>(R.id.readReceiptsView)
    }

    override fun setBubbleLayout(holder: Holder, bubbleStyle: String, bubbleStyleSetting: String, reverseBubble: Boolean) {
        val defaultDirection = holder.readReceiptsView.resources.configuration.layoutDirection;
        val defaultRtl = defaultDirection == View.LAYOUT_DIRECTION_RTL
        val reverseDirection = if (defaultRtl) View.LAYOUT_DIRECTION_LTR else View.LAYOUT_DIRECTION_RTL

        // Always keep read receipts of others on other side for dual side bubbles
        val dualBubbles = BubbleThemeUtils.drawsDualSide(bubbleStyleSetting)

        /*
        val receiptParent = holder.readReceiptsView.parent
        if (receiptParent is LinearLayout) {
            (holder.readReceiptsView.layoutParams as LinearLayout.LayoutParams).gravity = if (dualBubbles) Gravity.START else Gravity.END

            (receiptParent.layoutParams as RelativeLayout.LayoutParams).removeRule(RelativeLayout.END_OF)
            (receiptParent.layoutParams as RelativeLayout.LayoutParams).removeRule(RelativeLayout.ALIGN_PARENT_START)
            if (dualBubbles) {
                (receiptParent.layoutParams as RelativeLayout.LayoutParams).addRule(RelativeLayout.ALIGN_PARENT_START, RelativeLayout.TRUE)
            } else {
                (receiptParent.layoutParams as RelativeLayout.LayoutParams).addRule(RelativeLayout.END_OF, R.id.messageStartGuideline)
            }
        } else if (receiptParent is RelativeLayout) {
            if (dualBubbles) {
                (holder.readReceiptsView.layoutParams as RelativeLayout.LayoutParams).removeRule(RelativeLayout.ALIGN_PARENT_END)
            } else {
                (holder.readReceiptsView.layoutParams as RelativeLayout.LayoutParams).addRule(RelativeLayout.ALIGN_PARENT_END)
            }
        } else if (receiptParent is FrameLayout) {
         */
        if (dualBubbles) {
            (holder.readReceiptsView.layoutParams as FrameLayout.LayoutParams).gravity = Gravity.START
        } else {
            (holder.readReceiptsView.layoutParams as FrameLayout.LayoutParams).gravity = Gravity.END
        }
        /*
        } else {
            Timber.e("Unsupported layout for read receipts parent: $receiptParent")
        }
         */

        // Also set rtl to have members fill from the natural side
        setFlatRtl(holder.readReceiptsView, if (dualBubbles) reverseDirection else defaultDirection, defaultDirection)
    }
}
