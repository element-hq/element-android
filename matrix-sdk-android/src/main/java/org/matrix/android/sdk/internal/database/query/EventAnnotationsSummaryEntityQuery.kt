/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.database.query

import io.realm.kotlin.MutableRealm
import io.realm.kotlin.TypedRealm
import io.realm.kotlin.query.RealmQuery
import org.matrix.android.sdk.internal.database.model.EventAnnotationsSummaryEntity
import org.matrix.android.sdk.internal.database.model.TimelineEventEntity

internal fun EventAnnotationsSummaryEntity.Companion.where(realm: TypedRealm, eventId: String): RealmQuery<EventAnnotationsSummaryEntity> {
    return realm.query(EventAnnotationsSummaryEntity::class)
            .query("eventId == $0", eventId)
}

internal fun EventAnnotationsSummaryEntity.Companion.where(realm: TypedRealm, roomId: String, eventId: String): RealmQuery<EventAnnotationsSummaryEntity> {
    return realm.query(EventAnnotationsSummaryEntity::class)
            .query("roomId == $0", roomId)
            .query("eventId == $0", eventId)
}

internal fun EventAnnotationsSummaryEntity.Companion.create(realm: MutableRealm, roomId: String, eventId: String): EventAnnotationsSummaryEntity {
    val obj = EventAnnotationsSummaryEntity().apply {
        this.eventId = eventId
        this.roomId = roomId
    }
    val managedObj = realm.copyToRealm(obj)
    // Denormalization
    TimelineEventEntity.where(realm, roomId = roomId, eventId = eventId).find().forEach {
        it.annotations = managedObj
    }
    return obj
}

internal fun EventAnnotationsSummaryEntity.Companion.getOrCreate(realm: MutableRealm, roomId: String, eventId: String): EventAnnotationsSummaryEntity {
    return EventAnnotationsSummaryEntity.where(realm, roomId, eventId).first().find()
            ?: EventAnnotationsSummaryEntity.create(realm, roomId, eventId)
}

internal fun EventAnnotationsSummaryEntity.Companion.get(realm: TypedRealm, eventId: String): EventAnnotationsSummaryEntity? {
    return EventAnnotationsSummaryEntity.where(realm, eventId).first().find()
}
