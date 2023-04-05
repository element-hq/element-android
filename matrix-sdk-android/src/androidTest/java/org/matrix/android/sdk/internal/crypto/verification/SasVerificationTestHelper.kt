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

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.verification.EVerificationState
import org.matrix.android.sdk.api.session.crypto.verification.PendingVerificationRequest
import org.matrix.android.sdk.api.session.crypto.verification.VerificationMethod
import org.matrix.android.sdk.api.session.crypto.verification.getRequest
import org.matrix.android.sdk.common.CommonTestHelper
import org.matrix.android.sdk.common.CryptoTestData

class SasVerificationTestHelper(private val testHelper: CommonTestHelper) {
    suspend fun requestVerificationAndWaitForReadyState(
            scope: CoroutineScope,
            cryptoTestData: CryptoTestData, supportedMethods: List<VerificationMethod>
    ): String {
        val aliceSession = cryptoTestData.firstSession
        val bobSession = cryptoTestData.secondSession!!

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

        val bobUserId = bobSession.myUserId
        // Step 1: Alice starts a verification request
        val transactionId = aliceVerificationService.requestKeyVerificationInDMs(
                supportedMethods, bobUserId, cryptoTestData.roomId
        ).transactionId

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

        bobSeesVerification.await()
        bobVerificationService.readyPendingVerification(
                supportedMethods,
                aliceSession.myUserId,
                transactionId
        )

        aliceReady.await()
        return transactionId
    }

    suspend fun requestSelfKeyAndWaitForReadyState(session1: Session, session2: Session, supportedMethods: List<VerificationMethod>): String {
        val session1VerificationService = session1.cryptoService().verificationService()
        val session2VerificationService = session2.cryptoService().verificationService()

        val requestID = session1VerificationService.requestSelfKeyVerification(supportedMethods).transactionId

        val myUserId = session1.myUserId
        testHelper.retryWithBackoff {
            val incomingRequest = session2VerificationService.getExistingVerificationRequest(myUserId, requestID)
            if (incomingRequest != null) {
                session2VerificationService.readyPendingVerification(
                        supportedMethods,
                        myUserId,
                        incomingRequest.transactionId
                )
                true
            } else {
                false
            }
        }

        // wait for alice to see the ready
        testHelper.retryPeriodically {
            val pendingRequest = session1VerificationService.getExistingVerificationRequest(myUserId, requestID)
            pendingRequest?.state == EVerificationState.Ready
        }

        return requestID
    }
}
