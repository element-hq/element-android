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

import android.content.Context
import android.text.TextUtils
import android.text.method.MovementMethod
import android.widget.LinearLayout
import androidx.core.text.PrecomputedTextCompat
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.ui.views.FooteredTextView
import im.vector.app.features.home.room.detail.timeline.TimelineEventController
import im.vector.app.features.home.room.detail.timeline.tools.findPillsAndProcess
import im.vector.app.features.home.room.detail.timeline.url.PreviewUrlRetriever
import im.vector.app.features.home.room.detail.timeline.url.PreviewUrlUiState
import im.vector.app.features.home.room.detail.timeline.url.PreviewUrlView
import im.vector.app.features.media.ImageContentRenderer

@EpoxyModelClass(layout = R.layout.item_timeline_event_base)
abstract class MessageTextItem : AbsMessageItem<MessageTextItem.Holder>() {

    @EpoxyAttribute
    var searchForPills: Boolean = false

    @EpoxyAttribute
    var message: CharSequence? = null

    @EpoxyAttribute
    var useBigFont: Boolean = false

    @EpoxyAttribute
    var previewUrlRetriever: PreviewUrlRetriever? = null

    @EpoxyAttribute
    var previewUrlCallback: TimelineEventController.PreviewUrlCallback? = null

    @EpoxyAttribute
    var imageContentRenderer: ImageContentRenderer? = null

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var movementMethod: MovementMethod? = null

    private val previewUrlViewUpdater = PreviewUrlViewUpdater()

    override fun bind(holder: Holder) {
        // Revert potential MATCH_PARENT setting for url preview, before binding previewUrlRetriever
        //holder.messageLayout.layoutParams.width = LinearLayout.LayoutParams.WRAP_CONTENT

        // Preview URL
        previewUrlViewUpdater.holder = holder
        previewUrlViewUpdater.previewUrlView = holder.previewUrlView
        previewUrlViewUpdater.imageContentRenderer = imageContentRenderer
        val safePreviewUrlRetriever = previewUrlRetriever
        if (safePreviewUrlRetriever == null) {
            holder.previewUrlView.isVisible = false
        } else {
            safePreviewUrlRetriever.addListener(attributes.informationData.eventId, previewUrlViewUpdater)
        }
        holder.previewUrlView.delegate = previewUrlCallback

        if (useBigFont) {
            holder.messageView.textSize = 44F
        } else {
            holder.messageView.textSize = 14F
        }
        if (searchForPills) {
            message?.findPillsAndProcess(coroutineScope) {
                // mmm.. not sure this is so safe in regards to cell reuse
                it.bind(holder.messageView)
            }
        }
        var m = message
        if (m != null && m.isNotEmpty()) {
            // Remove last trailing newline: looks especially bad in message bubble
            if (m.last() == '\n') {
                m = m.subSequence(0, m.length-1)
            }
            // Add a narrow non-breakable space to work around wrap_content cutting italic text | https://stackoverflow.com/questions/4353836/italic-textview-with-wrap-contents-seems-to-clip-the-text-at-right-edge
            // (interestingly, this seems to be only relevant for the last character even for multi-line messages)
            m = TextUtils.concat(m, "\u202f")
        }
        val textFuture = PrecomputedTextCompat.getTextFuture(
                m ?: "",
                TextViewCompat.getTextMetricsParams(holder.messageView),
                null)
        super.bind(holder)
        holder.messageView.movementMethod = movementMethod

        renderSendState(holder.messageView, holder.messageView)
        holder.messageView.setOnClickListener(attributes.itemClickListener)
        holder.messageView.setOnLongClickListener(attributes.itemLongClickListener)
        holder.messageView.setTextFuture(textFuture)
    }

    override fun unbind(holder: Holder) {
        super.unbind(holder)
        previewUrlViewUpdater.previewUrlView = null
        previewUrlViewUpdater.imageContentRenderer = null
        previewUrlRetriever?.removeListener(attributes.informationData.eventId, previewUrlViewUpdater)
    }

    override fun getViewType() = STUB_ID

    class Holder : AbsMessageItem.Holder(STUB_ID) {
        val messageLayout by bind<LinearLayout>(R.id.messageTextLayout) // TODO match_parent if url preview, else wrap_content
        val messageView by bind<FooteredTextView>(R.id.messageTextView)
        val previewUrlView by bind<PreviewUrlView>(R.id.messageUrlPreview)
    }

    inner class PreviewUrlViewUpdater : PreviewUrlRetriever.PreviewUrlRetrieverListener {
        var previewUrlView: PreviewUrlView? = null
        var holder: Holder? = null
        var imageContentRenderer: ImageContentRenderer? = null

        override fun onStateUpdated(state: PreviewUrlUiState) {
            val safeImageContentRenderer = imageContentRenderer
            if (safeImageContentRenderer == null) {
                previewUrlView?.isVisible = false
                return
            }
            previewUrlView?.render(state, safeImageContentRenderer)

            // Don't reserve footer space in message view, but preview view | TODO
            /*
            previewUrlView?.footerWidth = holder?.messageView?.footerWidth ?: 0
            previewUrlView?.footerHeight = holder?.messageView?.footerHeight ?: 0
            holder?.messageView?.footerWidth = 0
            holder?.messageView?.footerHeight = 0
             */
            // Reserve more space for URL previews
            //holder?.messageLayout?.layoutParams?.width = LinearLayout.LayoutParams.MATCH_PARENT
            // Also increase width for the viewStubContainer, as set in AbsMessageItem (using getViewStubMinimumWidth)
            // We can use an unrealistic high number here, because we reduce bubble width by margins
            // TODO dis not working reliably...
            /*
            holder?.viewStubContainer?.minimumWidth = 1000000
            holder?.viewStubContainer?.parent?.requestLayout()
            holder?.viewStubContainer?.forceLayout()
            holder?.viewStubContainer?.requestLayout()
            holder?.messageLayout?.forceLayout()
            holder?.messageLayout?.requestLayout()
            holder?.messageView?.forceLayout()
            holder?.previewUrlView?.forceLayout()
             */
            //holder?.viewStubContainer?.layoutParams = holder?.viewStubContainer?.layoutParams
        }
    }
    companion object {
        private const val STUB_ID = R.id.messageContentTextStub
    }

    override fun messageBubbleAllowed(context: Context): Boolean {
        return true
    }

    override fun allowFooterOverlay(holder: Holder): Boolean {
        return true
    }

    override fun needsFooterReservation(holder: Holder): Boolean {
        return true
    }

    override fun reserveFooterSpace(holder: Holder, width: Int, height: Int) {
        holder.messageView.footerWidth = width
        holder.messageView.footerHeight = height
    }
}
