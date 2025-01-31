/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.database.migration

import io.realm.DynamicRealm
import io.realm.FieldAttribute
import org.matrix.android.sdk.internal.database.model.EventAnnotationsSummaryEntityFields
import org.matrix.android.sdk.internal.database.model.livelocation.LiveLocationShareAggregatedSummaryEntityFields
import org.matrix.android.sdk.internal.util.database.RealmMigrator

/**
 * Migrating to:
 * Live location sharing aggregated summary.
 */
internal class MigrateSessionTo027(realm: DynamicRealm) : RealmMigrator(realm, 27) {

    override fun doMigrate(realm: DynamicRealm) {
        val liveLocationSummaryEntity = realm.schema.get("LiveLocationShareAggregatedSummaryEntity")
                ?: realm.schema.create("LiveLocationShareAggregatedSummaryEntity")
                        .addField(LiveLocationShareAggregatedSummaryEntityFields.EVENT_ID, String::class.java, FieldAttribute.REQUIRED)
                        .addField(LiveLocationShareAggregatedSummaryEntityFields.ROOM_ID, String::class.java, FieldAttribute.REQUIRED)
                        .addField(LiveLocationShareAggregatedSummaryEntityFields.IS_ACTIVE, Boolean::class.java)
                        .setNullable(LiveLocationShareAggregatedSummaryEntityFields.IS_ACTIVE, true)
                        .addField(LiveLocationShareAggregatedSummaryEntityFields.END_OF_LIVE_TIMESTAMP_MILLIS, Long::class.java)
                        .setNullable(LiveLocationShareAggregatedSummaryEntityFields.END_OF_LIVE_TIMESTAMP_MILLIS, true)
                        .addField(LiveLocationShareAggregatedSummaryEntityFields.LAST_LOCATION_CONTENT, String::class.java)
                ?: return

        realm.schema.get("EventAnnotationsSummaryEntity")
                ?.addRealmObjectField(EventAnnotationsSummaryEntityFields.LIVE_LOCATION_SHARE_AGGREGATED_SUMMARY.`$`, liveLocationSummaryEntity)
    }
}
