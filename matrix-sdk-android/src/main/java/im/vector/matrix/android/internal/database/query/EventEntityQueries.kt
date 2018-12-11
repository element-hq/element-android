package im.vector.matrix.android.internal.database.query

import im.vector.matrix.android.internal.database.model.ChunkEntityFields
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.database.model.EventEntity.LinkFilterMode.*
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

internal fun EventEntity.Companion.where(realm: Realm,
                                         roomId: String? = null,
                                         type: String? = null,
                                         linkFilterMode: EventEntity.LinkFilterMode = LINKED_ONLY): RealmQuery<EventEntity> {
    val query = realm.where<EventEntity>()
    if (roomId != null) {
        query.beginGroup()
                .equalTo("${EventEntityFields.CHUNK}.${ChunkEntityFields.ROOM}.${RoomEntityFields.ROOM_ID}", roomId)
                .or()
                .equalTo("${EventEntityFields.ROOM}.${RoomEntityFields.ROOM_ID}", roomId)
                .endGroup()
    }
    if (type != null) {
        query.equalTo(EventEntityFields.TYPE, type)
    }
    return when (linkFilterMode) {
        LINKED_ONLY -> query.equalTo(EventEntityFields.IS_UNLINKED, false)
        UNLINKED_ONLY -> query.equalTo(EventEntityFields.IS_UNLINKED, true)
        BOTH -> query
    }
}

internal fun RealmQuery<EventEntity>.next(from: Int? = null, strict: Boolean = true): EventEntity? {
    if (from != null) {
        if (strict) {
            this.greaterThan(EventEntityFields.STATE_INDEX, from)
        } else {
            this.greaterThanOrEqualTo(EventEntityFields.STATE_INDEX, from)
        }
    }
    return this
            .sort(EventEntityFields.STATE_INDEX, Sort.ASCENDING)
            .findFirst()
}


internal fun RealmQuery<EventEntity>.last(since: Int? = null, strict: Boolean = false): EventEntity? {
    if (since != null) {
        if (strict) {
            this.lessThan(EventEntityFields.STATE_INDEX, since)
        } else {
            this.lessThanOrEqualTo(EventEntityFields.STATE_INDEX, since)
        }
    }
    return this
            .sort(EventEntityFields.STATE_INDEX, Sort.DESCENDING)
            .findFirst()
}

internal fun RealmList<EventEntity>.find(eventId: String): EventEntity? {
    return this.where().equalTo(EventEntityFields.EVENT_ID, eventId).findFirst()
}

internal fun RealmList<EventEntity>.
        fastContains(eventId: String): Boolean {
    return this.find(eventId) != null
}
