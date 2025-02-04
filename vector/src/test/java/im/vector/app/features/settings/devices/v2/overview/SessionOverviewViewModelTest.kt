/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.overview

import android.os.SystemClock
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.test.MavericksTestRule
import im.vector.app.features.settings.devices.v2.DeviceFullInfo
import im.vector.app.features.settings.devices.v2.RefreshDevicesUseCase
import im.vector.app.features.settings.devices.v2.ToggleIpAddressVisibilityUseCase
import im.vector.app.features.settings.devices.v2.notification.NotificationsStatus
import im.vector.app.features.settings.devices.v2.verification.CheckIfCurrentSessionCanBeVerifiedUseCase
import im.vector.app.test.fakes.FakeActiveSessionHolder
import im.vector.app.test.fakes.FakeGetNotificationsStatusUseCase
import im.vector.app.test.fakes.FakePendingAuthHandler
import im.vector.app.test.fakes.FakeSignoutSessionsUseCase
import im.vector.app.test.fakes.FakeToggleNotificationUseCase
import im.vector.app.test.fakes.FakeVectorPreferences
import im.vector.app.test.fakes.FakeVerificationService
import im.vector.app.test.test
import im.vector.app.test.testDispatcher
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import io.mockk.verifyAll
import kotlinx.coroutines.flow.flowOf
import org.amshove.kluent.shouldBeEqualTo
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.matrix.android.sdk.api.session.crypto.model.RoomEncryptionTrustLevel
import org.matrix.android.sdk.api.session.homeserver.HomeServerCapabilities
import org.matrix.android.sdk.api.session.uia.DefaultBaseAuth

private const val A_SESSION_ID_1 = "session-id-1"
private const val A_SESSION_ID_2 = "session-id-2"
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
    private val checkIfCurrentSessionCanBeVerifiedUseCase = mockk<CheckIfCurrentSessionCanBeVerifiedUseCase>()
    private val fakeSignoutSessionsUseCase = FakeSignoutSessionsUseCase()
    private val fakePendingAuthHandler = FakePendingAuthHandler()
    private val refreshDevicesUseCase = mockk<RefreshDevicesUseCase>(relaxed = true)
    private val toggleNotificationUseCase = FakeToggleNotificationUseCase()
    private val fakeGetNotificationsStatusUseCase = FakeGetNotificationsStatusUseCase()
    private val notificationsStatus = NotificationsStatus.ENABLED
    private val fakeVectorPreferences = FakeVectorPreferences()
    private val toggleIpAddressVisibilityUseCase = mockk<ToggleIpAddressVisibilityUseCase>()

    private fun createViewModel() = SessionOverviewViewModel(
            initialState = SessionOverviewViewState(args),
            getDeviceFullInfoUseCase = getDeviceFullInfoUseCase,
            checkIfCurrentSessionCanBeVerifiedUseCase = checkIfCurrentSessionCanBeVerifiedUseCase,
            signoutSessionsUseCase = fakeSignoutSessionsUseCase.instance,
            pendingAuthHandler = fakePendingAuthHandler.instance,
            activeSessionHolder = fakeActiveSessionHolder.instance,
            refreshDevicesUseCase = refreshDevicesUseCase,
            toggleNotificationsUseCase = toggleNotificationUseCase.instance,
            getNotificationsStatusUseCase = fakeGetNotificationsStatusUseCase.instance,
            vectorPreferences = fakeVectorPreferences.instance,
            toggleIpAddressVisibilityUseCase = toggleIpAddressVisibilityUseCase,
    )

    @Before
    fun setup() {
        // Needed for internal usage of Flow<T>.throttleFirst() inside the ViewModel
        mockkStatic(SystemClock::class)
        every { SystemClock.elapsedRealtime() } returns 1234

        fakeActiveSessionHolder.fakeSession.fakeHomeServerCapabilitiesService.givenCapabilities(
                HomeServerCapabilities()
        )
        givenVerificationService()
        fakeGetNotificationsStatusUseCase.givenExecuteReturns(
                fakeActiveSessionHolder.fakeSession,
                A_SESSION_ID_1,
                notificationsStatus
        )
        fakeVectorPreferences.givenSessionManagerShowIpAddress(false)
    }

    private fun givenVerificationService(): FakeVerificationService {
        val fakeVerificationService = fakeActiveSessionHolder
                .fakeSession
                .fakeCryptoService
                .fakeVerificationService
        fakeVerificationService.givenEventFlow()
        return fakeVerificationService
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given the viewModel when initializing it then verification listener is added`() {
        // Given
        val fakeVerificationService = givenVerificationService()

        // When
        createViewModel()

        // Then
        verify {
            fakeVerificationService.requestEventFlow()
        }
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
                notificationsStatus = notificationsStatus,
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
        fakeSignoutSessionsUseCase.givenSignoutSuccess(listOf(A_SESSION_ID_1))
        val signoutAction = SessionOverviewAction.SignoutOtherSession
        givenCurrentSessionIsTrusted()
        val expectedViewState = SessionOverviewViewState(
                deviceId = A_SESSION_ID_1,
                isCurrentSessionTrusted = true,
                deviceInfo = Success(deviceFullInfo),
                isLoading = false,
                notificationsStatus = notificationsStatus,
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
        coVerify {
            refreshDevicesUseCase.execute()
        }
    }

    @Test
    fun `given another session and unexpected error during signout when handling signout action then signout process is performed`() {
        // Given
        val deviceFullInfo = mockk<DeviceFullInfo>()
        every { deviceFullInfo.isCurrentDevice } returns false
        every { getDeviceFullInfoUseCase.execute(A_SESSION_ID_1) } returns flowOf(deviceFullInfo)
        val error = Exception()
        fakeSignoutSessionsUseCase.givenSignoutError(listOf(A_SESSION_ID_1), error)
        val signoutAction = SessionOverviewAction.SignoutOtherSession
        givenCurrentSessionIsTrusted()
        val expectedViewState = SessionOverviewViewState(
                deviceId = A_SESSION_ID_1,
                isCurrentSessionTrusted = true,
                deviceInfo = Success(deviceFullInfo),
                isLoading = false,
                notificationsStatus = notificationsStatus,
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
                .assertEvent { it is SessionOverviewViewEvent.SignoutError && it.error == error }
                .finish()
    }

    @Test
    fun `given another session and reAuth is needed during signout when handling signout action then requestReAuth is sent and pending auth is stored`() {
        // Given
        val deviceFullInfo = mockk<DeviceFullInfo>()
        every { deviceFullInfo.isCurrentDevice } returns false
        every { getDeviceFullInfoUseCase.execute(A_SESSION_ID_1) } returns flowOf(deviceFullInfo)
        val reAuthNeeded = fakeSignoutSessionsUseCase.givenSignoutReAuthNeeded(listOf(A_SESSION_ID_1))
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

    private fun givenCurrentSessionIsTrusted() {
        fakeActiveSessionHolder.fakeSession.givenSessionId(A_SESSION_ID_2)
        val deviceFullInfo = mockk<DeviceFullInfo>()
        every { deviceFullInfo.roomEncryptionTrustLevel } returns RoomEncryptionTrustLevel.Trusted
        every { getDeviceFullInfoUseCase.execute(A_SESSION_ID_2) } returns flowOf(deviceFullInfo)
    }

    @Test
    fun `when viewModel init, then observe pushers and emit to state`() {
        val viewModel = createViewModel()

        viewModel.test()
                .assertLatestState { state -> state.notificationsStatus == notificationsStatus }
                .finish()
    }

    @Test
    fun `when handle TogglePushNotifications, then execute use case and update state`() {
        val viewModel = createViewModel()

        viewModel.handle(SessionOverviewAction.TogglePushNotifications(A_SESSION_ID_1, true))

        toggleNotificationUseCase.verifyExecute(A_SESSION_ID_1, true)
        viewModel.test().assertLatestState { state -> state.notificationsStatus == NotificationsStatus.ENABLED }.finish()
    }
}
