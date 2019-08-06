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

package im.vector.riotx.features.home.room.detail.timeline.factory

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.view.View
import dagger.Lazy
import im.vector.matrix.android.api.permalinks.MatrixLinkify
import im.vector.matrix.android.api.permalinks.MatrixPermalinkSpan
import im.vector.matrix.android.api.session.events.model.RelationType
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.EditAggregatedSummary
import im.vector.matrix.android.api.session.room.model.message.*
import im.vector.matrix.android.api.session.room.send.SendState
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.matrix.android.api.session.room.timeline.getLastMessageContent
import im.vector.matrix.android.internal.crypto.attachments.toElementToDecrypt
import im.vector.matrix.android.internal.crypto.model.event.EncryptedEventContent
import im.vector.riotx.EmojiCompatFontProvider
import im.vector.riotx.R
import im.vector.riotx.core.epoxy.VectorEpoxyModel
import im.vector.riotx.core.linkify.VectorLinkify
import im.vector.riotx.core.resources.ColorProvider
import im.vector.riotx.core.resources.StringProvider
import im.vector.riotx.core.resources.UserPreferencesProvider
import im.vector.riotx.core.utils.DebouncedClickListener
import im.vector.riotx.features.home.AvatarRenderer
import im.vector.riotx.features.home.room.detail.timeline.TimelineEventController
import im.vector.riotx.features.home.room.detail.timeline.helper.ContentUploadStateTrackerBinder
import im.vector.riotx.features.home.room.detail.timeline.helper.TimelineMediaSizeProvider
import im.vector.riotx.features.home.room.detail.timeline.helper.senderAvatar
import im.vector.riotx.features.home.room.detail.timeline.item.*
import im.vector.riotx.features.home.room.detail.timeline.util.MessageInformationDataFactory
import im.vector.riotx.features.html.EventHtmlRenderer
import im.vector.riotx.features.media.ImageContentRenderer
import im.vector.riotx.features.media.VideoContentRenderer
import me.gujun.android.span.span
import javax.inject.Inject

class MessageItemFactory @Inject constructor(
        private val avatarRenderer: AvatarRenderer,
        private val colorProvider: ColorProvider,
        private val timelineMediaSizeProvider: TimelineMediaSizeProvider,
        private val htmlRenderer: Lazy<EventHtmlRenderer>,
        private val stringProvider: StringProvider,
        private val emojiCompatFontProvider: EmojiCompatFontProvider,
        private val imageContentRenderer: ImageContentRenderer,
        private val messageInformationDataFactory: MessageInformationDataFactory,
        private val contentUploadStateTrackerBinder: ContentUploadStateTrackerBinder,
        private val userPreferencesProvider: UserPreferencesProvider) {


    fun create(event: TimelineEvent,
               nextEvent: TimelineEvent?,
               highlight: Boolean,
               callback: TimelineEventController.Callback?
    ): VectorEpoxyModel<*>? {
        event.root.eventId ?: return null

        val informationData = messageInformationDataFactory.create(event, nextEvent)

        if (event.root.isRedacted()) {
            //message is redacted
            return buildRedactedItem(informationData, highlight, callback)
        }

        val messageContent: MessageContent =
                event.getLastMessageContent()
                        ?: //Malformed content, we should echo something on screen
                        return DefaultItem_().text(stringProvider.getString(R.string.malformed_message))

        if (messageContent.relatesTo?.type == RelationType.REPLACE
                || event.isEncrypted() && event.root.content.toModel<EncryptedEventContent>()?.relatesTo?.type == RelationType.REPLACE
        ) {
            // ignore replace event, the targeted id is already edited
            if (userPreferencesProvider.shouldShowHiddenEvents()) {
                //These are just for debug to display hidden event, they should be filtered out in normal mode
                val informationData = MessageInformationData(
                        eventId = event.root.eventId ?: "?",
                        senderId = event.root.senderId ?: "",
                        sendState = event.root.sendState,
                        time = "",
                        avatarUrl = event.senderAvatar(),
                        memberName = "",
                        showInformation = false
                )
                return NoticeItem_()
                        .avatarRenderer(avatarRenderer)
                        .informationData(informationData)
                        .noticeText("{ \"type\": ${event.root.getClearType()} }")
                        .highlighted(highlight)
                        .baseCallback(callback)
            } else {
                return BlankItem_()
            }
        }
//        val all = event.root.toContent()
//        val ev = all.toModel<Event>()
        return when (messageContent) {
            is MessageEmoteContent  -> buildEmoteMessageItem(messageContent,
                    informationData,
                    event.annotations?.editSummary,
                    highlight,
                    callback)
            is MessageTextContent   -> buildTextMessageItem(event.root.sendState,
                    messageContent,
                    informationData,
                    event.annotations?.editSummary,
                    highlight,
                    callback
            )
            is MessageImageContent  -> buildImageMessageItem(messageContent, informationData, highlight, callback)
            is MessageNoticeContent -> buildNoticeMessageItem(messageContent, informationData, highlight, callback)
            is MessageVideoContent  -> buildVideoMessageItem(messageContent, informationData, highlight, callback)
            is MessageFileContent   -> buildFileMessageItem(messageContent, informationData, highlight, callback)
            is MessageAudioContent  -> buildAudioMessageItem(messageContent, informationData, highlight, callback)
            else                    -> buildNotHandledMessageItem(messageContent, highlight)
        }
    }

    private fun buildAudioMessageItem(messageContent: MessageAudioContent,
                                      informationData: MessageInformationData,
                                      highlight: Boolean,
                                      callback: TimelineEventController.Callback?): MessageFileItem? {
        return MessageFileItem_()
                .avatarRenderer(avatarRenderer)
                .colorProvider(colorProvider)
                .informationData(informationData)
                .highlighted(highlight)
                .avatarCallback(callback)
                .filename(messageContent.body)
                .iconRes(R.drawable.filetype_audio)
                .reactionPillCallback(callback)
                .emojiTypeFace(emojiCompatFontProvider.typeface)
                .cellClickListener(
                        DebouncedClickListener(View.OnClickListener { view: View ->
                            callback?.onEventCellClicked(informationData, messageContent, view)
                        }))
                .clickListener(
                        DebouncedClickListener(View.OnClickListener {
                            callback?.onAudioMessageClicked(messageContent)
                        }))
                .longClickListener { view ->
                    return@longClickListener callback?.onEventLongClicked(informationData, messageContent, view)
                            ?: false
                }
    }

    private fun buildFileMessageItem(messageContent: MessageFileContent,
                                     informationData: MessageInformationData,
                                     highlight: Boolean,
                                     callback: TimelineEventController.Callback?): MessageFileItem? {
        return MessageFileItem_()
                .avatarRenderer(avatarRenderer)
                .colorProvider(colorProvider)
                .informationData(informationData)
                .highlighted(highlight)
                .avatarCallback(callback)
                .filename(messageContent.body)
                .reactionPillCallback(callback)
                .emojiTypeFace(emojiCompatFontProvider.typeface)
                .iconRes(R.drawable.filetype_attachment)
                .cellClickListener(
                        DebouncedClickListener(View.OnClickListener { view ->
                            callback?.onEventCellClicked(informationData, messageContent, view)
                        }))
                .longClickListener { view ->
                    return@longClickListener callback?.onEventLongClicked(informationData, messageContent, view)
                            ?: false
                }
                .clickListener(
                        DebouncedClickListener(View.OnClickListener { _ ->
                            callback?.onFileMessageClicked(informationData.eventId, messageContent)
                        }))
                .longClickListener { view ->
                    return@longClickListener callback?.onEventLongClicked(informationData, messageContent, view)
                            ?: false
                }
    }

    private fun buildNotHandledMessageItem(messageContent: MessageContent, highlight: Boolean): DefaultItem? {
        val text = "${messageContent.type} message events are not yet handled"
        return DefaultItem_()
                .text(text)
                .highlighted(highlight)
    }

    private fun buildImageMessageItem(messageContent: MessageImageContent,
                                      informationData: MessageInformationData,
                                      highlight: Boolean,
                                      callback: TimelineEventController.Callback?): MessageImageVideoItem? {

        val (maxWidth, maxHeight) = timelineMediaSizeProvider.getMaxSize()
        val data = ImageContentRenderer.Data(
                filename = messageContent.body,
                url = messageContent.getFileUrl(),
                elementToDecrypt = messageContent.encryptedFileInfo?.toElementToDecrypt(),
                height = messageContent.info?.height,
                maxHeight = maxHeight,
                width = messageContent.info?.width,
                maxWidth = maxWidth,
                orientation = messageContent.info?.orientation,
                rotation = messageContent.info?.rotation
        )
        return MessageImageVideoItem_()
                .avatarRenderer(avatarRenderer)
                .colorProvider(colorProvider)
                .imageContentRenderer(imageContentRenderer)
                .contentUploadStateTrackerBinder(contentUploadStateTrackerBinder)
                .playable(messageContent.info?.mimeType == "image/gif")
                .informationData(informationData)
                .highlighted(highlight)
                .avatarCallback(callback)
                .mediaData(data)
                .reactionPillCallback(callback)
                .emojiTypeFace(emojiCompatFontProvider.typeface)
                .clickListener(
                        DebouncedClickListener(View.OnClickListener { view ->
                            callback?.onImageMessageClicked(messageContent, data, view)
                        }))
                .cellClickListener(
                        DebouncedClickListener(View.OnClickListener { view ->
                            callback?.onEventCellClicked(informationData, messageContent, view)
                        }))
                .longClickListener { view ->
                    return@longClickListener callback?.onEventLongClicked(informationData, messageContent, view)
                            ?: false
                }
    }

    private fun buildVideoMessageItem(messageContent: MessageVideoContent,
                                      informationData: MessageInformationData,
                                      highlight: Boolean,
                                      callback: TimelineEventController.Callback?): MessageImageVideoItem? {

        val (maxWidth, maxHeight) = timelineMediaSizeProvider.getMaxSize()
        val thumbnailData = ImageContentRenderer.Data(
                filename = messageContent.body,
                url = messageContent.videoInfo?.thumbnailFile?.url
                        ?: messageContent.videoInfo?.thumbnailUrl,
                elementToDecrypt = messageContent.videoInfo?.thumbnailFile?.toElementToDecrypt(),
                height = messageContent.videoInfo?.height,
                maxHeight = maxHeight,
                width = messageContent.videoInfo?.width,
                maxWidth = maxWidth
        )

        val videoData = VideoContentRenderer.Data(
                eventId = informationData.eventId,
                filename = messageContent.body,
                url = messageContent.getFileUrl(),
                elementToDecrypt = messageContent.encryptedFileInfo?.toElementToDecrypt(),
                thumbnailMediaData = thumbnailData
        )

        return MessageImageVideoItem_()
                .imageContentRenderer(imageContentRenderer)
                .contentUploadStateTrackerBinder(contentUploadStateTrackerBinder)
                .avatarRenderer(avatarRenderer)
                .colorProvider(colorProvider)
                .playable(true)
                .informationData(informationData)
                .highlighted(highlight)
                .avatarCallback(callback)
                .mediaData(thumbnailData)
                .reactionPillCallback(callback)
                .emojiTypeFace(emojiCompatFontProvider.typeface)
                .cellClickListener(
                        DebouncedClickListener(View.OnClickListener { view ->
                            callback?.onEventCellClicked(informationData, messageContent, view)
                        }))
                .clickListener { view -> callback?.onVideoMessageClicked(messageContent, videoData, view) }
                .longClickListener { view ->
                    return@longClickListener callback?.onEventLongClicked(informationData, messageContent, view)
                            ?: false
                }
    }

    private fun buildTextMessageItem(sendState: SendState,
                                     messageContent: MessageTextContent,
                                     informationData: MessageInformationData,
                                     editSummary: EditAggregatedSummary?,
                                     highlight: Boolean,
                                     callback: TimelineEventController.Callback?): MessageTextItem? {

        val bodyToUse = messageContent.formattedBody?.let {
            htmlRenderer.get().render(it.trim())
        } ?: messageContent.body

        val linkifiedBody = linkifyBody(bodyToUse, callback)

        return MessageTextItem_()
                .apply {
                    if (informationData.hasBeenEdited) {
                        val spannable = annotateWithEdited(linkifiedBody, callback, informationData, editSummary)
                        message(spannable)
                    } else {
                        message(linkifiedBody)
                    }
                }
                .avatarRenderer(avatarRenderer)
                .informationData(informationData)
                .colorProvider(colorProvider)
                .highlighted(highlight)
                .avatarCallback(callback)
                .urlClickCallback(callback)
                .reactionPillCallback(callback)
                .emojiTypeFace(emojiCompatFontProvider.typeface)
                //click on the text
                .cellClickListener(
                        DebouncedClickListener(View.OnClickListener { view ->
                            callback?.onEventCellClicked(informationData, messageContent, view)
                        }))
                .longClickListener { view ->
                    return@longClickListener callback?.onEventLongClicked(informationData, messageContent, view)
                            ?: false
                }
    }

    private fun annotateWithEdited(linkifiedBody: CharSequence,
                                   callback: TimelineEventController.Callback?,
                                   informationData: MessageInformationData,
                                   editSummary: EditAggregatedSummary?): SpannableStringBuilder {
        val spannable = SpannableStringBuilder()
        spannable.append(linkifiedBody)
        val editedSuffix = stringProvider.getString(R.string.edited_suffix)
        spannable.append(" ").append(editedSuffix)
        val color = colorProvider.getColorFromAttribute(R.attr.vctr_list_header_secondary_text_color)
        val editStart = spannable.lastIndexOf(editedSuffix)
        val editEnd = editStart + editedSuffix.length
        spannable.setSpan(
                ForegroundColorSpan(color),
                editStart,
                editEnd,
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE)

        spannable.setSpan(RelativeSizeSpan(.9f), editStart, editEnd, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
        spannable.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View?) {
                callback?.onEditedDecorationClicked(informationData, editSummary)
            }

            override fun updateDrawState(ds: TextPaint?) {
                //nop
            }
        },
                editStart,
                editEnd,
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
        return spannable
    }

    private fun buildNoticeMessageItem(messageContent: MessageNoticeContent,
                                       informationData: MessageInformationData,
                                       highlight: Boolean,
                                       callback: TimelineEventController.Callback?): MessageTextItem? {

        val message = messageContent.body.let {
            val formattedBody = span {
                text = it
                textColor = colorProvider.getColorFromAttribute(R.attr.riotx_text_secondary)
                textStyle = "italic"
            }
            linkifyBody(formattedBody, callback)
        }
        return MessageTextItem_()
                .avatarRenderer(avatarRenderer)
                .message(message)
                .colorProvider(colorProvider)
                .informationData(informationData)
                .highlighted(highlight)
                .avatarCallback(callback)
                .reactionPillCallback(callback)
                .urlClickCallback(callback)
                .emojiTypeFace(emojiCompatFontProvider.typeface)
                .memberClickListener(
                        DebouncedClickListener(View.OnClickListener { view ->
                            callback?.onMemberNameClicked(informationData)
                        }))
                .cellClickListener(
                        DebouncedClickListener(View.OnClickListener { view ->
                            callback?.onEventCellClicked(informationData, messageContent, view)
                        }))
                .longClickListener { view ->
                    return@longClickListener callback?.onEventLongClicked(informationData, messageContent, view)
                            ?: false
                }
    }

    private fun buildEmoteMessageItem(messageContent: MessageEmoteContent,
                                      informationData: MessageInformationData,
                                      editSummary: EditAggregatedSummary?,
                                      highlight: Boolean,
                                      callback: TimelineEventController.Callback?): MessageTextItem? {

        val message = messageContent.body.let {
            val formattedBody = "* ${informationData.memberName} $it"
            linkifyBody(formattedBody, callback)
        }
        return MessageTextItem_()
                .apply {
                    if (informationData.hasBeenEdited) {
                        val spannable = annotateWithEdited(message, callback, informationData, editSummary)
                        message(spannable)
                    } else {
                        message(message)
                    }
                }
                .avatarRenderer(avatarRenderer)
                .colorProvider(colorProvider)
                .informationData(informationData)
                .highlighted(highlight)
                .avatarCallback(callback)
                .reactionPillCallback(callback)
                .urlClickCallback(callback)
                .emojiTypeFace(emojiCompatFontProvider.typeface)
                .cellClickListener(
                        DebouncedClickListener(View.OnClickListener { view ->
                            callback?.onEventCellClicked(informationData, messageContent, view)
                        }))
                .longClickListener { view ->
                    return@longClickListener callback?.onEventLongClicked(informationData, messageContent, view)
                            ?: false
                }
    }

    private fun buildRedactedItem(informationData: MessageInformationData,
                                  highlight: Boolean,
                                  callback: TimelineEventController.Callback?): RedactedMessageItem? {
        return RedactedMessageItem_()
                .avatarRenderer(avatarRenderer)
                .colorProvider(colorProvider)
                .informationData(informationData)
                .highlighted(highlight)
                .avatarCallback(callback)
                .cellClickListener(
                        DebouncedClickListener(View.OnClickListener { view ->
                            callback?.onEventCellClicked(informationData, null, view)
                        }))
                .longClickListener { view ->
                    return@longClickListener callback?.onEventLongClicked(informationData, null, view)
                            ?: false
                }
    }

    private fun linkifyBody(body: CharSequence, callback: TimelineEventController.Callback?): CharSequence {
        val spannable = SpannableStringBuilder(body)
        MatrixLinkify.addLinks(spannable, object : MatrixPermalinkSpan.Callback {
            override fun onUrlClicked(url: String) {
                callback?.onUrlClicked(url)
            }
        })
        VectorLinkify.addLinks(spannable, true)
        return spannable
    }
}