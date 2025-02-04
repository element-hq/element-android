/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.pin.lockscreen.crypto.migrations

import android.security.keystore.UserNotAuthenticatedException
import im.vector.app.features.pin.lockscreen.crypto.KeyStoreCrypto
import im.vector.app.features.settings.VectorPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.amshove.kluent.invoking
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldNotThrow
import org.junit.Test
import java.security.KeyStore

class SystemKeyV1MigratorTests {

    private val keyStoreCryptoFactory = mockk<KeyStoreCrypto.Factory>()
    private val keyStore = mockk<KeyStore>(relaxed = true)
    private val vectorPreferences = mockk<VectorPreferences>(relaxed = true)
    private val systemKeyV1Migrator = SystemKeyV1Migrator("vector.system_new", keyStore, keyStoreCryptoFactory, vectorPreferences)

    @Test
    fun isMigrationNeededReturnsTrueIfV1KeyExists() {
        every { keyStore.containsAlias(SystemKeyV1Migrator.SYSTEM_KEY_ALIAS_V1) } returns true
        systemKeyV1Migrator.isMigrationNeeded() shouldBe true

        every { keyStore.containsAlias(SystemKeyV1Migrator.SYSTEM_KEY_ALIAS_V1) } returns false
        systemKeyV1Migrator.isMigrationNeeded() shouldBe false
    }

    @Test
    fun migrateHandlesUserNotAuthenticatedException() {
        val keyStoreCryptoMock = mockk<KeyStoreCrypto> {
            every { ensureKey() } throws UserNotAuthenticatedException()
        }
        every { keyStoreCryptoFactory.provide("vector.system_new", any()) } returns keyStoreCryptoMock

        invoking { systemKeyV1Migrator.migrate() } shouldNotThrow UserNotAuthenticatedException::class

        verify { keyStore.deleteEntry(SystemKeyV1Migrator.SYSTEM_KEY_ALIAS_V1) }
        verify { keyStoreCryptoMock.ensureKey() }
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
