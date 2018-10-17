package im.vector.matrix.android.internal.session.room

import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.room.model.RoomNameContent
import im.vector.matrix.android.api.session.room.model.RoomTopicContent
import im.vector.matrix.android.internal.database.SessionRealmHolder
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.database.model.RoomEntity
import im.vector.matrix.android.internal.database.model.RoomSummaryEntity
import im.vector.matrix.android.internal.database.query.getAll
import im.vector.matrix.android.internal.database.query.getAllFromRoom
import im.vector.matrix.android.internal.database.query.getLast
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.RealmResults
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

internal class RoomSummaryObserver(private val mainThreadRealm: SessionRealmHolder,
                                   private val matrixCoroutineDispatchers: MatrixCoroutineDispatchers,
                                   private val realmConfiguration: RealmConfiguration
) {

    private lateinit var roomResults: RealmResults<RoomEntity>
    private var isStarted = AtomicBoolean(false)

    fun start() {
        if (isStarted.compareAndSet(false, true)) {
            roomResults = RoomEntity.getAll(mainThreadRealm.instance).findAllAsync()
            roomResults.addChangeListener { rooms, changeSet ->
                manageRoomResults(rooms, changeSet.changes)
                manageRoomResults(rooms, changeSet.insertions)
            }
        }
    }

    fun dispose() {
        if (isStarted.compareAndSet(true, false)) {
            roomResults.removeAllChangeListeners()
        }
    }

    // PRIVATE

    private fun manageRoomResults(rooms: RealmResults<RoomEntity>, indexes: IntArray) {
        indexes.forEach {
            val room = rooms[it]
            if (room != null) {
                manageRoom(room.roomId)
            }
        }
    }

    private fun manageRoom(roomId: String) = GlobalScope.launch(matrixCoroutineDispatchers.io) {
        val realm = Realm.getInstance(realmConfiguration)
        val roomEvents = EventEntity.getAllFromRoom(realm, roomId)
        val lastNameEvent = roomEvents.getLast(EventType.STATE_ROOM_NAME)?.asDomain()
        val lastTopicEvent = roomEvents.getLast(EventType.STATE_ROOM_TOPIC)?.asDomain()
        val lastMessageEvent = roomEvents.getLast(EventType.MESSAGE)

        realm.executeTransaction { realmInstance ->
            val roomSummary = realmInstance.copyToRealmOrUpdate(RoomSummaryEntity(roomId))
            roomSummary.displayName = lastNameEvent?.content<RoomNameContent>()?.name
            roomSummary.topic = lastTopicEvent?.content<RoomTopicContent>()?.topic
            roomSummary.lastMessage = lastMessageEvent
        }
        realm.close()
    }

}