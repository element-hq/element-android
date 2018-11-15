package im.vector.matrix.android.internal.database.mapper

import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.UnsignedData
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.di.MoshiProvider


internal object EventMapper {

    private val moshi = MoshiProvider.providesMoshi()
    private val adapter = moshi.adapter<Map<String, Any>>(Event.CONTENT_TYPE)

    fun map(event: Event): EventEntity {
        val eventEntity = EventEntity()
        fill(eventEntity, with = event)
        return eventEntity
    }

    fun map(eventEntity: EventEntity): Event {
        return Event(
                type = eventEntity.type,
                eventId = eventEntity.eventId,
                content = adapter.fromJson(eventEntity.content),
                prevContent = adapter.fromJson(eventEntity.prevContent ?: ""),
                originServerTs = eventEntity.originServerTs,
                sender = eventEntity.sender,
                stateKey = eventEntity.stateKey,
                roomId = null,
                unsignedData = UnsignedData(eventEntity.age),
                redacts = eventEntity.redacts
        )
    }

    fun fill(eventEntity: EventEntity, with: Event) {
        eventEntity.eventId = with.eventId ?: ""
        eventEntity.content = adapter.toJson(with.content)
        val resolvedPrevContent = with.prevContent ?: with.unsignedData?.prevContent
        eventEntity.prevContent = adapter.toJson(resolvedPrevContent)
        eventEntity.stateKey = with.stateKey
        eventEntity.type = with.type
        eventEntity.sender = with.sender
        eventEntity.originServerTs = with.originServerTs
        eventEntity.redacts = with.redacts
        eventEntity.age = with.unsignedData?.age ?: with.originServerTs
    }

}

internal fun EventEntity.asDomain(): Event {
    return EventMapper.map(this)
}

internal fun Event.asEntity(): EventEntity {
    return EventMapper.map(this)
}

internal fun EventEntity.fillWith(event: Event) {
    EventMapper.fill(this, with = event)
}
