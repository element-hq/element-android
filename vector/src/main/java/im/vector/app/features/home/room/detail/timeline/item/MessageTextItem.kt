/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.timeline.item

import android.text.Spanned
import android.text.method.MovementMethod
import android.view.ViewStub
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.PrecomputedTextCompat
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.onClick
import im.vector.app.core.epoxy.onLongClickIgnoringLinks
import im.vector.app.features.home.room.detail.timeline.TimelineEventController
import im.vector.app.features.home.room.detail.timeline.tools.findPillsAndProcess
import im.vector.app.features.home.room.detail.timeline.url.PreviewUrlRetriever
import im.vector.app.features.home.room.detail.timeline.url.PreviewUrlUiState
import im.vector.app.features.home.room.detail.timeline.url.PreviewUrlView
import im.vector.app.features.media.ImageContentRenderer
import im.vector.lib.core.utils.epoxy.charsequence.EpoxyCharSequence
import io.element.android.wysiwyg.EditorStyledTextView
import io.noties.markwon.MarkwonPlugin
import org.matrix.android.sdk.api.extensions.orFalse

@EpoxyModelClass
abstract class MessageTextItem : AbsMessageItem<MessageTextItem.Holder>() {

    @EpoxyAttribute
    var searchForPills: Boolean = false

    @EpoxyAttribute
    var message: EpoxyCharSequence? = null

    @EpoxyAttribute
    var bindingOptions: BindingOptions? = null

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

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var markwonPlugins: (List<MarkwonPlugin>)? = null

    @EpoxyAttribute
    var useRichTextEditorStyle: Boolean = false

    @EpoxyAttribute
    var originalMessage: EpoxyCharSequence? = null

    private val previewUrlViewUpdater = PreviewUrlViewUpdater()

    override fun bind(holder: Holder) {
        // Preview URL
        previewUrlViewUpdater.previewUrlView = holder.previewUrlView
        previewUrlViewUpdater.imageContentRenderer = imageContentRenderer
        val safePreviewUrlRetriever = previewUrlRetriever
        if (safePreviewUrlRetriever == null) {
            holder.previewUrlView.isVisible = false
        } else {
            safePreviewUrlRetriever.addListener(attributes.informationData.eventId, previewUrlViewUpdater)
        }
        holder.previewUrlView.delegate = previewUrlCallback
        holder.previewUrlView.renderMessageLayout(attributes.informationData.messageLayout)
        if (useRichTextEditorStyle) {
            holder.plainMessageView?.isVisible = false
        } else {
            holder.richMessageView?.isVisible = false
        }
        val messageView: AppCompatTextView = if (useRichTextEditorStyle) holder.requireRichMessageView() else holder.requirePlainMessageView()
        messageView.isVisible = true
        if (useBigFont) {
            messageView.textSize = 44F
        } else {
            messageView.textSize = 15.5F
        }
        if (searchForPills) {
            message?.charSequence?.findPillsAndProcess(coroutineScope) {
                // mmm.. not sure this is so safe in regards to cell reuse
                it.bind(messageView)
            }
        }
        message?.charSequence.let { charSequence ->
            markwonPlugins?.forEach { plugin -> plugin.beforeSetText(messageView, charSequence as Spanned) }
        }
        super.bind(holder)
        messageView.movementMethod = movementMethod
        renderSendState(messageView, messageView)
        messageView.onClick(attributes.itemClickListener)
        messageView.onLongClickIgnoringLinks(attributes.itemLongClickListener)
        messageView.setTextWithEmojiSupport(message?.charSequence, bindingOptions)
        markwonPlugins?.forEach { plugin -> plugin.afterSetText(messageView) }

        // Translation toggle
        val toggleView = holder.translationToggle
        val originalTextView = holder.originalTextView
        if (originalMessage != null) {
            toggleView.isVisible = true
            toggleView.text = holder.view.context.getString(R.string.translation_toggle_show_original)
            originalTextView.isVisible = false
            originalTextView.text = originalMessage?.charSequence
            toggleView.setOnClickListener {
                val isShowing = originalTextView.isVisible
                if (isShowing) {
                    originalTextView.isVisible = false
                    toggleView.text = holder.view.context.getString(R.string.translation_toggle_show_original)
                } else {
                    originalTextView.isVisible = true
                    toggleView.text = holder.view.context.getString(R.string.translation_toggle_hide_original)
                }
            }
        } else {
            toggleView.isVisible = false
            originalTextView.isVisible = false
            toggleView.setOnClickListener(null)
        }
    }

    private fun AppCompatTextView.setTextWithEmojiSupport(message: CharSequence?, bindingOptions: BindingOptions?) {
        if (bindingOptions?.canUseTextFuture.orFalse() && message != null) {
            val textFuture = PrecomputedTextCompat.getTextFuture(message, TextViewCompat.getTextMetricsParams(this), null)
            setTextFuture(textFuture)
        } else {
            setTextFuture(null)
            text = message
        }
    }

    override fun unbind(holder: Holder) {
        super.unbind(holder)
        previewUrlViewUpdater.previewUrlView = null
        previewUrlViewUpdater.imageContentRenderer = null
        previewUrlRetriever?.removeListener(attributes.informationData.eventId, previewUrlViewUpdater)
    }

    override fun getViewStubId() = STUB_ID

    class Holder : AbsMessageItem.Holder(STUB_ID) {
        val previewUrlView by bind<PreviewUrlView>(R.id.messageUrlPreview)
        val translationToggle by bind<TextView>(R.id.messageTranslationToggle)
        val originalTextView by bind<TextView>(R.id.messageOriginalText)
        private val richMessageStub by bind<ViewStub>(R.id.richMessageTextViewStub)
        private val plainMessageStub by bind<ViewStub>(R.id.plainMessageTextViewStub)
        var richMessageView: EditorStyledTextView? = null
            private set
        var plainMessageView: AppCompatTextView? = null
            private set

        fun requireRichMessageView(): AppCompatTextView {
            val view = richMessageView ?: richMessageStub.inflate().findViewById<EditorStyledTextView>(R.id.messageTextView).also {
                // Required to ensure that `inlineCodeBgHelper` and `codeBlockBgHelper` are initialized
                it.updateStyle(
                        styleConfig = it.styleConfig,
                        mentionDisplayHandler = null,
                )
            }
            richMessageView = view
            return view
        }

        fun requirePlainMessageView(): AppCompatTextView {
            val view = plainMessageView ?: plainMessageStub.inflate().findViewById(R.id.messageTextView)
            plainMessageView = view
            return view
        }
    }

    inner class PreviewUrlViewUpdater : PreviewUrlRetriever.PreviewUrlRetrieverListener {
        var previewUrlView: PreviewUrlView? = null
        var imageContentRenderer: ImageContentRenderer? = null

        override fun onStateUpdated(state: PreviewUrlUiState) {
            val safeImageContentRenderer = imageContentRenderer
            if (safeImageContentRenderer == null) {
                previewUrlView?.isVisible = false
                return
            }
            previewUrlView?.render(state, safeImageContentRenderer)
        }
    }

    companion object {
        private val STUB_ID = R.id.messageContentTextStub
    }
}
