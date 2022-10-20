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

package org.matrix.android.sdk.internal.database.model

import io.realm.kotlin.MutableRealm
import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.types.ObjectId
import io.realm.kotlin.types.RealmList
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.Index
import io.realm.kotlin.types.annotations.PrimaryKey
import org.matrix.android.sdk.internal.database.clearWith
import org.matrix.android.sdk.internal.database.query.where

internal class ChunkEntity : RealmObject {
    @PrimaryKey var chunkId: ObjectId = ObjectId.create()
    @Index var roomId: String = ""
    @Index var prevToken: String? = null

    // Because of gaps we can have several chunks with nextToken == null
    @Index var nextToken: String? = null
    var prevChunk: ChunkEntity? = null
    var nextChunk: ChunkEntity? = null
    var stateEvents: RealmList<EventEntity> = realmListOf()
    var timelineEvents: RealmList<TimelineEventEntity> = realmListOf()

    // Only one chunk will have isLastForward == true
    var isLastForward: Boolean = false
    var isLastBackward: Boolean = false

    // Threads
    @Index var rootThreadEventId: String? = null
    var isLastForwardThread: Boolean = false

    fun identifier() = "${prevToken}_$nextToken"

    // If true, then this chunk was previously a last forward chunk
    fun hasBeenALastForwardChunk() = nextToken == null && !isLastForward
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ChunkEntity

        if (chunkId != other.chunkId) return false

        return true
    }

    override fun hashCode(): Int {
        return chunkId.hashCode()
    }

    companion object
}

internal fun MutableRealm.deleteOnCascade(
        chunkEntity: ChunkEntity,
        deleteStateEvents: Boolean,
        canDeleteRoot: Boolean
) {
    if (deleteStateEvents) {
        delete(chunkEntity.stateEvents)
    }
    chunkEntity.timelineEvents.clearWith {
        val deleteRoot = canDeleteRoot && (it.root?.stateKey == null || deleteStateEvents)
        if (deleteRoot) {
            RoomEntity.where(this, chunkEntity.roomId).first().find()?.let { roomEntity ->
                removeThreadSummaryIfNeeded(roomEntity, it.eventId)
            }
        }
        deleteOnCascade(it, deleteRoot)
    }
    delete(chunkEntity)
}

internal fun MutableRealm.deleteAndClearThreadEvents(chunkEntity: ChunkEntity) {
    chunkEntity.timelineEvents
            .filter { it.ownedByThreadChunk }
            .forEach {
                deleteOnCascade(it, false)
            }
    delete(chunkEntity)
}
