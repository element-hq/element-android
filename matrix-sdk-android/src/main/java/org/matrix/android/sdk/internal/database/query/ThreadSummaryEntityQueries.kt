/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
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
