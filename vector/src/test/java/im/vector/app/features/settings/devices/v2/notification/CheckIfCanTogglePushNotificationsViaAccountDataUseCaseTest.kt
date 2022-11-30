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

import im.vector.app.test.fakes.FakeSession
import io.mockk.mockk
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import org.matrix.android.sdk.api.session.accountdata.UserAccountDataTypes

private const val A_DEVICE_ID = "device-id"

class CheckIfCanTogglePushNotificationsViaAccountDataUseCaseTest {

    private val fakeSession = FakeSession()

    private val checkIfCanTogglePushNotificationsViaAccountDataUseCase =
            CheckIfCanTogglePushNotificationsViaAccountDataUseCase()

    @Test
    fun `given current session and an account data for the device id when execute then result is true`() {
        // Given
        fakeSession
                .accountDataService()
                .givenGetUserAccountDataEventReturns(
                        type = UserAccountDataTypes.TYPE_LOCAL_NOTIFICATION_SETTINGS + A_DEVICE_ID,
                        content = mockk(),
                )

        // When
        val result = checkIfCanTogglePushNotificationsViaAccountDataUseCase.execute(fakeSession, A_DEVICE_ID)

        // Then
        result shouldBeEqualTo true
    }

    @Test
    fun `given current session and NO account data for the device id when execute then result is false`() {
        // Given
        fakeSession
                .accountDataService()
                .givenGetUserAccountDataEventReturns(
                        type = UserAccountDataTypes.TYPE_LOCAL_NOTIFICATION_SETTINGS + A_DEVICE_ID,
                        content = null,
                )

        // When
        val result = checkIfCanTogglePushNotificationsViaAccountDataUseCase.execute(fakeSession, A_DEVICE_ID)

        // Then
        result shouldBeEqualTo false
    }
}
