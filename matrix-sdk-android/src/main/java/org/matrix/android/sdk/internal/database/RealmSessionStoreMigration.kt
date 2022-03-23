/*
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
import org.matrix.android.sdk.internal.database.migration.MigrateSessionTo001
import org.matrix.android.sdk.internal.database.migration.MigrateSessionTo002
import org.matrix.android.sdk.internal.database.migration.MigrateSessionTo003
import org.matrix.android.sdk.internal.database.migration.MigrateSessionTo004
import org.matrix.android.sdk.internal.database.migration.MigrateSessionTo005
import org.matrix.android.sdk.internal.database.migration.MigrateSessionTo006
import org.matrix.android.sdk.internal.database.migration.MigrateSessionTo007
import org.matrix.android.sdk.internal.database.migration.MigrateSessionTo008
import org.matrix.android.sdk.internal.database.migration.MigrateSessionTo009
import org.matrix.android.sdk.internal.database.migration.MigrateSessionTo010
import org.matrix.android.sdk.internal.database.migration.MigrateSessionTo011
import org.matrix.android.sdk.internal.database.migration.MigrateSessionTo012
import org.matrix.android.sdk.internal.database.migration.MigrateSessionTo013
import org.matrix.android.sdk.internal.database.migration.MigrateSessionTo014
import org.matrix.android.sdk.internal.database.migration.MigrateSessionTo015
import org.matrix.android.sdk.internal.database.migration.MigrateSessionTo016
import org.matrix.android.sdk.internal.database.migration.MigrateSessionTo017
import org.matrix.android.sdk.internal.database.migration.MigrateSessionTo018
import org.matrix.android.sdk.internal.database.migration.MigrateSessionTo019
import org.matrix.android.sdk.internal.database.migration.MigrateSessionTo020
import org.matrix.android.sdk.internal.database.migration.MigrateSessionTo021
import org.matrix.android.sdk.internal.database.migration.MigrateSessionTo022
import org.matrix.android.sdk.internal.database.migration.MigrateSessionTo023
import org.matrix.android.sdk.internal.database.migration.MigrateSessionTo024
import org.matrix.android.sdk.internal.database.migration.MigrateSessionTo025
import org.matrix.android.sdk.internal.database.migration.MigrateSessionTo026
import org.matrix.android.sdk.internal.util.Normalizer
import timber.log.Timber
import javax.inject.Inject

internal class RealmSessionStoreMigration @Inject constructor(
        private val normalizer: Normalizer
) : RealmMigration {
    /**
     * Forces all RealmSessionStoreMigration instances to be equal
     * Avoids Realm throwing when multiple instances of the migration are set
     */
    override fun equals(other: Any?) = other is RealmSessionStoreMigration
    override fun hashCode() = 1000

    val schemaVersion = 26L

    override fun migrate(realm: DynamicRealm, oldVersion: Long, newVersion: Long) {
        Timber.d("Migrating Realm Session from $oldVersion to $newVersion")

        if (oldVersion < 1) MigrateSessionTo001(realm).perform()
        if (oldVersion < 2) MigrateSessionTo002(realm).perform()
        if (oldVersion < 3) MigrateSessionTo003(realm).perform()
        if (oldVersion < 4) MigrateSessionTo004(realm).perform()
        if (oldVersion < 5) MigrateSessionTo005(realm).perform()
        if (oldVersion < 6) MigrateSessionTo006(realm).perform()
        if (oldVersion < 7) MigrateSessionTo007(realm).perform()
        if (oldVersion < 8) MigrateSessionTo008(realm).perform()
        if (oldVersion < 9) MigrateSessionTo009(realm).perform()
        if (oldVersion < 10) MigrateSessionTo010(realm).perform()
        if (oldVersion < 11) MigrateSessionTo011(realm).perform()
        if (oldVersion < 12) MigrateSessionTo012(realm).perform()
        if (oldVersion < 13) MigrateSessionTo013(realm).perform()
        if (oldVersion < 14) MigrateSessionTo014(realm).perform()
        if (oldVersion < 15) MigrateSessionTo015(realm).perform()
        if (oldVersion < 16) MigrateSessionTo016(realm).perform()
        if (oldVersion < 17) MigrateSessionTo017(realm).perform()
        if (oldVersion < 18) MigrateSessionTo018(realm).perform()
        if (oldVersion < 19) MigrateSessionTo019(realm, normalizer).perform()
        if (oldVersion < 20) MigrateSessionTo020(realm).perform()
        if (oldVersion < 21) MigrateSessionTo021(realm).perform()
        if (oldVersion < 22) MigrateSessionTo022(realm).perform()
        if (oldVersion < 23) MigrateSessionTo023(realm).perform()
        if (oldVersion < 24) MigrateSessionTo024(realm).perform()
        if (oldVersion < 25) MigrateSessionTo025(realm).perform()
        if (oldVersion < 26) MigrateSessionTo026(realm).perform()
    }
}
