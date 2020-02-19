/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.matrix.android.internal.crypto.verification.qrcode

import androidx.test.ext.junit.runners.AndroidJUnit4
import im.vector.matrix.android.InstrumentedTest
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldEqual
import org.amshove.kluent.shouldEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.JVM)
class QrCodeV2Test : InstrumentedTest {

    private val qrCode1 = QrCodeDataV2.VerifyingAnotherUser(
            transactionId = "MaTransaction",
            userMasterCrossSigningPublicKey = "ktEwcUP6su1xh+GuE+CYkQ3H6W/DIl+ybHFdaEOrolU",
            otherUserMasterCrossSigningPublicKey = "TXluZKTZLvSRWOTPlOqLq534bA+/K4zLFKSu9cGLQaU",
            sharedSecret = "MTIzNDU2Nzg"
    )

    private val value1 = "MATRIX\u0002\u0000\u0000\u000DMaTransaction\u0092Ñ0qCú²íq\u0087á®\u0013à\u0098\u0091\u000DÇéoÃ\"_²lq]hC«¢UMynd¤Ù.ô\u0091XäÏ\u0094ê\u008B«\u009Døl\u000F¿+\u008CË\u0014¤®õÁ\u008BA¥12345678"

    private val qrCode2 = QrCodeDataV2.SelfVerifyingMasterKeyTrusted(
            transactionId = "MaTransaction",
            userMasterCrossSigningPublicKey = "ktEwcUP6su1xh+GuE+CYkQ3H6W/DIl+ybHFdaEOrolU",
            otherDeviceKey = "TXluZKTZLvSRWOTPlOqLq534bA+/K4zLFKSu9cGLQaU",
            sharedSecret = "MTIzNDU2Nzg"
    )

    private val value2 = "MATRIX\u0002\u0001\u0000\u000DMaTransaction\u0092Ñ0qCú²íq\u0087á®\u0013à\u0098\u0091\u000DÇéoÃ\"_²lq]hC«¢UMynd¤Ù.ô\u0091XäÏ\u0094ê\u008B«\u009Døl\u000F¿+\u008CË\u0014¤®õÁ\u008BA¥12345678"

    private val qrCode3 = QrCodeDataV2.SelfVerifyingMasterKeyNotTrusted(
            transactionId = "MaTransaction",
            deviceKey = "TXluZKTZLvSRWOTPlOqLq534bA+/K4zLFKSu9cGLQaU",
            userMasterCrossSigningPublicKey = "ktEwcUP6su1xh+GuE+CYkQ3H6W/DIl+ybHFdaEOrolU",
            sharedSecret = "MTIzNDU2Nzg"
    )

    private val value3 = "MATRIX\u0002\u0002\u0000\u000DMaTransactionMynd¤Ù.ô\u0091XäÏ\u0094ê\u008B«\u009Døl\u000F¿+\u008CË\u0014¤®õÁ\u008BA¥\u0092Ñ0qCú²íq\u0087á®\u0013à\u0098\u0091\u000DÇéoÃ\"_²lq]hC«¢U12345678"

    private val sharedSecretByteArray = "12345678".toByteArray(Charsets.ISO_8859_1)

    // 4d 79 6e 64 a4 d9 2e f4 91 58 e4 cf 94 ea 8b ab 9d f8 6c 0f bf 2b 8c cb 14 a4 ae f5 c1 8b 41 a5
    private val tlx_byteArray = ByteArray(32) {
        when (it) {
            0    -> 0x4D.toByte()
            1    -> 0x79.toByte()
            2    -> 0x6E.toByte()
            3    -> 0x64.toByte()
            4    -> 0xA4.toByte()
            5    -> 0xD9.toByte()
            6    -> 0x2E.toByte()
            7    -> 0xF4.toByte()
            8    -> 0x91.toByte()
            9    -> 0x58.toByte()
            10   -> 0xE4.toByte()
            11   -> 0xCF.toByte()
            12   -> 0x94.toByte()
            13   -> 0xEA.toByte()
            14   -> 0x8B.toByte()
            15   -> 0xAB.toByte()
            16   -> 0x9D.toByte()
            17   -> 0xF8.toByte()
            18   -> 0x6C.toByte()
            19   -> 0x0F.toByte()
            20   -> 0xBF.toByte()
            21   -> 0x2B.toByte()
            22   -> 0x8C.toByte()
            23   -> 0xCB.toByte()
            24   -> 0x14.toByte()
            25   -> 0xA4.toByte()
            26   -> 0xAE.toByte()
            27   -> 0xF5.toByte()
            28   -> 0xC1.toByte()
            29   -> 0x8B.toByte()
            30   -> 0x41.toByte()
            else -> 0xA5.toByte()
        }
    }

    // 92 d1 30 71 43 fa b2 ed 71 87 e1 ae 13 e0 98 91 0d c7 e9 6f c3 22 5f b2 6c 71 5d 68 43 ab a2 55
    private val kte_byteArray = ByteArray(32) {
        when (it) {
            0    -> 0x92.toByte()
            1    -> 0xd1.toByte()
            2    -> 0x30.toByte()
            3    -> 0x71.toByte()
            4    -> 0x43.toByte()
            5    -> 0xfa.toByte()
            6    -> 0xb2.toByte()
            7    -> 0xed.toByte()
            8    -> 0x71.toByte()
            9    -> 0x87.toByte()
            10   -> 0xe1.toByte()
            11   -> 0xae.toByte()
            12   -> 0x13.toByte()
            13   -> 0xe0.toByte()
            14   -> 0x98.toByte()
            15   -> 0x91.toByte()
            16   -> 0x0d.toByte()
            17   -> 0xc7.toByte()
            18   -> 0xe9.toByte()
            19   -> 0x6f.toByte()
            20   -> 0xc3.toByte()
            21   -> 0x22.toByte()
            22   -> 0x5f.toByte()
            23   -> 0xb2.toByte()
            24   -> 0x6c.toByte()
            25   -> 0x71.toByte()
            26   -> 0x5d.toByte()
            27   -> 0x68.toByte()
            28   -> 0x43.toByte()
            29   -> 0xab.toByte()
            30   -> 0xa2.toByte()
            else -> 0x55.toByte()
        }
    }

    @Test
    fun testEncoding1() {
        qrCode1.toEncodedString() shouldEqual value1
    }

    @Test
    fun testEncoding2() {
        qrCode2.toEncodedString() shouldEqual value2
    }

    @Test
    fun testEncoding3() {
        qrCode3.toEncodedString() shouldEqual value3
    }

    @Test
    fun testSymmetry1() {
        qrCode1.toEncodedString().toQrCodeDataV2() shouldEqual qrCode1
    }

    @Test
    fun testSymmetry2() {
        qrCode2.toEncodedString().toQrCodeDataV2() shouldEqual qrCode2
    }

    @Test
    fun testSymmetry3() {
        qrCode3.toEncodedString().toQrCodeDataV2() shouldEqual qrCode3
    }

    @Test
    fun testCase1() {
        val url = qrCode1.toEncodedString()

        val byteArray = url.toByteArray(Charsets.ISO_8859_1)
        checkHeader(byteArray)

        // Mode
        byteArray[7] shouldEqualTo 0

        checkSizeAndTransaction(byteArray)

        compareArray(byteArray.copyOfRange(23, 23 + 32), kte_byteArray)
        compareArray(byteArray.copyOfRange(23 + 32, 23 + 64), tlx_byteArray)

        compareArray(byteArray.copyOfRange(23 + 64, byteArray.size), sharedSecretByteArray)
    }

    @Test
    fun testCase2() {
        val url = qrCode2.toEncodedString()

        val byteArray = url.toByteArray(Charsets.ISO_8859_1)
        checkHeader(byteArray)

        // Mode
        byteArray[7] shouldEqualTo 1

        checkSizeAndTransaction(byteArray)
        compareArray(byteArray.copyOfRange(23, 23 + 32), kte_byteArray)
        compareArray(byteArray.copyOfRange(23 + 32, 23 + 64), tlx_byteArray)

        compareArray(byteArray.copyOfRange(23 + 64, byteArray.size), sharedSecretByteArray)
    }

    @Test
    fun testCase3() {
        val url = qrCode3.toEncodedString()

        val byteArray = url.toByteArray(Charsets.ISO_8859_1)
        checkHeader(byteArray)

        // Mode
        byteArray[7] shouldEqualTo 2

        checkSizeAndTransaction(byteArray)
        compareArray(byteArray.copyOfRange(23, 23 + 32), tlx_byteArray)
        compareArray(byteArray.copyOfRange(23 + 32, 23 + 64), kte_byteArray)

        compareArray(byteArray.copyOfRange(23 + 64, byteArray.size), sharedSecretByteArray)
    }

    // Error cases
    @Test
    fun testErrorHeader() {
        value1.replace("MATRIX", "MOTRIX").toQrCodeDataV2().shouldBeNull()
        value1.replace("MATRIX", "MATRI").toQrCodeDataV2().shouldBeNull()
        value1.replace("MATRIX", "").toQrCodeDataV2().shouldBeNull()
    }

    @Test
    fun testErrorVersion() {
        value1.replace("MATRIX\u0002", "MATRIX\u0000").toQrCodeDataV2().shouldBeNull()
        value1.replace("MATRIX\u0002", "MATRIX\u0001").toQrCodeDataV2().shouldBeNull()
        value1.replace("MATRIX\u0002", "MATRIX\u0003").toQrCodeDataV2().shouldBeNull()
        value1.replace("MATRIX\u0002", "MATRIX").toQrCodeDataV2().shouldBeNull()
    }

    @Test
    fun testErrorSecretTooShort() {
        value1.replace("12345678", "1234567").toQrCodeDataV2().shouldBeNull()
    }

    @Test
    fun testErrorNoTransactionNoKeyNoSecret() {
        // But keep transaction length
        "MATRIX\u0002\u0000\u0000\u000D".toQrCodeDataV2().shouldBeNull()
    }

    @Test
    fun testErrorNoKeyNoSecret() {
        "MATRIX\u0002\u0000\u0000\u000DMaTransaction".toQrCodeDataV2().shouldBeNull()
    }

    @Test
    fun testErrorTransactionLengthTooShort() {
        // In this case, the secret will be longer, so this is not an error, but it will lead to keys mismatch
        value1.replace("\u000DMaTransaction", "\u000CMaTransaction").toQrCodeDataV2().shouldNotBeNull()
    }

    @Test
    fun testErrorTransactionLengthTooBig() {
        value1.replace("\u000DMaTransaction", "\u000EMaTransaction").toQrCodeDataV2().shouldBeNull()
    }

    private fun compareArray(actual: ByteArray, expected: ByteArray) {
        actual.size shouldEqual expected.size

        for (i in actual.indices) {
            actual[i] shouldEqualTo expected[i]
        }
    }

    private fun checkHeader(byteArray: ByteArray) {
        // MATRIX
        byteArray[0] shouldEqualTo 'M'.toByte()
        byteArray[1] shouldEqualTo 'A'.toByte()
        byteArray[2] shouldEqualTo 'T'.toByte()
        byteArray[3] shouldEqualTo 'R'.toByte()
        byteArray[4] shouldEqualTo 'I'.toByte()
        byteArray[5] shouldEqualTo 'X'.toByte()

        // Version
        byteArray[6] shouldEqualTo 2
    }

    private fun checkSizeAndTransaction(byteArray: ByteArray) {
        // Size
        byteArray[8] shouldEqualTo 0
        byteArray[9] shouldEqualTo 13

        // Transaction
        byteArray.copyOfRange(10, 10 + "MaTransaction".length).toString(Charsets.ISO_8859_1) shouldEqual "MaTransaction"
    }
}
