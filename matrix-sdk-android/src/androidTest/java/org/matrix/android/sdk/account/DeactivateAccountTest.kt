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

package org.matrix.android.sdk.account

import org.junit.Assert.assertTrue
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.MethodSorters
import org.matrix.android.sdk.InstrumentedTest
import org.matrix.android.sdk.api.auth.UIABaseAuth
import org.matrix.android.sdk.api.auth.UserInteractiveAuthInterceptor
import org.matrix.android.sdk.api.auth.UserPasswordAuth
import org.matrix.android.sdk.api.auth.registration.RegistrationFlowResponse
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.failure.MatrixError
import org.matrix.android.sdk.common.CommonTestHelper
import org.matrix.android.sdk.common.SessionTestParams
import org.matrix.android.sdk.common.TestConstants
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

@RunWith(JUnit4::class)
@FixMethodOrder(MethodSorters.JVM)
class DeactivateAccountTest : InstrumentedTest {

    private val commonTestHelper = CommonTestHelper(context())

    @Test
    fun deactivateAccountTest() {
        val session = commonTestHelper.createAccount(TestConstants.USER_ALICE, SessionTestParams(withInitialSync = true))

        // Deactivate the account
        commonTestHelper.runBlockingTest {
            session.deactivateAccount(
                    eraseAllData = false,
                    userInteractiveAuthInterceptor = object : UserInteractiveAuthInterceptor {
                        override fun performStage(flowResponse: RegistrationFlowResponse, errCode: String?, promise: Continuation<UIABaseAuth>) {
                            promise.resume(
                                    UserPasswordAuth(
                                            user = session.myUserId,
                                            password = TestConstants.PASSWORD,
                                            session = flowResponse.session
                                    )
                            )
                        }
                    }
            )
        }

        // Try to login on the previous account, it will fail (M_USER_DEACTIVATED)
        val throwable = commonTestHelper.logAccountWithError(session.myUserId, TestConstants.PASSWORD)

        // Test the error
        assertTrue(throwable is Failure.ServerError &&
                throwable.error.code == MatrixError.M_USER_DEACTIVATED &&
                throwable.error.message == "This account has been deactivated")

        // Try to create an account with the deactivate account user id, it will fail (M_USER_IN_USE)
        val hs = commonTestHelper.createHomeServerConfig()

        commonTestHelper.runBlockingTest {
            commonTestHelper.matrix.authenticationService.getLoginFlow(hs)
        }

        var accountCreationError: Throwable? = null
        commonTestHelper.runBlockingTest {
            try {
                commonTestHelper.matrix.authenticationService
                        .getRegistrationWizard()
                        .createAccount(
                                session.myUserId.substringAfter("@").substringBefore(":"),
                                TestConstants.PASSWORD,
                                null
                        )
            } catch (failure: Throwable) {
                accountCreationError = failure
            }
        }

        // Test the error
        accountCreationError.let {
            assertTrue(it is Failure.ServerError &&
                    it.error.code == MatrixError.M_USER_IN_USE)
        }

        // No need to close the session, it has been deactivated
    }
}
