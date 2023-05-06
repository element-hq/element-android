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
import io.mockk.spyk
import io.realm.Realm
import io.realm.kotlin.where
import org.amshove.kluent.internal.assertEquals
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.matrix.android.sdk.InstrumentedTest
import org.matrix.android.sdk.TestBuildVersionSdkIntProvider
import org.matrix.android.sdk.api.securestorage.SecretStoringUtils
import org.matrix.android.sdk.internal.crypto.RustEncryptionConfiguration
import org.matrix.android.sdk.internal.crypto.store.db.RealmCryptoStoreMigration
import org.matrix.android.sdk.internal.crypto.store.db.RealmCryptoStoreModule
import org.matrix.android.sdk.internal.crypto.store.db.model.CryptoMetadataEntity
import org.matrix.android.sdk.internal.database.RealmKeysUtils
import org.matrix.android.sdk.internal.database.TestRealmConfigurationFactory
import org.matrix.android.sdk.internal.util.time.Clock
import org.matrix.olm.OlmAccount
import org.matrix.olm.OlmManager
import org.matrix.rustcomponents.sdk.crypto.OlmMachine
import java.io.File
import java.security.KeyStore

@RunWith(AndroidJUnit4::class)
class DynamicElementAndroidToElementRMigrationTest : InstrumentedTest {

    @get:Rule val configurationFactory = TestRealmConfigurationFactory()

    var context: Context = InstrumentationRegistry.getInstrumentation().context
    var realm: Realm? = null

    @Before
    fun setUp() {
        // Ensure Olm is initialized
        OlmManager()
    }

    @After
    fun tearDown() {
        realm?.close()
    }

    private val keyStore = spyk(KeyStore.getInstance("AndroidKeyStore")).also { it.load(null) }

    private val rustEncryptionConfiguration = RustEncryptionConfiguration(
            "foo",
            RealmKeysUtils(
                    context,
                    SecretStoringUtils(context, keyStore, TestBuildVersionSdkIntProvider(), false)
            )
    )

    private val fakeClock = object : Clock {
        override fun epochMillis() = 0L
    }

    @Test
<<<<<<< feature/bma/crypto_rust_default:matrix-sdk-android/src/androidTestRustCrypto/java/org/matrix/android/sdk/internal/crypto/store/migration/ElementAndroidToElementRMigrationTest.kt
    fun given_a_valid_crypto_store_realm_file_then_migration_should_be_successful() {
        testMigrate(false)
    }

    @Test
    @Ignore("We don't migrate group session for now, and it makes test suite unstable")
    fun given_a_valid_crypto_store_realm_file_no_lazy_then_migration_should_be_successful() {
        testMigrate(true)
    }

    private fun testMigrate(migrateGroupSessions: Boolean) {
=======
    fun dynamic_migration_to_rust() {
        val targetFile = File(configurationFactory.root, "rust-sdk")

>>>>>>> create rust db as a realm migration:matrix-sdk-android/src/androidTestRustCrypto/java/org/matrix/android/sdk/internal/crypto/store/migration/DynamicElementAndroidToElementRMigrationTest.kt
        val realmName = "crypto_store_migration_16.realm"
        val migration = RealmCryptoStoreMigration(fakeClock, rustEncryptionConfiguration, targetFile)

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

<<<<<<< feature/bma/crypto_rust_default:matrix-sdk-android/src/androidTestRustCrypto/java/org/matrix/android/sdk/internal/crypto/store/migration/ElementAndroidToElementRMigrationTest.kt
        val extractor = MigrateEAtoEROperation(migrateGroupSessions)

        val targetFile = File(configurationFactory.root, "rust-sdk")

        extractor.execute(realmConfiguration, targetFile, null)

        val machine = OlmMachine(userId, deviceId, targetFile.path, null)
=======
        val machine = OlmMachine(userId, deviceId, targetFile.path, rustEncryptionConfiguration.getDatabasePassphrase())
>>>>>>> create rust db as a realm migration:matrix-sdk-android/src/androidTestRustCrypto/java/org/matrix/android/sdk/internal/crypto/store/migration/DynamicElementAndroidToElementRMigrationTest.kt

        assertEquals(olmAccount.identityKeys()[OlmAccount.JSON_KEY_FINGER_PRINT_KEY], machine.identityKeys()["ed25519"])
        assertNotNull(machine.getBackupKeys())
        val crossSigningStatus = machine.crossSigningStatus()
        assertTrue(crossSigningStatus.hasMaster)
        assertTrue(crossSigningStatus.hasSelfSigning)
        assertTrue(crossSigningStatus.hasUserSigning)

<<<<<<< feature/bma/crypto_rust_default:matrix-sdk-android/src/androidTestRustCrypto/java/org/matrix/android/sdk/internal/crypto/store/migration/ElementAndroidToElementRMigrationTest.kt
        if (migrateGroupSessions) {
            val inboundGroupSessionEntities = realm!!.where<OlmInboundGroupSessionEntity>().findAll()
            assertEquals(inboundGroupSessionEntities.size, machine.roomKeyCounts().total.toInt())

            val backedUpInboundGroupSessionEntities = realm!!
                    .where<OlmInboundGroupSessionEntity>()
                    .equalTo(OlmInboundGroupSessionEntityFields.BACKED_UP, true)
                    .findAll()
            assertEquals(backedUpInboundGroupSessionEntities.size, machine.roomKeyCounts().backedUp.toInt())
        }
=======
        // How to check that olm sessions have been migrated?
        // Can see it from logs
>>>>>>> create rust db as a realm migration:matrix-sdk-android/src/androidTestRustCrypto/java/org/matrix/android/sdk/internal/crypto/store/migration/DynamicElementAndroidToElementRMigrationTest.kt
    }
}
