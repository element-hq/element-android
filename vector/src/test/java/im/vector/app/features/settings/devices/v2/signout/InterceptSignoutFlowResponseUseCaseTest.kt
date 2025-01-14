/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.signout

import im.vector.app.test.fakes.FakeActiveSessionHolder
import im.vector.app.test.fakes.FakeReAuthHelper
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.matrix.android.sdk.api.auth.UIABaseAuth
import org.matrix.android.sdk.api.auth.UserPasswordAuth
import org.matrix.android.sdk.api.auth.data.LoginFlowTypes
import org.matrix.android.sdk.api.auth.registration.RegistrationFlowResponse
import org.matrix.android.sdk.api.auth.registration.nextUncompletedStage
import org.matrix.android.sdk.api.session.uia.DefaultBaseAuth
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

private const val A_PASSWORD = "password"
private const val A_SESSION_ID = "session-id"
private const val AN_ERROR_CODE = "error-code"

class InterceptSignoutFlowResponseUseCaseTest {

    private val fakeReAuthHelper = FakeReAuthHelper()
    private val fakeActiveSessionHolder = FakeActiveSessionHolder()

    private val interceptSignoutFlowResponseUseCase = InterceptSignoutFlowResponseUseCase(
            reAuthHelper = fakeReAuthHelper.instance,
            activeSessionHolder = fakeActiveSessionHolder.instance,
    )

    @Before
    fun setUp() {
        mockkStatic("org.matrix.android.sdk.api.auth.registration.RegistrationFlowResponseKt")
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given no error and a stored password and a next stage as password when intercepting then promise is resumed and null is returned`() {
        // Given
        val registrationFlowResponse = givenNextUncompletedStage(LoginFlowTypes.PASSWORD, A_SESSION_ID)
        fakeReAuthHelper.givenStoredPassword(A_PASSWORD)
        val errorCode: String? = null
        val promise = mockk<Continuation<UIABaseAuth>>()
        every { promise.resume(any()) } just runs
        val expectedAuth = UserPasswordAuth(
                session = null,
                user = fakeActiveSessionHolder.fakeSession.myUserId,
                password = A_PASSWORD,
        )

        // When
        val result = interceptSignoutFlowResponseUseCase.execute(
                flowResponse = registrationFlowResponse,
                errCode = errorCode,
                promise = promise,
        )

        // Then
        result shouldBe null
        every {
            promise.resume(expectedAuth)
        }
    }

    @Test
    fun `given an error when intercepting then reAuthNeeded result is returned`() {
        // Given
        val registrationFlowResponse = givenNextUncompletedStage(LoginFlowTypes.PASSWORD, A_SESSION_ID)
        fakeReAuthHelper.givenStoredPassword(A_PASSWORD)
        val errorCode = AN_ERROR_CODE
        val promise = mockk<Continuation<UIABaseAuth>>()
        val expectedResult = SignoutSessionsReAuthNeeded(
                pendingAuth = DefaultBaseAuth(session = A_SESSION_ID),
                uiaContinuation = promise,
                flowResponse = registrationFlowResponse,
                errCode = errorCode
        )

        // When
        val result = interceptSignoutFlowResponseUseCase.execute(
                flowResponse = registrationFlowResponse,
                errCode = errorCode,
                promise = promise,
        )

        // Then
        result shouldBeEqualTo expectedResult
    }

    @Test
    fun `given next stage is not password when intercepting then reAuthNeeded result is returned`() {
        // Given
        val registrationFlowResponse = givenNextUncompletedStage(LoginFlowTypes.SSO, A_SESSION_ID)
        fakeReAuthHelper.givenStoredPassword(A_PASSWORD)
        val errorCode: String? = null
        val promise = mockk<Continuation<UIABaseAuth>>()
        val expectedResult = SignoutSessionsReAuthNeeded(
                pendingAuth = DefaultBaseAuth(session = A_SESSION_ID),
                uiaContinuation = promise,
                flowResponse = registrationFlowResponse,
                errCode = errorCode
        )

        // When
        val result = interceptSignoutFlowResponseUseCase.execute(
                flowResponse = registrationFlowResponse,
                errCode = errorCode,
                promise = promise,
        )

        // Then
        result shouldBeEqualTo expectedResult
    }

    @Test
    fun `given no existing stored password when intercepting then reAuthNeeded result is returned`() {
        // Given
        val registrationFlowResponse = givenNextUncompletedStage(LoginFlowTypes.PASSWORD, A_SESSION_ID)
        fakeReAuthHelper.givenStoredPassword(null)
        val errorCode: String? = null
        val promise = mockk<Continuation<UIABaseAuth>>()
        val expectedResult = SignoutSessionsReAuthNeeded(
                pendingAuth = DefaultBaseAuth(session = A_SESSION_ID),
                uiaContinuation = promise,
                flowResponse = registrationFlowResponse,
                errCode = errorCode
        )

        // When
        val result = interceptSignoutFlowResponseUseCase.execute(
                flowResponse = registrationFlowResponse,
                errCode = errorCode,
                promise = promise,
        )

        // Then
        result shouldBeEqualTo expectedResult
    }

    private fun givenNextUncompletedStage(nextStage: String, sessionId: String): RegistrationFlowResponse {
        val registrationFlowResponse = mockk<RegistrationFlowResponse>()
        every { registrationFlowResponse.nextUncompletedStage() } returns nextStage
        every { registrationFlowResponse.session } returns sessionId
        return registrationFlowResponse
    }
}
