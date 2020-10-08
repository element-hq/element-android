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

package org.matrix.android.sdk.internal.crypto

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

/**
 * Unit tests ExportEncryptionTest.
 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class ExportEncryptionTest {

    @Test
    fun checkExportError1() {
        val password = "password"
        val input = "-----"
        var failed = false

        try {
            MXMegolmExportEncryption.decryptMegolmKeyFile(input.toByteArray(charset("UTF-8")), password)
        } catch (e: Exception) {
            failed = true
        }

        assertTrue(failed)
    }

    @Test
    fun checkExportError2() {
        val password = "password"
        val input = "-----BEGIN MEGOLM SESSION DATA-----\n" + "-----"
        var failed = false

        try {
            MXMegolmExportEncryption.decryptMegolmKeyFile(input.toByteArray(charset("UTF-8")), password)
        } catch (e: Exception) {
            failed = true
        }

        assertTrue(failed)
    }

    @Test
    fun checkExportError3() {
        val password = "password"
        val input = "-----BEGIN MEGOLM SESSION DATA-----\n" +
                " AXNhbHRzYWx0c2FsdHNhbHSIiIiIiIiIiIiIiIiIiIiIAAAACmIRUW2OjZ3L2l6j9h0lHlV3M2dx\n" +
                " cissyYBxjsfsAn\n" +
                " -----END MEGOLM SESSION DATA-----"
        var failed = false

        try {
            MXMegolmExportEncryption.decryptMegolmKeyFile(input.toByteArray(charset("UTF-8")), password)
        } catch (e: Exception) {
            failed = true
        }

        assertTrue(failed)
    }

    @Test
    fun checkExportDecrypt1() {
        val password = "password"
        val input = "-----BEGIN MEGOLM SESSION DATA-----\nAXNhbHRzYWx0c2FsdHNhbHSIiIiIiIiIiIiIiIiIiIiIAAAACmIRUW2OjZ3L2l6j9h0lHlV3M2dx\n" + "cissyYBxjsfsAndErh065A8=\n-----END MEGOLM SESSION DATA-----"
        val expectedString = "plain"

        var decodedString: String? = null
        try {
            decodedString = MXMegolmExportEncryption.decryptMegolmKeyFile(input.toByteArray(charset("UTF-8")), password)
        } catch (e: Exception) {
            fail("## checkExportDecrypt1() failed : " + e.message)
        }

        assertEquals("## checkExportDecrypt1() : expectedString $expectedString -- decodedString $decodedString",
                expectedString,
                decodedString)
    }

    @Test
    fun checkExportDecrypt2() {
        val password = "betterpassword"
        val input = "-----BEGIN MEGOLM SESSION DATA-----\nAW1vcmVzYWx0bW9yZXNhbHT//////////wAAAAAAAAAAAAAD6KyBpe1Niv5M5NPm4ZATsJo5nghk\n" + "KYu63a0YQ5DRhUWEKk7CcMkrKnAUiZny\n-----END MEGOLM SESSION DATA-----"
        val expectedString = "Hello, World"

        var decodedString: String? = null
        try {
            decodedString = MXMegolmExportEncryption.decryptMegolmKeyFile(input.toByteArray(charset("UTF-8")), password)
        } catch (e: Exception) {
            fail("## checkExportDecrypt2() failed : " + e.message)
        }

        assertEquals("## checkExportDecrypt2() : expectedString $expectedString -- decodedString $decodedString",
                expectedString,
                decodedString)
    }

    @Test
    fun checkExportDecrypt3() {
        val password = "SWORDFISH"
        val input = "-----BEGIN MEGOLM SESSION DATA-----\nAXllc3NhbHR5Z29vZG5lc3P//////////wAAAAAAAAAAAAAD6OIW+Je7gwvjd4kYrb+49gKCfExw\n" + "MgJBMD4mrhLkmgAngwR1pHjbWXaoGybtiAYr0moQ93GrBQsCzPbvl82rZhaXO3iH5uHo/RCEpOqp\nPgg29363BGR+/Ripq/VCLKGNbw==\n-----END MEGOLM SESSION DATA-----"
        val expectedString = "alphanumericallyalphanumericallyalphanumericallyalphanumerically"

        var decodedString: String? = null
        try {
            decodedString = MXMegolmExportEncryption.decryptMegolmKeyFile(input.toByteArray(charset("UTF-8")), password)
        } catch (e: Exception) {
            fail("## checkExportDecrypt3() failed : " + e.message)
        }

        assertEquals("## checkExportDecrypt3() : expectedString $expectedString -- decodedString $decodedString",
                expectedString,
                decodedString)
    }

    @Test
    fun checkExportEncrypt1() {
        val password = "password"
        val expectedString = "plain"
        var decodedString: String? = null

        try {
            decodedString = MXMegolmExportEncryption
                    .decryptMegolmKeyFile(MXMegolmExportEncryption.encryptMegolmKeyFile(expectedString, password, 1000), password)
        } catch (e: Exception) {
            fail("## checkExportEncrypt1() failed : " + e.message)
        }

        assertEquals("## checkExportEncrypt1() : expectedString $expectedString -- decodedString $decodedString",
                expectedString,
                decodedString)
    }

    @Test
    fun checkExportEncrypt2() {
        val password = "betterpassword"
        val expectedString = "Hello, World"
        var decodedString: String? = null

        try {
            decodedString = MXMegolmExportEncryption
                    .decryptMegolmKeyFile(MXMegolmExportEncryption.encryptMegolmKeyFile(expectedString, password, 1000), password)
        } catch (e: Exception) {
            fail("## checkExportEncrypt2() failed : " + e.message)
        }

        assertEquals("## checkExportEncrypt2() : expectedString $expectedString -- decodedString $decodedString",
                expectedString,
                decodedString)
    }

    @Test
    fun checkExportEncrypt3() {
        val password = "SWORDFISH"
        val expectedString = "alphanumericallyalphanumericallyalphanumericallyalphanumerically"
        var decodedString: String? = null

        try {
            decodedString = MXMegolmExportEncryption
                    .decryptMegolmKeyFile(MXMegolmExportEncryption.encryptMegolmKeyFile(expectedString, password, 1000), password)
        } catch (e: Exception) {
            fail("## checkExportEncrypt3() failed : " + e.message)
        }

        assertEquals("## checkExportEncrypt3() : expectedString $expectedString -- decodedString $decodedString",
                expectedString,
                decodedString)
    }

    @Test
    fun checkExportEncrypt4() {
        val password = "passwordpasswordpasswordpasswordpasswordpasswordpasswordpasswordpasswordpasswordpasswordpasswordpasswordpasswordpasswordpassword" + "passwordpasswordpasswordpasswordpasswordpasswordpasswordpasswordpasswordpasswordpasswordpasswordpasswordpasswordpasswordpassword"
        val expectedString = "alphanumericallyalphanumericallyalphanumericallyalphanumerically"
        var decodedString: String? = null

        try {
            decodedString = MXMegolmExportEncryption
                    .decryptMegolmKeyFile(MXMegolmExportEncryption.encryptMegolmKeyFile(expectedString, password, 1000), password)
        } catch (e: Exception) {
            fail("## checkExportEncrypt4() failed : " + e.message)
        }

        assertEquals("## checkExportEncrypt4() : expectedString $expectedString -- decodedString $decodedString",
                expectedString,
                decodedString)
    }
}
