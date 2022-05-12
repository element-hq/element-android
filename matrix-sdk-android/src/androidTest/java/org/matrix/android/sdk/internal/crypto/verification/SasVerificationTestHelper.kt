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

import org.matrix.android.sdk.api.session.crypto.verification.PendingVerificationRequest
import org.matrix.android.sdk.api.session.crypto.verification.VerificationMethod
import org.matrix.android.sdk.api.session.crypto.verification.VerificationService
import org.matrix.android.sdk.common.CommonTestHelper
import org.matrix.android.sdk.common.CryptoTestData
import org.matrix.android.sdk.common.CryptoTestHelper
import timber.log.Timber
import java.util.concurrent.CountDownLatch

class SasVerificationTestHelper(private val testHelper: CommonTestHelper, private val cryptoTestHelper: CryptoTestHelper) {
    fun requestVerificationAndWaitForReadyState(cryptoTestData: CryptoTestData, supportedMethods: List<VerificationMethod>): String {
        val aliceSession = cryptoTestData.firstSession
        val bobSession = cryptoTestData.secondSession!!
        cryptoTestHelper.initializeCrossSigning(aliceSession)
        cryptoTestHelper.initializeCrossSigning(bobSession)

        val aliceVerificationService = aliceSession.cryptoService().verificationService()
        val bobVerificationService = bobSession.cryptoService().verificationService()

        var bobReadyPendingVerificationRequest: PendingVerificationRequest? = null

        val latch = CountDownLatch(2)
        val aliceListener = object : VerificationService.Listener {
            override fun verificationRequestUpdated(pr: PendingVerificationRequest) {
                // Step 4: Alice receive the ready request
                Timber.v("Alice request updated: $pr")
                if (pr.isReady) {
                    latch.countDown()
                }
            }
        }
        aliceVerificationService.addListener(aliceListener)

        val bobListener = object : VerificationService.Listener {
            override fun verificationRequestCreated(pr: PendingVerificationRequest) {
                // Step 2: Bob accepts the verification request
                Timber.v("Bob accepts the verification request")
                testHelper.runBlockingTest {
                    bobVerificationService.readyPendingVerification(
                            supportedMethods,
                            aliceSession.myUserId,
                            pr.transactionId!!
                    )
                }
            }

            override fun verificationRequestUpdated(pr: PendingVerificationRequest) {
                // Step 3: Bob is ready
                Timber.v("Bob request updated $pr")
                if (pr.isReady) {
                    bobReadyPendingVerificationRequest = pr
                    latch.countDown()
                }
            }
        }
        bobVerificationService.addListener(bobListener)

        val bobUserId = bobSession.myUserId
        // Step 1: Alice starts a verification request
        testHelper.runBlockingTest {
            aliceVerificationService.requestKeyVerificationInDMs(supportedMethods, bobUserId, cryptoTestData.roomId)
        }
        testHelper.await(latch)
        bobVerificationService.removeListener(bobListener)
        aliceVerificationService.removeListener(aliceListener)
        return bobReadyPendingVerificationRequest?.transactionId!!
    }
}
