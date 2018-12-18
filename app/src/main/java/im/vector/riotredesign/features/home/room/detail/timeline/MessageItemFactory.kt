package im.vector.riotredesign.features.home.room.detail.timeline

import im.vector.matrix.android.api.session.events.model.EnrichedEvent
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.MessageContent
import org.threeten.bp.LocalDateTime

class MessageItemFactory(private val timelineDateFormatter: TimelineDateFormatter) {

    private val messagesDisplayedWithInformation = HashSet<String?>()

    fun create(event: EnrichedEvent, nextEvent: EnrichedEvent?, addDaySeparator: Boolean, date: LocalDateTime): MessageItem? {
        val messageContent: MessageContent? = event.root.content.toModel()
        val roomMember = event.roomMember
        if (messageContent == null || roomMember == null) {
            return null
        }
        val nextRoomMember = nextEvent?.roomMember
        if (addDaySeparator || nextRoomMember != roomMember) {
            messagesDisplayedWithInformation.add(event.root.eventId)
        }
        val showInformation = messagesDisplayedWithInformation.contains(event.root.eventId)

        return MessageItem(
                message = messageContent.body,
                avatarUrl = roomMember.avatarUrl,
                showInformation = showInformation,
                time = timelineDateFormatter.formatMessageHour(date),
                memberName = roomMember.displayName ?: event.root.sender
        )
    }


}