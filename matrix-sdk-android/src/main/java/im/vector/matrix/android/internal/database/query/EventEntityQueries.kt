package im.vector.matrix.android.internal.database.query

import im.vector.matrix.android.internal.database.model.ChunkEntityFields
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.database.model.EventEntityFields
import im.vector.matrix.android.internal.database.model.RoomEntityFields
import io.realm.Realm
import io.realm.RealmList
import io.realm.RealmQuery
import io.realm.Sort
import io.realm.kotlin.where

internal fun EventEntity.Companion.where(realm: Realm, eventId: String): RealmQuery<EventEntity> {
    return realm.where<EventEntity>()
            .equalTo(EventEntityFields.EVENT_ID, eventId)
}

internal fun EventEntity.Companion.where(realm: Realm, roomId: String? = null, type: String? = null): RealmQuery<EventEntity> {
    val query = realm.where<EventEntity>()
    if (roomId != null) {
        query.equalTo("${EventEntityFields.CHUNK}.${ChunkEntityFields.ROOM}.${RoomEntityFields.ROOM_ID}", roomId)
    }
    if (type != null) {
        query.equalTo(EventEntityFields.TYPE, type)
    }
    return query
}

internal fun EventEntity.Companion.stateEvents(realm: Realm, roomId: String): RealmQuery<EventEntity> {
    return realm.where<EventEntity>()
            .equalTo("${EventEntityFields.CHUNK}.${ChunkEntityFields.ROOM}.${RoomEntityFields.ROOM_ID}", roomId)
            .isNotNull(EventEntityFields.STATE_KEY)
}

internal fun RealmQuery<EventEntity>.last(from: Long? = null): EventEntity? {
    if (from != null) {
        this.lessThan(EventEntityFields.ORIGIN_SERVER_TS, from)
    }
    return this
            .sort(EventEntityFields.ORIGIN_SERVER_TS, Sort.DESCENDING)
            .findFirst()
}

internal fun RealmList<EventEntity>.fastContains(eventEntity: EventEntity): Boolean {
    return this.where().equalTo(EventEntityFields.EVENT_ID, eventEntity.eventId).findFirst() != null
}
