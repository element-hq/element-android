/*
 * Copyright (c) 2022 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.database.query

import io.realm.Realm
import io.realm.RealmQuery
import io.realm.kotlin.where
import org.matrix.android.sdk.internal.database.model.EventAnnotationsSummaryEntity
import org.matrix.android.sdk.internal.database.model.livelocation.LiveLocationAggregatedSummaryEntity
import org.matrix.android.sdk.internal.database.model.livelocation.LiveLocationAggregatedSummaryEntityFields

internal fun LiveLocationAggregatedSummaryEntity.Companion.where(
        realm: Realm,
        roomId: String,
        eventId: String,
): RealmQuery<LiveLocationAggregatedSummaryEntity> {
    return realm.where<LiveLocationAggregatedSummaryEntity>()
            .equalTo(LiveLocationAggregatedSummaryEntityFields.ROOM_ID, roomId)
            .equalTo(LiveLocationAggregatedSummaryEntityFields.EVENT_ID, eventId)
}

internal fun LiveLocationAggregatedSummaryEntity.Companion.create(
        realm: Realm,
        roomId: String,
        eventId: String,
): LiveLocationAggregatedSummaryEntity {
    val obj = realm.createObject(LiveLocationAggregatedSummaryEntity::class.java, eventId).apply {
        this.roomId = roomId
    }
    val annotationSummary = EventAnnotationsSummaryEntity.getOrCreate(realm, roomId = roomId, eventId = eventId)
    annotationSummary.liveLocationAggregatedSummary = obj

    return obj
}

internal fun LiveLocationAggregatedSummaryEntity.Companion.getOrCreate(
        realm: Realm,
        roomId: String,
        eventId: String,
): LiveLocationAggregatedSummaryEntity {
    return LiveLocationAggregatedSummaryEntity.where(realm, roomId, eventId).findFirst()
            ?: LiveLocationAggregatedSummaryEntity.create(realm, roomId, eventId)
}
