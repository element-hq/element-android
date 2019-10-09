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
import im.vector.matrix.android.api.session.room.model.message.*
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.matrix.android.api.session.room.timeline.getLastMessageContent
import im.vector.matrix.android.internal.crypto.attachments.toElementToDecrypt
import im.vector.matrix.android.internal.crypto.model.event.EncryptedEventContent
import im.vector.riotx.R
import im.vector.riotx.core.epoxy.VectorEpoxyModel
import im.vector.riotx.core.linkify.VectorLinkify
import im.vector.riotx.core.resources.ColorProvider
import im.vector.riotx.core.resources.StringProvider
import im.vector.riotx.core.utils.DebouncedClickListener
import im.vector.riotx.core.utils.isLocalFile
import im.vector.riotx.features.home.room.detail.timeline.TimelineEventController
import im.vector.riotx.features.home.room.detail.timeline.helper.*
import im.vector.riotx.features.home.room.detail.timeline.item.*
import im.vector.riotx.features.html.EventHtmlRenderer
import im.vector.riotx.features.media.ImageContentRenderer
import im.vector.riotx.features.media.VideoContentRenderer
import me.gujun.android.span.span
import javax.inject.Inject

class MessageItemFactory @Inject constructor(
        private val colorProvider: ColorProvider,
        private val timelineMediaSizeProvider: TimelineMediaSizeProvider,
        private val htmlRenderer: Lazy<EventHtmlRenderer>,
        private val stringProvider: StringProvider,
        private val imageContentRenderer: ImageContentRenderer,
        private val messageInformationDataFactory: MessageInformationDataFactory,
        private val messageItemAttributesFactory: MessageItemAttributesFactory,
        private val contentUploadStateTrackerBinder: ContentUploadStateTrackerBinder,
        private val defaultItemFactory: DefaultItemFactory,
        private val noticeItemFactory: NoticeItemFactory,
        private val avatarSizeProvider: AvatarSizeProvider) {

    fun create(event: TimelineEvent,
               nextEvent: TimelineEvent?,
               highlight: Boolean,
               readMarkerVisible: Boolean,
               callback: TimelineEventController.Callback?
    ): VectorEpoxyModel<*>? {
        event.root.eventId ?: return null

        val informationData = messageInformationDataFactory.create(event, nextEvent, readMarkerVisible)

        if (event.root.isRedacted()) {
            // message is redacted
            val attributes = messageItemAttributesFactory.create(null, informationData, callback)
            return buildRedactedItem(attributes, highlight)
        }

        val messageContent = event.getLastMessageContent()
        if (messageContent == null) {
            val malformedText = stringProvider.getString(R.string.malformed_message)
            return defaultItemFactory.create(malformedText, informationData, highlight, callback)
        }
        if (messageContent.relatesTo?.type == RelationType.REPLACE
                || event.isEncrypted() && event.root.content.toModel<EncryptedEventContent>()?.relatesTo?.type == RelationType.REPLACE
        ) {
            // This is an edit event, we should it when debugging as a notice event
            return noticeItemFactory.create(event, highlight, readMarkerVisible, callback)
        }
        val attributes = messageItemAttributesFactory.create(messageContent, informationData, callback)

//        val all = event.root.toContent()
//        val ev = all.toModel<Event>()
        return when (messageContent) {
            is MessageEmoteContent  -> buildEmoteMessageItem(messageContent,
                    informationData,
                    highlight,
                    callback,
                    attributes)
            is MessageTextContent   -> buildTextMessageItem(messageContent,
                    informationData,
                    highlight,
                    callback,
                    attributes)
            is MessageImageContent  -> buildImageMessageItem(messageContent, informationData, highlight, callback, attributes)
            is MessageNoticeContent -> buildNoticeMessageItem(messageContent, informationData, highlight, callback, attributes)
            is MessageVideoContent  -> buildVideoMessageItem(messageContent, informationData, highlight, callback, attributes)
            is MessageFileContent   -> buildFileMessageItem(messageContent, informationData, highlight, callback, attributes)
            is MessageAudioContent  -> buildAudioMessageItem(messageContent, informationData, highlight, callback, attributes)
            else                    -> buildNotHandledMessageItem(messageContent, informationData, highlight, callback)
        }
    }

    private fun buildAudioMessageItem(messageContent: MessageAudioContent,
                                      @Suppress("UNUSED_PARAMETER")
                                      informationData: MessageInformationData,
                                      highlight: Boolean,
                                      callback: TimelineEventController.Callback?,
                                      attributes: AbsMessageItem.Attributes): MessageFileItem? {
        return MessageFileItem_()
                .attributes(attributes)
                .izLocalFile(messageContent.getFileUrl().isLocalFile())
                .contentUploadStateTrackerBinder(contentUploadStateTrackerBinder)
                .highlighted(highlight)
                .leftGuideline(avatarSizeProvider.leftGuideline)
                .filename(messageContent.body)
                .iconRes(R.drawable.filetype_audio)
                .clickListener(
                        DebouncedClickListener(View.OnClickListener {
                            callback?.onAudioMessageClicked(messageContent)
                        }))
    }

    private fun buildFileMessageItem(messageContent: MessageFileContent,
                                     informationData: MessageInformationData,
                                     highlight: Boolean,
                                     callback: TimelineEventController.Callback?,
                                     attributes: AbsMessageItem.Attributes): MessageFileItem? {
        return MessageFileItem_()
                .attributes(attributes)
                .leftGuideline(avatarSizeProvider.leftGuideline)
                .izLocalFile(messageContent.getFileUrl().isLocalFile())
                .contentUploadStateTrackerBinder(contentUploadStateTrackerBinder)
                .highlighted(highlight)
                .filename(messageContent.body)
                .iconRes(R.drawable.filetype_attachment)
                .clickListener(
                        DebouncedClickListener(View.OnClickListener {
                            callback?.onFileMessageClicked(informationData.eventId, messageContent)
                        }))
    }

    private fun buildNotHandledMessageItem(messageContent: MessageContent,
                                           informationData: MessageInformationData,
                                           highlight: Boolean,
                                           callback: TimelineEventController.Callback?): DefaultItem? {
        val text = "${messageContent.type} message events are not yet handled"
        return defaultItemFactory.create(text, informationData, highlight, callback)
    }

    private fun buildImageMessageItem(messageContent: MessageImageContent,
                                      @Suppress("UNUSED_PARAMETER")
                                      informationData: MessageInformationData,
                                      highlight: Boolean,
                                      callback: TimelineEventController.Callback?,
                                      attributes: AbsMessageItem.Attributes): MessageImageVideoItem? {

        val (maxWidth, maxHeight) = timelineMediaSizeProvider.getMaxSize()
        val data = ImageContentRenderer.Data(
                filename = messageContent.body,
                url = messageContent.getFileUrl(),
                elementToDecrypt = messageContent.encryptedFileInfo?.toElementToDecrypt(),
                height = messageContent.info?.height,
                maxHeight = maxHeight,
                width = messageContent.info?.width,
                maxWidth = maxWidth
        )
        return MessageImageVideoItem_()
                .attributes(attributes)
                .leftGuideline(avatarSizeProvider.leftGuideline)
                .imageContentRenderer(imageContentRenderer)
                .contentUploadStateTrackerBinder(contentUploadStateTrackerBinder)
                .playable(messageContent.info?.mimeType == "image/gif")
                .highlighted(highlight)
                .mediaData(data)
                .clickListener(
                        DebouncedClickListener(View.OnClickListener { view ->
                            callback?.onImageMessageClicked(messageContent, data, view)
                        }))
    }

    private fun buildVideoMessageItem(messageContent: MessageVideoContent,
                                      informationData: MessageInformationData,
                                      highlight: Boolean,
                                      callback: TimelineEventController.Callback?,
                                      attributes: AbsMessageItem.Attributes): MessageImageVideoItem? {

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
                .leftGuideline(avatarSizeProvider.leftGuideline)
                .attributes(attributes)
                .imageContentRenderer(imageContentRenderer)
                .contentUploadStateTrackerBinder(contentUploadStateTrackerBinder)
                .playable(true)
                .highlighted(highlight)
                .mediaData(thumbnailData)
                .clickListener { view -> callback?.onVideoMessageClicked(messageContent, videoData, view) }
    }

    private fun buildTextMessageItem(messageContent: MessageTextContent,
                                     informationData: MessageInformationData,
                                     highlight: Boolean,
                                     callback: TimelineEventController.Callback?,
                                     attributes: AbsMessageItem.Attributes): MessageTextItem? {

        val isFormatted = messageContent.formattedBody.isNullOrBlank().not()
        val bodyToUse = messageContent.formattedBody?.let {
            htmlRenderer.get().render(it.trim())
        } ?: messageContent.body

        val linkifiedBody = linkifyBody(bodyToUse, callback)

        return MessageTextItem_()
                .apply {
                    if (informationData.hasBeenEdited) {
                        val spannable = annotateWithEdited(linkifiedBody, callback, informationData)
                        message(spannable)
                    } else {
                        message(linkifiedBody)
                    }
                }
                .searchForPills(isFormatted)
                .leftGuideline(avatarSizeProvider.leftGuideline)
                .attributes(attributes)
                .highlighted(highlight)
                .urlClickCallback(callback)
        // click on the text
    }

    private fun annotateWithEdited(linkifiedBody: CharSequence,
                                   callback: TimelineEventController.Callback?,
                                   informationData: MessageInformationData): SpannableStringBuilder {
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
                callback?.onEditedDecorationClicked(informationData)
            }

            override fun updateDrawState(ds: TextPaint?) {
                // nop
            }
        },
                editStart,
                editEnd,
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
        return spannable
    }

    private fun buildNoticeMessageItem(messageContent: MessageNoticeContent,
                                       @Suppress("UNUSED_PARAMETER")
                                       informationData: MessageInformationData,
                                       highlight: Boolean,
                                       callback: TimelineEventController.Callback?,
                                       attributes: AbsMessageItem.Attributes): MessageTextItem? {

        val message = messageContent.body.let {
            val formattedBody = span {
                text = it
                textColor = colorProvider.getColorFromAttribute(R.attr.riotx_text_secondary)
                textStyle = "italic"
            }
            linkifyBody(formattedBody, callback)
        }
        return MessageTextItem_()
                .leftGuideline(avatarSizeProvider.leftGuideline)
                .attributes(attributes)
                .message(message)
                .highlighted(highlight)
                .urlClickCallback(callback)
    }

    private fun buildEmoteMessageItem(messageContent: MessageEmoteContent,
                                      informationData: MessageInformationData,
                                      highlight: Boolean,
                                      callback: TimelineEventController.Callback?,
                                      attributes: AbsMessageItem.Attributes): MessageTextItem? {

        val message = messageContent.body.let {
            val formattedBody = "* ${informationData.memberName} $it"
            linkifyBody(formattedBody, callback)
        }
        return MessageTextItem_()
                .apply {
                    if (informationData.hasBeenEdited) {
                        val spannable = annotateWithEdited(message, callback, informationData)
                        message(spannable)
                    } else {
                        message(message)
                    }
                }
                .leftGuideline(avatarSizeProvider.leftGuideline)
                .attributes(attributes)
                .highlighted(highlight)
                .urlClickCallback(callback)
    }

    private fun buildRedactedItem(attributes: AbsMessageItem.Attributes,
                                  highlight: Boolean): RedactedMessageItem? {
        return RedactedMessageItem_()
                .leftGuideline(avatarSizeProvider.leftGuideline)
                .attributes(attributes)
                .highlighted(highlight)
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
