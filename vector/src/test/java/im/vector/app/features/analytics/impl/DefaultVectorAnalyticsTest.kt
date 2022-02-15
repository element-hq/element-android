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
import im.vector.app.test.fakes.FakePostHog
import im.vector.app.test.fakes.FakePostHogFactory
import im.vector.app.test.fixtures.AnalyticsConfigFixture.anAnalyticsConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test

private const val AN_ANALYTICS_ID = "analytics-id"
private val A_SCREEN_EVENT = object : VectorAnalyticsScreen {
    override fun getName() = "a-screen-event-name"
    override fun getProperties() = mapOf("property-name" to "property-value")
}
private val AN_EVENT = object : VectorAnalyticsEvent {
    override fun getName() = "an-event-name"
    override fun getProperties() = mapOf("property-name" to "property-value")
}

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultVectorAnalyticsTest {

    private val fakePostHog = FakePostHog()
    private val fakeAnalyticsStore = FakeAnalyticsStore()

    private val defaultVectorAnalytics = DefaultVectorAnalytics(
            postHogFactory = FakePostHogFactory(fakePostHog.instance).instance,
            analyticsStore = fakeAnalyticsStore.instance,
            globalScope = CoroutineScope(Dispatchers.Unconfined),
            analyticsConfig = anAnalyticsConfig(isEnabled = true)
    )

    @Before
    fun setUp() {
        defaultVectorAnalytics.init()
    }

    @Test
    fun `when setting user consent then updates analytics store`() = runBlockingTest {
        defaultVectorAnalytics.setUserConsent(true)

        fakeAnalyticsStore.verifyConsentUpdated(updatedValue = true)
    }

    @Test
    fun `when consenting to analytics then updates posthog opt out to false`() = runBlockingTest {
        fakeAnalyticsStore.givenUserContent(consent = true)

        fakePostHog.verifyOptOutStatus(optedOut = false)
    }

    @Test
    fun `when revoking consent to analytics then updates posthog opt out to true`() = runBlockingTest {
        fakeAnalyticsStore.givenUserContent(consent = false)

        fakePostHog.verifyOptOutStatus(optedOut = true)
    }

    @Test
    fun `when setting the analytics id then updates analytics store`() = runBlockingTest {
        defaultVectorAnalytics.setAnalyticsId(AN_ANALYTICS_ID)

        fakeAnalyticsStore.verifyAnalyticsIdUpdated(updatedValue = AN_ANALYTICS_ID)
    }

    @Test
    fun `when valid analytics id updates then identify`() = runBlockingTest {
        fakeAnalyticsStore.givenAnalyticsId(AN_ANALYTICS_ID)

        fakePostHog.verifyIdentifies(AN_ANALYTICS_ID)
    }

    @Test
    fun `when signing out analytics id updates then resets`() = runBlockingTest {
        fakeAnalyticsStore.allowSettingAnalyticsIdToCallBackingFlow()

        defaultVectorAnalytics.onSignOut()

        fakePostHog.verifyReset()
    }

    @Test
    fun `given user consent when tracking screen events then submits to posthog`() = runBlockingTest {
        fakeAnalyticsStore.givenUserContent(consent = true)

        defaultVectorAnalytics.screen(A_SCREEN_EVENT)

        fakePostHog.verifyScreenTracked(A_SCREEN_EVENT.getName(), Properties().also {
            it.putAll(A_SCREEN_EVENT.getProperties())
        })
    }

    @Test
    fun `given user has not consented when tracking screen events then does not track`() = runBlockingTest {
        fakeAnalyticsStore.givenUserContent(consent = false)

        defaultVectorAnalytics.screen(A_SCREEN_EVENT)

        fakePostHog.verifyNoScreenTracking()
    }

    @Test
    fun `given user consent when tracking events then submits to posthog`() = runBlockingTest {
        fakeAnalyticsStore.givenUserContent(consent = true)

        defaultVectorAnalytics.capture(AN_EVENT)

        fakePostHog.verifyEventTracked(AN_EVENT.getName(), Properties().also {
            it.putAll(AN_EVENT.getProperties())
        })
    }

    @Test
    fun `given user has not consented when tracking events then does not track`() = runBlockingTest {
        fakeAnalyticsStore.givenUserContent(consent = false)

        defaultVectorAnalytics.capture(AN_EVENT)

        fakePostHog.verifyNoEventTracking()
    }
}
