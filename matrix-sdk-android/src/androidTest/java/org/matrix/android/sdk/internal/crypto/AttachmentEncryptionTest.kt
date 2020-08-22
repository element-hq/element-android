/*
 * Copyright 2019 New Vector Ltd
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

import android.os.MemoryFile
import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.matrix.android.sdk.internal.crypto.attachments.MXEncryptedAttachments
import org.matrix.android.sdk.internal.crypto.model.rest.EncryptedFileInfo
import org.matrix.android.sdk.internal.crypto.model.rest.EncryptedFileKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.io.ByteArrayInputStream
import java.io.InputStream

/**
 * Unit tests AttachmentEncryptionTest.
 */
@Suppress("SpellCheckingInspection")
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class AttachmentEncryptionTest {

    private fun checkDecryption(input: String, encryptedFileInfo: EncryptedFileInfo): String {
        val `in` = Base64.decode(input, Base64.DEFAULT)

        val inputStream: InputStream

        inputStream = if (`in`.isEmpty()) {
            ByteArrayInputStream(`in`)
        } else {
            val memoryFile = MemoryFile("file" + System.currentTimeMillis(), `in`.size)
            memoryFile.outputStream.write(`in`)
            memoryFile.inputStream
        }

        val decryptedStream = MXEncryptedAttachments.decryptAttachment(inputStream, encryptedFileInfo)

        assertNotNull(decryptedStream)

        val buffer = ByteArray(100)

        val len = decryptedStream!!.read(buffer)

        decryptedStream.close()

        return Base64.encodeToString(buffer, 0, len, Base64.DEFAULT).replace("\n".toRegex(), "").replace("=".toRegex(), "")
    }

    @Test
    fun checkDecrypt1() {
        val encryptedFileInfo = EncryptedFileInfo(
                v = "v2",
                hashes = mapOf("sha256" to "47DEQpj8HBSa+/TImW+5JCeuQeRkm5NMpJWZG3hSuFU"),
                key = EncryptedFileKey(
                        alg = "A256CTR",
                        k = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                        key_ops = listOf("encrypt", "decrypt"),
                        kty = "oct",
                        ext = true
                ),
                iv = "AAAAAAAAAAAAAAAAAAAAAA",
                url = "dummyUrl"
        )

        assertEquals("", checkDecryption("", encryptedFileInfo))
    }

    @Test
    fun checkDecrypt2() {
        val encryptedFileInfo = EncryptedFileInfo(
                v = "v2",
                hashes = mapOf("sha256" to "YzF08lARDdOCzJpzuSwsjTNlQc4pHxpdHcXiD/wpK6k"),
                key = EncryptedFileKey(
                        alg = "A256CTR",
                        k = "__________________________________________8",
                        key_ops = listOf("encrypt", "decrypt"),
                        kty = "oct",
                        ext = true
                ),
                iv = "//////////8AAAAAAAAAAA",
                url = "dummyUrl"
        )

        assertEquals("SGVsbG8sIFdvcmxk", checkDecryption("5xJZTt5cQicm+9f4", encryptedFileInfo))
    }

    @Test
    fun checkDecrypt3() {
        val encryptedFileInfo = EncryptedFileInfo(
                v = "v2",
                hashes = mapOf("sha256" to "IOq7/dHHB+mfHfxlRY5XMeCWEwTPmlf4cJcgrkf6fVU"),
                key = EncryptedFileKey(
                        alg = "A256CTR",
                        k = "__________________________________________8",
                        key_ops = listOf("encrypt", "decrypt"),
                        kty = "oct",
                        ext = true
                ),
                iv = "//////////8AAAAAAAAAAA",
                url = "dummyUrl"
        )

        assertEquals("YWxwaGFudW1lcmljYWxseWFscGhhbnVtZXJpY2FsbHlhbHBoYW51bWVyaWNhbGx5YWxwaGFudW1lcmljYWxseQ",
                checkDecryption("zhtFStAeFx0s+9L/sSQO+WQMtldqYEHqTxMduJrCIpnkyer09kxJJuA4K+adQE4w+7jZe/vR9kIcqj9rOhDR8Q",
                        encryptedFileInfo))
    }

    @Test
    fun checkDecrypt4() {
        val encryptedFileInfo = EncryptedFileInfo(
                v = "v2",
                hashes = mapOf("sha256" to "LYG/orOViuFwovJpv2YMLSsmVKwLt7pY3f8SYM7KU5E"),
                key = EncryptedFileKey(
                        alg = "A256CTR",
                        k = "__________________________________________8",
                        key_ops = listOf("encrypt", "decrypt"),
                        kty = "oct",
                        ext = true
                ),
                iv = "/////////////////////w",
                url = "dummyUrl"
        )

        assertNotEquals("YWxwaGFudW1lcmljYWxseWFscGhhbnVtZXJpY2FsbHlhbHBoYW51bWVyaWNhbGx5YWxwaGFudW1lcmljYWxseQ",
                checkDecryption("tJVNBVJ/vl36UQt4Y5e5m84bRUrQHhcdLPvS/7EkDvlkDLZXamBB6k8THbiawiKZ5Mnq9PZMSSbgOCvmnUBOMA",
                        encryptedFileInfo))
    }
}
