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
