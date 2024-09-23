/*
 * Copyright (c) 2024 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.contentscanner

import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBe
import org.junit.Test
import org.matrix.android.sdk.api.session.crypto.attachments.ElementToDecrypt
import org.matrix.android.sdk.api.session.crypto.model.EncryptedFileInfo
import org.matrix.android.sdk.api.session.crypto.model.EncryptedFileKey
import org.matrix.android.sdk.internal.session.contentscanner.model.DownloadBody

class ScanEncryptorUtilsTest {
    private val anMxcUrl = "mxc://matrix.org/123456"
    private val anElementToDecrypt = ElementToDecrypt(
            k = "key",
            iv = "iv",
            sha256 = "sha256"
    )
    private val aPublicKey = "6n3l15JqsNhpM1OwRIoDCL/3c1B5idcwvy07Y5qFRyw="
    private val aPrivateKey = "CLYwNaeA9d0KHE0DniO1bxGgmNsPJ/pyanF4b4tcK1M="

    @Test
    fun whenNoServerKeyIsProvidedTheContentIsNotEncrypted() {
        val result = ScanEncryptorUtils.getDownloadBodyAndEncryptIfNeeded(
                publicServerKey = null,
                mxcUrl = anMxcUrl,
                elementToDecrypt = anElementToDecrypt
        )
        result shouldBeEqualTo DownloadBody(
                file = EncryptedFileInfo(
                        url = anMxcUrl,
                        iv = anElementToDecrypt.iv,
                        hashes = mapOf("sha256" to anElementToDecrypt.sha256),
                        key = EncryptedFileKey(
                                k = anElementToDecrypt.k,
                                alg = "A256CTR",
                                keyOps = listOf("encrypt", "decrypt"),
                                kty = "oct",
                                ext = true
                        ),
                        v = "v2"
                ),
                encryptedBody = null
        )
    }

    @Test
    fun whenServerKeyIsProvidedTheContentIsEncrypted() {
        val result = ScanEncryptorUtils.getDownloadBodyAndEncryptIfNeeded(
                publicServerKey = aPublicKey,
                mxcUrl = anMxcUrl,
                elementToDecrypt = anElementToDecrypt
        )
        result.file shouldBe null
        // Note: we cannot check the members of EncryptedBody because they change on each call.
        result.encryptedBody shouldNotBe null
    }

    // Note: PkDecryption is not exposed in the FFI layer, so we cannot use this test.
    /*
    @Test
    fun checkThatTheCodeIsAbleToDecryptContent() {
        System.loadLibrary("olm")
        val clearInfo = ScanEncryptorUtils.getDownloadBodyAndEncryptIfNeeded(
                publicServerKey = null,
                mxcUrl = anMxcUrl,
                elementToDecrypt = anElementToDecrypt
        )
        // Uncomment to get a new encrypted body
        // val encryptedBody = ScanEncryptorUtils.getDownloadBodyAndEncryptIfNeeded(
        //         publicServerKey = aPublicKey,
        //         mxcUrl = anMxcUrl,
        //         elementToDecrypt = anElementToDecrypt
        // ).encryptedBody!!
        // println("libolmEncryptedBody: $encryptedBody")
        val libolmEncryptedBody = EncryptedBody(
                cipherText = "GTnDhm6xe5fPe/QCr6fyGcZXheFhZlPG" +
                        "nJZiCK8Xwq6qTg71vSUGWtLdt3uaTmK7" +
                        "F7fB3PBKchHu2VVv6MMgo8fpUQ7KBbmu" +
                        "NWTrNmf3QdhXuRwUwz/q4GxsbGR2zjSX" +
                        "/UoE5S4ymVtOVhvSfXQfssN56wVIzC6S" +
                        "dy57y6b1IXPihlCUdvb8LMkMvViHYeNf" +
                        "beFrAfMlsyr1+jdZEXZF5Q7iruhsH2iu" +
                        "k7+Ayl9rdILCD5tjE9pezwe1V6uc/Agb",
                mac = "Wk77HRg50oM",
                ephemeral = "rMTK6/CGASinfX4USFS5qmD3r4meffxKc/jCSFIBczw"
        )
        // Try to decrypt the body
        val result = withOlmDecryption { olmPkDecryption ->
            olmPkDecryption.setPrivateKey(aPrivateKey.decodeBase64()!!.toByteArray())
            olmPkDecryption.decrypt(
                    OlmPkMessage().apply {
                        mCipherText = libolmEncryptedBody.cipherText
                        mMac = libolmEncryptedBody.mac
                        mEphemeralKey = libolmEncryptedBody.ephemeral
                    }
            )
        }
        val parseResult = MoshiProvider.providesMoshi()
                .adapter(DownloadBody::class.java)
                .fromJson(result)
        parseResult shouldBeEqualTo clearInfo
    }
     */
}
