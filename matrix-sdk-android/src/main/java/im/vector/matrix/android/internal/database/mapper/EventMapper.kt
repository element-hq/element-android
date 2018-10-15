package im.vector.matrix.android.internal.database.mapper

import com.squareup.moshi.Types
import im.vector.matrix.android.api.events.Event
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.di.MoshiProvider


object EventMapper {

    private val moshi = MoshiProvider.providesMoshi()
    private val type = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
    private val adapter = moshi.adapter<Map<String, Any>>(type)

    fun map(event: Event): EventEntity {
        val eventEntity = EventEntity()
        eventEntity.eventId = event.eventId!!
        eventEntity.content = adapter.toJson(event.content)
        eventEntity.prevContent = adapter.toJson(event.prevContent)
        eventEntity.stateKey = event.stateKey
        eventEntity.type = event.type
        return eventEntity
    }

}