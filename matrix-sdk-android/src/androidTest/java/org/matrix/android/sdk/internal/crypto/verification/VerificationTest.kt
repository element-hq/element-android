/*
 * Copyright (c) 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.crypto.verification

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.amshove.kluent.shouldBe
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.matrix.android.sdk.InstrumentedTest
import org.matrix.android.sdk.api.session.crypto.verification.EVerificationState
import org.matrix.android.sdk.api.session.crypto.verification.VerificationMethod
import org.matrix.android.sdk.common.CommonTestHelper.Companion.runCryptoTest

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.JVM)
class VerificationTest : InstrumentedTest {

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

    private fun doTest(
            aliceSupportedMethods: List<VerificationMethod>,
            bobSupportedMethods: List<VerificationMethod>,
            expectedResultForAlice: ExpectedResult,
            expectedResultForBob: ExpectedResult
    ) = runCryptoTest(context()) { cryptoTestHelper, testHelper ->
        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoom()

        val aliceSession = cryptoTestData.firstSession
        val bobSession = cryptoTestData.secondSession!!

        cryptoTestHelper.initializeCrossSigning(aliceSession)
        cryptoTestHelper.initializeCrossSigning(bobSession)

        val aliceVerificationService = aliceSession.cryptoService().verificationService()
        val bobVerificationService = bobSession.cryptoService().verificationService()

        val transactionId = aliceVerificationService.requestKeyVerificationInDMs(
                aliceSupportedMethods, bobSession.myUserId, cryptoTestData.roomId
        )
                .transactionId

        testHelper.retryPeriodically {
            val incomingRequest = bobVerificationService.getExistingVerificationRequest(aliceSession.myUserId, transactionId)
            if (incomingRequest != null) {
                bobVerificationService.readyPendingVerification(
                        bobSupportedMethods,
                        aliceSession.myUserId,
                        incomingRequest.transactionId
                )
                true
            } else {
                false
            }
        }

        // wait for alice to see the ready
        testHelper.retryPeriodically {
            val pendingRequest = aliceVerificationService.getExistingVerificationRequest(bobSession.myUserId, transactionId)
            pendingRequest?.state == EVerificationState.Ready
        }

        val aliceReadyPendingVerificationRequest = aliceVerificationService.getExistingVerificationRequest(bobSession.myUserId, transactionId)!!
        val bobReadyPendingVerificationRequest = bobVerificationService.getExistingVerificationRequest(aliceSession.myUserId, transactionId)!!

        aliceReadyPendingVerificationRequest.let { pr ->
            pr.isSasSupported shouldBe expectedResultForAlice.sasIsSupported
            pr.weShouldShowScanOption shouldBe expectedResultForAlice.otherCanShowQrCode
            pr.weShouldDisplayQRCode shouldBe expectedResultForAlice.otherCanScanQrCode
        }

        bobReadyPendingVerificationRequest.let { pr ->
            pr.isSasSupported shouldBe expectedResultForBob.sasIsSupported
            pr.weShouldShowScanOption shouldBe expectedResultForBob.otherCanShowQrCode
            pr.weShouldDisplayQRCode shouldBe expectedResultForBob.otherCanScanQrCode
        }

        cryptoTestData.cleanUp(testHelper)
    }
}
