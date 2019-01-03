package im.vector.matrix.android.session.room.timeline

import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.room.model.MyMembership
import im.vector.matrix.android.internal.database.helper.addAll
import im.vector.matrix.android.internal.database.helper.addOrUpdate
import im.vector.matrix.android.internal.database.model.ChunkEntity
import im.vector.matrix.android.internal.database.model.RoomEntity
import im.vector.matrix.android.internal.session.room.timeline.PaginationDirection
import io.realm.kotlin.createObject
import kotlin.random.Random

object RoomDataHelper {

    fun createFakeListOfEvents(size: Int = 10): List<Event> {
        return (0 until size).map { createFakeEvent(Random.nextBoolean()) }
    }

    fun createFakeEvent(asStateEvent: Boolean = false): Event {
        val eventId = Random.nextLong(System.currentTimeMillis()).toString()
        val type = if (asStateEvent) EventType.STATE_ROOM_NAME else EventType.MESSAGE
        return Event(type, eventId)
    }

    fun fakeInitialSync(monarchy: Monarchy, roomId: String) {
        monarchy.runTransactionSync { realm ->
            val roomEntity = realm.createObject<RoomEntity>(roomId)
            roomEntity.membership = MyMembership.JOINED
            val eventList = createFakeListOfEvents(30)
            val chunkEntity = realm.createObject<ChunkEntity>().apply {
                nextToken = null
                prevToken = Random.nextLong(System.currentTimeMillis()).toString()
                isLast = true
            }
            chunkEntity.addAll("roomId", eventList, PaginationDirection.FORWARDS)
            roomEntity.addOrUpdate(chunkEntity)
        }
    }


}