/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.timeline.render

import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.resources.StringProvider
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.getPollQuestion
import org.matrix.android.sdk.api.session.events.model.getRelationContent
import org.matrix.android.sdk.api.session.events.model.isAudioMessage
import org.matrix.android.sdk.api.session.events.model.isFileMessage
import org.matrix.android.sdk.api.session.events.model.isImageMessage
import org.matrix.android.sdk.api.session.events.model.isLiveLocation
import org.matrix.android.sdk.api.session.events.model.isPollEnd
import org.matrix.android.sdk.api.session.events.model.isPollStart
import org.matrix.android.sdk.api.session.events.model.isSticker
import org.matrix.android.sdk.api.session.events.model.isVideoMessage
import org.matrix.android.sdk.api.session.events.model.isVoiceMessage
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.room.getTimelineEvent
import org.matrix.android.sdk.api.session.room.model.message.MessagePollContent
import org.matrix.android.sdk.api.session.room.model.relation.ReplyToContent
import org.matrix.android.sdk.api.session.room.timeline.getLastMessageContent
import javax.inject.Inject

private const val IN_REPLY_TO = "In reply to"
private const val BREAKING_LINE = "<br />"
private const val ENDING_BLOCK_QUOTE = "</blockquote>"

class ProcessBodyOfReplyToEventUseCase @Inject constructor(
        private val activeSessionHolder: ActiveSessionHolder,
        private val stringProvider: StringProvider,
) {

    fun execute(roomId: String, matrixFormattedBody: String, replyToContent: ReplyToContent): String {
        val repliedToEvent = replyToContent.eventId?.let { getEvent(it, roomId) }
        val breakingLineIndex = matrixFormattedBody.indexOf(BREAKING_LINE)
        val endOfBlockQuoteIndex = matrixFormattedBody.lastIndexOf(ENDING_BLOCK_QUOTE)

        val withTranslatedContent = if (repliedToEvent != null && breakingLineIndex != -1 && endOfBlockQuoteIndex != -1) {
            val afterBreakingLineIndex = breakingLineIndex + BREAKING_LINE.length
            when {
                repliedToEvent.isFileMessage() -> {
                    matrixFormattedBody.replaceRange(
                            afterBreakingLineIndex,
                            endOfBlockQuoteIndex,
                            stringProvider.getString(CommonStrings.message_reply_to_sender_sent_file)
                    )
                }
                repliedToEvent.isVoiceMessage() -> {
                    matrixFormattedBody.replaceRange(
                            afterBreakingLineIndex,
                            endOfBlockQuoteIndex,
                            stringProvider.getString(CommonStrings.message_reply_to_sender_sent_voice_message)
                    )
                }
                repliedToEvent.isAudioMessage() -> {
                    matrixFormattedBody.replaceRange(
                            afterBreakingLineIndex,
                            endOfBlockQuoteIndex,
                            stringProvider.getString(CommonStrings.message_reply_to_sender_sent_audio_file)
                    )
                }
                repliedToEvent.isImageMessage() -> {
                    matrixFormattedBody.replaceRange(
                            afterBreakingLineIndex,
                            endOfBlockQuoteIndex,
                            stringProvider.getString(CommonStrings.message_reply_to_sender_sent_image)
                    )
                }
                repliedToEvent.isVideoMessage() -> {
                    matrixFormattedBody.replaceRange(
                            afterBreakingLineIndex,
                            endOfBlockQuoteIndex,
                            stringProvider.getString(CommonStrings.message_reply_to_sender_sent_video)
                    )
                }
                repliedToEvent.isSticker() -> {
                    matrixFormattedBody.replaceRange(
                            afterBreakingLineIndex,
                            endOfBlockQuoteIndex,
                            stringProvider.getString(CommonStrings.message_reply_to_sender_sent_sticker)
                    )
                }
                repliedToEvent.isPollEnd() -> {
                    val fallbackText = stringProvider.getString(CommonStrings.message_reply_to_sender_ended_poll)
                    val repliedText = getPollQuestionFromPollEnd(repliedToEvent)
                    matrixFormattedBody.replaceRange(
                            afterBreakingLineIndex,
                            endOfBlockQuoteIndex,
                            repliedText ?: fallbackText,
                    )
                }
                repliedToEvent.isPollStart() -> {
                    val fallbackText = stringProvider.getString(CommonStrings.message_reply_to_sender_created_poll)
                    val repliedText = repliedToEvent.getPollQuestion()
                    matrixFormattedBody.replaceRange(
                            afterBreakingLineIndex,
                            endOfBlockQuoteIndex,
                            repliedText ?: fallbackText,
                    )
                }
                repliedToEvent.isLiveLocation() -> {
                    matrixFormattedBody.replaceRange(
                            afterBreakingLineIndex,
                            endOfBlockQuoteIndex,
                            stringProvider.getString(CommonStrings.live_location_description)
                    )
                }
                else -> matrixFormattedBody
            }
        } else {
            matrixFormattedBody
        }

        return withTranslatedContent.replace(
                IN_REPLY_TO,
                stringProvider.getString(CommonStrings.message_reply_to_prefix)
        )
    }

    private fun getEvent(eventId: String, roomId: String) =
            getTimelineEvent(eventId, roomId)
                    ?.root

    private fun getTimelineEvent(eventId: String, roomId: String) =
            activeSessionHolder.getSafeActiveSession()
                    ?.getRoom(roomId)
                    ?.getTimelineEvent(eventId)

    private fun getPollQuestionFromPollEnd(event: Event): String? {
        val eventId = event.getRelationContent()?.eventId.orEmpty()
        val roomId = event.roomId.orEmpty()
        return if (eventId.isEmpty() || roomId.isEmpty()) {
            null
        } else {
            (getTimelineEvent(eventId, roomId)
                    ?.getLastMessageContent() as? MessagePollContent)
                    ?.getBestPollCreationInfo()
                    ?.question
                    ?.getBestQuestion()
        }
    }
}
