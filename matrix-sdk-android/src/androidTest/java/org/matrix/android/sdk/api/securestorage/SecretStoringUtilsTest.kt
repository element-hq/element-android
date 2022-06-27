/*
 * Copyright (c) 2021 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.api.securestorage

import android.os.Build
import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.spyk
import org.amshove.kluent.invoking
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldNotThrow
import org.amshove.kluent.shouldThrow
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.matrix.android.sdk.TestBuildVersionSdkIntProvider
import java.io.ByteArrayOutputStream
import java.security.KeyStore
import java.security.KeyStoreException
import java.util.UUID

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.JVM)
class SecretStoringUtilsTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val buildVersionSdkIntProvider = TestBuildVersionSdkIntProvider()
    private val keyStore = spyk(KeyStore.getInstance("AndroidKeyStore")).also { it.load(null) }
    private val secretStoringUtils = SecretStoringUtils(context, keyStore, buildVersionSdkIntProvider)

    companion object {
        const val TEST_STR = "This is something I want to store safely!"
    }

    @Before
    fun setup() {
        clearAllMocks()
    }

    @Test
    fun testStringNominalCaseApi21() {
        val alias = generateAlias()
        buildVersionSdkIntProvider.value = Build.VERSION_CODES.LOLLIPOP
        // Encrypt
        val encrypted = secretStoringUtils.securelyStoreBytes(TEST_STR.toByteArray(), alias)
        // Decrypt
        val decrypted = String(secretStoringUtils.loadSecureSecretBytes(encrypted, alias))
        decrypted shouldBeEqualTo TEST_STR
        secretStoringUtils.safeDeleteKey(alias)
    }

    @Test
    fun testStringNominalCaseApi23() {
        val alias = generateAlias()
        buildVersionSdkIntProvider.value = Build.VERSION_CODES.M
        // Encrypt
        val encrypted = secretStoringUtils.securelyStoreBytes(TEST_STR.toByteArray(), alias)
        // Decrypt
        val decrypted = String(secretStoringUtils.loadSecureSecretBytes(encrypted, alias))
        decrypted shouldBeEqualTo TEST_STR
        secretStoringUtils.safeDeleteKey(alias)
    }

    @Test
    fun testStringNominalCaseApi30() {
        val alias = generateAlias()
        buildVersionSdkIntProvider.value = Build.VERSION_CODES.R
        // Encrypt
        val encrypted = secretStoringUtils.securelyStoreBytes(TEST_STR.toByteArray(), alias)
        // Decrypt
        val decrypted = String(secretStoringUtils.loadSecureSecretBytes(encrypted, alias))
        decrypted shouldBeEqualTo TEST_STR
        secretStoringUtils.safeDeleteKey(alias)
    }

    @Test
    fun testStringMigration21_23() {
        val alias = generateAlias()
        buildVersionSdkIntProvider.value = Build.VERSION_CODES.LOLLIPOP
        // Encrypt
        val encrypted = secretStoringUtils.securelyStoreBytes(TEST_STR.toByteArray(), alias)

        // Simulate a system upgrade
        buildVersionSdkIntProvider.value = Build.VERSION_CODES.M

        // Decrypt
        val decrypted = String(secretStoringUtils.loadSecureSecretBytes(encrypted, alias))
        decrypted shouldBeEqualTo TEST_STR
        secretStoringUtils.safeDeleteKey(alias)
    }

    @Test
    fun testObjectNominalCaseApi21() {
        val alias = generateAlias()
        buildVersionSdkIntProvider.value = Build.VERSION_CODES.LOLLIPOP

        // Encrypt
        val encrypted = ByteArrayOutputStream().also { outputStream ->
            outputStream.use {
                secretStoringUtils.securelyStoreObject(TEST_STR, alias, it)
            }
        }
                .toByteArray()
                .toBase64NoPadding()
        // Decrypt
        val decrypted = encrypted.fromBase64().inputStream().use {
            secretStoringUtils.loadSecureSecret<String>(it, alias)
        }
        decrypted shouldBeEqualTo TEST_STR
        secretStoringUtils.safeDeleteKey(alias)
    }

    @Test
    fun testObjectNominalCaseApi23() {
        val alias = generateAlias()
        buildVersionSdkIntProvider.value = Build.VERSION_CODES.M

        // Encrypt
        val encrypted = ByteArrayOutputStream().also { outputStream ->
            outputStream.use {
                secretStoringUtils.securelyStoreObject(TEST_STR, alias, it)
            }
        }
                .toByteArray()
                .toBase64NoPadding()
        // Decrypt
        val decrypted = encrypted.fromBase64().inputStream().use {
            secretStoringUtils.loadSecureSecret<String>(it, alias)
        }
        decrypted shouldBeEqualTo TEST_STR
        secretStoringUtils.safeDeleteKey(alias)
    }

    @Test
    fun testObjectNominalCaseApi30() {
        val alias = generateAlias()
        buildVersionSdkIntProvider.value = Build.VERSION_CODES.R

        // Encrypt
        val encrypted = ByteArrayOutputStream().also { outputStream ->
            outputStream.use {
                secretStoringUtils.securelyStoreObject(TEST_STR, alias, it)
            }
        }
                .toByteArray()
                .toBase64NoPadding()
        // Decrypt
        val decrypted = encrypted.fromBase64().inputStream().use {
            secretStoringUtils.loadSecureSecret<String>(it, alias)
        }
        decrypted shouldBeEqualTo TEST_STR
        secretStoringUtils.safeDeleteKey(alias)
    }

    @Test
    fun testObjectMigration21_23() {
        val alias = generateAlias()
        buildVersionSdkIntProvider.value = Build.VERSION_CODES.LOLLIPOP

        // Encrypt
        val encrypted = ByteArrayOutputStream().also { outputStream ->
            outputStream.use {
                secretStoringUtils.securelyStoreObject(TEST_STR, alias, it)
            }
        }
                .toByteArray()
                .toBase64NoPadding()

        // Simulate a system upgrade
        buildVersionSdkIntProvider.value = Build.VERSION_CODES.M

        // Decrypt
        val decrypted = encrypted.fromBase64().inputStream().use {
            secretStoringUtils.loadSecureSecret<String>(it, alias)
        }
        decrypted shouldBeEqualTo TEST_STR
        secretStoringUtils.safeDeleteKey(alias)
    }

    @Test
    fun testEnsureKeyReturnsSymmetricKeyOnAndroidM() {
        buildVersionSdkIntProvider.value = Build.VERSION_CODES.M
        val alias = generateAlias()

        val key = secretStoringUtils.ensureKey(alias)
        key shouldBeInstanceOf KeyStore.SecretKeyEntry::class

        secretStoringUtils.safeDeleteKey(alias)
    }

    @Test
    fun testEnsureKeyReturnsPrivateKeyOnAndroidL() {
        buildVersionSdkIntProvider.value = Build.VERSION_CODES.LOLLIPOP
        val alias = generateAlias()

        val key = secretStoringUtils.ensureKey(alias)
        key shouldBeInstanceOf KeyStore.PrivateKeyEntry::class

        secretStoringUtils.safeDeleteKey(alias)
    }

    @Test
    fun testSafeDeleteCanHandleKeyStoreExceptions() {
        every { keyStore.deleteEntry(any()) } throws KeyStoreException()

        invoking { secretStoringUtils.safeDeleteKey(generateAlias()) } shouldNotThrow KeyStoreException::class
    }

    @Test
    fun testLoadSecureSecretBytesWillThrowOnInvalidStreamFormat() {
        invoking {
            secretStoringUtils.loadSecureSecretBytes(byteArrayOf(255.toByte()), generateAlias())
        } shouldThrow IllegalArgumentException::class
    }

    @Test
    fun testLoadSecureSecretWillThrowOnInvalidStreamFormat() {
        invoking {
            secretStoringUtils.loadSecureSecret(byteArrayOf(255.toByte()).inputStream(), generateAlias())
        } shouldThrow IllegalArgumentException::class
    }

    private fun generateAlias() = UUID.randomUUID().toString()
}

private fun ByteArray.toBase64NoPadding(): String {
    return Base64.encodeToString(this, Base64.NO_PADDING or Base64.NO_WRAP)
}

private fun String.fromBase64(): ByteArray {
    return Base64.decode(this, Base64.DEFAULT)
}
