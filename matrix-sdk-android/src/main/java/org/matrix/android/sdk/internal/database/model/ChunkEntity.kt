/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.database.model

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.RealmResults
import io.realm.annotations.Index
import io.realm.annotations.LinkingObjects
import org.matrix.android.sdk.internal.extensions.assertIsManaged
import org.matrix.android.sdk.internal.extensions.clearWith

internal open class ChunkEntity(
        @Index var prevToken: String? = null,
        // Because of gaps we can have several chunks with nextToken == null
        @Index var nextToken: String? = null,
        var prevChunk: ChunkEntity? = null,
        var nextChunk: ChunkEntity? = null,
        var stateEvents: RealmList<EventEntity> = RealmList(),
        var timelineEvents: RealmList<TimelineEventEntity> = RealmList(),
        // Only one chunk will have isLastForward == true
        @Index var isLastForward: Boolean = false,
        @Index var isLastBackward: Boolean = false,
        // Threads
        @Index var rootThreadEventId: String? = null,
        @Index var isLastForwardThread: Boolean = false,
) : RealmObject() {

    fun identifier() = "${prevToken}_$nextToken"

    // If true, then this chunk was previously a last forward chunk
    fun hasBeenALastForwardChunk() = nextToken == null && !isLastForward

    @LinkingObjects("chunks")
    val room: RealmResults<RoomEntity>? = null

    companion object
}

internal fun ChunkEntity.deleteOnCascade(
        deleteStateEvents: Boolean,
        canDeleteRoot: Boolean
) {
    assertIsManaged()
    if (deleteStateEvents) {
        stateEvents.deleteAllFromRealm()
    }
    timelineEvents.clearWith {
        val deleteRoot = canDeleteRoot && (it.root?.stateKey == null || deleteStateEvents)
        if (deleteRoot) {
            room?.firstOrNull()?.removeThreadSummaryIfNeeded(it.eventId)
        }
        it.deleteOnCascade(deleteRoot)
    }
    deleteFromRealm()
}

/**
 * Delete the chunk along with the thread events that were temporarily created.
 */
internal fun ChunkEntity.deleteAndClearThreadEvents() {
    assertIsManaged()
    timelineEvents
            .filter { it.ownedByThreadChunk }
            .forEach {
                it.deleteOnCascade(false)
            }
    deleteFromRealm()
}
