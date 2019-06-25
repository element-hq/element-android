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

import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.PrecomputedTextCompat
import androidx.core.text.toSpannable
import androidx.core.widget.TextViewCompat
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.riotredesign.R
import im.vector.riotredesign.core.utils.containsOnlyEmojis
import im.vector.riotredesign.features.home.room.detail.timeline.TimelineEventController
import im.vector.riotredesign.features.html.PillImageSpan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.saket.bettermovementmethod.BetterLinkMovementMethod

@EpoxyModelClass(layout = R.layout.item_timeline_event_base)
abstract class MessageTextItem : AbsMessageItem<MessageTextItem.Holder>() {

    @EpoxyAttribute
    var message: CharSequence? = null
    @EpoxyAttribute
    override lateinit var informationData: MessageInformationData
    @EpoxyAttribute
    var urlClickCallback: TimelineEventController.UrlClickCallback? = null

    // TODO Move this instantiation somewhere else?
    private val mvmtMethod = BetterLinkMovementMethod.newInstance().also {
        it.setOnLinkClickListener { _, url ->
            //Return false to let android manage the click on the link, or true if the link is handled by the application
            urlClickCallback?.onUrlClicked(url) == true
        }
        it.setOnLinkLongClickListener { _, url ->
            //Long clicks are handled by parent, return true to block android to do something with url
            urlClickCallback?.onUrlLongClicked(url) == true
        }
    }

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.messageView.movementMethod = mvmtMethod


        val msg = message ?: ""
        if (msg.length <= 4 && containsOnlyEmojis(msg.toString())) {
            holder.messageView.textSize = 44F
        } else {
            holder.messageView.textSize = 14F
        }

        val textFuture = PrecomputedTextCompat.getTextFuture(message ?: "",
                TextViewCompat.getTextMetricsParams(holder.messageView),
                null)

        holder.messageView.setTextFuture(textFuture)
        holder.messageView.renderSendState()
        holder.messageView.setOnClickListener(cellClickListener)
        holder.messageView.setOnLongClickListener(longClickListener)
        findPillsAndProcess { it.bind(holder.messageView) }
    }

    private fun findPillsAndProcess(processBlock: (span: PillImageSpan) -> Unit) {
        GlobalScope.launch(Dispatchers.Main) {
            val pillImageSpans: Array<PillImageSpan>? = withContext(Dispatchers.IO) {
                message?.toSpannable()?.let { spannable ->
                    spannable.getSpans(0, spannable.length, PillImageSpan::class.java)
                }
            }
            pillImageSpans?.forEach { processBlock(it) }
        }
    }

    override fun getStubType(): Int = R.id.messageContentTextStub

    class Holder : AbsMessageItem.Holder() {
        val messageView by bind<AppCompatTextView>(R.id.messageTextView)
        override fun getStubId(): Int = R.id.messageContentTextStub

    }

}