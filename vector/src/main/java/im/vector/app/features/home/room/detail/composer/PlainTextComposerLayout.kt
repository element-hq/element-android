/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.composer

import android.content.Context
import android.net.Uri
import android.text.Editable
import android.text.format.DateUtils
import android.util.AttributeSet
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.text.toSpannable
import androidx.core.view.isVisible
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.extensions.getVectorLastMessageContent
import im.vector.app.core.extensions.setTextIfDifferent
import im.vector.app.core.extensions.showKeyboard
import im.vector.app.core.utils.DimensionConverter
import im.vector.app.databinding.ComposerLayoutBinding
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.detail.timeline.helper.MatrixItemColorProvider
import im.vector.app.features.home.room.detail.timeline.image.buildImageContentRendererData
import im.vector.app.features.html.EventHtmlRenderer
import im.vector.app.features.html.PillsPostProcessor
import im.vector.app.features.media.ImageContentRenderer
import im.vector.lib.strings.CommonStrings
import org.commonmark.parser.Parser
import org.matrix.android.sdk.api.session.room.model.message.MessageAudioContent
import org.matrix.android.sdk.api.session.room.model.message.MessageBeaconInfoContent
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.session.room.model.message.MessageEndPollContent
import org.matrix.android.sdk.api.session.room.model.message.MessageFormat
import org.matrix.android.sdk.api.session.room.model.message.MessagePollContent
import org.matrix.android.sdk.api.session.room.model.message.MessageTextContent
import org.matrix.android.sdk.api.util.ContentUtils
import org.matrix.android.sdk.api.util.MatrixItem
import org.matrix.android.sdk.api.util.toMatrixItem
import javax.inject.Inject

/**
 * Encapsulate the timeline composer UX.
 */
@AndroidEntryPoint
class PlainTextComposerLayout @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), MessageComposerView {

    @Inject lateinit var avatarRenderer: AvatarRenderer
    @Inject lateinit var matrixItemColorProvider: MatrixItemColorProvider
    @Inject lateinit var eventHtmlRenderer: EventHtmlRenderer
    @Inject lateinit var dimensionConverter: DimensionConverter
    @Inject lateinit var imageContentRenderer: ImageContentRenderer
    @Inject lateinit var pillsPostProcessorFactory: PillsPostProcessor.Factory

    private val views: ComposerLayoutBinding

    override var callback: Callback? = null

    override val text: Editable?
        get() = views.composerEditText.text

    override val formattedText: String? = null

    override val editText: EditText
        get() = views.composerEditText

    @Suppress("RedundantNullableReturnType")
    override val emojiButton: ImageButton?
        get() = views.composerEmojiButton

    override val sendButton: ImageButton
        get() = views.sendButton

    override val attachmentButton: ImageButton
        get() = views.attachmentButton

    init {
        inflate(context, R.layout.composer_layout, this)
        views = ComposerLayoutBinding.bind(this)

        views.composerEditText.maxLines = MessageComposerView.MAX_LINES_WHEN_COLLAPSED

        collapse()

        views.composerEditText.callback = object : ComposerEditText.Callback {
            override fun onRichContentSelected(contentUri: Uri): Boolean {
                return callback?.onRichContentSelected(contentUri) ?: false
            }

            override fun onTextChanged(text: CharSequence) {
                callback?.onTextChanged(text)
            }
        }
        views.composerRelatedMessageCloseButton.setOnClickListener {
            collapse()
            callback?.onCloseRelatedMessage()
        }

        views.sendButton.setOnClickListener {
            val textMessage = text?.toSpannable() ?: ""
            callback?.onSendMessage(textMessage)
        }

        views.attachmentButton.setOnClickListener {
            callback?.onAddAttachment()
        }
    }

    private fun collapse(transitionComplete: (() -> Unit)? = null) {
        views.relatedMessageGroup.isVisible = false
        transitionComplete?.invoke()
        callback?.onExpandOrCompactChange()
    }

    private fun expand(transitionComplete: (() -> Unit)? = null) {
        views.relatedMessageGroup.isVisible = true
        transitionComplete?.invoke()
        callback?.onExpandOrCompactChange()
    }

    override fun setTextIfDifferent(text: CharSequence?): Boolean {
        return views.composerEditText.setTextIfDifferent(text)
    }

    override fun renderComposerMode(mode: MessageComposerMode) {
        val specialMode = mode as? MessageComposerMode.Special
        if (specialMode != null) {
            renderSpecialMode(specialMode)
        } else if (mode is MessageComposerMode.Normal) {
            collapse()
            editText.setTextIfDifferent(mode.content)
        }

        views.sendButton.apply {
            if (mode is MessageComposerMode.Edit) {
                contentDescription = resources.getString(CommonStrings.action_save)
                setImageResource(R.drawable.ic_composer_rich_text_save)
            } else {
                contentDescription = resources.getString(CommonStrings.action_send)
                setImageResource(R.drawable.ic_rich_composer_send)
            }
        }
    }

    private fun renderSpecialMode(specialMode: MessageComposerMode.Special) {
        val event = specialMode.event
        val defaultContent = specialMode.defaultContent

        val iconRes: Int = when (specialMode) {
            is MessageComposerMode.Reply -> R.drawable.ic_reply
            is MessageComposerMode.Edit -> R.drawable.ic_edit
            is MessageComposerMode.Quote -> R.drawable.ic_quote
        }

        val pillsPostProcessor = pillsPostProcessorFactory.create(event.roomId)

        // switch to expanded bar
        views.composerRelatedMessageTitle.apply {
            text = event.senderInfo.disambiguatedDisplayName
            setTextColor(matrixItemColorProvider.getColor(MatrixItem.UserItem(event.root.senderId ?: "@")))
        }

        val messageContent: MessageContent? = event.getVectorLastMessageContent()
        val nonFormattedBody = when (messageContent) {
            is MessageAudioContent -> getAudioContentBodyText(messageContent)
            is MessagePollContent -> messageContent.getBestPollCreationInfo()?.question?.getBestQuestion()
            is MessageBeaconInfoContent -> resources.getString(CommonStrings.live_location_description)
            is MessageEndPollContent -> resources.getString(CommonStrings.message_reply_to_ended_poll_preview)
            else -> messageContent?.body.orEmpty()
        }
        var formattedBody: CharSequence? = null
        if (messageContent is MessageTextContent && messageContent.format == MessageFormat.FORMAT_MATRIX_HTML) {
            val parser = Parser.builder().build()

            val bodyToParse = messageContent.formattedBody?.let {
                ContentUtils.extractUsefulTextFromHtmlReply(it)
            } ?: ContentUtils.extractUsefulTextFromReply(messageContent.body)

            val document = parser.parse(bodyToParse)
            formattedBody = eventHtmlRenderer.render(document, pillsPostProcessor)
        }
        views.composerRelatedMessageContent.text = (formattedBody ?: nonFormattedBody)

        // Image Event
        val data = event.buildImageContentRendererData(dimensionConverter.dpToPx(66))
        val isImageVisible = if (data != null) {
            imageContentRenderer.render(data, ImageContentRenderer.Mode.THUMBNAIL, views.composerRelatedMessageImage)
            true
        } else {
            imageContentRenderer.clear(views.composerRelatedMessageImage)
            false
        }

        views.composerRelatedMessageImage.isVisible = isImageVisible

        views.composerRelatedMessageActionIcon.setImageDrawable(ContextCompat.getDrawable(context, iconRes))

        avatarRenderer.render(event.senderInfo.toMatrixItem(), views.composerRelatedMessageAvatar)

        val content = if (specialMode is MessageComposerMode.Edit) {
            formattedBody ?: defaultContent
        } else {
            defaultContent
        }

        views.composerEditText.setText(content)

        expand {
            // need to do it here also when not using quick reply
            if (isVisible) {
                showKeyboard(andRequestFocus = true)
            }
            views.composerRelatedMessageImage.isVisible = isImageVisible
        }
    }

    private fun getAudioContentBodyText(messageContent: MessageAudioContent): String {
        val formattedDuration = DateUtils.formatElapsedTime(((messageContent.audioInfo?.duration ?: 0) / 1000).toLong())
        return if (messageContent.voiceMessageIndicator != null) {
            resources.getString(CommonStrings.voice_message_reply_content, formattedDuration)
        } else {
            resources.getString(CommonStrings.audio_message_reply_content, messageContent.body, formattedDuration)
        }
    }
}
