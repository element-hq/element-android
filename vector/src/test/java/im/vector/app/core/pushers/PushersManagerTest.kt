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

package im.vector.app.core.pushers

import im.vector.app.R
import im.vector.app.features.mdm.NoOpMdmService
import im.vector.app.test.fakes.FakeActiveSessionHolder
import im.vector.app.test.fakes.FakeAppNameProvider
import im.vector.app.test.fakes.FakeGetDeviceInfoUseCase
import im.vector.app.test.fakes.FakeLocaleProvider
import im.vector.app.test.fakes.FakePushersService
import im.vector.app.test.fakes.FakeSession
import im.vector.app.test.fakes.FakeStringProvider
import im.vector.app.test.fixtures.CredentialsFixture
import im.vector.app.test.fixtures.CryptoDeviceInfoFixture.aCryptoDeviceInfo
import im.vector.app.test.fixtures.PusherFixture
import im.vector.app.test.fixtures.SessionParamsFixture
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import org.matrix.android.sdk.api.session.crypto.model.UnsignedDeviceInfo
import org.matrix.android.sdk.api.session.pushers.HttpPusher
import java.util.Locale
import kotlin.math.abs

class PushersManagerTest {

    private val pushersService = FakePushersService()
    private val session = FakeSession(fakePushersService = pushersService)
    private val activeSessionHolder = FakeActiveSessionHolder(session)
    private val stringProvider = FakeStringProvider()
    private val localeProvider = FakeLocaleProvider()
    private val appNameProvider = FakeAppNameProvider()
    private val getDeviceInfoUseCase = FakeGetDeviceInfoUseCase()

    private val pushersManager = PushersManager(
            mockk(),
            activeSessionHolder.instance,
            localeProvider,
            stringProvider.instance,
            appNameProvider,
            getDeviceInfoUseCase,
            NoOpMdmService(),
    )

    @Test
    fun `when enqueueRegisterPusher, then HttpPusher created and enqueued`() = runTest {
        val pushKey = "abc"
        val gateway = "123"
        val pusherAppId = "app-id"
        val appName = "element"
        val deviceDisplayName = "iPhone Lollipop"
        stringProvider.given(R.string.pusher_app_id, pusherAppId)
        localeProvider.givenCurrent(Locale.UK)
        appNameProvider.givenAppName(appName)
        getDeviceInfoUseCase.givenDeviceInfo(aCryptoDeviceInfo(unsigned = UnsignedDeviceInfo(deviceDisplayName)))
        val expectedHttpPusher = HttpPusher(
                pushkey = pushKey,
                appId = pusherAppId,
                profileTag = DEFAULT_PUSHER_FILE_TAG + "_" + abs(session.myUserId.hashCode()),
                lang = Locale.UK.language,
                appDisplayName = appName,
                deviceDisplayName = deviceDisplayName,
                url = gateway,
                enabled = true,
                deviceId = session.sessionParams.deviceId,
                append = false,
                withEventIdOnly = true,
        )

        pushersManager.enqueueRegisterPusher(pushKey, gateway)

        val httpPusher = pushersService.verifyEnqueueAddHttpPusher()
        httpPusher shouldBeEqualTo expectedHttpPusher
    }

    @Test
    fun `when getPusherForCurrentSession, then return pusher`() {
        val deviceId = "device_id"
        val sessionParams = SessionParamsFixture.aSessionParams(
                credentials = CredentialsFixture.aCredentials(deviceId = deviceId)
        )
        session.givenSessionParams(sessionParams)
        val expectedPusher = PusherFixture.aPusher(deviceId = deviceId)
        pushersService.givenGetPushers(listOf(expectedPusher))

        val pusher = pushersManager.getPusherForCurrentSession()

        pusher shouldBeEqualTo expectedPusher
    }
}
