/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.notification

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import im.vector.app.test.fakes.FakeSession
import im.vector.app.test.fixtures.PusherFixture
import im.vector.app.test.testDispatcher
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.amshove.kluent.shouldBeEqualTo
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.matrix.android.sdk.api.account.LocalNotificationSettingsContent
import org.matrix.android.sdk.api.session.accountdata.UserAccountDataTypes
import org.matrix.android.sdk.api.session.events.model.toContent

private const val A_DEVICE_ID = "device-id"

class GetNotificationsStatusUseCaseTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val fakeSession = FakeSession()
    private val fakeCanToggleNotificationsViaAccountDataUseCase =
            mockk<CanToggleNotificationsViaAccountDataUseCase>()
    private val fakeCanToggleNotificationsViaPusherUseCase =
            mockk<CanToggleNotificationsViaPusherUseCase>()

    private val getNotificationsStatusUseCase =
            GetNotificationsStatusUseCase(
                    canToggleNotificationsViaAccountDataUseCase = fakeCanToggleNotificationsViaAccountDataUseCase,
                    canToggleNotificationsViaPusherUseCase = fakeCanToggleNotificationsViaPusherUseCase,
            )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `given current session and toggle is not supported when execute then resulting flow contains NOT_SUPPORTED value`() = runTest {
        // Given
        every { fakeCanToggleNotificationsViaAccountDataUseCase.execute(fakeSession, A_DEVICE_ID) } returns flowOf(false)
        every { fakeCanToggleNotificationsViaPusherUseCase.execute(fakeSession) } returns flowOf(false)

        // When
        val result = getNotificationsStatusUseCase.execute(fakeSession, A_DEVICE_ID)

        // Then
        result.firstOrNull() shouldBeEqualTo NotificationsStatus.NOT_SUPPORTED
        verifyOrder {
            // we should first check account data
            fakeCanToggleNotificationsViaAccountDataUseCase.execute(fakeSession, A_DEVICE_ID)
            fakeCanToggleNotificationsViaPusherUseCase.execute(fakeSession)
        }
    }

    @Test
    fun `given current session and toggle via pusher is supported when execute then resulting flow contains status based on pusher value`() = runTest {
        // Given
        val pushers = listOf(
                PusherFixture.aPusher(
                        deviceId = A_DEVICE_ID,
                        enabled = true,
                )
        )
        fakeSession.pushersService().givenPushersLive(pushers)
        every { fakeCanToggleNotificationsViaAccountDataUseCase.execute(fakeSession, A_DEVICE_ID) } returns flowOf(false)
        every { fakeCanToggleNotificationsViaPusherUseCase.execute(fakeSession) } returns flowOf(true)

        // When
        val result = getNotificationsStatusUseCase.execute(fakeSession, A_DEVICE_ID)

        // Then
        result.firstOrNull() shouldBeEqualTo NotificationsStatus.ENABLED
    }

    @Test
    fun `given toggle via pusher is supported and no registered pusher when execute then resulting flow contains NOT_SUPPORTED value`() = runTest {
        // Given
        fakeSession.pushersService().givenPushersLive(emptyList())
        every { fakeCanToggleNotificationsViaAccountDataUseCase.execute(fakeSession, A_DEVICE_ID) } returns flowOf(false)
        every { fakeCanToggleNotificationsViaPusherUseCase.execute(fakeSession) } returns flowOf(true)

        // When
        val result = getNotificationsStatusUseCase.execute(fakeSession, A_DEVICE_ID)

        // Then
        result.firstOrNull() shouldBeEqualTo NotificationsStatus.NOT_SUPPORTED
    }

    @Test
    fun `given current session and toggle via account data is supported when execute then resulting flow contains status based on account data`() = runTest {
        // Given
        fakeSession
                .accountDataService()
                .givenGetUserAccountDataEventReturns(
                        type = UserAccountDataTypes.TYPE_LOCAL_NOTIFICATION_SETTINGS + A_DEVICE_ID,
                        content = LocalNotificationSettingsContent(
                                isSilenced = false
                        ).toContent(),
                )
        every { fakeCanToggleNotificationsViaAccountDataUseCase.execute(fakeSession, A_DEVICE_ID) } returns flowOf(true)
        every { fakeCanToggleNotificationsViaPusherUseCase.execute(fakeSession) } returns flowOf(false)

        // When
        val result = getNotificationsStatusUseCase.execute(fakeSession, A_DEVICE_ID)

        // Then
        result.firstOrNull() shouldBeEqualTo NotificationsStatus.ENABLED
        verify {
            fakeCanToggleNotificationsViaAccountDataUseCase.execute(fakeSession, A_DEVICE_ID)
        }
        verify(inverse = true) {
            fakeCanToggleNotificationsViaPusherUseCase.execute(fakeSession)
        }
    }
}
