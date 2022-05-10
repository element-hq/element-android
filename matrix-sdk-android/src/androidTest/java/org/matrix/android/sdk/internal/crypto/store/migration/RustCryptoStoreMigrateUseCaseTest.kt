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

package org.matrix.android.sdk.internal.crypto.store.migration

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.internal.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.matrix.android.sdk.InstrumentedTest
import org.matrix.android.sdk.common.TemporaryRealmConfigurationFactory
import org.matrix.android.sdk.internal.crypto.store.db.RealmCryptoStoreModule
import org.matrix.android.sdk.internal.crypto.store.migration.fixtures.rustCryptoStoreMigrationConfiguration
import org.matrix.olm.OlmManager
import uniffi.olm.OlmMachine
import java.util.concurrent.CountDownLatch

@RunWith(AndroidJUnit4::class)
class RustCryptoStoreMigrateUseCaseTest : InstrumentedTest {

    @Rule
    @JvmField
    val realmConfigurationFactory = TemporaryRealmConfigurationFactory()

    private val extractMigrationData = ExtractMigrationDataUseCase()

    @Before
    fun setup() {
        // Ensure Olm is initialized
        OlmManager()
    }

    @Test
    fun given_a_valid_crypto_store_realm_file_then_migration_should_be_successful() = runBlocking {
        val realmConfiguration = realmConfigurationFactory.rustCryptoStoreMigrationConfiguration(populateCryptoStore = true)
        val cryptoStoreMigrate = RustCryptoStoreMigrateUseCase(realmConfiguration, realmConfigurationFactory.root, extractMigrationData)
        val latch = CountDownLatch(1)
        val progressListener = ProgressListener(latch)
        val result = cryptoStoreMigrate(progressListener)
        latch.await()
        assert(result.isSuccess)

        val machine = OlmMachine("@ganfra146:matrix.org", "UTDQCHKKNS",realmConfigurationFactory.root.path, null)
        assertEquals("mW7LWO4zmhH8Ttuvmzn27vm/USXSKBPgmg7FKQITLiU", machine.identityKeys()["ed25519"])
        assertNotNull(machine.getBackupKeys())
        val crossSigningStatus = machine.crossSigningStatus()
        assertTrue(crossSigningStatus.hasMaster)
        assertTrue(crossSigningStatus.hasSelfSigning)
        assertTrue(crossSigningStatus.hasUserSigning)
    }

    @Test
    fun given_an_empty_crypto_store_realm_file_then_migration_should_fail() = runBlocking {
        val realmConfiguration = realmConfigurationFactory.rustCryptoStoreMigrationConfiguration(populateCryptoStore = false)
        val cryptoStoreMigrate = RustCryptoStoreMigrateUseCase(realmConfiguration, realmConfigurationFactory.root, extractMigrationData)
        val progressListener = ProgressListener()
        val result = cryptoStoreMigrate(progressListener)
        assert(result.isFailure)
    }

    private class ProgressListener(val latch: CountDownLatch? = null) : uniffi.olm.ProgressListener {
        override fun onProgress(progress: Int, total: Int) {
            if (progress == total) {
                latch?.countDown()
            }
        }
    }
}
