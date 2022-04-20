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
