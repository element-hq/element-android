/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.database.query

import io.realm.Realm
import io.realm.RealmList
import io.realm.RealmQuery
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import org.matrix.android.sdk.internal.database.model.threads.ThreadSummaryEntity
import org.matrix.android.sdk.internal.database.model.threads.ThreadSummaryEntityFields

internal fun ThreadSummaryEntity.Companion.where(realm: Realm, roomId: String): RealmQuery<ThreadSummaryEntity> {
    return realm.where<ThreadSummaryEntity>()
            .equalTo(ThreadSummaryEntityFields.ROOM.ROOM_ID, roomId)
}

internal fun ThreadSummaryEntity.Companion.where(realm: Realm, roomId: String, rootThreadEventId: String): RealmQuery<ThreadSummaryEntity> {
    return where(realm, roomId)
            .equalTo(ThreadSummaryEntityFields.ROOT_THREAD_EVENT_ID, rootThreadEventId)
}

internal fun ThreadSummaryEntity.Companion.getOrCreate(realm: Realm, roomId: String, rootThreadEventId: String): ThreadSummaryEntity {
    return where(realm, roomId, rootThreadEventId).findFirst() ?: realm.createObject<ThreadSummaryEntity>().apply {
        this.rootThreadEventId = rootThreadEventId
    }
}

internal fun ThreadSummaryEntity.Companion.getOrNull(realm: Realm, roomId: String, rootThreadEventId: String): ThreadSummaryEntity? {
    return where(realm, roomId, rootThreadEventId).findFirst()
}

internal fun RealmList<ThreadSummaryEntity>.find(rootThreadEventId: String): ThreadSummaryEntity? {
    return this.where()
            .equalTo(ThreadSummaryEntityFields.ROOT_THREAD_EVENT_ID, rootThreadEventId)
            .findFirst()
}

internal fun RealmList<ThreadSummaryEntity>.findRootOrLatest(eventId: String): ThreadSummaryEntity? {
    return this.where()
            .beginGroup()
            .equalTo(ThreadSummaryEntityFields.ROOT_THREAD_EVENT_ID, eventId)
            .or()
            .equalTo(ThreadSummaryEntityFields.LATEST_THREAD_EVENT_ENTITY.EVENT_ID, eventId)
            .endGroup()
            .findFirst()
}
