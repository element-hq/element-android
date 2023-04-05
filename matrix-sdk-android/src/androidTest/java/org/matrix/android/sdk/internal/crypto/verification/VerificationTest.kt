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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.launch
import org.amshove.kluent.shouldBe
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.matrix.android.sdk.InstrumentedTest
import org.matrix.android.sdk.api.session.crypto.verification.EVerificationState
import org.matrix.android.sdk.api.session.crypto.verification.PendingVerificationRequest
import org.matrix.android.sdk.api.session.crypto.verification.VerificationMethod
import org.matrix.android.sdk.api.session.crypto.verification.getRequest
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
    ) = runCryptoTest(context()) { cryptoTestHelper, _ ->
        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoom()

        val aliceSession = cryptoTestData.firstSession
        val bobSession = cryptoTestData.secondSession!!

        cryptoTestHelper.initializeCrossSigning(aliceSession)
        cryptoTestHelper.initializeCrossSigning(bobSession)

        val scope = CoroutineScope(SupervisorJob())

        val aliceVerificationService = aliceSession.cryptoService().verificationService()
        val bobVerificationService = bobSession.cryptoService().verificationService()

        val bobSeesVerification = CompletableDeferred<PendingVerificationRequest>()
        scope.launch(Dispatchers.IO) {
            bobVerificationService.requestEventFlow()
                    .cancellable()
                    .collect {
                        val request = it.getRequest()
                        if (request != null) {
                            bobSeesVerification.complete(request)
                            return@collect cancel()
                        }
                    }
        }

        val aliceReady = CompletableDeferred<PendingVerificationRequest>()
        scope.launch(Dispatchers.IO) {
            aliceVerificationService.requestEventFlow()
                    .cancellable()
                    .collect {
                        val request = it.getRequest()
                        if (request?.state == EVerificationState.Ready) {
                            aliceReady.complete(request)
                            return@collect cancel()
                        }
                    }
        }
        val bobReady = CompletableDeferred<PendingVerificationRequest>()
        scope.launch(Dispatchers.IO) {
            bobVerificationService.requestEventFlow()
                    .cancellable()
                    .collect {
                        val request = it.getRequest()
                        if (request?.state == EVerificationState.Ready) {
                            bobReady.complete(request)
                            return@collect cancel()
                        }
                    }
        }

        val requestID = aliceVerificationService.requestKeyVerificationInDMs(
                methods = aliceSupportedMethods,
                otherUserId = bobSession.myUserId,
                roomId = cryptoTestData.roomId
        ).transactionId

        bobSeesVerification.await()
        bobVerificationService.readyPendingVerification(
                bobSupportedMethods,
                aliceSession.myUserId,
                requestID
        )
        val aliceRequest = aliceReady.await()
        val bobRequest = bobReady.await()

        aliceRequest.let { pr ->
            pr.isSasSupported shouldBe expectedResultForAlice.sasIsSupported
            pr.weShouldShowScanOption shouldBe expectedResultForAlice.otherCanShowQrCode
            pr.weShouldDisplayQRCode shouldBe expectedResultForAlice.otherCanScanQrCode
        }

        bobRequest.let { pr ->
            pr.isSasSupported shouldBe expectedResultForBob.sasIsSupported
            pr.weShouldShowScanOption shouldBe expectedResultForBob.otherCanShowQrCode
            pr.weShouldDisplayQRCode shouldBe expectedResultForBob.otherCanScanQrCode
        }

        scope.cancel()
    }
}
