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
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntityFields
import org.matrix.android.sdk.internal.util.Normalizer
import org.matrix.android.sdk.internal.util.database.RealmMigrator

internal class MigrateSessionTo019(realm: DynamicRealm,
                          private val normalizer: Normalizer) : RealmMigrator(realm, 19) {

    override fun doMigrate(realm: DynamicRealm) {
        realm.schema.get("RoomSummaryEntity")
                ?.addField(RoomSummaryEntityFields.NORMALIZED_DISPLAY_NAME, String::class.java)
                ?.transform {
                    it.getString(RoomSummaryEntityFields.DISPLAY_NAME)?.let { displayName ->
                        val normalised = normalizer.normalize(displayName)
                        it.set(RoomSummaryEntityFields.NORMALIZED_DISPLAY_NAME, normalised)
                    }
                }
    }
}
