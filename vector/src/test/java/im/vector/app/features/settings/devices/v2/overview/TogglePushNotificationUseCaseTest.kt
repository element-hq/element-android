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

import im.vector.app.test.fakes.FakeActiveSessionHolder
import im.vector.app.test.fixtures.PusherFixture
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.matrix.android.sdk.api.account.LocalNotificationSettingsContent
import org.matrix.android.sdk.api.session.accountdata.UserAccountDataTypes
import org.matrix.android.sdk.api.session.events.model.toContent

class TogglePushNotificationUseCaseTest {

    private val activeSessionHolder = FakeActiveSessionHolder()
    private val togglePushNotificationUseCase = TogglePushNotificationUseCase(activeSessionHolder.instance)

    @Test
    fun `when execute, then toggle enabled for device pushers`() = runTest {
        val sessionId = "a_session_id"
        val pushers = listOf(
                PusherFixture.aPusher(deviceId = sessionId, enabled = false),
                PusherFixture.aPusher(deviceId = "another id", enabled = false)
        )
        activeSessionHolder.fakeSession.pushersService().givenPushersLive(pushers)
        activeSessionHolder.fakeSession.pushersService().givenGetPushers(pushers)

        togglePushNotificationUseCase.execute(sessionId, true)

        activeSessionHolder.fakeSession.pushersService().verifyTogglePusherCalled(pushers.first(), true)
    }

    @Test
    fun `when execute, then toggle local notification settings`() = runTest {
        val sessionId = "a_session_id"
        val pushers = listOf(
                PusherFixture.aPusher(deviceId = sessionId, enabled = false),
                PusherFixture.aPusher(deviceId = "another id", enabled = false)
        )
        activeSessionHolder.fakeSession.pushersService().givenPushersLive(pushers)
        activeSessionHolder.fakeSession.accountDataService().givenGetUserAccountDataEventReturns(
                UserAccountDataTypes.TYPE_LOCAL_NOTIFICATION_SETTINGS + sessionId,
                LocalNotificationSettingsContent(isSilenced = true).toContent()
        )

        togglePushNotificationUseCase.execute(sessionId, true)

        activeSessionHolder.fakeSession.accountDataService().verifyUpdateUserAccountDataEventSucceeds(
                UserAccountDataTypes.TYPE_LOCAL_NOTIFICATION_SETTINGS + sessionId,
                LocalNotificationSettingsContent(isSilenced = false).toContent(),
        )
    }
}
