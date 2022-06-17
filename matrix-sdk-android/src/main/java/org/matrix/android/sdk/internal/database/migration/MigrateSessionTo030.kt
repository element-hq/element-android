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

package org.matrix.android.sdk.internal.database.migration

import io.realm.DynamicRealm
import org.matrix.android.sdk.internal.database.model.ChunkEntityFields
import org.matrix.android.sdk.internal.database.model.TimelineEventEntityFields
import org.matrix.android.sdk.internal.extensions.clearWith
import org.matrix.android.sdk.internal.util.database.RealmMigrator

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

        chunks.forEach { chunk ->
            chunk.getList(ChunkEntityFields.TIMELINE_EVENTS.`$`).clearWith { timelineEvent ->
                // Don't delete state events
                if (timelineEvent.isNull(TimelineEventEntityFields.ROOT.STATE_KEY)) {
                    timelineEvent.getObject(TimelineEventEntityFields.ROOT.`$`)?.deleteFromRealm()
                    timelineEvent.deleteFromRealm()
                }
            }
            chunk.deleteFromRealm()
        }
    }
}
