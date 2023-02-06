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
import org.matrix.android.sdk.api.session.room.model.LocalRoomCreationState
import org.matrix.android.sdk.internal.database.model.LocalRoomSummaryEntityFields
import org.matrix.android.sdk.internal.util.database.RealmMigrator

internal class MigrateSessionTo037(realm: DynamicRealm) : RealmMigrator(realm, 37) {

    override fun doMigrate(realm: DynamicRealm) {
        realm.schema.get("LocalRoomSummaryEntity")
                ?.addField(LocalRoomSummaryEntityFields.REPLACEMENT_ROOM_ID, String::class.java)
                ?.addField(LocalRoomSummaryEntityFields.STATE_STR, String::class.java)
                ?.transform { obj ->
                    obj.set(LocalRoomSummaryEntityFields.STATE_STR, LocalRoomCreationState.NOT_CREATED.name)
                }
    }
}
