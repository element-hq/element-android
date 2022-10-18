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

import io.realm.kotlin.MutableRealm
import io.realm.kotlin.TypedRealm
import io.realm.kotlin.query.RealmQuery
import org.matrix.android.sdk.internal.database.model.threads.ThreadSummaryEntity

internal fun ThreadSummaryEntity.Companion.where(realm: TypedRealm, roomId: String): RealmQuery<ThreadSummaryEntity> {
    return realm.query(ThreadSummaryEntity::class)
            .query("room.roomId == $0", roomId)
}

internal fun ThreadSummaryEntity.Companion.where(realm: TypedRealm, roomId: String, rootThreadEventId: String): RealmQuery<ThreadSummaryEntity> {
    return where(realm, roomId)
            .query("rootThreadEventId == $0", rootThreadEventId)
}

internal fun ThreadSummaryEntity.Companion.getOrCreate(realm: MutableRealm, roomId: String, rootThreadEventId: String): ThreadSummaryEntity {
    return where(realm, roomId, rootThreadEventId).first().find() ?: create(realm, roomId, rootThreadEventId)
}

internal fun ThreadSummaryEntity.Companion.create(realm: MutableRealm, roomId: String, rootThreadEventId: String): ThreadSummaryEntity {
    val threadSummaryEntity = ThreadSummaryEntity().apply {
        this.rootThreadEventId = rootThreadEventId
        this.roomId = roomId
    }
    return realm.copyToRealm(threadSummaryEntity)
}

internal fun ThreadSummaryEntity.Companion.getOrNull(realm: TypedRealm, roomId: String, rootThreadEventId: String): ThreadSummaryEntity? {
    return where(realm, roomId, rootThreadEventId).first().find()
}

/*
internal fun RealmList<ThreadSummaryEntity>.find(rootThreadEventId: String): ThreadSummaryEntity? {
    return this.where()
            .equalTo(ThreadSummaryEntityFields.ROOT_THREAD_EVENT_ID, rootThreadEventId)
            .findFirst()
}
 */

internal fun ThreadSummaryEntity.Companion.findRootOrLatest(realm: TypedRealm, eventId: String): ThreadSummaryEntity? {
    return realm.query(ThreadSummaryEntity::class)
            .query("rootThreadEventId == $0 OR latestThreadEventEntity.eventId == $1", eventId, eventId)
            .first()
            .find()
}
