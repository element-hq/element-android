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
import io.realm.FieldAttribute
import org.matrix.android.sdk.internal.database.model.EventAnnotationsSummaryEntityFields
import org.matrix.android.sdk.internal.database.model.livelocation.LiveLocationAggregatedSummaryEntityFields
import org.matrix.android.sdk.internal.util.database.RealmMigrator

/**
 * Migrating to:
 * Live location sharing aggregated summary
 */
internal class MigrateSessionTo027(realm: DynamicRealm) : RealmMigrator(realm, 27) {

    override fun doMigrate(realm: DynamicRealm) {
        val liveLocationSummaryEntity = realm.schema.get("LiveLocationAggregatedSummaryEntity")
                ?: realm.schema.create("LiveLocationAggregatedSummaryEntity")
                        .addField(LiveLocationAggregatedSummaryEntityFields.EVENT_ID, String::class.java, FieldAttribute.REQUIRED)
                        .addField(LiveLocationAggregatedSummaryEntityFields.ROOM_ID, String::class.java, FieldAttribute.REQUIRED)
                        .addField(LiveLocationAggregatedSummaryEntityFields.IS_ACTIVE, Boolean::class.java)
                        .setNullable(LiveLocationAggregatedSummaryEntityFields.IS_ACTIVE, true)
                        .addField(LiveLocationAggregatedSummaryEntityFields.END_OF_LIVE_TIMESTAMP_AS_MILLISECONDS, Long::class.java)
                        .setNullable(LiveLocationAggregatedSummaryEntityFields.END_OF_LIVE_TIMESTAMP_AS_MILLISECONDS, true)
                        .addField(LiveLocationAggregatedSummaryEntityFields.LAST_LOCATION_CONTENT, String::class.java)
                ?: return

        realm.schema.get("EventAnnotationsSummaryEntity")
                ?.addRealmObjectField(EventAnnotationsSummaryEntityFields.LIVE_LOCATION_AGGREGATED_SUMMARY.`$`, liveLocationSummaryEntity)
    }
}
