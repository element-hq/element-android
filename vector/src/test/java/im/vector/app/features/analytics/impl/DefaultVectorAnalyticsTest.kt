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

package im.vector.app.features.analytics.impl

import com.posthog.android.Properties
import im.vector.app.features.analytics.itf.VectorAnalyticsEvent
import im.vector.app.features.analytics.itf.VectorAnalyticsScreen
import im.vector.app.test.fakes.FakeAnalyticsStore
import im.vector.app.test.fakes.FakeLateInitUserPropertiesFactory
import im.vector.app.test.fakes.FakePostHog
import im.vector.app.test.fakes.FakePostHogFactory
import im.vector.app.test.fixtures.AnalyticsConfigFixture.anAnalyticsConfig
import im.vector.app.test.fixtures.aUserProperties
import im.vector.app.test.fixtures.aVectorAnalyticsEvent
import im.vector.app.test.fixtures.aVectorAnalyticsScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

private const val AN_ANALYTICS_ID = "analytics-id"
private val A_SCREEN_EVENT = aVectorAnalyticsScreen()
private val AN_EVENT = aVectorAnalyticsEvent()
private val A_LATE_INIT_USER_PROPERTIES = aUserProperties()

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultVectorAnalyticsTest {

    private val fakePostHog = FakePostHog()
    private val fakeAnalyticsStore = FakeAnalyticsStore()
    private val fakeLateInitUserPropertiesFactory = FakeLateInitUserPropertiesFactory()

    private val defaultVectorAnalytics = DefaultVectorAnalytics(
            postHogFactory = FakePostHogFactory(fakePostHog.instance).instance,
            analyticsStore = fakeAnalyticsStore.instance,
            globalScope = CoroutineScope(Dispatchers.Unconfined),
            analyticsConfig = anAnalyticsConfig(isEnabled = true),
            lateInitUserPropertiesFactory = fakeLateInitUserPropertiesFactory.instance
    )

    @Before
    fun setUp() {
        defaultVectorAnalytics.init()
    }

    @Test
    fun `when setting user consent then updates analytics store`() = runTest {
        defaultVectorAnalytics.setUserConsent(true)

        fakeAnalyticsStore.verifyConsentUpdated(updatedValue = true)
    }

    @Test
    fun `when consenting to analytics then updates posthog opt out to false`() = runTest {
        fakeAnalyticsStore.givenUserContent(consent = true)

        fakePostHog.verifyOptOutStatus(optedOut = false)
    }

    @Test
    fun `when revoking consent to analytics then updates posthog opt out to true`() = runTest {
        fakeAnalyticsStore.givenUserContent(consent = false)

        fakePostHog.verifyOptOutStatus(optedOut = true)
    }

    @Test
    fun `when setting the analytics id then updates analytics store`() = runTest {
        defaultVectorAnalytics.setAnalyticsId(AN_ANALYTICS_ID)

        fakeAnalyticsStore.verifyAnalyticsIdUpdated(updatedValue = AN_ANALYTICS_ID)
    }

    @Test
    fun `given lateinit user properties when valid analytics id updates then identify with lateinit properties`() = runTest {
        fakeLateInitUserPropertiesFactory.givenCreatesProperties(A_LATE_INIT_USER_PROPERTIES)

        fakeAnalyticsStore.givenAnalyticsId(AN_ANALYTICS_ID)

        fakePostHog.verifyIdentifies(AN_ANALYTICS_ID, A_LATE_INIT_USER_PROPERTIES)
    }

    @Test
    fun `when signing out then resets posthog`() = runTest {
        fakeAnalyticsStore.allowSettingAnalyticsIdToCallBackingFlow()

        defaultVectorAnalytics.onSignOut()

        fakePostHog.verifyReset()
    }

    @Test
    fun `given user consent when tracking screen events then submits to posthog`() = runTest {
        fakeAnalyticsStore.givenUserContent(consent = true)

        defaultVectorAnalytics.screen(A_SCREEN_EVENT)

        fakePostHog.verifyScreenTracked(A_SCREEN_EVENT.getName(), A_SCREEN_EVENT.toPostHogProperties())
    }

    @Test
    fun `given user has not consented when tracking screen events then does not track`() = runTest {
        fakeAnalyticsStore.givenUserContent(consent = false)

        defaultVectorAnalytics.screen(A_SCREEN_EVENT)

        fakePostHog.verifyNoScreenTracking()
    }

    @Test
    fun `given user consent when tracking events then submits to posthog`() = runTest {
        fakeAnalyticsStore.givenUserContent(consent = true)

        defaultVectorAnalytics.capture(AN_EVENT)

        fakePostHog.verifyEventTracked(AN_EVENT.getName(), AN_EVENT.toPostHogProperties())
    }

    @Test
    fun `given user has not consented when tracking events then does not track`() = runTest {
        fakeAnalyticsStore.givenUserContent(consent = false)

        defaultVectorAnalytics.capture(AN_EVENT)

        fakePostHog.verifyNoEventTracking()
    }
}

private fun VectorAnalyticsScreen.toPostHogProperties(): Properties? {
    return getProperties()?.let { properties ->
        Properties().also { it.putAll(properties) }
    }
}

private fun VectorAnalyticsEvent.toPostHogProperties(): Properties? {
    return getProperties()?.let { properties ->
        Properties().also { it.putAll(properties) }
    }
}
