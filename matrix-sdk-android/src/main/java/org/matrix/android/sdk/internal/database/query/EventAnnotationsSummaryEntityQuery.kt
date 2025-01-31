/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.database.query

import io.realm.Realm
import io.realm.RealmQuery
import io.realm.kotlin.where
import org.matrix.android.sdk.internal.database.model.EventAnnotationsSummaryEntity
import org.matrix.android.sdk.internal.database.model.EventAnnotationsSummaryEntityFields
import org.matrix.android.sdk.internal.database.model.TimelineEventEntity

internal fun EventAnnotationsSummaryEntity.Companion.where(realm: Realm, eventId: String): RealmQuery<EventAnnotationsSummaryEntity> {
    return realm.where<EventAnnotationsSummaryEntity>()
            .equalTo(EventAnnotationsSummaryEntityFields.EVENT_ID, eventId)
}

internal fun EventAnnotationsSummaryEntity.Companion.where(realm: Realm, roomId: String, eventId: String): RealmQuery<EventAnnotationsSummaryEntity> {
    return realm.where<EventAnnotationsSummaryEntity>()
            .equalTo(EventAnnotationsSummaryEntityFields.ROOM_ID, roomId)
            .equalTo(EventAnnotationsSummaryEntityFields.EVENT_ID, eventId)
}

internal fun EventAnnotationsSummaryEntity.Companion.create(realm: Realm, roomId: String, eventId: String): EventAnnotationsSummaryEntity {
    val obj = realm.createObject(EventAnnotationsSummaryEntity::class.java, eventId).apply {
        this.roomId = roomId
    }
    // Denormalization
    TimelineEventEntity.where(realm, roomId = roomId, eventId = eventId).findAll()?.forEach {
        it.annotations = obj
    }
    return obj
}

internal fun EventAnnotationsSummaryEntity.Companion.getOrCreate(realm: Realm, roomId: String, eventId: String): EventAnnotationsSummaryEntity {
    return EventAnnotationsSummaryEntity.where(realm, roomId, eventId).findFirst()
            ?: EventAnnotationsSummaryEntity.create(realm, roomId, eventId)
}

internal fun EventAnnotationsSummaryEntity.Companion.get(realm: Realm, eventId: String): EventAnnotationsSummaryEntity? {
    return EventAnnotationsSummaryEntity.where(realm, eventId).findFirst()
}
