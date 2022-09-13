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
import org.matrix.android.sdk.api.session.events.model.content.EncryptionEventContent
import org.matrix.android.sdk.internal.database.model.CurrentStateEventEntityFields
import org.matrix.android.sdk.internal.database.model.EventEntityFields
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntityFields
import org.matrix.android.sdk.internal.di.MoshiProvider
import org.matrix.android.sdk.internal.util.database.RealmMigrator

internal class MigrateSessionTo021(realm: DynamicRealm) : RealmMigrator(realm, 21) {

    override fun doMigrate(realm: DynamicRealm) {
        realm.schema.get("RoomSummaryEntity")
                ?.addField(RoomSummaryEntityFields.E2E_ALGORITHM, String::class.java)
                ?.transform { obj ->

                    val encryptionContentAdapter = MoshiProvider.providesMoshi().adapter(EncryptionEventContent::class.java)

                    val encryptionEvent = realm.where("CurrentStateEventEntity")
                            .equalTo(CurrentStateEventEntityFields.ROOM_ID, obj.getString(RoomSummaryEntityFields.ROOM_ID))
                            .equalTo(CurrentStateEventEntityFields.TYPE, EventType.STATE_ROOM_ENCRYPTION)
                            .findFirst()

                    val encryptionEventRoot = encryptionEvent?.getObject(CurrentStateEventEntityFields.ROOT.`$`)
                    val algorithm = encryptionEventRoot
                            ?.getString(EventEntityFields.CONTENT)?.let {
                                encryptionContentAdapter.fromJson(it)?.algorithm
                            }

                    obj.setString(RoomSummaryEntityFields.E2E_ALGORITHM, algorithm)
                    obj.setBoolean(RoomSummaryEntityFields.IS_ENCRYPTED, encryptionEvent != null)
                    encryptionEventRoot?.getLong(EventEntityFields.ORIGIN_SERVER_TS)?.let {
                        obj.setLong(RoomSummaryEntityFields.ENCRYPTION_EVENT_TS, it)
                    }
                }
    }
}
