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
import org.matrix.android.sdk.internal.util.database.RealmMigrator

internal class MigrateSessionTo020(realm: DynamicRealm) : RealmMigrator(realm, 20) {

    override fun doMigrate(realm: DynamicRealm) {
        realm.schema.get("ChunkEntity")?.apply {
            if (hasField("numberOfTimelineEvents")) {
                removeField("numberOfTimelineEvents")
            }
            var cleanOldChunks = false
            if (!hasField(ChunkEntityFields.NEXT_CHUNK.`$`)) {
                cleanOldChunks = true
                addRealmObjectField(ChunkEntityFields.NEXT_CHUNK.`$`, this)
            }
            if (!hasField(ChunkEntityFields.PREV_CHUNK.`$`)) {
                cleanOldChunks = true
                addRealmObjectField(ChunkEntityFields.PREV_CHUNK.`$`, this)
            }
            if (cleanOldChunks) {
                val chunkEntities = realm.where("ChunkEntity").equalTo(ChunkEntityFields.IS_LAST_FORWARD, false).findAll()
                chunkEntities.deleteAllFromRealm()
            }
        }
    }
}
