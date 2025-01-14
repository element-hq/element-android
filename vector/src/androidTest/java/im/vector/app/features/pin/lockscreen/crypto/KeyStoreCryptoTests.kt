/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.pin.lockscreen.crypto

import android.os.Build
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.UserNotAuthenticatedException
import androidx.test.platform.app.InstrumentationRegistry
import im.vector.app.TestBuildVersionSdkIntProvider
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import org.amshove.kluent.invoking
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldThrow
import org.junit.After
import org.junit.Test
import org.matrix.android.sdk.api.securestorage.SecretStoringUtils
import java.security.KeyStore

class KeyStoreCryptoTests {

    private val alias = "some_alias"

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").also { it.load(null) }
    private val versionProvider = TestBuildVersionSdkIntProvider().also { it.value = Build.VERSION_CODES.M }
    private val secretStoringUtils = spyk(SecretStoringUtils(context, keyStore, versionProvider))
    private val keyStoreCrypto = spyk(
            KeyStoreCrypto(alias, false, context, versionProvider, keyStore).also {
                it.secretStoringUtils = secretStoringUtils
            }
    )

    @After
    fun setup() {
        keyStore.deleteEntry(alias)
    }

    @Test
    fun ensureKeyChecksValidityOfKeyAndThrows() {
        keyStore.containsAlias(alias) shouldBe false

        val exception = KeyPermanentlyInvalidatedException()
        every { secretStoringUtils.getEncryptCipher(any()) } throws exception

        invoking { keyStoreCrypto.ensureKey() } shouldThrow exception
        keyStoreCrypto.hasValidKey() shouldBe false
    }

    @Test
    fun hasValidKeyChecksValidityOfKey() {
        runCatching { keyStoreCrypto.ensureKey() }
        keyStoreCrypto.hasValidKey() shouldBe true

        val keyInvalidatedException = KeyPermanentlyInvalidatedException()
        every { secretStoringUtils.getEncryptCipher(any()) } throws keyInvalidatedException
        keyStoreCrypto.hasValidKey() shouldBe false

        val userNotAuthenticatedException = UserNotAuthenticatedException()
        every { secretStoringUtils.getEncryptCipher(any()) } throws userNotAuthenticatedException
        keyStoreCrypto.hasValidKey() shouldBe false
    }

    @Test
    fun hasKeyChecksIfKeyExists() {
        keyStoreCrypto.hasKey() shouldBe false

        keyStoreCrypto.ensureKey()
        keyStoreCrypto.hasKey() shouldBe true
        keyStore.containsAlias(keyStoreCrypto.alias)

        keyStoreCrypto.deleteKey()
        keyStoreCrypto.hasKey() shouldBe false
    }

    @Test
    fun deleteKeyRemovesTheKey() {
        keyStore.containsAlias(alias) shouldBe false

        keyStoreCrypto.ensureKey()
        keyStore.containsAlias(alias) shouldBe true

        keyStoreCrypto.deleteKey()
        keyStore.containsAlias(alias) shouldBe false
    }

    @Test
    fun checkEncryptionAndDecryptionOfStringsWorkAsExpected() {
        val original = "some plain text"
        val encryptedString = keyStoreCrypto.encryptToString(original)
        val encryptedBytes = keyStoreCrypto.encrypt(original)
        val result = keyStoreCrypto.decryptToString(encryptedString)
        val resultFromBytes = keyStoreCrypto.decryptToString(encryptedBytes)
        result shouldBeEqualTo original
        resultFromBytes shouldBeEqualTo original
    }

    @Test
    fun checkEncryptionAndDecryptionWorkAsExpected() {
        val original = "some plain text".toByteArray()
        val encryptedBytes = keyStoreCrypto.encrypt(original)
        val encryptedString = keyStoreCrypto.encryptToString(original)
        val result = keyStoreCrypto.decrypt(encryptedBytes)
        val resultFromString = keyStoreCrypto.decrypt(encryptedString)
        result shouldBeEqualTo original
        resultFromString shouldBeEqualTo original
    }

    @Test
    fun hasValidKeyReturnsFalseWhenKeyPermanentlyInvalidatedExceptionIsThrown() {
        every { keyStoreCrypto.hasKey() } returns true
        every { secretStoringUtils.getEncryptCipher(any()) } throws KeyPermanentlyInvalidatedException()

        keyStoreCrypto.hasValidKey().shouldBeFalse()
    }

    @Test
    fun hasValidKeyReturnsFalseWhenKeyDoesNotExist() {
        every { keyStoreCrypto.hasKey() } returns false
        keyStoreCrypto.hasValidKey().shouldBeFalse()
    }

    @Test
    fun hasValidKeyReturnsIfKeyExistsOnAndroidL() {
        versionProvider.value = Build.VERSION_CODES.LOLLIPOP

        every { keyStoreCrypto.hasKey() } returns true
        keyStoreCrypto.hasValidKey().shouldBeTrue()

        every { keyStoreCrypto.hasKey() } returns false
        keyStoreCrypto.hasValidKey().shouldBeFalse()
    }

    @Test
    fun getCryptoObjectUsesCipherFromSecretStoringUtils() {
        keyStoreCrypto.getAuthCryptoObject()
        verify { secretStoringUtils.getEncryptCipher(any()) }

        every { secretStoringUtils.getEncryptCipher(any()) } throws KeyPermanentlyInvalidatedException()
        invoking { keyStoreCrypto.getAuthCryptoObject() } shouldThrow KeyPermanentlyInvalidatedException::class
    }
}
