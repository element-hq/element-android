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

package org.matrix.android.sdk.internal.database

import io.mockk.mockk
import io.realm.DynamicRealm
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import org.matrix.android.sdk.internal.auth.db.migration.MigrateAuthTo001
import org.matrix.android.sdk.internal.auth.db.migration.MigrateAuthTo004
import org.matrix.android.sdk.internal.crypto.store.db.migration.MigrateCryptoTo001Legacy
import org.matrix.android.sdk.internal.crypto.store.db.migration.MigrateCryptoTo002Legacy
import org.matrix.android.sdk.internal.crypto.store.db.migration.MigrateCryptoTo003RiotX
import org.matrix.android.sdk.internal.crypto.store.db.migration.MigrateCryptoTo004
import org.matrix.android.sdk.internal.crypto.store.db.migration.MigrateCryptoTo014
import org.matrix.android.sdk.internal.database.migration.MigrateSessionTo001
import org.matrix.android.sdk.internal.database.migration.MigrateSessionTo013
import org.matrix.android.sdk.internal.database.migration.MigrateSessionTo025
import org.matrix.android.sdk.internal.raw.migration.MigrateGlobalTo001
import org.matrix.android.sdk.internal.session.identity.db.migration.MigrateIdentityTo001
import org.matrix.android.sdk.internal.util.database.RealmMigrator
import org.matrix.android.sdk.internal.util.database.exceptions.RealmSchemaVersionException

class RealmMigrationSchemaVersionTest {

    @Test
    fun `Session Migration Schema version number should match class name`() {
        MigrateSessionTo001(realm = mockk()).getSchemaVersion() shouldBeEqualTo 1
        MigrateSessionTo013(realm = mockk()).getSchemaVersion() shouldBeEqualTo 13
        MigrateSessionTo025(realm = mockk()).getSchemaVersion() shouldBeEqualTo 25
    }

    @Test
    fun `Crypto Migration Schema version number should match class name`() {
        MigrateCryptoTo001Legacy(realm = mockk()).getSchemaVersion() shouldBeEqualTo 1
        MigrateCryptoTo002Legacy(realm = mockk()).getSchemaVersion() shouldBeEqualTo 2
        MigrateCryptoTo003RiotX(realm = mockk()).getSchemaVersion() shouldBeEqualTo 3
        MigrateCryptoTo004(realm = mockk()).getSchemaVersion() shouldBeEqualTo 4
        MigrateCryptoTo014(realm = mockk()).getSchemaVersion() shouldBeEqualTo 14
    }

    @Test
    fun `Identity Migration Schema version number should match class name`() {
        MigrateIdentityTo001(realm = mockk()).getSchemaVersion() shouldBeEqualTo 1
    }

    @Test
    fun `Auth Migration Schema version number should match class name`() {
        MigrateAuthTo001(realm = mockk()).getSchemaVersion() shouldBeEqualTo 1
        MigrateAuthTo004(realm = mockk()).getSchemaVersion() shouldBeEqualTo 4
    }

    @Test
    fun `Global Migration Schema version number should match class name`() {
        MigrateGlobalTo001(realm = mockk()).getSchemaVersion() shouldBeEqualTo 1
    }

    class MigrateWrongName001(realm: DynamicRealm) : RealmMigrator(realm) {
        override fun doMigrate(realm: DynamicRealm) {}
    }

    @Test(expected = RealmSchemaVersionException::class)
    fun `Wrong class name should throw exception`() {
        MigrateWrongName001(realm = mockk()).getSchemaVersion() shouldBeEqualTo 1
    }
}
