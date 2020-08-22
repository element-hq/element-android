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

package org.matrix.android.sdk.internal.crypto.verification.qrcode

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.matrix.android.sdk.InstrumentedTest
import org.matrix.android.sdk.api.session.crypto.verification.VerificationMethod
import org.matrix.android.sdk.api.session.crypto.verification.VerificationService
import org.matrix.android.sdk.common.CommonTestHelper
import org.matrix.android.sdk.common.CryptoTestHelper
import org.matrix.android.sdk.common.TestConstants
import org.matrix.android.sdk.internal.crypto.model.rest.UserPasswordAuth
import org.matrix.android.sdk.api.session.crypto.verification.PendingVerificationRequest
import org.amshove.kluent.shouldBe
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.util.concurrent.CountDownLatch

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.JVM)
class VerificationTest : InstrumentedTest {
    private val mTestHelper = CommonTestHelper(context())
    private val mCryptoTestHelper = CryptoTestHelper(mTestHelper)

    data class ExpectedResult(
            val sasIsSupported: Boolean = false,
            val otherCanScanQrCode: Boolean = false,
            val otherCanShowQrCode: Boolean = false
    )

    private val sas = listOf(
            VerificationMethod.SAS
    )

    private val sasShow = listOf(
            VerificationMethod.SAS,
            VerificationMethod.QR_CODE_SHOW
    )

    private val sasScan = listOf(
            VerificationMethod.SAS,
            VerificationMethod.QR_CODE_SCAN
    )

    private val sasShowScan = listOf(
            VerificationMethod.SAS,
            VerificationMethod.QR_CODE_SHOW,
            VerificationMethod.QR_CODE_SCAN
    )

    @Test
    fun test_aliceAndBob_sas_sas() = doTest(
            sas,
            sas,
            ExpectedResult(sasIsSupported = true),
            ExpectedResult(sasIsSupported = true)
    )

    @Test
    fun test_aliceAndBob_sas_show() = doTest(
            sas,
            sasShow,
            ExpectedResult(sasIsSupported = true),
            ExpectedResult(sasIsSupported = true)
    )

    @Test
    fun test_aliceAndBob_show_sas() = doTest(
            sasShow,
            sas,
            ExpectedResult(sasIsSupported = true),
            ExpectedResult(sasIsSupported = true)
    )

    @Test
    fun test_aliceAndBob_sas_scan() = doTest(
            sas,
            sasScan,
            ExpectedResult(sasIsSupported = true),
            ExpectedResult(sasIsSupported = true)
    )

    @Test
    fun test_aliceAndBob_scan_sas() = doTest(
            sasScan,
            sas,
            ExpectedResult(sasIsSupported = true),
            ExpectedResult(sasIsSupported = true)
    )

    @Test
    fun test_aliceAndBob_scan_scan() = doTest(
            sasScan,
            sasScan,
            ExpectedResult(sasIsSupported = true),
            ExpectedResult(sasIsSupported = true)
    )

    @Test
    fun test_aliceAndBob_show_show() = doTest(
            sasShow,
            sasShow,
            ExpectedResult(sasIsSupported = true),
            ExpectedResult(sasIsSupported = true)
    )

    @Test
    fun test_aliceAndBob_show_scan() = doTest(
            sasShow,
            sasScan,
            ExpectedResult(sasIsSupported = true, otherCanScanQrCode = true),
            ExpectedResult(sasIsSupported = true, otherCanShowQrCode = true)
    )

    @Test
    fun test_aliceAndBob_scan_show() = doTest(
            sasScan,
            sasShow,
            ExpectedResult(sasIsSupported = true, otherCanShowQrCode = true),
            ExpectedResult(sasIsSupported = true, otherCanScanQrCode = true)
    )

    @Test
    fun test_aliceAndBob_all_all() = doTest(
            sasShowScan,
            sasShowScan,
            ExpectedResult(sasIsSupported = true, otherCanShowQrCode = true, otherCanScanQrCode = true),
            ExpectedResult(sasIsSupported = true, otherCanShowQrCode = true, otherCanScanQrCode = true)
    )

    // TODO Add tests without SAS

    private fun doTest(aliceSupportedMethods: List<VerificationMethod>,
                       bobSupportedMethods: List<VerificationMethod>,
                       expectedResultForAlice: ExpectedResult,
                       expectedResultForBob: ExpectedResult) {
        val cryptoTestData = mCryptoTestHelper.doE2ETestWithAliceAndBobInARoom()

        val aliceSession = cryptoTestData.firstSession
        val bobSession = cryptoTestData.secondSession!!

        mTestHelper.doSync<Unit> { callback ->
            aliceSession.cryptoService().crossSigningService()
                    .initializeCrossSigning(UserPasswordAuth(
                            user = aliceSession.myUserId,
                            password = TestConstants.PASSWORD
                    ), callback)
        }

        mTestHelper.doSync<Unit> { callback ->
            bobSession.cryptoService().crossSigningService()
                    .initializeCrossSigning(UserPasswordAuth(
                            user = bobSession.myUserId,
                            password = TestConstants.PASSWORD
                    ), callback)
        }

        val aliceVerificationService = aliceSession.cryptoService().verificationService()
        val bobVerificationService = bobSession.cryptoService().verificationService()

        var aliceReadyPendingVerificationRequest: PendingVerificationRequest? = null
        var bobReadyPendingVerificationRequest: PendingVerificationRequest? = null

        val latch = CountDownLatch(2)
        val aliceListener = object : VerificationService.Listener {
            override fun verificationRequestUpdated(pr: PendingVerificationRequest) {
                // Step 4: Alice receive the ready request
                if (pr.isReady) {
                    aliceReadyPendingVerificationRequest = pr
                    latch.countDown()
                }
            }
        }
        aliceVerificationService.addListener(aliceListener)

        val bobListener = object : VerificationService.Listener {
            override fun verificationRequestCreated(pr: PendingVerificationRequest) {
                // Step 2: Bob accepts the verification request
                bobVerificationService.readyPendingVerificationInDMs(
                        bobSupportedMethods,
                        aliceSession.myUserId,
                        cryptoTestData.roomId,
                        pr.transactionId!!
                )
            }

            override fun verificationRequestUpdated(pr: PendingVerificationRequest) {
                // Step 3: Bob is ready
                if (pr.isReady) {
                    bobReadyPendingVerificationRequest = pr
                    latch.countDown()
                }
            }
        }
        bobVerificationService.addListener(bobListener)

        val bobUserId = bobSession.myUserId
        // Step 1: Alice starts a verification request
        aliceVerificationService.requestKeyVerificationInDMs(aliceSupportedMethods, bobUserId, cryptoTestData.roomId)
        mTestHelper.await(latch)

        aliceReadyPendingVerificationRequest!!.let { pr ->
            pr.isSasSupported() shouldBe expectedResultForAlice.sasIsSupported
            pr.otherCanShowQrCode() shouldBe expectedResultForAlice.otherCanShowQrCode
            pr.otherCanScanQrCode() shouldBe expectedResultForAlice.otherCanScanQrCode
        }

        bobReadyPendingVerificationRequest!!.let { pr ->
            pr.isSasSupported() shouldBe expectedResultForBob.sasIsSupported
            pr.otherCanShowQrCode() shouldBe expectedResultForBob.otherCanShowQrCode
            pr.otherCanScanQrCode() shouldBe expectedResultForBob.otherCanScanQrCode
        }

        cryptoTestData.cleanUp(mTestHelper)
    }
}
