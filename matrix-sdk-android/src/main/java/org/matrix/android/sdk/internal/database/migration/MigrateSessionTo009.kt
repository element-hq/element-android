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
import org.matrix.android.sdk.api.session.room.model.tag.RoomTag
import org.matrix.android.sdk.internal.database.model.EventEntityFields
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntityFields
import org.matrix.android.sdk.internal.database.model.RoomTagEntityFields
import org.matrix.android.sdk.internal.database.model.TimelineEventEntityFields
import org.matrix.android.sdk.internal.util.database.RealmMigrator

internal class MigrateSessionTo009(realm: DynamicRealm) : RealmMigrator(realm, 9) {

    override fun doMigrate(realm: DynamicRealm) {
        realm.schema.get("RoomSummaryEntity")
                ?.addField(RoomSummaryEntityFields.LAST_ACTIVITY_TIME, Long::class.java, FieldAttribute.INDEXED)
                ?.setNullable(RoomSummaryEntityFields.LAST_ACTIVITY_TIME, true)
                ?.addIndex(RoomSummaryEntityFields.MEMBERSHIP_STR)
                ?.addIndex(RoomSummaryEntityFields.IS_DIRECT)
                ?.addIndex(RoomSummaryEntityFields.VERSIONING_STATE_STR)

                ?.addField(RoomSummaryEntityFields.IS_FAVOURITE, Boolean::class.java)
                ?.addIndex(RoomSummaryEntityFields.IS_FAVOURITE)
                ?.addField(RoomSummaryEntityFields.IS_LOW_PRIORITY, Boolean::class.java)
                ?.addIndex(RoomSummaryEntityFields.IS_LOW_PRIORITY)
                ?.addField(RoomSummaryEntityFields.IS_SERVER_NOTICE, Boolean::class.java)
                ?.addIndex(RoomSummaryEntityFields.IS_SERVER_NOTICE)

                ?.transform { obj ->
                    val isFavorite = obj.getList(RoomSummaryEntityFields.TAGS.`$`).any {
                        it.getString(RoomTagEntityFields.TAG_NAME) == RoomTag.ROOM_TAG_FAVOURITE
                    }
                    obj.setBoolean(RoomSummaryEntityFields.IS_FAVOURITE, isFavorite)

                    val isLowPriority = obj.getList(RoomSummaryEntityFields.TAGS.`$`).any {
                        it.getString(RoomTagEntityFields.TAG_NAME) == RoomTag.ROOM_TAG_LOW_PRIORITY
                    }

                    obj.setBoolean(RoomSummaryEntityFields.IS_LOW_PRIORITY, isLowPriority)

//                    XXX migrate last message origin server ts
                    obj.getObject(RoomSummaryEntityFields.LATEST_PREVIEWABLE_EVENT.`$`)
                            ?.getObject(TimelineEventEntityFields.ROOT.`$`)
                            ?.getLong(EventEntityFields.ORIGIN_SERVER_TS)?.let {
                                obj.setLong(RoomSummaryEntityFields.LAST_ACTIVITY_TIME, it)
                            }
                }
    }
}
