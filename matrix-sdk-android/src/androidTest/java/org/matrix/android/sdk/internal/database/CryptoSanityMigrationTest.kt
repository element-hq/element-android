/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
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

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import io.realm.Realm
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.matrix.android.sdk.internal.crypto.store.db.RealmCryptoStoreMigration
import org.matrix.android.sdk.internal.crypto.store.db.RealmCryptoStoreModule
import org.matrix.android.sdk.internal.util.time.Clock

class CryptoSanityMigrationTest {
    @get:Rule val configurationFactory = TestRealmConfigurationFactory()

    lateinit var context: Context
    var realm: Realm? = null

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().context
    }

    @After
    fun tearDown() {
        realm?.close()
    }

    @Test
    fun cryptoDatabaseShouldMigrateGracefully() {
        val realmName = "crypto_store_20.realm"
        val migration = RealmCryptoStoreMigration(object : Clock {
            override fun epochMillis(): Long {
                return 0L
            }
        })
        val realmConfiguration = configurationFactory.createConfiguration(
                realmName,
                "7b9a21a8a311e85d75b069a343c23fc952fc3fec5e0c83ecfa13f24b787479c487c3ed587db3dd1f5805d52041fc0ac246516e94b27ffa699ff928622e621aca",
                RealmCryptoStoreModule(),
                migration.schemaVersion,
                migration
        )
        configurationFactory.copyRealmFromAssets(context, realmName, realmName)

        realm = Realm.getInstance(realmConfiguration)
    }
}
