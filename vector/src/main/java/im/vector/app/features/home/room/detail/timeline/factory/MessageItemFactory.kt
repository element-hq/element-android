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

package im.vector.app.features.home.room.detail.timeline.factory

import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.style.AbsoluteSizeSpan
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.View
import dagger.Lazy
import im.vector.app.R
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.files.LocalFilesHelper
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.utils.DimensionConverter
import im.vector.app.core.utils.containsOnlyEmojis
import im.vector.app.features.home.room.detail.timeline.TimelineEventController
import im.vector.app.features.home.room.detail.timeline.helper.AvatarSizeProvider
import im.vector.app.features.home.room.detail.timeline.helper.ContentDownloadStateTrackerBinder
import im.vector.app.features.home.room.detail.timeline.helper.ContentUploadStateTrackerBinder
import im.vector.app.features.home.room.detail.timeline.helper.LocationPinProvider
import im.vector.app.features.home.room.detail.timeline.helper.MessageInformationDataFactory
import im.vector.app.features.home.room.detail.timeline.helper.MessageItemAttributesFactory
import im.vector.app.features.home.room.detail.timeline.helper.TimelineMediaSizeProvider
import im.vector.app.features.home.room.detail.timeline.helper.VoiceMessagePlaybackTracker
import im.vector.app.features.home.room.detail.timeline.item.AbsMessageItem
import im.vector.app.features.home.room.detail.timeline.item.MessageFileItem
import im.vector.app.features.home.room.detail.timeline.item.MessageFileItem_
import im.vector.app.features.home.room.detail.timeline.item.MessageImageVideoItem
import im.vector.app.features.home.room.detail.timeline.item.MessageImageVideoItem_
import im.vector.app.features.home.room.detail.timeline.item.MessageInformationData
import im.vector.app.features.home.room.detail.timeline.item.MessageLocationItem
import im.vector.app.features.home.room.detail.timeline.item.MessageLocationItem_
import im.vector.app.features.home.room.detail.timeline.item.MessageTextItem
import im.vector.app.features.home.room.detail.timeline.item.MessageTextItem_
import im.vector.app.features.home.room.detail.timeline.item.MessageVoiceItem
import im.vector.app.features.home.room.detail.timeline.item.MessageVoiceItem_
import im.vector.app.features.home.room.detail.timeline.item.PollItem
import im.vector.app.features.home.room.detail.timeline.item.PollItem_
import im.vector.app.features.home.room.detail.timeline.item.PollOptionViewState
import im.vector.app.features.home.room.detail.timeline.item.RedactedMessageItem
import im.vector.app.features.home.room.detail.timeline.item.RedactedMessageItem_
import im.vector.app.features.home.room.detail.timeline.item.VerificationRequestItem
import im.vector.app.features.home.room.detail.timeline.item.VerificationRequestItem_
import im.vector.app.features.home.room.detail.timeline.render.EventTextRenderer
import im.vector.app.features.home.room.detail.timeline.tools.createLinkMovementMethod
import im.vector.app.features.home.room.detail.timeline.tools.linkify
import im.vector.app.features.html.EventHtmlRenderer
import im.vector.app.features.html.PillsPostProcessor
import im.vector.app.features.html.SpanUtils
import im.vector.app.features.html.VectorHtmlCompressor
import im.vector.app.features.location.INITIAL_MAP_ZOOM_IN_TIMELINE
import im.vector.app.features.location.UrlMapProvider
import im.vector.app.features.location.toLocationData
import im.vector.app.features.media.ImageContentRenderer
import im.vector.app.features.media.VideoContentRenderer
import im.vector.app.features.settings.VectorPreferences
import im.vector.lib.core.utils.epoxy.charsequence.toEpoxyCharSequence
import me.gujun.android.span.span
import org.matrix.android.sdk.api.MatrixUrls.isMxcUrl
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.RelationType
import org.matrix.android.sdk.api.session.events.model.isThread
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.message.MessageAudioContent
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.session.room.model.message.MessageContentWithFormattedBody
import org.matrix.android.sdk.api.session.room.model.message.MessageEmoteContent
import org.matrix.android.sdk.api.session.room.model.message.MessageFileContent
import org.matrix.android.sdk.api.session.room.model.message.MessageImageInfoContent
import org.matrix.android.sdk.api.session.room.model.message.MessageLocationContent
import org.matrix.android.sdk.api.session.room.model.message.MessageNoticeContent
import org.matrix.android.sdk.api.session.room.model.message.MessagePollContent
import org.matrix.android.sdk.api.session.room.model.message.MessageTextContent
import org.matrix.android.sdk.api.session.room.model.message.MessageType
import org.matrix.android.sdk.api.session.room.model.message.MessageVerificationRequestContent
import org.matrix.android.sdk.api.session.room.model.message.MessageVideoContent
import org.matrix.android.sdk.api.session.room.model.message.PollType
import org.matrix.android.sdk.api.session.room.model.message.getFileName
import org.matrix.android.sdk.api.session.room.model.message.getFileUrl
import org.matrix.android.sdk.api.session.room.model.message.getThumbnailUrl
import org.matrix.android.sdk.api.session.room.timeline.getLastMessageContent
import org.matrix.android.sdk.api.util.MimeTypes
import org.matrix.android.sdk.internal.crypto.attachments.toElementToDecrypt
import org.matrix.android.sdk.internal.crypto.model.event.EncryptedEventContent
import org.matrix.android.sdk.internal.database.lightweight.LightweightSettingsStorage
import javax.inject.Inject

class MessageItemFactory @Inject constructor(
        private val localFilesHelper: LocalFilesHelper,
        private val colorProvider: ColorProvider,
        private val dimensionConverter: DimensionConverter,
        private val timelineMediaSizeProvider: TimelineMediaSizeProvider,
        private val htmlRenderer: Lazy<EventHtmlRenderer>,
        private val htmlCompressor: VectorHtmlCompressor,
        private val textRendererFactory: EventTextRenderer.Factory,
        private val stringProvider: StringProvider,
        private val imageContentRenderer: ImageContentRenderer,
        private val messageInformationDataFactory: MessageInformationDataFactory,
        private val messageItemAttributesFactory: MessageItemAttributesFactory,
        private val contentUploadStateTrackerBinder: ContentUploadStateTrackerBinder,
        private val contentDownloadStateTrackerBinder: ContentDownloadStateTrackerBinder,
        private val defaultItemFactory: DefaultItemFactory,
        private val noticeItemFactory: NoticeItemFactory,
        private val avatarSizeProvider: AvatarSizeProvider,
        private val pillsPostProcessorFactory: PillsPostProcessor.Factory,
        private val lightweightSettingsStorage: LightweightSettingsStorage,
        private val spanUtils: SpanUtils,
        private val session: Session,
        private val voiceMessagePlaybackTracker: VoiceMessagePlaybackTracker,
        private val locationPinProvider: LocationPinProvider,
        private val vectorPreferences: VectorPreferences,
        private val urlMapProvider: UrlMapProvider,
) {

    // TODO inject this properly?
    private var roomId: String = ""

    private val pillsPostProcessor by lazy {
        pillsPostProcessorFactory.create(roomId)
    }

    private val textRenderer by lazy {
        textRendererFactory.create(roomId)
    }

    fun create(params: TimelineItemFactoryParams): VectorEpoxyModel<*>? {
        val event = params.event
        val highlight = params.isHighlighted
        val callback = params.callback
        event.root.eventId ?: return null
        roomId = event.roomId
        val informationData = messageInformationDataFactory.create(params)
        val threadDetails = if (params.isFromThreadTimeline()) null else event.root.threadDetails

        if (event.root.isRedacted()) {
            // message is redacted
            val attributes = messageItemAttributesFactory.create(null, informationData, callback, params.reactionsSummaryEvents)
            return buildRedactedItem(attributes, highlight)
        }

        val messageContent = event.getLastMessageContent()
        if (messageContent == null) {
            val malformedText = stringProvider.getString(R.string.malformed_message)
            return defaultItemFactory.create(malformedText, informationData, highlight, callback)
        }
        if (messageContent.relatesTo?.type == RelationType.REPLACE ||
                event.isEncrypted() && event.root.content.toModel<EncryptedEventContent>()?.relatesTo?.type == RelationType.REPLACE
        ) {
            // This is an edit event, we should display it when debugging as a notice event
            return noticeItemFactory.create(params)
        }

        if (lightweightSettingsStorage.areThreadMessagesEnabled() && !params.isFromThreadTimeline() && event.root.isThread()) {
            // This is a thread event and we will [debug] display it when we are in the main timeline
            return noticeItemFactory.create(params)
        }

        // always hide summary when we are on thread timeline
        val attributes = messageItemAttributesFactory.create(messageContent, informationData, callback, params.reactionsSummaryEvents, threadDetails)

//        val all = event.root.toContent()
//        val ev = all.toModel<Event>()
        val messageItem = when (messageContent) {
            is MessageEmoteContent               -> buildEmoteMessageItem(messageContent, informationData, highlight, callback, attributes)
            is MessageTextContent                -> buildItemForTextContent(messageContent, informationData, highlight, callback, attributes)
            is MessageImageInfoContent           -> buildImageMessageItem(messageContent, informationData, highlight, callback, attributes)
            is MessageNoticeContent              -> buildNoticeMessageItem(messageContent, informationData, highlight, callback, attributes)
            is MessageVideoContent               -> buildVideoMessageItem(messageContent, informationData, highlight, callback, attributes)
            is MessageFileContent                -> buildFileMessageItem(messageContent, highlight, attributes)
            is MessageAudioContent               -> {
                if (messageContent.voiceMessageIndicator != null) {
                    buildVoiceMessageItem(params, messageContent, informationData, highlight, attributes)
                } else {
                    buildAudioMessageItem(messageContent, informationData, highlight, attributes)
                }
            }
            is MessageVerificationRequestContent -> buildVerificationRequestMessageItem(messageContent, informationData, highlight, callback, attributes)
            is MessagePollContent                -> buildPollItem(messageContent, informationData, highlight, callback, attributes)
            is MessageLocationContent            -> {
                if (vectorPreferences.labsRenderLocationsInTimeline()) {
                    buildLocationItem(messageContent, informationData, highlight, attributes)
                } else {
                    buildMessageTextItem(messageContent.body, false, informationData, highlight, callback, attributes)
                }
            }
            else                                 -> buildNotHandledMessageItem(messageContent, informationData, highlight, callback, attributes)
        }
        return messageItem?.apply {
            layout(informationData.messageLayout.layoutRes)
        }
    }

    private fun buildLocationItem(locationContent: MessageLocationContent,
                                  informationData: MessageInformationData,
                                  highlight: Boolean,
                                  attributes: AbsMessageItem.Attributes): MessageLocationItem? {
        val width = timelineMediaSizeProvider.getMaxSize().first
        val height = dimensionConverter.dpToPx(200)

        val locationUrl = locationContent.toLocationData()?.let {
            urlMapProvider.buildStaticMapUrl(it, INITIAL_MAP_ZOOM_IN_TIMELINE, width, height)
        }

        val userId = if (locationContent.isSelfLocation()) informationData.senderId else null

        return MessageLocationItem_()
                .attributes(attributes)
                .locationUrl(locationUrl)
                .mapWidth(width)
                .mapHeight(height)
                .userId(userId)
                .locationPinProvider(locationPinProvider)
                .highlighted(highlight)
                .leftGuideline(avatarSizeProvider.leftGuideline)
    }

    private fun buildPollItem(pollContent: MessagePollContent,
                              informationData: MessageInformationData,
                              highlight: Boolean,
                              callback: TimelineEventController.Callback?,
                              attributes: AbsMessageItem.Attributes): PollItem? {
        val optionViewStates = mutableListOf<PollOptionViewState>()

        val pollResponseSummary = informationData.pollResponseAggregatedSummary
        val isEnded = pollResponseSummary?.isClosed.orFalse()
        val didUserVoted = pollResponseSummary?.myVote?.isNotEmpty().orFalse()
        val winnerVoteCount = pollResponseSummary?.winnerVoteCount
        val isPollSent = informationData.sendState.isSent()
        val isPollUndisclosed = pollContent.pollCreationInfo?.kind == PollType.UNDISCLOSED

        val totalVotesText = (pollResponseSummary?.totalVotes ?: 0).let {
            when {
                isEnded           -> stringProvider.getQuantityString(R.plurals.poll_total_vote_count_after_ended, it, it)
                isPollUndisclosed -> ""
                didUserVoted      -> stringProvider.getQuantityString(R.plurals.poll_total_vote_count_before_ended_and_voted, it, it)
                else              -> if (it == 0) {
                    stringProvider.getString(R.string.poll_no_votes_cast)
                } else {
                    stringProvider.getQuantityString(R.plurals.poll_total_vote_count_before_ended_and_not_voted, it, it)
                }
            }
        }

        pollContent.pollCreationInfo?.answers?.forEach { option ->
            val voteSummary = pollResponseSummary?.votes?.get(option.id)
            val isMyVote = pollResponseSummary?.myVote == option.id
            val voteCount = voteSummary?.total ?: 0
            val votePercentage = voteSummary?.percentage ?: 0.0
            val optionId = option.id ?: ""
            val optionAnswer = option.answer ?: ""

            optionViewStates.add(
                    if (!isPollSent) {
                        // Poll event is not send yet. Disable option.
                        PollOptionViewState.PollSending(optionId, optionAnswer)
                    } else if (isEnded) {
                        // Poll is ended. Disable option, show votes and mark the winner.
                        val isWinner = winnerVoteCount != 0 && voteCount == winnerVoteCount
                        PollOptionViewState.PollEnded(optionId, optionAnswer, voteCount, votePercentage, isWinner)
                    } else if (isPollUndisclosed) {
                        // Poll is closed. Enable option, hide votes and mark the user's selection.
                        PollOptionViewState.PollUndisclosed(optionId, optionAnswer, isMyVote)
                    } else if (didUserVoted) {
                        // User voted to the poll, but poll is not ended. Enable option, show votes and mark the user's selection.
                        PollOptionViewState.PollVoted(optionId, optionAnswer, voteCount, votePercentage, isMyVote)
                    } else {
                        // User didn't voted yet and poll is not ended yet. Enable options, hide votes.
                        PollOptionViewState.PollReady(optionId, optionAnswer)
                    }
            )
        }

        val question = pollContent.pollCreationInfo?.question?.question ?: ""

        return PollItem_()
                .attributes(attributes)
                .eventId(informationData.eventId)
                .pollQuestion(
                        if (informationData.hasBeenEdited) {
                            annotateWithEdited(question, callback, informationData)
                        } else {
                            question
                        }.toEpoxyCharSequence()
                )
                .pollSent(isPollSent)
                .totalVotesText(totalVotesText)
                .optionViewStates(optionViewStates)
                .edited(informationData.hasBeenEdited)
                .highlighted(highlight)
                .leftGuideline(avatarSizeProvider.leftGuideline)
                .callback(callback)
    }

    private fun buildAudioMessageItem(messageContent: MessageAudioContent,
                                      @Suppress("UNUSED_PARAMETER")
                                      informationData: MessageInformationData,
                                      highlight: Boolean,
                                      attributes: AbsMessageItem.Attributes): MessageFileItem? {
        val fileUrl = messageContent.getFileUrl()?.let {
            if (informationData.sentByMe && !informationData.sendState.isSent()) {
                it
            } else {
                it.takeIf { it.isMxcUrl() }
            }
        } ?: ""
        return MessageFileItem_()
                .attributes(attributes)
                .izLocalFile(localFilesHelper.isLocalFile(fileUrl))
                .izDownloaded(session.fileService().isFileInCache(
                        fileUrl,
                        messageContent.getFileName(),
                        messageContent.mimeType,
                        messageContent.encryptedFileInfo?.toElementToDecrypt())
                )
                .mxcUrl(fileUrl)
                .contentUploadStateTrackerBinder(contentUploadStateTrackerBinder)
                .contentDownloadStateTrackerBinder(contentDownloadStateTrackerBinder)
                .highlighted(highlight)
                .leftGuideline(avatarSizeProvider.leftGuideline)
                .filename(messageContent.body)
                .iconRes(R.drawable.ic_headphones)
    }

    private fun buildVoiceMessageItem(params: TimelineItemFactoryParams,
                                      messageContent: MessageAudioContent,
                                      @Suppress("UNUSED_PARAMETER")
                                      informationData: MessageInformationData,
                                      highlight: Boolean,
                                      attributes: AbsMessageItem.Attributes): MessageVoiceItem? {
        val fileUrl = messageContent.getFileUrl()?.let {
            if (informationData.sentByMe && !informationData.sendState.isSent()) {
                it
            } else {
                it.takeIf { it.isMxcUrl() }
            }
        } ?: ""

        val playbackControlButtonClickListener: ClickListener = object : ClickListener {
            override fun invoke(view: View) {
                params.callback?.onVoiceControlButtonClicked(informationData.eventId, messageContent)
            }
        }

        return MessageVoiceItem_()
                .attributes(attributes)
                .duration(messageContent.audioWaveformInfo?.duration ?: 0)
                .waveform(messageContent.audioWaveformInfo?.waveform?.toFft().orEmpty())
                .playbackControlButtonClickListener(playbackControlButtonClickListener)
                .voiceMessagePlaybackTracker(voiceMessagePlaybackTracker)
                .izLocalFile(localFilesHelper.isLocalFile(fileUrl))
                .izDownloaded(session.fileService().isFileInCache(
                        fileUrl,
                        messageContent.getFileName(),
                        messageContent.mimeType,
                        messageContent.encryptedFileInfo?.toElementToDecrypt())
                )
                .mxcUrl(fileUrl)
                .contentUploadStateTrackerBinder(contentUploadStateTrackerBinder)
                .contentDownloadStateTrackerBinder(contentDownloadStateTrackerBinder)
                .highlighted(highlight)
                .leftGuideline(avatarSizeProvider.leftGuideline)
    }

    private fun buildVerificationRequestMessageItem(messageContent: MessageVerificationRequestContent,
                                                    @Suppress("UNUSED_PARAMETER")
                                                    informationData: MessageInformationData,
                                                    highlight: Boolean,
                                                    callback: TimelineEventController.Callback?,
                                                    attributes: AbsMessageItem.Attributes): VerificationRequestItem? {
        // If this request is not sent by me or sent to me, we should ignore it in timeline
        val myUserId = session.myUserId
        if (informationData.senderId != myUserId && messageContent.toUserId != myUserId) {
            return null
        }

        val otherUserId = if (informationData.sentByMe) messageContent.toUserId else informationData.senderId
        val otherUserName = if (informationData.sentByMe) {
            session.getRoomMember(messageContent.toUserId, roomId)?.displayName
        } else {
            informationData.memberName
        }
        return VerificationRequestItem_()
                .attributes(
                        VerificationRequestItem.Attributes(
                                otherUserId = otherUserId,
                                otherUserName = otherUserName.toString(),
                                referenceId = informationData.eventId,
                                informationData = informationData,
                                avatarRenderer = attributes.avatarRenderer,
                                messageColorProvider = attributes.messageColorProvider,
                                itemLongClickListener = attributes.itemLongClickListener,
                                itemClickListener = attributes.itemClickListener,
                                reactionPillCallback = attributes.reactionPillCallback,
                                readReceiptsCallback = attributes.readReceiptsCallback,
                                emojiTypeFace = attributes.emojiTypeFace,
                                reactionsSummaryEvents = attributes.reactionsSummaryEvents
                        )
                )
                .callback(callback)
                .highlighted(highlight)
                .leftGuideline(avatarSizeProvider.leftGuideline)
    }

    private fun buildFileMessageItem(messageContent: MessageFileContent,
//                                     informationData: MessageInformationData,
                                     highlight: Boolean,
//                                     callback: TimelineEventController.Callback?,
                                     attributes: AbsMessageItem.Attributes): MessageFileItem? {
        val mxcUrl = messageContent.getFileUrl() ?: ""
        return MessageFileItem_()
                .attributes(attributes)
                .leftGuideline(avatarSizeProvider.leftGuideline)
                .izLocalFile(localFilesHelper.isLocalFile(messageContent.getFileUrl()))
                .izDownloaded(session.fileService().isFileInCache(messageContent))
                .mxcUrl(mxcUrl)
                .contentUploadStateTrackerBinder(contentUploadStateTrackerBinder)
                .contentDownloadStateTrackerBinder(contentDownloadStateTrackerBinder)
                .highlighted(highlight)
                .filename(messageContent.body)
                .iconRes(R.drawable.ic_paperclip)
    }

    private fun buildNotHandledMessageItem(messageContent: MessageContent,
                                           informationData: MessageInformationData,
                                           highlight: Boolean,
                                           callback: TimelineEventController.Callback?,
                                           attributes: AbsMessageItem.Attributes): MessageTextItem? {
        // For compatibility reason we should display the body
        return buildMessageTextItem(messageContent.body, false, informationData, highlight, callback, attributes)
    }

    private fun buildImageMessageItem(messageContent: MessageImageInfoContent,
                                      @Suppress("UNUSED_PARAMETER")
                                      informationData: MessageInformationData,
                                      highlight: Boolean,
                                      callback: TimelineEventController.Callback?,
                                      attributes: AbsMessageItem.Attributes): MessageImageVideoItem? {
        val (maxWidth, maxHeight) = timelineMediaSizeProvider.getMaxSize()
        val data = ImageContentRenderer.Data(
                eventId = informationData.eventId,
                filename = messageContent.body,
                mimeType = messageContent.mimeType,
                url = messageContent.getFileUrl(),
                elementToDecrypt = messageContent.encryptedFileInfo?.toElementToDecrypt(),
                height = messageContent.info?.height,
                maxHeight = maxHeight,
                width = messageContent.info?.width,
                maxWidth = maxWidth,
                allowNonMxcUrls = informationData.sendState.isSending()
        )
        return MessageImageVideoItem_()
                .attributes(attributes)
                .leftGuideline(avatarSizeProvider.leftGuideline)
                .imageContentRenderer(imageContentRenderer)
                .contentUploadStateTrackerBinder(contentUploadStateTrackerBinder)
                .playable(messageContent.mimeType == MimeTypes.Gif)
                .highlighted(highlight)
                .mediaData(data)
                .apply {
                    if (messageContent.msgType == MessageType.MSGTYPE_STICKER_LOCAL) {
                        mode(ImageContentRenderer.Mode.STICKER)
                    } else {
                        clickListener { view ->
                            callback?.onImageMessageClicked(messageContent, data, view)
                        }
                    }
                }
    }

    private fun buildVideoMessageItem(messageContent: MessageVideoContent,
                                      informationData: MessageInformationData,
                                      highlight: Boolean,
                                      callback: TimelineEventController.Callback?,
                                      attributes: AbsMessageItem.Attributes): MessageImageVideoItem? {
        val (maxWidth, maxHeight) = timelineMediaSizeProvider.getMaxSize()
        val thumbnailData = ImageContentRenderer.Data(
                eventId = informationData.eventId,
                filename = messageContent.body,
                mimeType = messageContent.mimeType,
                url = messageContent.videoInfo?.getThumbnailUrl(),
                elementToDecrypt = messageContent.videoInfo?.thumbnailFile?.toElementToDecrypt(),
                height = messageContent.videoInfo?.height,
                maxHeight = maxHeight,
                width = messageContent.videoInfo?.width,
                maxWidth = maxWidth,
                allowNonMxcUrls = informationData.sendState.isSending()
        )

        val videoData = VideoContentRenderer.Data(
                eventId = informationData.eventId,
                filename = messageContent.body,
                mimeType = messageContent.mimeType,
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
                .clickListener { view -> callback?.onVideoMessageClicked(messageContent, videoData, view.findViewById(R.id.messageThumbnailView)) }
    }

    private fun buildItemForTextContent(messageContent: MessageTextContent,
                                        informationData: MessageInformationData,
                                        highlight: Boolean,
                                        callback: TimelineEventController.Callback?,
                                        attributes: AbsMessageItem.Attributes): VectorEpoxyModel<*>? {
        val matrixFormattedBody = messageContent.matrixFormattedBody
        return if (matrixFormattedBody != null) {
            buildFormattedTextItem(matrixFormattedBody, informationData, highlight, callback, attributes)
        } else {
            buildMessageTextItem(messageContent.body, false, informationData, highlight, callback, attributes)
        }
    }

    private fun buildFormattedTextItem(matrixFormattedBody: String,
                                       informationData: MessageInformationData,
                                       highlight: Boolean,
                                       callback: TimelineEventController.Callback?,
                                       attributes: AbsMessageItem.Attributes): MessageTextItem? {
        val compressed = htmlCompressor.compress(matrixFormattedBody)
        val renderedFormattedBody = htmlRenderer.get().render(compressed, pillsPostProcessor) as Spanned
        return buildMessageTextItem(renderedFormattedBody, true, informationData, highlight, callback, attributes)
    }

    private fun buildMessageTextItem(body: CharSequence,
                                     isFormatted: Boolean,
                                     informationData: MessageInformationData,
                                     highlight: Boolean,
                                     callback: TimelineEventController.Callback?,
                                     attributes: AbsMessageItem.Attributes): MessageTextItem? {
        val renderedBody = textRenderer.render(body)
        val bindingOptions = spanUtils.getBindingOptions(renderedBody)
        val linkifiedBody = renderedBody.linkify(callback)

        return MessageTextItem_()
                .message(
                        if (informationData.hasBeenEdited) {
                            annotateWithEdited(linkifiedBody, callback, informationData)
                        } else {
                            linkifiedBody
                        }.toEpoxyCharSequence()
                )
                .useBigFont(linkifiedBody.length <= MAX_NUMBER_OF_EMOJI_FOR_BIG_FONT * 2 && containsOnlyEmojis(linkifiedBody.toString()))
                .bindingOptions(bindingOptions)
                .markwonPlugins(htmlRenderer.get().plugins)
                .searchForPills(isFormatted)
                .previewUrlRetriever(callback?.getPreviewUrlRetriever())
                .imageContentRenderer(imageContentRenderer)
                .previewUrlCallback(callback)
                .leftGuideline(avatarSizeProvider.leftGuideline)
                .attributes(attributes)
                .highlighted(highlight)
                .movementMethod(createLinkMovementMethod(callback))
    }

    private fun annotateWithEdited(linkifiedBody: CharSequence,
                                   callback: TimelineEventController.Callback?,
                                   informationData: MessageInformationData): Spannable {
        val spannable = SpannableStringBuilder()
        spannable.append(linkifiedBody)
        val editedSuffix = stringProvider.getString(R.string.edited_suffix)
        spannable.append(" ").append(editedSuffix)
        val color = colorProvider.getColorFromAttribute(R.attr.vctr_content_secondary)
        val editStart = spannable.lastIndexOf(editedSuffix)
        val editEnd = editStart + editedSuffix.length
        spannable.setSpan(
                ForegroundColorSpan(color),
                editStart,
                editEnd,
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE)

        // Note: text size is set to 14sp
        spannable.setSpan(
                AbsoluteSizeSpan(dimensionConverter.spToPx(13)),
                editStart,
                editEnd,
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE)

        spannable.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {
                callback?.onEditedDecorationClicked(informationData)
            }

            override fun updateDrawState(ds: TextPaint) {
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
        val htmlBody = messageContent.getHtmlBody()
        val formattedBody = span {
            text = htmlBody
            textColor = colorProvider.getColorFromAttribute(R.attr.vctr_content_secondary)
            textStyle = "italic"
        }

        val bindingOptions = spanUtils.getBindingOptions(htmlBody)
        val message = formattedBody.linkify(callback)

        return MessageTextItem_()
                .leftGuideline(avatarSizeProvider.leftGuideline)
                .previewUrlRetriever(callback?.getPreviewUrlRetriever())
                .imageContentRenderer(imageContentRenderer)
                .previewUrlCallback(callback)
                .attributes(attributes)
                .message(message.toEpoxyCharSequence())
                .bindingOptions(bindingOptions)
                .highlighted(highlight)
                .movementMethod(createLinkMovementMethod(callback))
    }

    private fun buildEmoteMessageItem(messageContent: MessageEmoteContent,
                                      informationData: MessageInformationData,
                                      highlight: Boolean,
                                      callback: TimelineEventController.Callback?,
                                      attributes: AbsMessageItem.Attributes): MessageTextItem? {
        val formattedBody = SpannableStringBuilder()
        formattedBody.append("* ${informationData.memberName} ")
        formattedBody.append(messageContent.getHtmlBody())
        val bindingOptions = spanUtils.getBindingOptions(formattedBody)
        val message = formattedBody.linkify(callback)

        return MessageTextItem_()
                .message(
                        if (informationData.hasBeenEdited) {
                            annotateWithEdited(message, callback, informationData)
                        } else {
                            message
                        }.toEpoxyCharSequence()
                )
                .bindingOptions(bindingOptions)
                .leftGuideline(avatarSizeProvider.leftGuideline)
                .previewUrlRetriever(callback?.getPreviewUrlRetriever())
                .imageContentRenderer(imageContentRenderer)
                .previewUrlCallback(callback)
                .attributes(attributes)
                .highlighted(highlight)
                .movementMethod(createLinkMovementMethod(callback))
    }

    private fun MessageContentWithFormattedBody.getHtmlBody(): CharSequence {
        return matrixFormattedBody
                ?.let { htmlCompressor.compress(it) }
                ?.let { htmlRenderer.get().render(it, pillsPostProcessor) }
                ?: body
    }

    private fun buildRedactedItem(attributes: AbsMessageItem.Attributes,
                                  highlight: Boolean): RedactedMessageItem? {
        return RedactedMessageItem_()
                .layout(attributes.informationData.messageLayout.layoutRes)
                .leftGuideline(avatarSizeProvider.leftGuideline)
                .attributes(attributes)
                .highlighted(highlight)
    }

    private fun List<Int?>?.toFft(): List<Int>? {
        return this
                ?.filterNotNull()
                ?.map {
                    // Value comes from AudioRecordView.maxReportableAmp, and 1024 is the max value in the Matrix spec
                    it * 22760 / 1024
                }
    }

    companion object {
        private const val MAX_NUMBER_OF_EMOJI_FOR_BIG_FONT = 5
    }
}
