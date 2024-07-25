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
import org.matrix.android.sdk.internal.database.migration.MigrateSessionTo027
import org.matrix.android.sdk.internal.database.migration.MigrateSessionTo028
import org.matrix.android.sdk.internal.database.migration.MigrateSessionTo029
import org.matrix.android.sdk.internal.database.migration.MigrateSessionTo030
import org.matrix.android.sdk.internal.database.migration.MigrateSessionTo031
import org.matrix.android.sdk.internal.database.migration.MigrateSessionTo032
import org.matrix.android.sdk.internal.database.migration.MigrateSessionTo033
import org.matrix.android.sdk.internal.database.migration.MigrateSessionTo034
import org.matrix.android.sdk.internal.database.migration.MigrateSessionTo035
import org.matrix.android.sdk.internal.database.migration.MigrateSessionTo036
import org.matrix.android.sdk.internal.database.migration.MigrateSessionTo037
import org.matrix.android.sdk.internal.database.migration.MigrateSessionTo038
import org.matrix.android.sdk.internal.database.migration.MigrateSessionTo039
import org.matrix.android.sdk.internal.database.migration.MigrateSessionTo040
import org.matrix.android.sdk.internal.database.migration.MigrateSessionTo041
import org.matrix.android.sdk.internal.database.migration.MigrateSessionTo042
import org.matrix.android.sdk.internal.database.migration.MigrateSessionTo043
import org.matrix.android.sdk.internal.database.migration.MigrateSessionTo044
import org.matrix.android.sdk.internal.database.migration.MigrateSessionTo045
import org.matrix.android.sdk.internal.database.migration.MigrateSessionTo046
import org.matrix.android.sdk.internal.database.migration.MigrateSessionTo047
import org.matrix.android.sdk.internal.database.migration.MigrateSessionTo048
import org.matrix.android.sdk.internal.database.migration.MigrateSessionTo049
import org.matrix.android.sdk.internal.database.migration.MigrateSessionTo050
import org.matrix.android.sdk.internal.database.migration.MigrateSessionTo051
import org.matrix.android.sdk.internal.database.migration.MigrateSessionTo052
import org.matrix.android.sdk.internal.database.migration.MigrateSessionTo053
import org.matrix.android.sdk.internal.database.migration.MigrateSessionTo054
import org.matrix.android.sdk.internal.database.migration.MigrateSessionTo055
import org.matrix.android.sdk.internal.database.migration.MigrateSessionTo056
import org.matrix.android.sdk.internal.util.Normalizer
import org.matrix.android.sdk.internal.util.database.MatrixRealmMigration
import javax.inject.Inject

internal class RealmSessionStoreMigration @Inject constructor(
        private val normalizer: Normalizer
) : MatrixRealmMigration(
        dbName = "Session",
        schemaVersion = 56L,
) {
    /**
     * Forces all RealmSessionStoreMigration instances to be equal.
     * Avoids Realm throwing when multiple instances of the migration are set.
     */
    override fun equals(other: Any?) = other is RealmSessionStoreMigration
    override fun hashCode() = 1000

    override fun doMigrate(realm: DynamicRealm, oldVersion: Long) {
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
        if (oldVersion < 27) MigrateSessionTo027(realm).perform()
        if (oldVersion < 28) MigrateSessionTo028(realm).perform()
        if (oldVersion < 29) MigrateSessionTo029(realm).perform()
        if (oldVersion < 30) MigrateSessionTo030(realm).perform()
        if (oldVersion < 31) MigrateSessionTo031(realm).perform()
        if (oldVersion < 32) MigrateSessionTo032(realm).perform()
        if (oldVersion < 33) MigrateSessionTo033(realm).perform()
        if (oldVersion < 34) MigrateSessionTo034(realm).perform()
        if (oldVersion < 35) MigrateSessionTo035(realm).perform()
        if (oldVersion < 36) MigrateSessionTo036(realm).perform()
        if (oldVersion < 37) MigrateSessionTo037(realm).perform()
        if (oldVersion < 38) MigrateSessionTo038(realm).perform()
        if (oldVersion < 39) MigrateSessionTo039(realm).perform()
        if (oldVersion < 40) MigrateSessionTo040(realm).perform()
        if (oldVersion < 41) MigrateSessionTo041(realm).perform()
        if (oldVersion < 42) MigrateSessionTo042(realm).perform()
        if (oldVersion < 43) MigrateSessionTo043(realm).perform()
        if (oldVersion < 44) MigrateSessionTo044(realm).perform()
        if (oldVersion < 45) MigrateSessionTo045(realm).perform()
        if (oldVersion < 46) MigrateSessionTo046(realm).perform()
        if (oldVersion < 47) MigrateSessionTo047(realm).perform()
        if (oldVersion < 48) MigrateSessionTo048(realm).perform()
        if (oldVersion < 49) MigrateSessionTo049(realm).perform()
        if (oldVersion < 50) MigrateSessionTo050(realm).perform()
        if (oldVersion < 51) MigrateSessionTo051(realm).perform()
        if (oldVersion < 52) MigrateSessionTo052(realm).perform()
        if (oldVersion < 53) MigrateSessionTo053(realm).perform()
        if (oldVersion < 54) MigrateSessionTo054(realm).perform()
        if (oldVersion < 55) MigrateSessionTo055(realm).perform()
        if (oldVersion < 56) MigrateSessionTo056(realm).perform()
    }
}
