/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

@file:Suppress("DEPRECATION")

package im.vector.app.features.pin.lockscreen.crypto.migrations

import android.os.Build
import android.security.KeyPairGeneratorSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.preference.PreferenceManager
import androidx.test.platform.app.InstrumentationRegistry
import im.vector.app.TestBuildVersionSdkIntProvider
import im.vector.app.features.pin.PinCodeStore
import im.vector.app.features.pin.SharedPrefPinCodeStore
import im.vector.app.features.pin.lockscreen.crypto.LockScreenCryptoConstants.ANDROID_KEY_STORE
import im.vector.app.features.pin.lockscreen.crypto.LockScreenCryptoConstants.LEGACY_PIN_CODE_KEY_ALIAS
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.junit.After
import org.junit.Test
import org.matrix.android.sdk.api.securestorage.SecretStoringUtils
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.spec.MGF1ParameterSpec
import java.security.spec.X509EncodedKeySpec
import java.util.Calendar
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource
import javax.security.auth.x500.X500Principal
import kotlin.math.abs

class LegacyPinCodeMigratorTests {

    private val alias = UUID.randomUUID().toString()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val pinCodeStore: PinCodeStore = spyk(
            SharedPrefPinCodeStore(PreferenceManager.getDefaultSharedPreferences(InstrumentationRegistry.getInstrumentation().context))
    )
    private val keyStore: KeyStore = spyk(KeyStore.getInstance(ANDROID_KEY_STORE)).also { it.load(null) }
    private val buildVersionSdkIntProvider = TestBuildVersionSdkIntProvider(Build.VERSION_CODES.M)
    private val secretStoringUtils: SecretStoringUtils = spyk(
            SecretStoringUtils(context, keyStore, buildVersionSdkIntProvider)
    )
    private val legacyPinCodeMigrator = spyk(
            LegacyPinCodeMigrator(alias, pinCodeStore, keyStore, secretStoringUtils, buildVersionSdkIntProvider)
    )

    @After
    fun tearDown() {
        if (keyStore.containsAlias(LEGACY_PIN_CODE_KEY_ALIAS)) {
            keyStore.deleteEntry(LEGACY_PIN_CODE_KEY_ALIAS)
        }
        if (keyStore.containsAlias(alias)) {
            keyStore.deleteEntry(alias)
        }
        runBlocking { pinCodeStore.deletePinCode() }
    }

    @Test
    fun isMigrationNeededReturnsTrueIfLegacyKeyExists() {
        legacyPinCodeMigrator.isMigrationNeeded() shouldBe false

        generateLegacyKey()

        legacyPinCodeMigrator.isMigrationNeeded() shouldBe true
    }

    @Test
    fun migrateWillReturnEarlyIfPinCodeDoesNotExist() = runTest {
        every { legacyPinCodeMigrator.isMigrationNeeded() } returns false
        coEvery { pinCodeStore.getPinCode() } returns null

        legacyPinCodeMigrator.migrate()

        coVerify(exactly = 0) { legacyPinCodeMigrator.getDecryptedPinCode() }
        verify(exactly = 0) { secretStoringUtils.securelyStoreBytes(any(), any()) }
        coVerify(exactly = 0) { pinCodeStore.savePinCode(any()) }
        verify(exactly = 0) { keyStore.deleteEntry(LEGACY_PIN_CODE_KEY_ALIAS) }
    }

    @Test
    fun migrateWillReturnEarlyIfIsNotNeeded() = runTest {
        every { legacyPinCodeMigrator.isMigrationNeeded() } returns false
        coEvery { legacyPinCodeMigrator.getDecryptedPinCode() } returns "1234"
        every { secretStoringUtils.securelyStoreBytes(any(), any()) } returns ByteArray(0)

        legacyPinCodeMigrator.migrate()

        coVerify(exactly = 0) { legacyPinCodeMigrator.getDecryptedPinCode() }
        verify(exactly = 0) { secretStoringUtils.securelyStoreBytes(any(), any()) }
        coVerify(exactly = 0) { pinCodeStore.savePinCode(any()) }
        verify(exactly = 0) { keyStore.deleteEntry(LEGACY_PIN_CODE_KEY_ALIAS) }
    }

    @Test
    fun migratePinCodeM() = runTest {
        buildVersionSdkIntProvider.value = Build.VERSION_CODES.M
        migratePinCode()
    }

    @Test
    fun migratePinCodeL() = runTest {
        buildVersionSdkIntProvider.value = Build.VERSION_CODES.LOLLIPOP
        migratePinCode()
    }

    private suspend fun migratePinCode() {
        val pinCode = "1234"
        saveLegacyPinCode(pinCode)

        legacyPinCodeMigrator.migrate()

        coVerify { legacyPinCodeMigrator.getDecryptedPinCode() }
        verify { secretStoringUtils.securelyStoreBytes(any(), any()) }
        coVerify { pinCodeStore.savePinCode(any()) }
        verify { keyStore.deleteEntry(LEGACY_PIN_CODE_KEY_ALIAS) }

        val decodedPinCode = String(secretStoringUtils.loadSecureSecretBytes(Base64.decode(pinCodeStore.getPinCode().orEmpty(), Base64.NO_WRAP), alias))
        decodedPinCode shouldBeEqualTo pinCode
        keyStore.containsAlias(LEGACY_PIN_CODE_KEY_ALIAS) shouldBe false
        keyStore.containsAlias(alias) shouldBe true
    }

    private fun generateLegacyKey() {
        if (keyStore.containsAlias(LEGACY_PIN_CODE_KEY_ALIAS)) return

        if (buildVersionSdkIntProvider.isAtLeast(Build.VERSION_CODES.M)) {
            generateLegacyKeyM()
        } else {
            generateLegacyKeyL()
        }
    }

    private fun generateLegacyKeyL() {
        val start = Calendar.getInstance()
        val end = Calendar.getInstance().also { it.add(Calendar.YEAR, 25) }

        val keyGen = KeyPairGenerator
                .getInstance(KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEY_STORE)

        val spec = KeyPairGeneratorSpec.Builder(context)
                .setAlias(LEGACY_PIN_CODE_KEY_ALIAS)
                .setSubject(X500Principal("CN=$LEGACY_PIN_CODE_KEY_ALIAS"))
                .setSerialNumber(BigInteger.valueOf(abs(LEGACY_PIN_CODE_KEY_ALIAS.hashCode()).toLong()))
                .setEndDate(end.time)
                .setStartDate(start.time)
                .setSerialNumber(BigInteger.ONE)
                .setSubject(X500Principal("CN = Secured Preference Store, O = Devliving Online"))
                .build()

        keyGen.initialize(spec)
        keyGen.generateKeyPair()
    }

    private fun generateLegacyKeyM() {
        val keyGenerator: KeyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEY_STORE)
        keyGenerator.initialize(
                KeyGenParameterSpec.Builder(LEGACY_PIN_CODE_KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                        .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
                        .build()
        )
        keyGenerator.generateKeyPair()
    }

    private suspend fun saveLegacyPinCode(value: String) {
        generateLegacyKey()
        val publicKey = keyStore.getCertificate(LEGACY_PIN_CODE_KEY_ALIAS).publicKey
        val cipher = getLegacyCipher()
        if (buildVersionSdkIntProvider.isAtLeast(Build.VERSION_CODES.M)) {
            val unrestrictedKey = KeyFactory.getInstance(publicKey.algorithm).generatePublic(X509EncodedKeySpec(publicKey.encoded))
            val spec = OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA1, PSource.PSpecified.DEFAULT)
            cipher.init(Cipher.ENCRYPT_MODE, unrestrictedKey, spec)
        } else {
            cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        }
        val bytes = cipher.doFinal(value.toByteArray())
        val encryptedPinCode = Base64.encodeToString(bytes, Base64.NO_WRAP)
        pinCodeStore.savePinCode(encryptedPinCode)
    }

    private fun getLegacyCipher(): Cipher {
        return if (buildVersionSdkIntProvider.isAtLeast(Build.VERSION_CODES.M)) {
            getCipherM()
        } else {
            getCipherL()
        }
    }

    private fun getCipherL(): Cipher {
        val provider = if (buildVersionSdkIntProvider.isAtLeast(Build.VERSION_CODES.M)) "AndroidKeyStoreBCWorkaround" else "AndroidOpenSSL"
        val transformation = "RSA/ECB/PKCS1Padding"
        return Cipher.getInstance(transformation, provider)
    }

    private fun getCipherM(): Cipher {
        val transformation = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"
        return Cipher.getInstance(transformation)
    }
}
