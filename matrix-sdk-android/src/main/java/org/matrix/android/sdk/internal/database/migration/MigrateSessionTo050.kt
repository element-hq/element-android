/*
 * Copyright (c) 2023 The Matrix.org Foundation C.I.C.
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
import org.matrix.android.sdk.internal.database.model.PollHistoryStatusEntityFields
import org.matrix.android.sdk.internal.util.database.RealmMigrator

/**
 * Adding new entity PollHistoryStatusEntity.
 */
internal class MigrateSessionTo050(realm: DynamicRealm) : RealmMigrator(realm, 50) {

    override fun doMigrate(realm: DynamicRealm) {
        realm.schema.create("PollHistoryStatusEntity")
                .addField(PollHistoryStatusEntityFields.ROOM_ID, String::class.java)
                .addPrimaryKey(PollHistoryStatusEntityFields.ROOM_ID)
                .setRequired(PollHistoryStatusEntityFields.ROOM_ID, true)
                .addField(PollHistoryStatusEntityFields.CURRENT_TIMESTAMP_TARGET_BACKWARD_MS, Long::class.java)
                .setNullable(PollHistoryStatusEntityFields.CURRENT_TIMESTAMP_TARGET_BACKWARD_MS, true)
                .addField(PollHistoryStatusEntityFields.OLDEST_TIMESTAMP_TARGET_REACHED_MS, Long::class.java)
                .setNullable(PollHistoryStatusEntityFields.OLDEST_TIMESTAMP_TARGET_REACHED_MS, true)
                .addField(PollHistoryStatusEntityFields.OLDEST_EVENT_ID_REACHED, String::class.java)
                .addField(PollHistoryStatusEntityFields.MOST_RECENT_EVENT_ID_REACHED, String::class.java)
                .addField(PollHistoryStatusEntityFields.IS_END_OF_POLLS_BACKWARD, Boolean::class.java)
    }
}
