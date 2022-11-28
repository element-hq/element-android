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
import org.matrix.android.sdk.internal.database.model.SyncFilterParamsEntityFields
import org.matrix.android.sdk.internal.util.database.RealmMigrator

internal class MigrateSessionTo045(realm: DynamicRealm) : RealmMigrator(realm, 45) {

    override fun doMigrate(realm: DynamicRealm) {
        realm.schema.create("SyncFilterParamsEntity")
                .addField(SyncFilterParamsEntityFields.LAZY_LOAD_MEMBERS_FOR_STATE_EVENTS, Boolean::class.java)
                .setNullable(SyncFilterParamsEntityFields.LAZY_LOAD_MEMBERS_FOR_STATE_EVENTS, true)
                .addField(SyncFilterParamsEntityFields.LAZY_LOAD_MEMBERS_FOR_MESSAGE_EVENTS, Boolean::class.java)
                .setNullable(SyncFilterParamsEntityFields.LAZY_LOAD_MEMBERS_FOR_MESSAGE_EVENTS, true)
                .addField(SyncFilterParamsEntityFields.LIST_OF_SUPPORTED_EVENT_TYPES_HAS_BEEN_SET, Boolean::class.java)
                .addField(SyncFilterParamsEntityFields.LIST_OF_SUPPORTED_STATE_EVENT_TYPES_HAS_BEEN_SET, Boolean::class.java)
                .addField(SyncFilterParamsEntityFields.USE_THREAD_NOTIFICATIONS, Boolean::class.java)
                .setNullable(SyncFilterParamsEntityFields.USE_THREAD_NOTIFICATIONS, true)
                .addRealmListField(SyncFilterParamsEntityFields.LIST_OF_SUPPORTED_EVENT_TYPES.`$`, String::class.java)
                .addRealmListField(SyncFilterParamsEntityFields.LIST_OF_SUPPORTED_STATE_EVENT_TYPES.`$`, String::class.java)
    }
}
