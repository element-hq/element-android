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

package org.matrix.android.sdk.internal.crypto.keysbackup

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.matrix.android.sdk.InstrumentedTest
import org.matrix.android.sdk.api.listeners.ProgressListener
import org.matrix.android.sdk.common.assertByteArrayNotEqual
import org.matrix.olm.OlmManager
import org.matrix.olm.OlmPkDecryption

@Ignore("Ignored in order to speed up test run time")
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.JVM)
class KeysBackupPasswordTest : InstrumentedTest {

    @Before
    fun ensureLibLoaded() {
        OlmManager()
    }

    /**
     * Check KeysBackupPassword utilities
     */
    @Test
    fun passwordConverter_ok() {
        val generatePrivateKeyResult = generatePrivateKeyWithPassword(PASSWORD, null)

        assertEquals(32, generatePrivateKeyResult.salt.length)
        assertEquals(500_000, generatePrivateKeyResult.iterations)
        assertEquals(OlmPkDecryption.privateKeyLength(), generatePrivateKeyResult.privateKey.size)

        // Reverse operation
        val retrievedPrivateKey = retrievePrivateKeyWithPassword(PASSWORD,
                generatePrivateKeyResult.salt,
                generatePrivateKeyResult.iterations)

        assertEquals(OlmPkDecryption.privateKeyLength(), retrievedPrivateKey.size)
        assertArrayEquals(generatePrivateKeyResult.privateKey, retrievedPrivateKey)
    }

    /**
     * Check generatePrivateKeyWithPassword progress listener behavior
     */
    @Test
    fun passwordConverter_progress_ok() {
        val progressValues = ArrayList<Int>(101)
        var lastTotal = 0

        generatePrivateKeyWithPassword(PASSWORD, object : ProgressListener {
            override fun onProgress(progress: Int, total: Int) {
                if (!progressValues.contains(progress)) {
                    progressValues.add(progress)
                }

                lastTotal = total
            }
        })

        assertEquals(100, lastTotal)

        // Ensure all values are here
        assertEquals(101, progressValues.size)

        for (i in 0..100) {
            assertTrue(progressValues[i] == i)
        }
    }

    /**
     * Check KeysBackupPassword utilities, with bad password
     */
    @Test
    fun passwordConverter_badPassword_ok() {
        val generatePrivateKeyResult = generatePrivateKeyWithPassword(PASSWORD, null)

        assertEquals(32, generatePrivateKeyResult.salt.length)
        assertEquals(500_000, generatePrivateKeyResult.iterations)
        assertEquals(OlmPkDecryption.privateKeyLength(), generatePrivateKeyResult.privateKey.size)

        // Reverse operation, with bad password
        val retrievedPrivateKey = retrievePrivateKeyWithPassword(BAD_PASSWORD,
                generatePrivateKeyResult.salt,
                generatePrivateKeyResult.iterations)

        assertEquals(OlmPkDecryption.privateKeyLength(), retrievedPrivateKey.size)
        assertByteArrayNotEqual(generatePrivateKeyResult.privateKey, retrievedPrivateKey)
    }

    /**
     * Check KeysBackupPassword utilities, with bad password
     */
    @Test
    fun passwordConverter_badIteration_ok() {
        val generatePrivateKeyResult = generatePrivateKeyWithPassword(PASSWORD, null)

        assertEquals(32, generatePrivateKeyResult.salt.length)
        assertEquals(500_000, generatePrivateKeyResult.iterations)
        assertEquals(OlmPkDecryption.privateKeyLength(), generatePrivateKeyResult.privateKey.size)

        // Reverse operation, with bad iteration
        val retrievedPrivateKey = retrievePrivateKeyWithPassword(PASSWORD,
                generatePrivateKeyResult.salt,
                500_001)

        assertEquals(OlmPkDecryption.privateKeyLength(), retrievedPrivateKey.size)
        assertByteArrayNotEqual(generatePrivateKeyResult.privateKey, retrievedPrivateKey)
    }

    /**
     * Check KeysBackupPassword utilities, with bad salt
     */
    @Test
    fun passwordConverter_badSalt_ok() {
        val generatePrivateKeyResult = generatePrivateKeyWithPassword(PASSWORD, null)

        assertEquals(32, generatePrivateKeyResult.salt.length)
        assertEquals(500_000, generatePrivateKeyResult.iterations)
        assertEquals(OlmPkDecryption.privateKeyLength(), generatePrivateKeyResult.privateKey.size)

        // Reverse operation, with bad iteration
        val retrievedPrivateKey = retrievePrivateKeyWithPassword(PASSWORD,
                BAD_SALT,
                generatePrivateKeyResult.iterations)

        assertEquals(OlmPkDecryption.privateKeyLength(), retrievedPrivateKey.size)
        assertByteArrayNotEqual(generatePrivateKeyResult.privateKey, retrievedPrivateKey)
    }

    /**
     * Check [retrievePrivateKeyWithPassword] with data coming from another platform (RiotWeb).
     */
    @Test
    fun passwordConverter_crossPlatform_ok() {
        val password = "This is a passphrase!"
        val salt = "TO0lxhQ9aYgGfMsclVWPIAublg8h9Nlu"
        val iteration = 500_000

        val retrievedPrivateKey = retrievePrivateKeyWithPassword(password, salt, iteration)

        assertEquals(OlmPkDecryption.privateKeyLength(), retrievedPrivateKey.size)

        // Data from RiotWeb
        val privateKeyBytes = byteArrayOf(
                116.toByte(), 224.toByte(), 229.toByte(), 224.toByte(), 9.toByte(), 3.toByte(), 178.toByte(), 162.toByte(),
                120.toByte(), 23.toByte(), 108.toByte(), 218.toByte(), 22.toByte(), 61.toByte(), 241.toByte(), 200.toByte(),
                235.toByte(), 173.toByte(), 236.toByte(), 100.toByte(), 115.toByte(), 247.toByte(), 33.toByte(), 132.toByte(),
                195.toByte(), 154.toByte(), 64.toByte(), 158.toByte(), 184.toByte(), 148.toByte(), 20.toByte(), 85.toByte())

        assertArrayEquals(privateKeyBytes, retrievedPrivateKey)
    }

    companion object {
        private const val PASSWORD = "password"
        private const val BAD_PASSWORD = "passw0rd"

        private const val BAD_SALT = "AA0lxhQ9aYgGfMsclVWPIAublg8h9Nlu"
    }
}
