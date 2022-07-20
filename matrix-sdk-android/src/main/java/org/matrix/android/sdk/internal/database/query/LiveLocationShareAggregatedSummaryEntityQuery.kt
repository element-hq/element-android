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
import org.matrix.android.sdk.internal.database.model.livelocation.LiveLocationShareAggregatedSummaryEntity
import org.matrix.android.sdk.internal.database.model.livelocation.LiveLocationShareAggregatedSummaryEntityFields

internal fun LiveLocationShareAggregatedSummaryEntity.Companion.where(
        realm: Realm,
        eventId: String,
): RealmQuery<LiveLocationShareAggregatedSummaryEntity> {
    return realm.where<LiveLocationShareAggregatedSummaryEntity>()
            .equalTo(LiveLocationShareAggregatedSummaryEntityFields.EVENT_ID, eventId)
}

internal fun LiveLocationShareAggregatedSummaryEntity.Companion.where(
        realm: Realm,
        roomId: String,
        eventId: String,
): RealmQuery<LiveLocationShareAggregatedSummaryEntity> {
    return LiveLocationShareAggregatedSummaryEntity
            .whereRoomId(realm, roomId = roomId)
            .equalTo(LiveLocationShareAggregatedSummaryEntityFields.EVENT_ID, eventId)
}

internal fun LiveLocationShareAggregatedSummaryEntity.Companion.whereRoomId(
        realm: Realm,
        roomId: String
): RealmQuery<LiveLocationShareAggregatedSummaryEntity> {
    return realm.where<LiveLocationShareAggregatedSummaryEntity>()
            .equalTo(LiveLocationShareAggregatedSummaryEntityFields.ROOM_ID, roomId)
}

internal fun LiveLocationShareAggregatedSummaryEntity.Companion.create(
        realm: Realm,
        roomId: String,
        eventId: String,
): LiveLocationShareAggregatedSummaryEntity {
    val obj = realm.createObject(LiveLocationShareAggregatedSummaryEntity::class.java, eventId).apply {
        this.roomId = roomId
    }
    val annotationSummary = EventAnnotationsSummaryEntity.getOrCreate(realm, roomId = roomId, eventId = eventId)
    annotationSummary.liveLocationShareAggregatedSummary = obj

    return obj
}

internal fun LiveLocationShareAggregatedSummaryEntity.Companion.getOrCreate(
        realm: Realm,
        roomId: String,
        eventId: String,
): LiveLocationShareAggregatedSummaryEntity {
    return LiveLocationShareAggregatedSummaryEntity.where(realm, roomId, eventId).findFirst()
            ?: LiveLocationShareAggregatedSummaryEntity.create(realm, roomId, eventId)
}

internal fun LiveLocationShareAggregatedSummaryEntity.Companion.get(
        realm: Realm,
        roomId: String,
        eventId: String,
): LiveLocationShareAggregatedSummaryEntity? {
    return LiveLocationShareAggregatedSummaryEntity.where(realm, roomId, eventId).findFirst()
}

internal fun LiveLocationShareAggregatedSummaryEntity.Companion.get(
        realm: Realm,
        eventId: String,
): LiveLocationShareAggregatedSummaryEntity? {
    return LiveLocationShareAggregatedSummaryEntity.where(realm, eventId).findFirst()
}

internal fun LiveLocationShareAggregatedSummaryEntity.Companion.findActiveLiveInRoomForUser(
        realm: Realm,
        roomId: String,
        userId: String,
        ignoredEventId: String,
        startOfLiveTimestampThreshold: Long,
): List<LiveLocationShareAggregatedSummaryEntity> {
    return LiveLocationShareAggregatedSummaryEntity
            .whereRoomId(realm, roomId = roomId)
            .equalTo(LiveLocationShareAggregatedSummaryEntityFields.USER_ID, userId)
            .equalTo(LiveLocationShareAggregatedSummaryEntityFields.IS_ACTIVE, true)
            .notEqualTo(LiveLocationShareAggregatedSummaryEntityFields.EVENT_ID, ignoredEventId)
            .lessThan(LiveLocationShareAggregatedSummaryEntityFields.START_OF_LIVE_TIMESTAMP_MILLIS, startOfLiveTimestampThreshold)
            .findAll()
            .toList()
}

/**
 * A live is considered as running when active and with at least a last known location.
 */
internal fun LiveLocationShareAggregatedSummaryEntity.Companion.findRunningLiveInRoom(
        realm: Realm,
        roomId: String,
): RealmQuery<LiveLocationShareAggregatedSummaryEntity> {
    return LiveLocationShareAggregatedSummaryEntity
            .whereRoomId(realm, roomId = roomId)
            .equalTo(LiveLocationShareAggregatedSummaryEntityFields.IS_ACTIVE, true)
            .isNotEmpty(LiveLocationShareAggregatedSummaryEntityFields.USER_ID)
            .isNotNull(LiveLocationShareAggregatedSummaryEntityFields.LAST_LOCATION_CONTENT)
}
