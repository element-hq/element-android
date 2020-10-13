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

import android.text.method.MovementMethod
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.PrecomputedTextCompat
import androidx.core.widget.TextViewCompat
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.features.home.room.detail.timeline.tools.findPillsAndProcess

@EpoxyModelClass(layout = R.layout.item_timeline_event_base)
abstract class MessageTextItem : AbsMessageItem<MessageTextItem.Holder>() {

    @EpoxyAttribute
    var searchForPills: Boolean = false

    @EpoxyAttribute
    var message: CharSequence? = null

    @EpoxyAttribute
    var useBigFont: Boolean = false

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var movementMethod: MovementMethod? = null

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
    }

    override fun getViewType() = STUB_ID

    class Holder : AbsMessageItem.Holder(STUB_ID) {
        val messageView by bind<AppCompatTextView>(R.id.messageTextView)
    }

    companion object {
        private const val STUB_ID = R.id.messageContentTextStub
    }
}
