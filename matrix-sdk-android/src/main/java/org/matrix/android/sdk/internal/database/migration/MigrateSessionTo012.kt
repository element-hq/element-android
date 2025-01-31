/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.database.migration

import io.realm.DynamicRealm
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.room.model.RoomJoinRulesContent
import org.matrix.android.sdk.internal.database.model.CurrentStateEventEntityFields
import org.matrix.android.sdk.internal.database.model.EventEntityFields
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntityFields
import org.matrix.android.sdk.internal.database.model.SpaceChildSummaryEntityFields
import org.matrix.android.sdk.internal.di.MoshiProvider
import org.matrix.android.sdk.internal.util.database.RealmMigrator

internal class MigrateSessionTo012(realm: DynamicRealm) : RealmMigrator(realm, 12) {

    override fun doMigrate(realm: DynamicRealm) {
        val joinRulesContentAdapter = MoshiProvider.providesMoshi().adapter(RoomJoinRulesContent::class.java)
        realm.schema.get("RoomSummaryEntity")
                ?.addField(RoomSummaryEntityFields.JOIN_RULES_STR, String::class.java)
                ?.transform { obj ->
                    val joinRulesEvent = realm.where("CurrentStateEventEntity")
                            .equalTo(CurrentStateEventEntityFields.ROOM_ID, obj.getString(RoomSummaryEntityFields.ROOM_ID))
                            .equalTo(CurrentStateEventEntityFields.TYPE, EventType.STATE_ROOM_JOIN_RULES)
                            .findFirst()

                    val roomJoinRules = joinRulesEvent?.getObject(CurrentStateEventEntityFields.ROOT.`$`)
                            ?.getString(EventEntityFields.CONTENT)?.let {
                                joinRulesContentAdapter.fromJson(it)?.joinRules
                            }

                    obj.setString(RoomSummaryEntityFields.JOIN_RULES_STR, roomJoinRules?.name)
                }

        realm.schema.get("SpaceChildSummaryEntity")
                ?.addField(SpaceChildSummaryEntityFields.SUGGESTED, Boolean::class.java)
                ?.setNullable(SpaceChildSummaryEntityFields.SUGGESTED, true)
    }
}
