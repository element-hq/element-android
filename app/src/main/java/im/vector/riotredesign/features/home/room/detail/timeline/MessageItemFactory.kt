package im.vector.riotredesign.features.home.room.detail.timeline

import android.text.SpannableStringBuilder
import android.text.util.Linkify
import im.vector.matrix.android.api.permalinks.MatrixLinkify
import im.vector.matrix.android.api.permalinks.MatrixPermalinkSpan
import im.vector.matrix.android.api.session.events.model.TimelineEvent
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.MessageContent
import org.threeten.bp.LocalDateTime

class MessageItemFactory(private val timelineDateFormatter: TimelineDateFormatter) {

    private val messagesDisplayedWithInformation = HashSet<String?>()

    fun create(event: TimelineEvent,
               nextEvent: TimelineEvent?,
               addDaySeparator: Boolean,
               date: LocalDateTime,
               callback: TimelineEventController.Callback?
    ): MessageItem? {

        val messageContent: MessageContent? = event.root.content.toModel()
        val roomMember = event.roomMember
        if (messageContent == null || roomMember == null) {
            return null
        }
        val nextRoomMember = nextEvent?.roomMember
        if (addDaySeparator || nextRoomMember != roomMember) {
            messagesDisplayedWithInformation.add(event.root.eventId)
        }

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
        val showInformation = messagesDisplayedWithInformation.contains(event.root.eventId)
        return MessageItem(
                message = message,
                avatarUrl = roomMember.avatarUrl,
                showInformation = showInformation,
                time = timelineDateFormatter.formatMessageHour(date),
                memberName = roomMember.displayName ?: event.root.sender
        )
    }


}