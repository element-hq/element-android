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
import org.matrix.android.sdk.api.session.room.model.VersioningState
import org.matrix.android.sdk.internal.database.model.RoomAccountDataEntityFields
import org.matrix.android.sdk.internal.database.model.RoomEntityFields
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntityFields
import org.matrix.android.sdk.internal.util.database.RealmMigrator

internal class MigrateSessionTo014(realm: DynamicRealm) : RealmMigrator(realm, 14) {

    override fun doMigrate(realm: DynamicRealm) {
        val roomAccountDataSchema = realm.schema.create("RoomAccountDataEntity")
                .addField(RoomAccountDataEntityFields.CONTENT_STR, String::class.java)
                .addField(RoomAccountDataEntityFields.TYPE, String::class.java, FieldAttribute.INDEXED)

        realm.schema.get("RoomEntity")
                ?.addRealmListField(RoomEntityFields.ACCOUNT_DATA.`$`, roomAccountDataSchema)

        realm.schema.get("RoomSummaryEntity")
                ?.addField(RoomSummaryEntityFields.IS_HIDDEN_FROM_USER, Boolean::class.java, FieldAttribute.INDEXED)
                ?.transform {
                    val isHiddenFromUser = it.getString(RoomSummaryEntityFields.VERSIONING_STATE_STR) == VersioningState.UPGRADED_ROOM_JOINED.name
                    it.setBoolean(RoomSummaryEntityFields.IS_HIDDEN_FROM_USER, isHiddenFromUser)
                }

        roomAccountDataSchema.isEmbedded = true
    }
}
