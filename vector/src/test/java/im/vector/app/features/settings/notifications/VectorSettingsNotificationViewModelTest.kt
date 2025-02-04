/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.notifications

import com.airbnb.mvrx.test.MavericksTestRule
import im.vector.app.core.platform.VectorDummyViewState
import im.vector.app.core.pushers.EnsureFcmTokenIsRetrievedUseCase
import im.vector.app.core.pushers.RegisterUnifiedPushUseCase
import im.vector.app.core.pushers.UnregisterUnifiedPushUseCase
import im.vector.app.features.settings.VectorPreferences.Companion.SETTINGS_ENABLE_THIS_DEVICE_PREFERENCE_KEY
import im.vector.app.features.settings.notifications.usecase.DisableNotificationsForCurrentSessionUseCase
import im.vector.app.features.settings.notifications.usecase.EnableNotificationsForCurrentSessionUseCase
import im.vector.app.features.settings.notifications.usecase.ToggleNotificationsForCurrentSessionUseCase
import im.vector.app.test.fakes.FakePushersManager
import im.vector.app.test.fakes.FakeVectorPreferences
import im.vector.app.test.test
import im.vector.app.test.testDispatcher
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.justRun
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test

class VectorSettingsNotificationViewModelTest {

    @get:Rule
    val mavericksTestRule = MavericksTestRule(testDispatcher = testDispatcher)

    private val fakePushersManager = FakePushersManager()
    private val fakeVectorPreferences = FakeVectorPreferences()
    private val fakeEnableNotificationsForCurrentSessionUseCase = mockk<EnableNotificationsForCurrentSessionUseCase>()
    private val fakeDisableNotificationsForCurrentSessionUseCase = mockk<DisableNotificationsForCurrentSessionUseCase>()
    private val fakeUnregisterUnifiedPushUseCase = mockk<UnregisterUnifiedPushUseCase>()
    private val fakeRegisterUnifiedPushUseCase = mockk<RegisterUnifiedPushUseCase>()
    private val fakeEnsureFcmTokenIsRetrievedUseCase = mockk<EnsureFcmTokenIsRetrievedUseCase>()
    private val fakeToggleNotificationsForCurrentSessionUseCase = mockk<ToggleNotificationsForCurrentSessionUseCase>()

    private fun createViewModel() = VectorSettingsNotificationViewModel(
            initialState = VectorDummyViewState(),
            pushersManager = fakePushersManager.instance,
            vectorPreferences = fakeVectorPreferences.instance,
            enableNotificationsForCurrentSessionUseCase = fakeEnableNotificationsForCurrentSessionUseCase,
            disableNotificationsForCurrentSessionUseCase = fakeDisableNotificationsForCurrentSessionUseCase,
            unregisterUnifiedPushUseCase = fakeUnregisterUnifiedPushUseCase,
            registerUnifiedPushUseCase = fakeRegisterUnifiedPushUseCase,
            ensureFcmTokenIsRetrievedUseCase = fakeEnsureFcmTokenIsRetrievedUseCase,
            toggleNotificationsForCurrentSessionUseCase = fakeToggleNotificationsForCurrentSessionUseCase,
    )

    @Test
    fun `given view model init when notifications are enabled in preferences then view event is posted`() {
        // Given
        fakeVectorPreferences.givenAreNotificationsEnabledForDevice(true)
        val expectedEvent = VectorSettingsNotificationViewEvent.NotificationsForDeviceEnabled
        val viewModel = createViewModel()

        // When
        val viewModelTest = viewModel.test()
        viewModel.notificationsPreferenceListener.onSharedPreferenceChanged(mockk(), SETTINGS_ENABLE_THIS_DEVICE_PREFERENCE_KEY)

        // Then
        viewModelTest
                .assertEvent { event -> event == expectedEvent }
                .finish()
    }

    @Test
    fun `given view model init when notifications are disabled in preferences then view event is posted`() {
        // Given
        fakeVectorPreferences.givenAreNotificationsEnabledForDevice(false)
        val expectedEvent = VectorSettingsNotificationViewEvent.NotificationsForDeviceDisabled
        val viewModel = createViewModel()

        // When
        val viewModelTest = viewModel.test()
        viewModel.notificationsPreferenceListener.onSharedPreferenceChanged(mockk(), SETTINGS_ENABLE_THIS_DEVICE_PREFERENCE_KEY)

        // Then
        viewModelTest
                .assertEvent { event -> event == expectedEvent }
                .finish()
    }

    @Test
    fun `given DisableNotificationsForDevice action when handling action then disable use case is called`() {
        // Given
        val viewModel = createViewModel()
        val action = VectorSettingsNotificationViewAction.DisableNotificationsForDevice
        coJustRun { fakeDisableNotificationsForCurrentSessionUseCase.execute() }
        val expectedEvent = VectorSettingsNotificationViewEvent.NotificationsForDeviceDisabled

        // When
        val viewModelTest = viewModel.test()
        viewModel.handle(action)

        // Then
        viewModelTest
                .assertEvent { event -> event == expectedEvent }
                .finish()
        coVerify {
            fakeDisableNotificationsForCurrentSessionUseCase.execute()
        }
    }

    @Test
    fun `given EnableNotificationsForDevice action and enable success when handling action then enable use case is called`() {
        // Given
        val viewModel = createViewModel()
        val aDistributor = "aDistributor"
        val action = VectorSettingsNotificationViewAction.EnableNotificationsForDevice(aDistributor)
        coEvery { fakeEnableNotificationsForCurrentSessionUseCase.execute(any()) } returns
                EnableNotificationsForCurrentSessionUseCase.EnableNotificationsResult.Success
        val expectedEvent = VectorSettingsNotificationViewEvent.NotificationsForDeviceEnabled

        // When
        val viewModelTest = viewModel.test()
        viewModel.handle(action)

        // Then
        viewModelTest
                .assertEvent { event -> event == expectedEvent }
                .finish()
        coVerify {
            fakeEnableNotificationsForCurrentSessionUseCase.execute(aDistributor)
        }
    }

    @Test
    fun `given EnableNotificationsForDevice action and enable needs user choice when handling action then enable use case is called`() {
        // Given
        val viewModel = createViewModel()
        val aDistributor = "aDistributor"
        val action = VectorSettingsNotificationViewAction.EnableNotificationsForDevice(aDistributor)
        coEvery { fakeEnableNotificationsForCurrentSessionUseCase.execute(any()) } returns
                EnableNotificationsForCurrentSessionUseCase.EnableNotificationsResult.NeedToAskUserForDistributor
        val expectedEvent = VectorSettingsNotificationViewEvent.AskUserForPushDistributor

        // When
        val viewModelTest = viewModel.test()
        viewModel.handle(action)

        // Then
        viewModelTest
                .assertEvent { event -> event == expectedEvent }
                .finish()
        coVerify {
            fakeEnableNotificationsForCurrentSessionUseCase.execute(aDistributor)
        }
    }

    @Test
    fun `given RegisterPushDistributor action and register success when handling action then register use case is called`() {
        // Given
        val viewModel = createViewModel()
        val aDistributor = "aDistributor"
        val action = VectorSettingsNotificationViewAction.RegisterPushDistributor(aDistributor)
        coEvery { fakeRegisterUnifiedPushUseCase.execute(any()) } returns RegisterUnifiedPushUseCase.RegisterUnifiedPushResult.Success
        coJustRun { fakeUnregisterUnifiedPushUseCase.execute(any()) }
        val areNotificationsEnabled = true
        fakeVectorPreferences.givenAreNotificationsEnabledForDevice(areNotificationsEnabled)
        coJustRun { fakeToggleNotificationsForCurrentSessionUseCase.execute(any()) }
        justRun { fakeEnsureFcmTokenIsRetrievedUseCase.execute(any(), any()) }
        val expectedEvent = VectorSettingsNotificationViewEvent.NotificationMethodChanged

        // When
        val viewModelTest = viewModel.test()
        viewModel.handle(action)

        // Then
        viewModelTest
                .assertEvent { event -> event == expectedEvent }
                .finish()
        coVerifyOrder {
            fakeUnregisterUnifiedPushUseCase.execute(fakePushersManager.instance)
            fakeRegisterUnifiedPushUseCase.execute(aDistributor)
            fakeEnsureFcmTokenIsRetrievedUseCase.execute(fakePushersManager.instance, registerPusher = areNotificationsEnabled)
            fakeToggleNotificationsForCurrentSessionUseCase.execute(enabled = areNotificationsEnabled)
        }
    }

    @Test
    fun `given RegisterPushDistributor action and register needs user choice when handling action then register use case is called`() {
        // Given
        val viewModel = createViewModel()
        val aDistributor = "aDistributor"
        val action = VectorSettingsNotificationViewAction.RegisterPushDistributor(aDistributor)
        coEvery { fakeRegisterUnifiedPushUseCase.execute(any()) } returns RegisterUnifiedPushUseCase.RegisterUnifiedPushResult.NeedToAskUserForDistributor
        coJustRun { fakeUnregisterUnifiedPushUseCase.execute(any()) }
        val expectedEvent = VectorSettingsNotificationViewEvent.AskUserForPushDistributor

        // When
        val viewModelTest = viewModel.test()
        viewModel.handle(action)

        // Then
        viewModelTest
                .assertEvent { event -> event == expectedEvent }
                .finish()
        coVerifyOrder {
            fakeUnregisterUnifiedPushUseCase.execute(fakePushersManager.instance)
            fakeRegisterUnifiedPushUseCase.execute(aDistributor)
        }
    }
}
