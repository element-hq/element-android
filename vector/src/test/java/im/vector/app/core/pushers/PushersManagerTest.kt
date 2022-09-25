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

import im.vector.app.AppBuildConfig
import im.vector.app.R
import im.vector.app.test.fakes.FakeActiveSessionHolder
import im.vector.app.test.fakes.FakeAppNameProvider
import im.vector.app.test.fakes.FakeLocaleProvider
import im.vector.app.test.fakes.FakePushersService
import im.vector.app.test.fakes.FakeSession
import im.vector.app.test.fakes.FakeStringProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test
import java.util.Locale
import kotlin.math.abs

class PushersManagerTest {

    private val pushersService = FakePushersService()
    private val session = FakeSession(fakePushersService = pushersService)
    private val activeSessionHolder = FakeActiveSessionHolder(session)
    private val stringProvider = FakeStringProvider()
    private val localeProvider = FakeLocaleProvider()
    private val appNameProvider = FakeAppNameProvider()

    private val pushersManager = PushersManager(
            mockk(),
            activeSessionHolder.instance,
            localeProvider,
            stringProvider.instance,
            appNameProvider,
    )

    @Before
    fun setUp() {
        mockkObject(AppBuildConfig)
    }

    @Test
    fun `when enqueueRegisterPusher, then HttpPusher created and enqueued`() {
        val pushKey = "abc"
        val gateway = "123"
        val pusherAppId = "app-id"
        val appName = "element"
        val deviceDisplayName = "iPhone Lollipop"
        stringProvider.given(R.string.pusher_app_id, pusherAppId)
        localeProvider.givenCurrent(Locale.UK)
        appNameProvider.givenAppName(appName)
        every { AppBuildConfig.getModel() } returns deviceDisplayName

        pushersManager.enqueueRegisterPusher(pushKey, gateway)

        val httpPusher = pushersService.verifyEnqueueAddHttpPusher()
        httpPusher.pushkey shouldBeEqualTo pushKey
        httpPusher.appId shouldBeEqualTo pusherAppId
        httpPusher.profileTag shouldBeEqualTo DEFAULT_PUSHER_FILE_TAG + "_" + abs(session.myUserId.hashCode())
        httpPusher.lang shouldBeEqualTo Locale.UK.language
        httpPusher.appDisplayName shouldBeEqualTo appName
        httpPusher.deviceDisplayName shouldBeEqualTo deviceDisplayName
        httpPusher.enabled shouldBeEqualTo true
        httpPusher.deviceId shouldBeEqualTo session.sessionParams.deviceId
        httpPusher.append shouldBeEqualTo false
        httpPusher.withEventIdOnly shouldBeEqualTo true
        httpPusher.url shouldBeEqualTo gateway
    }
}
