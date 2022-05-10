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
import io.realm.Realm
import org.amshove.kluent.internal.assertFails
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.matrix.android.sdk.InstrumentedTest
import org.matrix.android.sdk.common.TemporaryRealmConfigurationFactory
import org.matrix.android.sdk.internal.crypto.store.db.RealmCryptoStoreModule
import org.matrix.android.sdk.internal.crypto.store.migration.fixtures.rustCryptoStoreMigrationConfiguration
import org.matrix.olm.OlmManager

@RunWith(AndroidJUnit4::class)
class ExtractMigrationDataUseCaseTest : InstrumentedTest {

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
    fun given_a_valid_crypto_store_realm_file_then_extraction_should_be_successful() {
        val realmConfiguration = realmConfigurationFactory.rustCryptoStoreMigrationConfiguration(populateCryptoStore = true)
        val migrationData = Realm.getInstance(realmConfiguration).use {
            extractMigrationData(it)
        }
        assertNotNull(migrationData)
    }

    @Test
    fun given_an_empty_crypto_store_realm_file_then_extraction_should_throw() {
        val realmConfiguration = realmConfigurationFactory.rustCryptoStoreMigrationConfiguration(populateCryptoStore = false)
        assertFails {
            Realm.getInstance(realmConfiguration).use {
                extractMigrationData(it)
            }
        }
    }
}
