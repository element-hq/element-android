/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.features.pin.lockscreen.crypto.migrations

import im.vector.app.features.pin.lockscreen.crypto.KeyStoreCrypto
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.amshove.kluent.shouldBe
import org.junit.Test
import java.security.KeyStore

class SystemKeyV1MigratorTests {

    private val keyStoreCryptoFactory = mockk<KeyStoreCrypto.Factory>()
    private val keyStore = mockk<KeyStore>(relaxed = true)
    private val systemKeyV1Migrator = SystemKeyV1Migrator("vector.system_new", keyStore, keyStoreCryptoFactory)

    @Test
    fun isMigrationNeededReturnsTrueIfV1KeyExists() {
        every { keyStore.containsAlias(SystemKeyV1Migrator.SYSTEM_KEY_ALIAS_V1) } returns true
        systemKeyV1Migrator.isMigrationNeeded() shouldBe true

        every { keyStore.containsAlias(SystemKeyV1Migrator.SYSTEM_KEY_ALIAS_V1) } returns false
        systemKeyV1Migrator.isMigrationNeeded() shouldBe false
    }

    @Test
    fun migrateDeletesOldEntryAndEnsuresNewKey() {
        val keyStoreCryptoMock = mockk<KeyStoreCrypto> {
            every { ensureKey() } returns mockk()
        }
        every { keyStoreCryptoFactory.provide("vector.system_new", any()) } returns keyStoreCryptoMock

        systemKeyV1Migrator.migrate()

        verify { keyStore.deleteEntry(SystemKeyV1Migrator.SYSTEM_KEY_ALIAS_V1) }
        verify { keyStoreCryptoMock.ensureKey() }
    }
}
