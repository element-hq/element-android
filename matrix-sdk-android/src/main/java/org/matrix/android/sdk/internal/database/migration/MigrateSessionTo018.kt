/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
