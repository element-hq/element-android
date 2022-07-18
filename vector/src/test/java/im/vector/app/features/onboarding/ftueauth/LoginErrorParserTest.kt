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

package im.vector.app.features.onboarding.ftueauth

import im.vector.app.R
import im.vector.app.test.fakes.FakeErrorFormatter
import im.vector.app.test.fakes.FakeStringProvider
import im.vector.app.test.fakes.toTestString
import im.vector.app.test.fixtures.aHomeserverUnavailableError
import im.vector.app.test.fixtures.aLoginEmailUnknownError
import im.vector.app.test.fixtures.anInvalidPasswordError
import im.vector.app.test.fixtures.anInvalidUserNameError
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

private const val A_VALID_PASSWORD = "11111111"
private const val A_FORMATTED_ERROR_MESSAGE = "error message"
private const val ANOTHER_FORMATTED_ERROR_MESSAGE = "error message 2"
private val AN_ERROR = RuntimeException()

class LoginErrorParserTest {

    private val fakeErrorFormatter = FakeErrorFormatter()
    private val fakeStringProvider = FakeStringProvider()

    private val loginErrorParser = LoginErrorParser(fakeErrorFormatter, fakeStringProvider.instance)

    @Test
    fun `given a generic error, when parsing, then has null username and password errors`() {
        val cause = RuntimeException()

        val result = loginErrorParser.parse(throwable = cause, password = A_VALID_PASSWORD)

        result shouldBeEqualTo LoginErrorParser.LoginErrorResult(cause, usernameOrIdError = null, passwordError = null)
    }

    @Test
    fun `given an invalid username error, when parsing, then has username error`() {
        val cause = anInvalidUserNameError()
        fakeErrorFormatter.given(cause, formatsTo = A_FORMATTED_ERROR_MESSAGE)

        val result = loginErrorParser.parse(throwable = cause, password = A_VALID_PASSWORD)

        result shouldBeEqualTo LoginErrorParser.LoginErrorResult(
                cause,
                usernameOrIdError = A_FORMATTED_ERROR_MESSAGE,
                passwordError = null
        )
    }

    @Test
    fun `given a homeserver unavailable error, when parsing, then has username error`() {
        val cause = aHomeserverUnavailableError()

        val result = loginErrorParser.parse(throwable = cause, password = A_VALID_PASSWORD)

        result shouldBeEqualTo LoginErrorParser.LoginErrorResult(
                cause,
                usernameOrIdError = R.string.login_error_homeserver_not_found.toTestString(),
                passwordError = null
        )
    }

    @Test
    fun `given a login email unknown error, when parsing, then has username error`() {
        val cause = aLoginEmailUnknownError()

        val result = loginErrorParser.parse(throwable = cause, password = A_VALID_PASSWORD)

        result shouldBeEqualTo LoginErrorParser.LoginErrorResult(
                cause,
                usernameOrIdError = R.string.login_login_with_email_error.toTestString(),
                passwordError = null
        )
    }

    @Test
    fun `given a password with surrounding spaces and an invalid password error, when parsing, then has password error`() {
        val cause = anInvalidPasswordError()

        val result = loginErrorParser.parse(throwable = cause, password = " $A_VALID_PASSWORD ")

        result shouldBeEqualTo LoginErrorParser.LoginErrorResult(
                cause,
                usernameOrIdError = null,
                passwordError = R.string.auth_invalid_login_param_space_in_password.toTestString()
        )
    }

    @Test
    fun `given an error result with no known errors, then is unknown`() {
        val errorResult = LoginErrorParser.LoginErrorResult(AN_ERROR, usernameOrIdError = null, passwordError = null)
        val captures = Captures(expectUnknownError = true)

        errorResult.callOnMethods(captures)

        captures.unknownResult shouldBeEqualTo AN_ERROR
    }

    @Test
    fun `given an error result with only username error, then is username or id error`() {
        val errorResult = LoginErrorParser.LoginErrorResult(AN_ERROR, usernameOrIdError = A_FORMATTED_ERROR_MESSAGE, passwordError = null)
        val captures = Captures(expectUsernameOrIdError = true)

        errorResult.callOnMethods(captures)

        captures.usernameOrIdError shouldBeEqualTo A_FORMATTED_ERROR_MESSAGE
    }

    @Test
    fun `given an error result with only password error, then is password error`() {
        val errorResult = LoginErrorParser.LoginErrorResult(AN_ERROR, usernameOrIdError = null, passwordError = A_FORMATTED_ERROR_MESSAGE)
        val captures = Captures(expectPasswordError = true)

        errorResult.callOnMethods(captures)

        captures.passwordError shouldBeEqualTo A_FORMATTED_ERROR_MESSAGE
    }

    @Test
    fun `given an error result with username and password error, then triggers both username and password error`() {
        val errorResult = LoginErrorParser.LoginErrorResult(
                AN_ERROR,
                usernameOrIdError = A_FORMATTED_ERROR_MESSAGE,
                passwordError = ANOTHER_FORMATTED_ERROR_MESSAGE
        )
        val captures = Captures(expectPasswordError = true, expectUsernameOrIdError = true)

        errorResult.callOnMethods(captures)

        captures.usernameOrIdError shouldBeEqualTo A_FORMATTED_ERROR_MESSAGE
        captures.passwordError shouldBeEqualTo ANOTHER_FORMATTED_ERROR_MESSAGE
    }
}

private fun LoginErrorParser.LoginErrorResult.callOnMethods(captures: Captures) {
    onUnknown(captures.onUnknown)
    onUsernameOrIdError(captures.onUsernameOrIdError)
    onPasswordError(captures.onPasswordError)
}

private class Captures(
        val expectUnknownError: Boolean = false,
        val expectUsernameOrIdError: Boolean = false,
        val expectPasswordError: Boolean = false,
) {
    var unknownResult: Throwable? = null
    var usernameOrIdError: String? = null
    var passwordError: String? = null

    val onUnknown: (Throwable) -> Unit = {
        if (expectUnknownError) unknownResult = it else throw IllegalStateException("Not expected to be called")
    }
    val onUsernameOrIdError: (String) -> Unit = {
        if (expectUsernameOrIdError) usernameOrIdError = it else throw IllegalStateException("Not expected to be called")
    }
    val onPasswordError: (String) -> Unit = {
        if (expectPasswordError) passwordError = it else throw IllegalStateException("Not expected to be called")
    }
}
