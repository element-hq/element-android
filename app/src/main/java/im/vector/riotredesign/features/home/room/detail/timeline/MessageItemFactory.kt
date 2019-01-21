package im.vector.riotredesign.features.home.room.detail.timeline

import android.text.SpannableStringBuilder
import android.text.util.Linkify
import im.vector.matrix.android.api.permalinks.MatrixLinkify
import im.vector.matrix.android.api.permalinks.MatrixPermalinkSpan
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.TimelineEvent
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.message.MessageContent
import im.vector.matrix.android.api.session.room.model.message.MessageTextContent
import im.vector.riotredesign.core.extensions.localDateTime

class MessageItemFactory(private val timelineDateFormatter: TimelineDateFormatter) {

    private val messagesDisplayedWithInformation = HashSet<String?>()

    fun create(event: TimelineEvent,
               nextEvent: TimelineEvent?,
               callback: TimelineEventController.Callback?
    ): MessageTextItem? {

        val roomMember = event.roomMember
        val nextRoomMember = nextEvent?.roomMember

        val date = event.root.localDateTime()
        val nextDate = nextEvent?.root?.localDateTime()
        val addDaySeparator = date.toLocalDate() != nextDate?.toLocalDate()
        val isNextMessageReceivedMoreThanOneHourAgo = nextDate?.isBefore(date.minusMinutes(60))
                                                      ?: false

        if (addDaySeparator
            || nextRoomMember != roomMember
            || nextEvent?.root?.type != EventType.MESSAGE
            || isNextMessageReceivedMoreThanOneHourAgo) {
            messagesDisplayedWithInformation.add(event.root.eventId)
        }

        val messageContent: MessageContent = event.root.content.toModel() ?: return null
        val showInformation = messagesDisplayedWithInformation.contains(event.root.eventId)
        val time = timelineDateFormatter.formatMessageHour(date)
        val avatarUrl = roomMember?.avatarUrl
        val memberName = roomMember?.displayName ?: event.root.sender

        return when (messageContent) {
            is MessageTextContent -> buildTextMessageItem(messageContent, memberName, avatarUrl, time, showInformation, callback)
            else                  -> null
        }
    }

    private fun buildTextMessageItem(messageContent: MessageTextContent,
                                     memberName: String?,
                                     avatarUrl: String?,
                                     time: String,
                                     showInformation: Boolean,
                                     callback: TimelineEventController.Callback?): MessageTextItem? {

        val message = messageContent.body?.let {
            val spannable = SpannableStringBuilder(it)
            MatrixLinkify.addLinks(spannable, object : MatrixPermalinkSpan.Callback {
                override fun onUrlClicked(url: String) {
                    callback?.onUrlClicked(url)
                }
            })
            Linkify.addLinks(spannable, Linkify.ALL)
            spannable
        }
        return MessageTextItem(
                message = message,
                avatarUrl = avatarUrl,
                showInformation = showInformation,
                time = time,
                memberName = memberName
        )
    }


}