/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.auth

import android.util.Base64
import im.vector.app.test.fakes.FakeActiveSessionHolder
import im.vector.app.test.fakes.FakeMatrix
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import org.amshove.kluent.shouldBe
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.matrix.android.sdk.api.auth.UIABaseAuth
import org.matrix.android.sdk.api.auth.UserPasswordAuth
import org.matrix.android.sdk.api.session.uia.exceptions.UiaCancelledException
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val A_PASSWORD = "a-password"
private const val A_SESSION_ID = "session-id"

class PendingAuthHandlerTest {

    private val fakeMatrix = FakeMatrix()
    private val fakeActiveSessionHolder = FakeActiveSessionHolder()

    private val pendingAuthHandler = PendingAuthHandler(
            matrix = fakeMatrix.instance,
            activeSessionHolder = fakeActiveSessionHolder.instance,
    )

    @Before
    fun setUp() {
        mockkStatic(Base64::class)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given a pending auth and continuation when SSO auth is done then continuation is resumed with pending auth`() {
        // Given
        val pendingAuth = mockk<UIABaseAuth>()
        val continuation = mockk<Continuation<UIABaseAuth>>()
        every { continuation.resume(any()) } just runs
        pendingAuthHandler.pendingAuth = pendingAuth
        pendingAuthHandler.uiaContinuation = continuation

        // When
        pendingAuthHandler.ssoAuthDone()

        // Then
        verify { continuation.resume(pendingAuth) }
    }

    @Test
    @Ignore("Ignored due because of problem to mock the inline method continuation.resumeWithException")
    fun `given missing pending auth and continuation when SSO auth is done then continuation is resumed with error`() {
        // Given
        val pendingAuth = null
        val continuation = mockk<Continuation<UIABaseAuth>>()
        every { continuation.resumeWithException(any()) } just runs
        pendingAuthHandler.pendingAuth = pendingAuth
        pendingAuthHandler.uiaContinuation = continuation

        // When
        pendingAuthHandler.ssoAuthDone()

        // Then
        verify { continuation.resumeWithException(match { it is IllegalArgumentException }) }
    }

    @Test
    fun `given a password, pending auth and continuation when password auth is done then continuation is resumed with correct auth`() {
        // Given
        val pendingAuth = mockk<UIABaseAuth>()
        every { pendingAuth.session } returns A_SESSION_ID
        val continuation = mockk<Continuation<UIABaseAuth>>()
        every { continuation.resume(any()) } just runs
        pendingAuthHandler.pendingAuth = pendingAuth
        pendingAuthHandler.uiaContinuation = continuation
        val decryptedPwd = "decrypted-pwd"
        val decodedPwd = byteArrayOf()
        every { Base64.decode(A_PASSWORD, any()) } returns decodedPwd
        fakeMatrix.fakeSecureStorageService.givenLoadSecureSecretReturns(decryptedPwd)
        val expectedAuth = UserPasswordAuth(
                session = A_SESSION_ID,
                password = decryptedPwd,
                user = fakeActiveSessionHolder.fakeSession.myUserId
        )

        // When
        pendingAuthHandler.passwordAuthDone(A_PASSWORD)

        // Then
        verify {
            fakeMatrix.fakeSecureStorageService.loadSecureSecret<String>(any(), ReAuthActivity.DEFAULT_RESULT_KEYSTORE_ALIAS)
            continuation.resume(expectedAuth)
        }
    }

    @Test
    @Ignore("Ignored because of problem to mock the inline method continuation.resumeWithException")
    fun `given pending auth and continuation when reAuth is cancelled then pending auth and continuation are reset`() {
        // Given
        val pendingAuth = mockk<UIABaseAuth>()
        val continuation = mockk<Continuation<UIABaseAuth>>()
        every { continuation.resumeWithException(any()) } just runs
        pendingAuthHandler.pendingAuth = pendingAuth
        pendingAuthHandler.uiaContinuation = continuation

        // When
        pendingAuthHandler.reAuthCancelled()

        // Then
        pendingAuthHandler.pendingAuth shouldBe null
        pendingAuthHandler.uiaContinuation shouldBe null
        verify { continuation.resumeWithException(match { it is UiaCancelledException }) }
    }
}
