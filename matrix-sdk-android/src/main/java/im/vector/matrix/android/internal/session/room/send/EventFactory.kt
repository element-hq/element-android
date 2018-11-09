package im.vector.matrix.android.internal.session.room.send

import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.api.session.events.model.Content
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.room.model.MessageContent
import im.vector.matrix.android.api.session.room.model.MessageType
import im.vector.matrix.android.internal.di.MoshiProvider

internal class EventFactory(private val credentials: Credentials) {

    private val moshi = MoshiProvider.providesMoshi()

    fun createTextEvent(roomId: String, text: String): Event {
        val content = MessageContent(type = MessageType.MSGTYPE_TEXT, body = text)

        return Event(
                roomId = roomId,
                originServerTs = dummyOriginServerTs(),
                sender = credentials.userId,
                eventId = dummyEventId(roomId),
                type = EventType.MESSAGE,
                content = toContent(content)
        )
    }

    private fun dummyOriginServerTs(): Long {
        return System.currentTimeMillis()
    }

    private fun dummyEventId(roomId: String): String {
        return roomId + "-" + dummyOriginServerTs()
    }

    @Suppress("UNCHECKED_CAST")
    private inline fun <reified T> toContent(data: T?): Content? {
        val moshiAdapter = moshi.adapter(T::class.java)
        val jsonValue = moshiAdapter.toJsonValue(data)
        return jsonValue as? Content?
    }


}