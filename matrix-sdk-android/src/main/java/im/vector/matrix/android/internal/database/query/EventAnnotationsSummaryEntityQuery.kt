package im.vector.matrix.android.internal.database.query

import im.vector.matrix.android.internal.database.model.EventAnnotationsSummaryEntity
import im.vector.matrix.android.internal.database.model.EventAnnotationsSummaryEntityFields
import io.realm.Realm
import io.realm.RealmQuery
import io.realm.kotlin.where

internal fun EventAnnotationsSummaryEntity.Companion.where(realm: Realm, eventId: String): RealmQuery<EventAnnotationsSummaryEntity> {
    val query = realm.where<EventAnnotationsSummaryEntity>()
    query.equalTo(EventAnnotationsSummaryEntityFields.EVENT_ID, eventId)
    return query
}

internal fun EventAnnotationsSummaryEntity.Companion.whereInRoom(realm: Realm, roomId: String?): RealmQuery<EventAnnotationsSummaryEntity> {
    val query = realm.where<EventAnnotationsSummaryEntity>()
    if (roomId != null) {
        query.equalTo(EventAnnotationsSummaryEntityFields.ROOM_ID, roomId)
    }
    return query
}


internal fun EventAnnotationsSummaryEntity.Companion.create(realm: Realm, eventId: String): EventAnnotationsSummaryEntity {
    val obj = realm.createObject(EventAnnotationsSummaryEntity::class.java, eventId)
    return obj
}