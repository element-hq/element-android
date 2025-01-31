/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
