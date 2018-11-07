package im.vector.matrix.android.internal.session.room

import android.arch.lifecycle.Observer
import android.content.Context
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.room.Room
import im.vector.matrix.android.api.session.room.model.RoomTopicContent
import im.vector.matrix.android.internal.auth.data.Credentials
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.database.model.RoomEntity
import im.vector.matrix.android.internal.database.model.RoomSummaryEntity
import im.vector.matrix.android.internal.database.query.last
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.session.room.members.RoomDisplayNameResolver
import im.vector.matrix.android.internal.session.room.members.RoomMembers
import io.realm.Realm
import io.realm.kotlin.createObject
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

internal class RoomSummaryUpdater(private val monarchy: Monarchy,
                                  private val roomDisplayNameResolver: RoomDisplayNameResolver,
                                  private val roomAvatarResolver: RoomAvatarResolver,
                                  private val context: Context,
                                  private val credentials: Credentials
) : Observer<Monarchy.ManagedChangeSet<RoomEntity>> {

    private var isStarted = AtomicBoolean(false)
    private val liveResults = monarchy.findAllManagedWithChanges { RoomEntity.where(it) }

    fun start() {
        if (isStarted.compareAndSet(false, true)) {
            liveResults.observeForever(this)
        }
    }

    fun dispose() {
        if (isStarted.compareAndSet(true, false)) {
            liveResults.removeObserver(this)
        }
    }

    // PRIVATE

    override fun onChanged(changeSet: Monarchy.ManagedChangeSet<RoomEntity>?) {
        if (changeSet == null) {
            return
        }
        val rooms = changeSet.realmResults.map { it.asDomain() }
        val indexesToUpdate = changeSet.orderedCollectionChangeSet.changes + changeSet.orderedCollectionChangeSet.insertions
        monarchy.writeAsync { realm ->
            updateRoomList(realm, rooms, indexesToUpdate)
        }
    }


    private fun updateRoomList(realm: Realm, rooms: List<Room>, indexes: IntArray) {
        indexes.forEach {
            val room = rooms[it]
            try {
                updateRoom(realm, room)
            } catch (e: Exception) {
                Timber.e(e, "An error occured when updating room summaries")
            }
        }
    }

    private fun updateRoom(realm: Realm, room: Room?) {
        if (room == null) {
            return
        }
        val roomSummary = RoomSummaryEntity.where(realm, room.roomId).findFirst()
                ?: realm.createObject(room.roomId)

        val lastMessageEvent = EventEntity.where(realm, room.roomId, EventType.MESSAGE).last()
        val lastTopicEvent = EventEntity.where(realm, room.roomId, EventType.STATE_ROOM_TOPIC).last()?.asDomain()

        val otherRoomMembers = RoomMembers(realm, room.roomId).getLoaded().filterKeys { it != credentials.userId }

        roomSummary.displayName = roomDisplayNameResolver.resolve(context, room).toString()
        roomSummary.avatarUrl = roomAvatarResolver.resolve(room)
        roomSummary.topic = lastTopicEvent?.content<RoomTopicContent>()?.topic
        roomSummary.lastMessage = lastMessageEvent
        roomSummary.otherMemberIds.clear()
        roomSummary.otherMemberIds.addAll(otherRoomMembers.keys)
    }

}