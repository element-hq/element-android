/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.test.fakes

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.matrix.android.sdk.api.auth.registration.RegisterThreePid
import org.matrix.android.sdk.api.auth.registration.RegistrationAvailability
import org.matrix.android.sdk.api.auth.registration.RegistrationResult
import org.matrix.android.sdk.api.auth.registration.RegistrationWizard
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.session.Session

class FakeRegistrationWizard : RegistrationWizard by mockk(relaxed = false) {

    fun givenSuccessFor(result: Session, expect: suspend RegistrationWizard.() -> RegistrationResult) {
        coEvery { expect(this@FakeRegistrationWizard) } returns RegistrationResult.Success(result)
    }

    fun givenAddThreePidErrors(cause: Throwable, threePid: RegisterThreePid) {
        coEvery { addThreePid(threePid) } throws cause
    }

    fun givenCheckIfEmailHasBeenValidatedErrors(errors: List<Throwable>, finally: RegistrationResult? = null) {
        var index = 0
        coEvery { checkIfEmailHasBeenValidated(any()) } answers {
            val current = index
            index++
            errors.getOrNull(current)?.let { throw it } ?: finally ?: throw RuntimeException("Developer error")
        }
    }

    fun givenRegistrationStarted(hasStarted: Boolean) {
        coEvery { isRegistrationStarted() } returns hasStarted
    }

    fun givenCurrentThreePid(threePid: String?) {
        coEvery { getCurrentThreePid() } returns threePid
    }

    fun givenUserNameIsAvailable(userName: String) {
        coEvery { registrationAvailable(userName) } returns RegistrationAvailability.Available
    }

    fun givenUserNameIsAvailableThrows(userName: String, cause: Throwable) {
        coEvery { registrationAvailable(userName) } throws cause
    }

    fun givenUserNameIsUnavailable(userName: String, failure: Failure.ServerError) {
        coEvery { registrationAvailable(userName) } returns RegistrationAvailability.NotAvailable(failure)
    }

    fun verifyCheckedEmailedVerification(times: Int) {
        coVerify(exactly = times) { checkIfEmailHasBeenValidated(any()) }
    }
}
