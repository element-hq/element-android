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

package im.vector.riotredesign.features.home.room.detail.timeline.item

import android.text.Spannable
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.PrecomputedTextCompat
import androidx.core.widget.TextViewCompat
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.matrix.android.api.permalinks.MatrixLinkify
import im.vector.riotredesign.R
import im.vector.riotredesign.features.html.PillImageSpan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@EpoxyModelClass(layout = R.layout.item_timeline_event_text_message)
abstract class MessageTextItem : AbsMessageItem<MessageTextItem.Holder>() {

    @EpoxyAttribute var message: Spannable? = null
    @EpoxyAttribute override lateinit var informationData: MessageInformationData

    override fun bind(holder: Holder) {
        super.bind(holder)
        MatrixLinkify.addLinkMovementMethod(holder.messageView)
        val textFuture = PrecomputedTextCompat.getTextFuture(message ?: "",
                                                             TextViewCompat.getTextMetricsParams(holder.messageView),
                                                             null)
        holder.messageView.setTextFuture(textFuture)
        holder.messageView.renderSendState()
        holder.messageView.setOnLongClickListener(longClickListener)
        findPillsAndProcess { it.bind(holder.messageView) }
    }

    private fun findPillsAndProcess(processBlock: (span: PillImageSpan) -> Unit) {
        GlobalScope.launch(Dispatchers.Main) {
            val pillImageSpans: Array<PillImageSpan>? = withContext(Dispatchers.IO) {
                message?.let { spannable ->
                    spannable.getSpans(0, spannable.length, PillImageSpan::class.java)
                }
            }
            pillImageSpans?.forEach { processBlock(it) }
        }
    }

    class Holder : AbsMessageItem.Holder() {
        override val avatarImageView by bind<ImageView>(R.id.messageAvatarImageView)
        override val memberNameView by bind<TextView>(R.id.messageMemberNameView)
        override val timeView by bind<TextView>(R.id.messageTimeView)
        val messageView by bind<AppCompatTextView>(R.id.messageTextView)
    }


}