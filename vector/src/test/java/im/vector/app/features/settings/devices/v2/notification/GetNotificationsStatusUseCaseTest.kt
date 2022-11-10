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

package im.vector.app.features.settings.devices.v2.notification

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import im.vector.app.test.fakes.FakeSession
import im.vector.app.test.fixtures.PusherFixture
import im.vector.app.test.testDispatcher
import io.mockk.every
import io.mockk.mockk
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
    private val fakeCheckIfCanTogglePushNotificationsViaAccountDataUseCase =
            mockk<CheckIfCanTogglePushNotificationsViaAccountDataUseCase>()
    private val fakeCanTogglePushNotificationsViaPusherUseCase =
            mockk<CanTogglePushNotificationsViaPusherUseCase>()

    private val getNotificationsStatusUseCase =
            GetNotificationsStatusUseCase(
                    checkIfCanTogglePushNotificationsViaAccountDataUseCase = fakeCheckIfCanTogglePushNotificationsViaAccountDataUseCase,
                    canTogglePushNotificationsViaPusherUseCase = fakeCanTogglePushNotificationsViaPusherUseCase,
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
        every { fakeCheckIfCanTogglePushNotificationsViaAccountDataUseCase.execute(fakeSession, A_DEVICE_ID) } returns false
        every { fakeCanTogglePushNotificationsViaPusherUseCase.execute(fakeSession) } returns flowOf(false)

        // When
        val result = getNotificationsStatusUseCase.execute(fakeSession, A_DEVICE_ID)

        // Then
        result.firstOrNull() shouldBeEqualTo NotificationsStatus.NOT_SUPPORTED
        verifyOrder {
            // we should first check account data
            fakeCheckIfCanTogglePushNotificationsViaAccountDataUseCase.execute(fakeSession, A_DEVICE_ID)
            fakeCanTogglePushNotificationsViaPusherUseCase.execute(fakeSession)
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
        every { fakeCheckIfCanTogglePushNotificationsViaAccountDataUseCase.execute(fakeSession, A_DEVICE_ID) } returns false
        every { fakeCanTogglePushNotificationsViaPusherUseCase.execute(fakeSession) } returns flowOf(true)

        // When
        val result = getNotificationsStatusUseCase.execute(fakeSession, A_DEVICE_ID)

        // Then
        result.firstOrNull() shouldBeEqualTo NotificationsStatus.ENABLED
    }

    @Test
    fun `given current session and toggle via account data is supported when execute then resulting flow contains status based on settings value`() = runTest {
        // Given
        fakeSession
                .accountDataService()
                .givenGetUserAccountDataEventReturns(
                        type = UserAccountDataTypes.TYPE_LOCAL_NOTIFICATION_SETTINGS + A_DEVICE_ID,
                        content = LocalNotificationSettingsContent(
                                isSilenced = false
                        ).toContent(),
                )
        every { fakeCheckIfCanTogglePushNotificationsViaAccountDataUseCase.execute(fakeSession, A_DEVICE_ID) } returns true
        every { fakeCanTogglePushNotificationsViaPusherUseCase.execute(fakeSession) } returns flowOf(false)

        // When
        val result = getNotificationsStatusUseCase.execute(fakeSession, A_DEVICE_ID)

        // Then
        result.firstOrNull() shouldBeEqualTo NotificationsStatus.ENABLED
    }
}
