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
import org.matrix.android.sdk.internal.database.model.ChunkEntityFields
import org.matrix.android.sdk.internal.database.model.HomeServerCapabilitiesEntityFields
import org.matrix.android.sdk.internal.database.model.RoomEntityFields
import org.matrix.android.sdk.internal.database.model.TimelineEventEntityFields
import org.matrix.android.sdk.internal.database.model.threads.ThreadSummaryEntityFields
import org.matrix.android.sdk.internal.extensions.forceRefreshOfHomeServerCapabilities
import org.matrix.android.sdk.internal.util.database.RealmMigrator

/**
 * Migrating to:
 * Live thread list: using enhanced /messages api MSC3440
 * Live thread timeline: using /relations api
 */
class MigrateSessionTo026(realm: DynamicRealm) : RealmMigrator(realm, 26) {

    override fun doMigrate(realm: DynamicRealm) {
        realm.schema.get("ChunkEntity")
                ?.addField(ChunkEntityFields.ROOT_THREAD_EVENT_ID, String::class.java, FieldAttribute.INDEXED)
                ?.addField(ChunkEntityFields.IS_LAST_FORWARD_THREAD, Boolean::class.java, FieldAttribute.INDEXED)

        realm.schema.get("TimelineEventEntity")
                ?.addField(TimelineEventEntityFields.OWNED_BY_THREAD_CHUNK, Boolean::class.java)

        val eventEntity = realm.schema.get("EventEntity") ?: return
        val threadSummaryEntity = realm.schema.create("ThreadSummaryEntity")
                .addField(ThreadSummaryEntityFields.ROOT_THREAD_EVENT_ID, String::class.java, FieldAttribute.INDEXED)
                .addField(ThreadSummaryEntityFields.ROOT_THREAD_SENDER_NAME, String::class.java)
                .addField(ThreadSummaryEntityFields.ROOT_THREAD_SENDER_AVATAR, String::class.java)
                .addField(ThreadSummaryEntityFields.ROOT_THREAD_IS_UNIQUE_DISPLAY_NAME, Boolean::class.java)
                .addField(ThreadSummaryEntityFields.LATEST_THREAD_SENDER_NAME, String::class.java)
                .addField(ThreadSummaryEntityFields.LATEST_THREAD_SENDER_AVATAR, String::class.java)
                .addField(ThreadSummaryEntityFields.LATEST_THREAD_IS_UNIQUE_DISPLAY_NAME, Boolean::class.java)
                .addField(ThreadSummaryEntityFields.NUMBER_OF_THREADS, Int::class.java)
                .addField(ThreadSummaryEntityFields.IS_USER_PARTICIPATING, Boolean::class.java)
                .addRealmObjectField(ThreadSummaryEntityFields.ROOT_THREAD_EVENT_ENTITY.`$`, eventEntity)
                .addRealmObjectField(ThreadSummaryEntityFields.LATEST_THREAD_EVENT_ENTITY.`$`, eventEntity)

        realm.schema.get("RoomEntity")
                ?.addRealmListField(RoomEntityFields.THREAD_SUMMARIES.`$`, threadSummaryEntity)

        realm.schema.get("HomeServerCapabilitiesEntity")
                ?.addField(HomeServerCapabilitiesEntityFields.CAN_USE_THREADING, Boolean::class.java)
                ?.forceRefreshOfHomeServerCapabilities()
    }
}
