package im.vector.matrix.android.internal.session.room

import android.content.Context
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.Room
import im.vector.matrix.android.api.session.room.model.RoomTopicContent
import im.vector.matrix.android.internal.database.RealmLiveEntityObserver
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

internal class RoomSummaryUpdater(monarchy: Monarchy,
                                  private val roomDisplayNameResolver: RoomDisplayNameResolver,
                                  private val roomAvatarResolver: RoomAvatarResolver,
                                  private val context: Context,
                                  private val credentials: Credentials
) : RealmLiveEntityObserver<RoomEntity>(monarchy) {

    override val query = Monarchy.Query<RoomEntity> { RoomEntity.where(it) }

    override fun process(inserted: List<RoomEntity>, updated: List<RoomEntity>, deleted: List<RoomEntity>) {
        val rooms = (inserted + updated).map { it.asDomain() }
        monarchy.writeAsync { realm ->
            rooms.forEach { updateRoom(realm, it) }
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
        roomSummary.topic = lastTopicEvent?.content.toModel<RoomTopicContent>()?.topic
        roomSummary.lastMessage = lastMessageEvent
        roomSummary.otherMemberIds.clear()
        roomSummary.otherMemberIds.addAll(otherRoomMembers.keys)
    }

}