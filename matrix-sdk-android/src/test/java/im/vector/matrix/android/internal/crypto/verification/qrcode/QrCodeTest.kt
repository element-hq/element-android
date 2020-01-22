/*
 * Copyright 2020 New Vector Ltd
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

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

@Suppress("SpellCheckingInspection")
@FixMethodOrder(MethodSorters.JVM)
class QrCodeTest {

    private val basicQrCodeData = QrCodeData(
            userId = "@benoit:matrix.org",
            requestEventId = "\$azertyazerty",
            action = QrCodeData.ACTION_VERIFY,
            keys = mapOf(
                    "1" to "abcdef",
                    "2" to "ghijql"
            ),
            sharedSecret = "sharedSecret",
            otherUserKey = "otherUserKey"
    )

    private val basicUrl = "https://matrix.to/#/@benoit:matrix.org?request=\$azertyazerty&action=verify&key_1=abcdef&key_2=ghijql&secret=sharedSecret&other_user_key=otherUserKey"

    @Test
    fun testNominalCase() {
        val url = basicQrCodeData.toUrl()

        url shouldBeEqualTo basicUrl

        val decodedData = url.toQrCodeData()

        decodedData.shouldNotBeNull()

        decodedData.userId shouldBeEqualTo "@benoit:matrix.org"
        decodedData.requestEventId shouldBeEqualTo "\$azertyazerty"
        decodedData.keys["1"]?.shouldBeEqualTo("abcdef")
        decodedData.keys["2"]?.shouldBeEqualTo("ghijql")
        decodedData.sharedSecret shouldBeEqualTo "sharedSecret"
        decodedData.otherUserKey shouldBeEqualTo "otherUserKey"
    }

    @Test
    fun testSlashCase() {
        val url = basicQrCodeData
                .copy(
                        userId = "@benoit/foo:matrix.org",
                        requestEventId = "\$azertyazerty/bar"
                )
                .toUrl()

        url shouldBeEqualTo basicUrl
                .replace("@benoit", "@benoit%2Ffoo")
                .replace("azertyazerty", "azertyazerty%2Fbar")

        val decodedData = url.toQrCodeData()

        decodedData.shouldNotBeNull()

        decodedData.userId shouldBeEqualTo "@benoit/foo:matrix.org"
        decodedData.requestEventId shouldBeEqualTo "\$azertyazerty/bar"
        decodedData.keys["1"]?.shouldBeEqualTo("abcdef")
        decodedData.keys["2"]?.shouldBeEqualTo("ghijql")
        decodedData.sharedSecret shouldBeEqualTo "sharedSecret"
        decodedData.otherUserKey shouldBeEqualTo "otherUserKey"
    }

    @Test
    fun testMissingActionCase() {
        basicUrl.replace("&action=verify", "")
                .toQrCodeData()
                .shouldBeNull()
    }

    @Test
    fun testOtherActionCase() {
        basicUrl.replace("&action=verify", "&action=confirm")
                .toQrCodeData()
                ?.action
                ?.shouldBeEqualTo("confirm")
    }

    @Test
    fun testBadRequestEventId() {
        basicUrl.replace("\$azertyazerty", "@azertyazerty")
                .toQrCodeData()
                .shouldBeNull()
    }

    @Test
    fun testMissingUserId() {
        basicUrl.replace("@benoit:matrix.org", "")
                .toQrCodeData()
                .shouldBeNull()
    }

    @Test
    fun testBadUserId() {
        basicUrl.replace("@benoit:matrix.org", "@benoit")
                .toQrCodeData()
                .shouldBeNull()
    }

    @Test
    fun testMissingSecret() {
        basicUrl.replace("&secret=sharedSecret", "")
                .toQrCodeData()
                .shouldBeNull()
    }

    @Test
    fun testMissingOtherUserKey() {
        basicUrl.replace("&other_user_key=otherUserKey", "")
                .toQrCodeData()
                .shouldBeNull()
    }
}
