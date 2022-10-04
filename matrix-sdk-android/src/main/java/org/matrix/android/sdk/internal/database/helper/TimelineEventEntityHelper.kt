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

package org.matrix.android.sdk.internal.database.helper

import io.realm.kotlin.TypedRealm
import org.matrix.android.sdk.internal.database.model.ChunkEntity
import org.matrix.android.sdk.internal.database.model.TimelineEventEntity
import org.matrix.android.sdk.internal.database.query.find
import org.matrix.android.sdk.internal.database.query.where

internal fun TimelineEventEntity.Companion.nextId(realm: TypedRealm): Long {
    val currentIdNum = TimelineEventEntity.where(realm).max("localId", Long::class).find()
    return if (currentIdNum == null) {
        1
    } else {
        currentIdNum.toLong() + 1
    }
}

internal fun TimelineEventEntity.isMoreRecentThan(realm: TypedRealm, eventToCheck: TimelineEventEntity): Boolean {
    val chunkId = this.chunkId ?: return false
    val currentChunk = ChunkEntity.find(realm, chunkId = chunkId) ?: return false
    val chunkIdOfEventToCheck = eventToCheck.chunkId ?: return false
    val chunkToCheck = ChunkEntity.find(realm, chunkId = chunkIdOfEventToCheck) ?: return false
    return if (currentChunk == chunkToCheck) {
        this.displayIndex >= eventToCheck.displayIndex
    } else {
        currentChunk.isMoreRecentThan(chunkToCheck)
    }
}
