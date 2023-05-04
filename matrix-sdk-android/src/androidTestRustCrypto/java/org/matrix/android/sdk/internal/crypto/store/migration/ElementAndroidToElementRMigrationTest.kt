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

package org.matrix.android.sdk.internal.crypto.store.migration

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.realm.Realm
import io.realm.kotlin.where
import org.amshove.kluent.internal.assertEquals
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.matrix.android.sdk.InstrumentedTest
import org.matrix.android.sdk.internal.crypto.store.db.RealmCryptoStoreMigration
import org.matrix.android.sdk.internal.crypto.store.db.RealmCryptoStoreModule
import org.matrix.android.sdk.internal.crypto.store.db.model.CryptoMetadataEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.OlmInboundGroupSessionEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.OlmInboundGroupSessionEntityFields
import org.matrix.android.sdk.internal.database.TestRealmConfigurationFactory
import org.matrix.android.sdk.internal.session.MigrateEAtoEROperation
import org.matrix.android.sdk.internal.util.time.Clock
import org.matrix.olm.OlmAccount
import org.matrix.olm.OlmManager
import org.matrix.rustcomponents.sdk.crypto.OlmMachine
import java.io.File

@RunWith(AndroidJUnit4::class)
class ElementAndroidToElementRMigrationTest : InstrumentedTest {

    @get:Rule val configurationFactory = TestRealmConfigurationFactory()

    lateinit var context: Context
    var realm: Realm? = null

    @Before
    fun setUp() {
        // Ensure Olm is initialized
        OlmManager()
        context = InstrumentationRegistry.getInstrumentation().context
    }

    @After
    fun tearDown() {
        realm?.close()
    }

    @Test
    fun given_a_valid_crypto_store_realm_file_then_migration_should_be_successful() {
        testMigrate(false)
    }

    @Test
    fun given_a_valid_crypto_store_realm_file_no_lazy_then_migration_should_be_successful() {
        testMigrate(true)
    }

    private fun testMigrate(migrateGroupSessions: Boolean) {
        val realmName = "crypto_store_migration_16.realm"
        val migration = RealmCryptoStoreMigration(object : Clock {
            override fun epochMillis() = 0L
        })

        val realmConfiguration = configurationFactory.createConfiguration(
                realmName,
                null,
                RealmCryptoStoreModule(),
                migration.schemaVersion,
                migration
        )
        configurationFactory.copyRealmFromAssets(context, realmName, realmName)

        realm = Realm.getInstance(realmConfiguration)

        val metaData = realm!!.where<CryptoMetadataEntity>().findFirst()!!
        val userId = metaData.userId!!
        val deviceId = metaData.deviceId!!
        val olmAccount = metaData.getOlmAccount()!!

        val extractor = MigrateEAtoEROperation(migrateGroupSessions)

        val targetFile = File(configurationFactory.root, "rust-sdk")

        extractor.execute(realmConfiguration, targetFile, null)

        val machine = OlmMachine(userId, deviceId, targetFile.path, null)

        assertEquals(olmAccount.identityKeys()[OlmAccount.JSON_KEY_FINGER_PRINT_KEY], machine.identityKeys()["ed25519"])
        assertNotNull(machine.getBackupKeys())
        val crossSigningStatus = machine.crossSigningStatus()
        assertTrue(crossSigningStatus.hasMaster)
        assertTrue(crossSigningStatus.hasSelfSigning)
        assertTrue(crossSigningStatus.hasUserSigning)

        if (migrateGroupSessions) {
            val inboundGroupSessionEntities = realm!!.where<OlmInboundGroupSessionEntity>().findAll()
            assertEquals(inboundGroupSessionEntities.size, machine.roomKeyCounts().total.toInt())

            val backedUpInboundGroupSessionEntities = realm!!
                    .where<OlmInboundGroupSessionEntity>()
                    .equalTo(OlmInboundGroupSessionEntityFields.BACKED_UP, true)
                    .findAll()
            assertEquals(backedUpInboundGroupSessionEntities.size, machine.roomKeyCounts().backedUp.toInt())
        }
    }

//    @Test
//    fun given_an_empty_crypto_store_realm_file_then_migration_should_not_happen() {
//        val realmConfiguration = realmConfigurationFactory.configurationForMigrationFrom15To16(populateCryptoStore = false)
//        Realm.getInstance(realmConfiguration).use {
//            assertTrue(it.isEmpty)
//        }
//        val machine = OlmMachine("@ganfra146:matrix.org", "UTDQCHKKNS", realmConfigurationFactory.root.path, null)
//        assertNull(machine.getBackupKeys())
//        val crossSigningStatus = machine.crossSigningStatus()
//        assertFalse(crossSigningStatus.hasMaster)
//        assertFalse(crossSigningStatus.hasSelfSigning)
//        assertFalse(crossSigningStatus.hasUserSigning)
//    }
}
