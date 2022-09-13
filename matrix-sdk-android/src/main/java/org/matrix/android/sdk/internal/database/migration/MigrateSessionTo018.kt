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
import org.matrix.android.sdk.internal.database.model.RoomMemberSummaryEntityFields
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntityFields
import org.matrix.android.sdk.internal.database.model.presence.UserPresenceEntityFields
import org.matrix.android.sdk.internal.util.database.RealmMigrator

internal class MigrateSessionTo018(realm: DynamicRealm) : RealmMigrator(realm, 18) {

    override fun doMigrate(realm: DynamicRealm) {
        realm.schema.create("UserPresenceEntity")
                ?.addField(UserPresenceEntityFields.USER_ID, String::class.java)
                ?.addPrimaryKey(UserPresenceEntityFields.USER_ID)
                ?.setRequired(UserPresenceEntityFields.USER_ID, true)
                ?.addField(UserPresenceEntityFields.PRESENCE_STR, String::class.java)
                ?.addField(UserPresenceEntityFields.LAST_ACTIVE_AGO, Long::class.java)
                ?.setNullable(UserPresenceEntityFields.LAST_ACTIVE_AGO, true)
                ?.addField(UserPresenceEntityFields.STATUS_MESSAGE, String::class.java)
                ?.addField(UserPresenceEntityFields.IS_CURRENTLY_ACTIVE, Boolean::class.java)
                ?.setNullable(UserPresenceEntityFields.IS_CURRENTLY_ACTIVE, true)
                ?.addField(UserPresenceEntityFields.AVATAR_URL, String::class.java)
                ?.addField(UserPresenceEntityFields.DISPLAY_NAME, String::class.java)

        val userPresenceEntity = realm.schema.get("UserPresenceEntity") ?: return
        realm.schema.get("RoomSummaryEntity")
                ?.addRealmObjectField(RoomSummaryEntityFields.DIRECT_USER_PRESENCE.`$`, userPresenceEntity)

        realm.schema.get("RoomMemberSummaryEntity")
                ?.addRealmObjectField(RoomMemberSummaryEntityFields.USER_PRESENCE_ENTITY.`$`, userPresenceEntity)
    }
}
