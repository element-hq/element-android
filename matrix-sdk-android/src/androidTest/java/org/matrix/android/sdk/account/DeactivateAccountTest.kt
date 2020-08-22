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

package org.matrix.android.sdk.account

import org.matrix.android.sdk.InstrumentedTest
import org.matrix.android.sdk.api.auth.data.LoginFlowResult
import org.matrix.android.sdk.api.auth.registration.RegistrationResult
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.failure.MatrixError
import org.matrix.android.sdk.common.CommonTestHelper
import org.matrix.android.sdk.common.SessionTestParams
import org.matrix.android.sdk.common.TestConstants
import org.matrix.android.sdk.common.TestMatrixCallback
import org.junit.Assert.assertTrue
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.MethodSorters

@RunWith(JUnit4::class)
@FixMethodOrder(MethodSorters.JVM)
class DeactivateAccountTest : InstrumentedTest {

    private val commonTestHelper = CommonTestHelper(context())

    @Test
    fun deactivateAccountTest() {
        val session = commonTestHelper.createAccount(TestConstants.USER_ALICE, SessionTestParams(withInitialSync = false))

        // Deactivate the account
        commonTestHelper.doSync<Unit> {
            session.deactivateAccount(TestConstants.PASSWORD, false, it)
        }

        // Try to login on the previous account, it will fail (M_USER_DEACTIVATED)
        val throwable = commonTestHelper.logAccountWithError(session.myUserId, TestConstants.PASSWORD)

        // Test the error
        assertTrue(throwable is Failure.ServerError
                && throwable.error.code == MatrixError.M_USER_DEACTIVATED
                && throwable.error.message == "This account has been deactivated")

        // Try to create an account with the deactivate account user id, it will fail (M_USER_IN_USE)
        val hs = commonTestHelper.createHomeServerConfig()

        commonTestHelper.doSync<LoginFlowResult> {
            commonTestHelper.matrix.authenticationService.getLoginFlow(hs, it)
        }

        var accountCreationError: Throwable? = null
        commonTestHelper.waitWithLatch {
            commonTestHelper.matrix.authenticationService
                    .getRegistrationWizard()
                    .createAccount(session.myUserId.substringAfter("@").substringBefore(":"),
                            TestConstants.PASSWORD,
                            null,
                            object : TestMatrixCallback<RegistrationResult>(it, false) {
                                override fun onFailure(failure: Throwable) {
                                    accountCreationError = failure
                                    super.onFailure(failure)
                                }
                            })
        }

        // Test the error
        accountCreationError.let {
            assertTrue(it is Failure.ServerError
                    && it.error.code == MatrixError.M_USER_IN_USE)
        }

        // No need to close the session, it has been deactivated
    }
}
