package im.vector.matrix.android.internal.database.query

import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.room.model.RoomMember
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.database.model.ChunkEntityFields
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.database.model.EventEntityFields
import im.vector.matrix.android.internal.database.model.RoomEntityFields
import io.realm.Realm
import io.realm.RealmList
import io.realm.RealmQuery
import io.realm.Sort
import io.realm.kotlin.where

fun EventEntity.Companion.where(realm: Realm, eventId: String): RealmQuery<EventEntity> {
    return realm.where<EventEntity>()
            .equalTo(EventEntityFields.EVENT_ID, eventId)
}

fun EventEntity.Companion.where(realm: Realm, roomId: String, type: String? = null): RealmQuery<EventEntity> {
    val query = realm.where<EventEntity>()
            .equalTo("${EventEntityFields.CHUNK}.${ChunkEntityFields.ROOM}.${RoomEntityFields.ROOM_ID}", roomId)
    if (type != null) {
        query.equalTo(EventEntityFields.TYPE, type)
    }
    return query
}

fun EventEntity.Companion.stateEvents(realm: Realm, roomId: String): RealmQuery<EventEntity> {
    return realm.where<EventEntity>()
            .equalTo("${EventEntityFields.CHUNK}.${ChunkEntityFields.ROOM}.${RoomEntityFields.ROOM_ID}", roomId)
            .isNotNull(EventEntityFields.STATE_KEY)
}

fun RealmQuery<EventEntity>.last(from: Long? = null): EventEntity? {
    if (from != null) {
        this.lessThanOrEqualTo(EventEntityFields.ORIGIN_SERVER_TS, from)
    }
    return this
            .sort(EventEntityFields.ORIGIN_SERVER_TS, Sort.DESCENDING)
            .findFirst()
}

fun RealmList<EventEntity>.fastContains(eventEntity: EventEntity): Boolean {
    return this.where().equalTo(EventEntityFields.EVENT_ID, eventEntity.eventId).findFirst() != null
}

fun EventEntity.Companion.findAllRoomMembers(realm: Realm, roomId: String): Map<String, RoomMember> {
    return EventEntity
            .where(realm, roomId, EventType.STATE_ROOM_MEMBER)
            .sort(EventEntityFields.ORIGIN_SERVER_TS)
            .findAll()
            .map { it.asDomain() }
            .associateBy { it.stateKey!! }
            .mapValues { it.value.content<RoomMember>()!! }
}