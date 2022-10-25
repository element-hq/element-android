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

package im.vector.app.features.settings.devices.v2.overview

import android.os.SystemClock
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.test.MavericksTestRule
import im.vector.app.R
import im.vector.app.features.settings.devices.v2.DeviceFullInfo
import im.vector.app.features.settings.devices.v2.RefreshDevicesUseCase
import im.vector.app.features.settings.devices.v2.signout.InterceptSignoutFlowResponseUseCase
import im.vector.app.features.settings.devices.v2.signout.SignoutSessionResult
import im.vector.app.features.settings.devices.v2.signout.SignoutSessionUseCase
import im.vector.app.features.settings.devices.v2.verification.CheckIfCurrentSessionCanBeVerifiedUseCase
import im.vector.app.test.fakes.FakeActiveSessionHolder
import im.vector.app.test.fakes.FakePendingAuthHandler
import im.vector.app.test.fakes.FakeStringProvider
import im.vector.app.test.fakes.FakeTogglePushNotificationUseCase
import im.vector.app.test.fakes.FakeVerificationService
import im.vector.app.test.fixtures.PusherFixture.aPusher
import im.vector.app.test.test
import im.vector.app.test.testDispatcher
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import io.mockk.verifyAll
import kotlinx.coroutines.flow.flowOf
import org.amshove.kluent.shouldBeEqualTo
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.matrix.android.sdk.api.auth.UIABaseAuth
import org.matrix.android.sdk.api.auth.UserInteractiveAuthInterceptor
import org.matrix.android.sdk.api.auth.registration.RegistrationFlowResponse
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.session.crypto.model.RoomEncryptionTrustLevel
import org.matrix.android.sdk.api.session.uia.DefaultBaseAuth
import javax.net.ssl.HttpsURLConnection
import kotlin.coroutines.Continuation

private const val A_SESSION_ID_1 = "session-id-1"
private const val A_SESSION_ID_2 = "session-id-2"
private const val AUTH_ERROR_MESSAGE = "auth-error-message"
private const val AN_ERROR_MESSAGE = "error-message"
private const val A_PASSWORD = "password"

class SessionOverviewViewModelTest {

    @get:Rule
    val mavericksTestRule = MavericksTestRule(testDispatcher = testDispatcher)

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val args = SessionOverviewArgs(
            deviceId = A_SESSION_ID_1
    )
    private val getDeviceFullInfoUseCase = mockk<GetDeviceFullInfoUseCase>(relaxed = true)
    private val fakeActiveSessionHolder = FakeActiveSessionHolder()
    private val fakeStringProvider = FakeStringProvider()
    private val checkIfCurrentSessionCanBeVerifiedUseCase = mockk<CheckIfCurrentSessionCanBeVerifiedUseCase>()
    private val signoutSessionUseCase = mockk<SignoutSessionUseCase>()
    private val interceptSignoutFlowResponseUseCase = mockk<InterceptSignoutFlowResponseUseCase>()
    private val fakePendingAuthHandler = FakePendingAuthHandler()
    private val refreshDevicesUseCase = mockk<RefreshDevicesUseCase>()
    private val togglePushNotificationUseCase = FakeTogglePushNotificationUseCase()

    private fun createViewModel() = SessionOverviewViewModel(
            initialState = SessionOverviewViewState(args),
            stringProvider = fakeStringProvider.instance,
            getDeviceFullInfoUseCase = getDeviceFullInfoUseCase,
            checkIfCurrentSessionCanBeVerifiedUseCase = checkIfCurrentSessionCanBeVerifiedUseCase,
            signoutSessionUseCase = signoutSessionUseCase,
            interceptSignoutFlowResponseUseCase = interceptSignoutFlowResponseUseCase,
            pendingAuthHandler = fakePendingAuthHandler.instance,
            activeSessionHolder = fakeActiveSessionHolder.instance,
            refreshDevicesUseCase = refreshDevicesUseCase,
            togglePushNotificationUseCase = togglePushNotificationUseCase.instance,
    )

    @Before
    fun setup() {
        // Needed for internal usage of Flow<T>.throttleFirst() inside the ViewModel
        mockkStatic(SystemClock::class)
        every { SystemClock.elapsedRealtime() } returns 1234

        givenVerificationService()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given the viewModel has been initialized then pushers are refreshed`() {
        createViewModel()

        fakeActiveSessionHolder.fakeSession.pushersService().verifyRefreshPushers()
    }

    @Test
    fun `given the viewModel has been initialized then viewState is updated with session info`() {
        val deviceFullInfo = mockk<DeviceFullInfo>()
        every { getDeviceFullInfoUseCase.execute(A_SESSION_ID_1) } returns flowOf(deviceFullInfo)
        givenCurrentSessionIsTrusted()
        val expectedState = SessionOverviewViewState(
                deviceId = A_SESSION_ID_1,
                deviceInfo = Success(deviceFullInfo),
                isCurrentSessionTrusted = true,
                notificationsEnabled = true,
        )

        val viewModel = createViewModel()

        viewModel.test()
                .assertLatestState { state -> state == expectedState }
                .finish()
        verify {
            getDeviceFullInfoUseCase.execute(A_SESSION_ID_1)
        }
    }

    @Test
    fun `given current session can be verified when handling verify current session action then self verification event is posted`() {
        // Given
        val deviceFullInfo = mockk<DeviceFullInfo>()
        every { deviceFullInfo.isCurrentDevice } returns true
        every { getDeviceFullInfoUseCase.execute(A_SESSION_ID_1) } returns flowOf(deviceFullInfo)
        val verifySessionAction = SessionOverviewAction.VerifySession
        coEvery { checkIfCurrentSessionCanBeVerifiedUseCase.execute() } returns true
        givenCurrentSessionIsTrusted()

        // When
        val viewModel = createViewModel()
        val viewModelTest = viewModel.test()
        viewModel.handle(verifySessionAction)

        // Then
        viewModelTest
                .assertEvent { it is SessionOverviewViewEvent.ShowVerifyCurrentSession }
                .finish()
        coVerify {
            checkIfCurrentSessionCanBeVerifiedUseCase.execute()
        }
    }

    @Test
    fun `given current session cannot be verified when handling verify current session action then reset secrets event is posted`() {
        // Given
        val deviceFullInfo = mockk<DeviceFullInfo>()
        every { deviceFullInfo.isCurrentDevice } returns true
        every { getDeviceFullInfoUseCase.execute(A_SESSION_ID_1) } returns flowOf(deviceFullInfo)
        val verifySessionAction = SessionOverviewAction.VerifySession
        coEvery { checkIfCurrentSessionCanBeVerifiedUseCase.execute() } returns false
        givenCurrentSessionIsTrusted()

        // When
        val viewModel = createViewModel()
        val viewModelTest = viewModel.test()
        viewModel.handle(verifySessionAction)

        // Then
        viewModelTest
                .assertEvent { it is SessionOverviewViewEvent.PromptResetSecrets }
                .finish()
        coVerify {
            checkIfCurrentSessionCanBeVerifiedUseCase.execute()
        }
    }

    @Test
    fun `given another session when handling verify session action then verify session event is posted`() {
        // Given
        val deviceFullInfo = mockk<DeviceFullInfo>()
        every { deviceFullInfo.isCurrentDevice } returns false
        every { getDeviceFullInfoUseCase.execute(A_SESSION_ID_1) } returns flowOf(deviceFullInfo)
        val verifySessionAction = SessionOverviewAction.VerifySession
        givenCurrentSessionIsTrusted()

        // When
        val viewModel = createViewModel()
        val viewModelTest = viewModel.test()
        viewModel.handle(verifySessionAction)

        // Then
        viewModelTest
                .assertEvent { it is SessionOverviewViewEvent.ShowVerifyOtherSession }
                .finish()
    }

    @Test
    fun `given another session and no reAuth is needed when handling signout action then signout process is performed`() {
        // Given
        val deviceFullInfo = mockk<DeviceFullInfo>()
        every { deviceFullInfo.isCurrentDevice } returns false
        every { getDeviceFullInfoUseCase.execute(A_SESSION_ID_1) } returns flowOf(deviceFullInfo)
        givenSignoutSuccess(A_SESSION_ID_1)
        every { refreshDevicesUseCase.execute() } just runs
        val signoutAction = SessionOverviewAction.SignoutOtherSession
        givenCurrentSessionIsTrusted()
        val expectedViewState = SessionOverviewViewState(
                deviceId = A_SESSION_ID_1,
                isCurrentSessionTrusted = true,
                deviceInfo = Success(deviceFullInfo),
                isLoading = false,
                notificationsEnabled = true,
        )

        // When
        val viewModel = createViewModel()
        val viewModelTest = viewModel.test()
        viewModel.handle(signoutAction)

        // Then
        viewModelTest
                .assertStatesChanges(
                        expectedViewState,
                        { copy(isLoading = true) },
                        { copy(isLoading = false) }
                )
                .assertEvent { it is SessionOverviewViewEvent.SignoutSuccess }
                .finish()
        verify {
            refreshDevicesUseCase.execute()
        }
    }

    @Test
    fun `given another session and server error during signout when handling signout action then signout process is performed`() {
        // Given
        val deviceFullInfo = mockk<DeviceFullInfo>()
        every { deviceFullInfo.isCurrentDevice } returns false
        every { getDeviceFullInfoUseCase.execute(A_SESSION_ID_1) } returns flowOf(deviceFullInfo)
        val serverError = Failure.OtherServerError(errorBody = "", httpCode = HttpsURLConnection.HTTP_UNAUTHORIZED)
        givenSignoutError(A_SESSION_ID_1, serverError)
        val signoutAction = SessionOverviewAction.SignoutOtherSession
        givenCurrentSessionIsTrusted()
        val expectedViewState = SessionOverviewViewState(
                deviceId = A_SESSION_ID_1,
                isCurrentSessionTrusted = true,
                deviceInfo = Success(deviceFullInfo),
                isLoading = false,
                notificationsEnabled = true,
        )
        fakeStringProvider.given(R.string.authentication_error, AUTH_ERROR_MESSAGE)

        // When
        val viewModel = createViewModel()
        val viewModelTest = viewModel.test()
        viewModel.handle(signoutAction)

        // Then
        viewModelTest
                .assertStatesChanges(
                        expectedViewState,
                        { copy(isLoading = true) },
                        { copy(isLoading = false) }
                )
                .assertEvent { it is SessionOverviewViewEvent.SignoutError && it.error.message == AUTH_ERROR_MESSAGE }
                .finish()
    }

    @Test
    fun `given another session and unexpected error during signout when handling signout action then signout process is performed`() {
        // Given
        val deviceFullInfo = mockk<DeviceFullInfo>()
        every { deviceFullInfo.isCurrentDevice } returns false
        every { getDeviceFullInfoUseCase.execute(A_SESSION_ID_1) } returns flowOf(deviceFullInfo)
        val error = Exception()
        givenSignoutError(A_SESSION_ID_1, error)
        val signoutAction = SessionOverviewAction.SignoutOtherSession
        givenCurrentSessionIsTrusted()
        val expectedViewState = SessionOverviewViewState(
                deviceId = A_SESSION_ID_1,
                isCurrentSessionTrusted = true,
                deviceInfo = Success(deviceFullInfo),
                isLoading = false,
                notificationsEnabled = true,
        )
        fakeStringProvider.given(R.string.matrix_error, AN_ERROR_MESSAGE)

        // When
        val viewModel = createViewModel()
        val viewModelTest = viewModel.test()
        viewModel.handle(signoutAction)

        // Then
        viewModelTest
                .assertStatesChanges(
                        expectedViewState,
                        { copy(isLoading = true) },
                        { copy(isLoading = false) }
                )
                .assertEvent { it is SessionOverviewViewEvent.SignoutError && it.error.message == AN_ERROR_MESSAGE }
                .finish()
    }

    @Test
    fun `given another session and reAuth is needed during signout when handling signout action then requestReAuth is sent and pending auth is stored`() {
        // Given
        val deviceFullInfo = mockk<DeviceFullInfo>()
        every { deviceFullInfo.isCurrentDevice } returns false
        every { getDeviceFullInfoUseCase.execute(A_SESSION_ID_1) } returns flowOf(deviceFullInfo)
        val reAuthNeeded = givenSignoutReAuthNeeded(A_SESSION_ID_1)
        val signoutAction = SessionOverviewAction.SignoutOtherSession
        givenCurrentSessionIsTrusted()
        val expectedPendingAuth = DefaultBaseAuth(session = reAuthNeeded.flowResponse.session)
        val expectedReAuthEvent = SessionOverviewViewEvent.RequestReAuth(reAuthNeeded.flowResponse, reAuthNeeded.errCode)

        // When
        val viewModel = createViewModel()
        val viewModelTest = viewModel.test()
        viewModel.handle(signoutAction)

        // Then
        viewModelTest
                .assertEvent { it == expectedReAuthEvent }
                .finish()
        fakePendingAuthHandler.instance.pendingAuth shouldBeEqualTo expectedPendingAuth
        fakePendingAuthHandler.instance.uiaContinuation shouldBeEqualTo reAuthNeeded.uiaContinuation
    }

    @Test
    fun `given SSO auth has been done when handling ssoAuthDone action then corresponding method of pending auth handler is called`() {
        // Given
        val deviceFullInfo = mockk<DeviceFullInfo>()
        every { deviceFullInfo.isCurrentDevice } returns false
        every { getDeviceFullInfoUseCase.execute(A_SESSION_ID_1) } returns flowOf(deviceFullInfo)
        val action = SessionOverviewAction.SsoAuthDone
        givenCurrentSessionIsTrusted()
        every { fakePendingAuthHandler.instance.ssoAuthDone() } just runs

        // When
        val viewModel = createViewModel()
        val viewModelTest = viewModel.test()
        viewModel.handle(action)

        // Then
        viewModelTest.finish()
        verifyAll {
            fakePendingAuthHandler.instance.ssoAuthDone()
        }
    }

    @Test
    fun `given password auth has been done when handling passwordAuthDone action then corresponding method of pending auth handler is called`() {
        // Given
        val deviceFullInfo = mockk<DeviceFullInfo>()
        every { deviceFullInfo.isCurrentDevice } returns false
        every { getDeviceFullInfoUseCase.execute(A_SESSION_ID_1) } returns flowOf(deviceFullInfo)
        val action = SessionOverviewAction.PasswordAuthDone(password = A_PASSWORD)
        givenCurrentSessionIsTrusted()
        every { fakePendingAuthHandler.instance.passwordAuthDone(any()) } just runs

        // When
        val viewModel = createViewModel()
        val viewModelTest = viewModel.test()
        viewModel.handle(action)

        // Then
        viewModelTest.finish()
        verifyAll {
            fakePendingAuthHandler.instance.passwordAuthDone(A_PASSWORD)
        }
    }

    @Test
    fun `given reAuth has been cancelled when handling reAuthCancelled action then corresponding method of pending auth handler is called`() {
        // Given
        val deviceFullInfo = mockk<DeviceFullInfo>()
        every { deviceFullInfo.isCurrentDevice } returns false
        every { getDeviceFullInfoUseCase.execute(A_SESSION_ID_1) } returns flowOf(deviceFullInfo)
        val action = SessionOverviewAction.ReAuthCancelled
        givenCurrentSessionIsTrusted()
        every { fakePendingAuthHandler.instance.reAuthCancelled() } just runs

        // When
        val viewModel = createViewModel()
        val viewModelTest = viewModel.test()
        viewModel.handle(action)

        // Then
        viewModelTest.finish()
        verifyAll {
            fakePendingAuthHandler.instance.reAuthCancelled()
        }
    }

    private fun givenSignoutSuccess(deviceId: String) {
        val interceptor = slot<UserInteractiveAuthInterceptor>()
        val flowResponse = mockk<RegistrationFlowResponse>()
        val errorCode = "errorCode"
        val promise = mockk<Continuation<UIABaseAuth>>()
        every { interceptSignoutFlowResponseUseCase.execute(flowResponse, errorCode, promise) } returns SignoutSessionResult.Completed
        coEvery { signoutSessionUseCase.execute(deviceId, capture(interceptor)) } coAnswers {
            secondArg<UserInteractiveAuthInterceptor>().performStage(flowResponse, errorCode, promise)
            Result.success(Unit)
        }
    }

    private fun givenSignoutReAuthNeeded(deviceId: String): SignoutSessionResult.ReAuthNeeded {
        val interceptor = slot<UserInteractiveAuthInterceptor>()
        val flowResponse = mockk<RegistrationFlowResponse>()
        every { flowResponse.session } returns A_SESSION_ID_1
        val errorCode = "errorCode"
        val promise = mockk<Continuation<UIABaseAuth>>()
        val reAuthNeeded = SignoutSessionResult.ReAuthNeeded(
                pendingAuth = mockk(),
                uiaContinuation = promise,
                flowResponse = flowResponse,
                errCode = errorCode,
        )
        every { interceptSignoutFlowResponseUseCase.execute(flowResponse, errorCode, promise) } returns reAuthNeeded
        coEvery { signoutSessionUseCase.execute(deviceId, capture(interceptor)) } coAnswers {
            secondArg<UserInteractiveAuthInterceptor>().performStage(flowResponse, errorCode, promise)
            Result.success(Unit)
        }

        return reAuthNeeded
    }

    private fun givenSignoutError(deviceId: String, error: Throwable) {
        coEvery { signoutSessionUseCase.execute(deviceId, any()) } returns Result.failure(error)
    }

    private fun givenVerificationService(): FakeVerificationService {
        val fakeVerificationService = fakeActiveSessionHolder
                .fakeSession
                .fakeCryptoService
                .fakeVerificationService
        fakeVerificationService.givenAddListenerSucceeds()
        fakeVerificationService.givenRemoveListenerSucceeds()
        return fakeVerificationService
    }

    private fun givenCurrentSessionIsTrusted() {
        fakeActiveSessionHolder.fakeSession.givenSessionId(A_SESSION_ID_2)
        val deviceFullInfo = mockk<DeviceFullInfo>()
        every { deviceFullInfo.roomEncryptionTrustLevel } returns RoomEncryptionTrustLevel.Trusted
        every { getDeviceFullInfoUseCase.execute(A_SESSION_ID_2) } returns flowOf(deviceFullInfo)
    }

    @Test
    fun `when viewModel init, then observe pushers and emit to state`() {
        val pushers = listOf(aPusher(deviceId = A_SESSION_ID_1))
        fakeActiveSessionHolder.fakeSession.pushersService().givenPushersLive(pushers)

        val viewModel = createViewModel()

        viewModel.test()
                .assertLatestState { state -> state.notificationsEnabled }
                .finish()
    }

    @Test
    fun `when handle TogglePushNotifications, then execute use case and update state`() {
        val viewModel = createViewModel()

        viewModel.handle(SessionOverviewAction.TogglePushNotifications(A_SESSION_ID_1, true))

        togglePushNotificationUseCase.verifyExecute(A_SESSION_ID_1, true)
        viewModel.test().assertLatestState { state -> state.notificationsEnabled }.finish()
    }
}
