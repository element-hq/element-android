package im.vector.matrix.android.internal.database.query

import im.vector.matrix.android.internal.database.model.ReferencesAggregatedSummaryEntity
import im.vector.matrix.android.internal.database.model.ReferencesAggregatedSummaryEntityFields
import io.realm.Realm
import io.realm.RealmQuery
import io.realm.kotlin.where

internal fun ReferencesAggregatedSummaryEntity.Companion.where(realm: Realm, eventId: String): RealmQuery<ReferencesAggregatedSummaryEntity> {
    val query = realm.where<ReferencesAggregatedSummaryEntity>()
    query.equalTo(ReferencesAggregatedSummaryEntityFields.EVENT_ID, eventId)
    return query
}

internal fun ReferencesAggregatedSummaryEntity.Companion.create(realm: Realm, txID: String): ReferencesAggregatedSummaryEntity {
    return realm.createObject(ReferencesAggregatedSummaryEntity::class.java).apply {
        this.eventId = txID
    }
}



