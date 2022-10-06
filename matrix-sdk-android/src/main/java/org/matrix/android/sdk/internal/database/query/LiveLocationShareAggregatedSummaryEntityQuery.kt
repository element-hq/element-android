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

import io.realm.kotlin.MutableRealm
import io.realm.kotlin.TypedRealm
import io.realm.kotlin.query.RealmQuery
import org.matrix.android.sdk.internal.database.model.EventAnnotationsSummaryEntity
import org.matrix.android.sdk.internal.database.model.livelocation.LiveLocationShareAggregatedSummaryEntity

internal fun LiveLocationShareAggregatedSummaryEntity.Companion.where(
        realm: TypedRealm,
        eventId: String,
): RealmQuery<LiveLocationShareAggregatedSummaryEntity> {
    return realm.query(LiveLocationShareAggregatedSummaryEntity::class)
            .query("eventId == $0", eventId)
}

internal fun LiveLocationShareAggregatedSummaryEntity.Companion.where(
        realm: TypedRealm,
        roomId: String,
        eventId: String,
): RealmQuery<LiveLocationShareAggregatedSummaryEntity> {
    return LiveLocationShareAggregatedSummaryEntity
            .whereRoomId(realm, roomId = roomId)
            .query("eventId == $0", eventId)
}

internal fun LiveLocationShareAggregatedSummaryEntity.Companion.whereRoomId(
        realm: TypedRealm,
        roomId: String
): RealmQuery<LiveLocationShareAggregatedSummaryEntity> {
    return realm.query(LiveLocationShareAggregatedSummaryEntity::class)
            .query("roomId == $0", roomId)
}

internal fun LiveLocationShareAggregatedSummaryEntity.Companion.create(
        realm: MutableRealm,
        roomId: String,
        eventId: String,
): LiveLocationShareAggregatedSummaryEntity {
    val entity = realm.copyToRealm(
            LiveLocationShareAggregatedSummaryEntity().apply {
                this.eventId = eventId
                this.roomId = roomId
            }
    )
    val annotationSummary = EventAnnotationsSummaryEntity.getOrCreate(realm, roomId = roomId, eventId = eventId)
    annotationSummary.liveLocationShareAggregatedSummary = entity
    return entity
}

internal fun LiveLocationShareAggregatedSummaryEntity.Companion.getOrCreate(
        realm: MutableRealm,
        roomId: String,
        eventId: String,
): LiveLocationShareAggregatedSummaryEntity {
    return LiveLocationShareAggregatedSummaryEntity.where(realm, roomId, eventId).first().find()
            ?: LiveLocationShareAggregatedSummaryEntity.create(realm, roomId, eventId)
}

internal fun LiveLocationShareAggregatedSummaryEntity.Companion.get(
        realm: TypedRealm,
        roomId: String,
        eventId: String,
): LiveLocationShareAggregatedSummaryEntity? {
    return LiveLocationShareAggregatedSummaryEntity.where(realm, roomId, eventId).first().find()
}

internal fun LiveLocationShareAggregatedSummaryEntity.Companion.get(
        realm: TypedRealm,
        eventId: String,
): LiveLocationShareAggregatedSummaryEntity? {
    return LiveLocationShareAggregatedSummaryEntity.where(realm, eventId).first().find()
}

internal fun LiveLocationShareAggregatedSummaryEntity.Companion.findActiveLiveInRoomForUser(
        realm: TypedRealm,
        roomId: String,
        userId: String,
        ignoredEventId: String,
        startOfLiveTimestampThreshold: Long,
): List<LiveLocationShareAggregatedSummaryEntity> {
    return LiveLocationShareAggregatedSummaryEntity
            .whereRoomId(realm, roomId = roomId)
            .query("userId == $0", userId)
            .query("isActive == true")
            .query("eventId != $0", ignoredEventId)
            .query("startOfLiveTimestampMillis < $0", startOfLiveTimestampThreshold)
            .find()
            .toList()
}

/**
 * A live is considered as running when active and with at least a last known location.
 */
internal fun LiveLocationShareAggregatedSummaryEntity.Companion.findRunningLiveInRoom(
        realm: TypedRealm,
        roomId: String,
): RealmQuery<LiveLocationShareAggregatedSummaryEntity> {
    return LiveLocationShareAggregatedSummaryEntity
            .whereRoomId(realm, roomId = roomId)
            .query("isActive == true")
            .query("userId != ''")
            .query("lastLocationContent != ''")
}
