/*
 * Copyright (c) 2020 New Vector Ltd
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.database

import io.realm.DynamicRealm
import io.realm.RealmMigration
import org.matrix.android.sdk.internal.database.model.HomeServerCapabilitiesEntityFields
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntityFields
import timber.log.Timber
import javax.inject.Inject

class RealmSessionStoreMigration @Inject constructor() : RealmMigration {

    override fun migrate(realm: DynamicRealm, oldVersion: Long, newVersion: Long) {
        Timber.v("Migrating Realm Session from $oldVersion to $newVersion")

        if (oldVersion <= 0) migrateTo1(realm)
        if (oldVersion <= 1) migrateTo2(realm)
        if (oldVersion <= 2) migrateTo3(realm)
    }

    private fun migrateTo1(realm: DynamicRealm) {
        Timber.d("Step 0 -> 1")
        // Add hasFailedSending in RoomSummary and a small warning icon on room list

        realm.schema.get("RoomSummaryEntity")
                ?.addField(RoomSummaryEntityFields.HAS_FAILED_SENDING, Boolean::class.java)
                ?.transform { obj ->
                    obj.setBoolean(RoomSummaryEntityFields.HAS_FAILED_SENDING, false)
                }
    }

    private fun migrateTo2(realm: DynamicRealm) {
        Timber.d("Step 1 -> 2")
        realm.schema.get("HomeServerCapabilitiesEntity")
                ?.addField(HomeServerCapabilitiesEntityFields.ADMIN_E2_E_BY_DEFAULT, Boolean::class.java)
                ?.transform { obj ->
                    obj.setBoolean(HomeServerCapabilitiesEntityFields.ADMIN_E2_E_BY_DEFAULT, true)
                }
    }

    private fun migrateTo3(realm: DynamicRealm) {
        Timber.d("Step 2 -> 3")
        realm.schema.get("HomeServerCapabilitiesEntity")
                ?.addField(HomeServerCapabilitiesEntityFields.PREFERRED_JITSI_DOMAIN, String::class.java)
                ?.transform { obj ->
                    // Schedule a refresh of the capabilities
                    obj.setLong(HomeServerCapabilitiesEntityFields.LAST_UPDATED_TIMESTAMP, 0)
                }
    }
}
