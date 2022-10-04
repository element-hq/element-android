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

package org.matrix.android.sdk.internal.crypto.store.db.migration

import io.realm.DynamicRealm
import org.matrix.android.sdk.internal.util.database.RealmMigrator
import timber.log.Timber

// Version 13L delete unreferenced TrustLevelEntity
internal class MigrateCryptoTo013(realm: DynamicRealm) : RealmMigrator(realm, 13) {

    override fun doMigrate(realm: DynamicRealm) {
        // Use a trick to do that... Ref: https://stackoverflow.com/questions/55221366
        val trustLevelEntitySchema = realm.schema.get("TrustLevelEntity")

        /*
        Creating a new temp field called isLinked which is set to true for those which are
        references by other objects. Rest of them are set to false. Then removing all
        those which are false and hence duplicate and unnecessary. Then removing the temp field
        isLinked
         */
        var mainCounter = 0
        var deviceInfoCounter = 0
        var keyInfoCounter = 0
        val deleteCounter: Int

        trustLevelEntitySchema
                ?.addField("isLinked", Boolean::class.java)
                ?.transform { obj ->
                    // Setting to false for all by default
                    obj.set("isLinked", false)
                    mainCounter++
                }

        realm.schema.get("DeviceInfoEntity")?.transform { obj ->
            // Setting to true for those which are referenced in DeviceInfoEntity
            deviceInfoCounter++
            obj.getObject("trustLevelEntity")?.set("isLinked", true)
        }

        realm.schema.get("KeyInfoEntity")?.transform { obj ->
            // Setting to true for those which are referenced in KeyInfoEntity
            keyInfoCounter++
            obj.getObject("trustLevelEntity")?.set("isLinked", true)
        }

        // Removing all those which are set as false
        realm.where("TrustLevelEntity")
                .equalTo("isLinked", false)
                .findAll()
                .also { deleteCounter = it.size }
                .deleteAllFromRealm()

        trustLevelEntitySchema?.removeField("isLinked")

        Timber.w("TrustLevelEntity cleanup: $mainCounter entities")
        Timber.w("TrustLevelEntity cleanup: $deviceInfoCounter entities referenced in DeviceInfoEntities")
        Timber.w("TrustLevelEntity cleanup: $keyInfoCounter entities referenced in KeyInfoEntity")
        Timber.w("TrustLevelEntity cleanup: $deleteCounter entities deleted!")
        if (mainCounter != deviceInfoCounter + keyInfoCounter + deleteCounter) {
            Timber.e("TrustLevelEntity cleanup: Something is not correct...")
        }
    }
}
