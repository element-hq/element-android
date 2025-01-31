/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.database.migration

import io.realm.DynamicRealm
import org.matrix.android.sdk.internal.database.model.ChunkEntityFields
import org.matrix.android.sdk.internal.database.model.EventEntityFields
import org.matrix.android.sdk.internal.database.model.TimelineEventEntityFields
import org.matrix.android.sdk.internal.util.database.RealmMigrator
import timber.log.Timber

/**
 * Migrating to:
 * Cleaning old chunks which may have broken links.
 */
internal class MigrateSessionTo030(realm: DynamicRealm) : RealmMigrator(realm, 30) {

    override fun doMigrate(realm: DynamicRealm) {
        // Delete all previous chunks
        val chunks = realm.where("ChunkEntity")
                .equalTo(ChunkEntityFields.IS_LAST_FORWARD, false)
                .findAll()

        val nbOfDeletedChunks = chunks.size
        var nbOfDeletedTimelineEvents = 0
        var nbOfDeletedEvents = 0
        chunks.forEach { chunk ->
            val timelineEvents = chunk.getList(ChunkEntityFields.TIMELINE_EVENTS.`$`)
            timelineEvents.forEach { timelineEvent ->
                // Don't delete state events
                val event = timelineEvent.getObject(TimelineEventEntityFields.ROOT.`$`)
                if (event?.isNull(EventEntityFields.STATE_KEY) == true) {
                    nbOfDeletedEvents++
                    event.deleteFromRealm()
                }
            }
            nbOfDeletedTimelineEvents += timelineEvents.size
            timelineEvents.deleteAllFromRealm()
        }
        chunks.deleteAllFromRealm()
        Timber.d(
                "MigrateSessionTo030: $nbOfDeletedChunks deleted chunk(s)," +
                        " $nbOfDeletedTimelineEvents deleted TimelineEvent(s)" +
                        " and $nbOfDeletedEvents deleted Event(s)."
        )
    }
}
