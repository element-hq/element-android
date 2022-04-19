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
import org.matrix.android.sdk.api.session.room.model.create.RoomCreateContent
import org.matrix.android.sdk.internal.database.model.CurrentStateEventEntityFields
import org.matrix.android.sdk.internal.database.model.EventEntityFields
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntityFields
import org.matrix.android.sdk.internal.database.model.SpaceChildSummaryEntityFields
import org.matrix.android.sdk.internal.database.model.SpaceParentSummaryEntityFields
import org.matrix.android.sdk.internal.di.MoshiProvider
import org.matrix.android.sdk.internal.util.database.RealmMigrator

internal class MigrateSessionTo010(realm: DynamicRealm) : RealmMigrator(realm, 10) {

    override fun doMigrate(realm: DynamicRealm) {
        realm.schema.create("SpaceChildSummaryEntity")
                ?.addField(SpaceChildSummaryEntityFields.ORDER, String::class.java)
                ?.addField(SpaceChildSummaryEntityFields.CHILD_ROOM_ID, String::class.java)
                ?.addField(SpaceChildSummaryEntityFields.AUTO_JOIN, Boolean::class.java)
                ?.setNullable(SpaceChildSummaryEntityFields.AUTO_JOIN, true)
                ?.addRealmObjectField(SpaceChildSummaryEntityFields.CHILD_SUMMARY_ENTITY.`$`, realm.schema.get("RoomSummaryEntity")!!)
                ?.addRealmListField(SpaceChildSummaryEntityFields.VIA_SERVERS.`$`, String::class.java)

        realm.schema.create("SpaceParentSummaryEntity")
                ?.addField(SpaceParentSummaryEntityFields.PARENT_ROOM_ID, String::class.java)
                ?.addField(SpaceParentSummaryEntityFields.CANONICAL, Boolean::class.java)
                ?.setNullable(SpaceParentSummaryEntityFields.CANONICAL, true)
                ?.addRealmObjectField(SpaceParentSummaryEntityFields.PARENT_SUMMARY_ENTITY.`$`, realm.schema.get("RoomSummaryEntity")!!)
                ?.addRealmListField(SpaceParentSummaryEntityFields.VIA_SERVERS.`$`, String::class.java)

        val creationContentAdapter = MoshiProvider.providesMoshi().adapter(RoomCreateContent::class.java)
        realm.schema.get("RoomSummaryEntity")
                ?.addField(RoomSummaryEntityFields.ROOM_TYPE, String::class.java)
                ?.addField(RoomSummaryEntityFields.FLATTEN_PARENT_IDS, String::class.java)
                ?.addField(RoomSummaryEntityFields.GROUP_IDS, String::class.java)
                ?.transform { obj ->

                    val creationEvent = realm.where("CurrentStateEventEntity")
                            .equalTo(CurrentStateEventEntityFields.ROOM_ID, obj.getString(RoomSummaryEntityFields.ROOM_ID))
                            .equalTo(CurrentStateEventEntityFields.TYPE, EventType.STATE_ROOM_CREATE)
                            .findFirst()

                    val roomType = creationEvent?.getObject(CurrentStateEventEntityFields.ROOT.`$`)
                            ?.getString(EventEntityFields.CONTENT)?.let {
                                creationContentAdapter.fromJson(it)?.type
                            }

                    obj.setString(RoomSummaryEntityFields.ROOM_TYPE, roomType)
                }
                ?.addRealmListField(RoomSummaryEntityFields.PARENTS.`$`, realm.schema.get("SpaceParentSummaryEntity")!!)
                ?.addRealmListField(RoomSummaryEntityFields.CHILDREN.`$`, realm.schema.get("SpaceChildSummaryEntity")!!)
    }
}
