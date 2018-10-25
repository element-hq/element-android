package im.vector.matrix.android.internal.database.query

import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.room.model.RoomMember
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.database.model.EventEntity
import io.realm.Realm
import io.realm.RealmQuery
import io.realm.Sort

fun EventEntity.Companion.where(realm: Realm, eventId: String): RealmQuery<EventEntity> {
    return realm.where(EventEntity::class.java)
            .equalTo("eventId", eventId)
}

fun EventEntity.Companion.where(realm: Realm, roomId: String, type: String? = null): RealmQuery<EventEntity> {
    val query = realm.where(EventEntity::class.java)
            .equalTo("chunk.room.roomId", roomId)
    if (type != null) {
        query.equalTo("type", type)
    }
    return query
}

fun EventEntity.Companion.stateEvents(realm: Realm, roomId: String): RealmQuery<EventEntity> {
    return realm.where(EventEntity::class.java)
            .equalTo("chunk.room.roomId", roomId)
            .isNotNull("stateKey")
}

fun RealmQuery<EventEntity>.last(from: Long? = null): EventEntity? {
    if (from != null) {
        this.lessThanOrEqualTo("originServerTs", from)
    }
    return this
            .sort("originServerTs", Sort.DESCENDING)
            .findFirst()
}

fun EventEntity.Companion.findAllRoomMembers(realm: Realm, roomId: String): Map<String, RoomMember> {
    return EventEntity
            .where(realm, roomId, EventType.STATE_ROOM_MEMBER)
            .sort("originServerTs")
            .findAll()
            .map { it.asDomain() }
            .associateBy { it.stateKey!! }
            .mapValues { it.value.content<RoomMember>()!! }
}