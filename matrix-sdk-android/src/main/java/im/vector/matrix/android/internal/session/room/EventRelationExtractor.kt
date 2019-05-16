package im.vector.matrix.android.internal.session.room

import im.vector.matrix.android.api.session.room.model.EventAnnotationsSummary
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.database.model.EventAnnotationsSummaryEntity
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.database.query.where
import io.realm.Realm


internal class EventRelationExtractor {

    fun extractFrom(event: EventEntity, realm: Realm = event.realm): EventAnnotationsSummary? {
        return EventAnnotationsSummaryEntity.where(realm, event.eventId).findFirst()?.asDomain()
    }
}