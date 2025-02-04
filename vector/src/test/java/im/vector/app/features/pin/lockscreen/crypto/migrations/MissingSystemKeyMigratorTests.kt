/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.pin.lockscreen.crypto.migrations

import android.os.Build
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.UserNotAuthenticatedException
import im.vector.app.features.pin.lockscreen.crypto.KeyStoreCrypto
import im.vector.app.features.settings.VectorPreferences
import im.vector.app.test.TestBuildVersionSdkIntProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.amshove.kluent.invoking
import org.amshove.kluent.shouldNotThrow
import org.junit.Test

class MissingSystemKeyMigratorTests {

    private val keyStoreCryptoFactory = mockk<KeyStoreCrypto.Factory>()
    private val vectorPreferences = mockk<VectorPreferences>(relaxed = true)
    private val versionProvider = TestBuildVersionSdkIntProvider().also { it.value = Build.VERSION_CODES.M }
    private val missingSystemKeyMigrator = MissingSystemKeyMigrator("vector.system", keyStoreCryptoFactory, vectorPreferences, versionProvider)

    @Test
    fun migrateEnsuresSystemKeyExistsIfBiometricAuthIsEnabledAndSupported() {
        val keyStoreCryptoMock = mockk<KeyStoreCrypto> {
            every { ensureKey() } returns mockk()
        }
        every { keyStoreCryptoFactory.provide(any(), any()) } returns keyStoreCryptoMock
        every { vectorPreferences.useBiometricsToUnlock() } returns true

        missingSystemKeyMigrator.migrateIfNeeded()

        verify { keyStoreCryptoMock.ensureKey() }
    }

    @Test
    fun migrateHandlesKeyPermanentlyInvalidatedExceptions() {
        val keyStoreCryptoMock = mockk<KeyStoreCrypto> {
            every { ensureKey() } throws KeyPermanentlyInvalidatedException()
        }
        every { keyStoreCryptoFactory.provide(any(), any()) } returns keyStoreCryptoMock
        every { vectorPreferences.useBiometricsToUnlock() } returns true

        invoking { missingSystemKeyMigrator.migrateIfNeeded() } shouldNotThrow KeyPermanentlyInvalidatedException::class
    }

    @Test
    fun migrateHandlesUserNotAuthenticatedExceptions() {
        val keyStoreCryptoMock = mockk<KeyStoreCrypto> {
            every { ensureKey() } throws UserNotAuthenticatedException()
        }
        every { keyStoreCryptoFactory.provide(any(), any()) } returns keyStoreCryptoMock
        every { vectorPreferences.useBiometricsToUnlock() } returns true

        invoking { missingSystemKeyMigrator.migrateIfNeeded() } shouldNotThrow UserNotAuthenticatedException::class
    }

    @Test
    fun migrateReturnsEarlyIfBiometricAuthIsDisabled() {
        val keyStoreCryptoMock = mockk<KeyStoreCrypto> {
            every { ensureKey() } returns mockk()
        }
        every { keyStoreCryptoFactory.provide(any(), any()) } returns keyStoreCryptoMock
        every { vectorPreferences.useBiometricsToUnlock() } returns false

        missingSystemKeyMigrator.migrateIfNeeded()

        verify(exactly = 0) { keyStoreCryptoMock.ensureKey() }
    }

    @Test
    fun migrateReturnsEarlyIfAndroidVersionCantHandleBiometrics() {
        versionProvider.value = Build.VERSION_CODES.LOLLIPOP
        val keyStoreCryptoMock = mockk<KeyStoreCrypto> {
            every { ensureKey() } returns mockk()
        }
        every { keyStoreCryptoFactory.provide(any(), any()) } returns keyStoreCryptoMock
        every { vectorPreferences.useBiometricsToUnlock() } returns false

        missingSystemKeyMigrator.migrateIfNeeded()

        verify(exactly = 0) { keyStoreCryptoMock.ensureKey() }
    }
}
