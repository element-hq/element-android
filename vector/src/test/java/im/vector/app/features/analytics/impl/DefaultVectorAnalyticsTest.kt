/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.analytics.impl

import im.vector.app.features.analytics.plan.SuperProperties
import im.vector.app.test.fakes.FakeAnalyticsStore
import im.vector.app.test.fakes.FakeLateInitUserPropertiesFactory
import im.vector.app.test.fakes.FakePostHog
import im.vector.app.test.fakes.FakePostHogFactory
import im.vector.app.test.fakes.FakeSentryAnalytics
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
    private val fakeSentryAnalytics = FakeSentryAnalytics()

    private val defaultVectorAnalytics = DefaultVectorAnalytics(
            postHogFactory = FakePostHogFactory(fakePostHog.instance).instance,
            sentryAnalytics = fakeSentryAnalytics.instance,
            analyticsStore = fakeAnalyticsStore.instance,
            globalScope = CoroutineScope(Dispatchers.Unconfined),
            analyticsConfig = anAnalyticsConfig(isEnabled = true),
            lateInitUserPropertiesFactory = fakeLateInitUserPropertiesFactory.instance,
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
    fun `when consenting to analytics then updates posthog opt out to false and initialize Sentry`() = runTest {
        fakeAnalyticsStore.givenUserContent(consent = true)

        fakePostHog.verifyOptOutStatus(optedOut = false)

        fakeSentryAnalytics.verifySentryInit()
    }

    @Test
    fun `when revoking consent to analytics then updates posthog opt out to true and closes Sentry`() = runTest {
        // For opt-out to have effect on Posthog, it has to be used first, so it has to be opt-in first
        fakeAnalyticsStore.givenUserContent(consent = true)
        fakePostHog.verifyOptOutStatus(optedOut = false)
        fakeSentryAnalytics.verifySentryInit()

        // Then test opt-out
        fakeAnalyticsStore.givenUserContent(consent = false)

        fakePostHog.verifyOptOutStatus(optedOut = true)

        fakeSentryAnalytics.verifySentryClose()
    }

    @Test
    fun `when setting the analytics id then updates analytics store`() = runTest {
        defaultVectorAnalytics.setAnalyticsId(AN_ANALYTICS_ID)

        fakeAnalyticsStore.verifyAnalyticsIdUpdated(updatedValue = AN_ANALYTICS_ID)
    }

    @Test
    fun `given lateinit user properties when valid analytics id updates then identify with lateinit properties`() = runTest {
        fakeLateInitUserPropertiesFactory.givenCreatesProperties(A_LATE_INIT_USER_PROPERTIES)
        fakeAnalyticsStore.givenUserContent(true)

        fakeAnalyticsStore.givenAnalyticsId(AN_ANALYTICS_ID)

        fakePostHog.verifyIdentifies(AN_ANALYTICS_ID, A_LATE_INIT_USER_PROPERTIES)
    }

    @Test
    fun `when signing out then resets posthog and closes Sentry`() = runTest {
        fakeAnalyticsStore.allowSettingAnalyticsIdToCallBackingFlow()
        fakeAnalyticsStore.givenUserContent(true)

        defaultVectorAnalytics.onSignOut()

        fakePostHog.verifyReset()

        fakeSentryAnalytics.verifySentryClose()
    }

    @Test
    fun `given user consent when tracking screen events then submits to posthog`() = runTest {
        fakeAnalyticsStore.givenUserContent(consent = true)

        defaultVectorAnalytics.screen(A_SCREEN_EVENT)

        fakePostHog.verifyScreenTracked(A_SCREEN_EVENT.getName(), A_SCREEN_EVENT.getProperties())
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

        fakePostHog.verifyEventTracked(AN_EVENT.getName(), AN_EVENT.getProperties().clearNulls())
    }

    @Test
    fun `given user has not consented when tracking events then does not track`() = runTest {
        fakeAnalyticsStore.givenUserContent(consent = false)

        defaultVectorAnalytics.capture(AN_EVENT)

        fakePostHog.verifyNoEventTracking()
    }

    @Test
    fun `given user has consented, when tracking exception, then submits to sentry`() = runTest {
        fakeAnalyticsStore.givenUserContent(consent = true)
        val exception = Exception("test")

        defaultVectorAnalytics.trackError(exception)

        fakeSentryAnalytics.verifySentryTrackError(exception)
    }

    @Test
    fun `given user has not consented, when tracking exception, then does not track to sentry`() = runTest {
        fakeAnalyticsStore.givenUserContent(consent = false)

        defaultVectorAnalytics.trackError(Exception("test"))

        fakeSentryAnalytics.verifyNoErrorTracking()
    }

    @Test
    fun `Super properties should be added to all captured events`() = runTest {
        fakeAnalyticsStore.givenUserContent(consent = true)

        val updatedProperties = SuperProperties(
                appPlatform = SuperProperties.AppPlatform.EA,
                cryptoSDKVersion = "0.0",
                cryptoSDK = SuperProperties.CryptoSDK.Rust
        )

        defaultVectorAnalytics.updateSuperProperties(updatedProperties)

        val fakeEvent = aVectorAnalyticsEvent("THE_NAME", mutableMapOf("foo" to "bar"))
        defaultVectorAnalytics.capture(fakeEvent)

        fakePostHog.verifyEventTracked(
                "THE_NAME",
                fakeEvent.getProperties().clearNulls()?.toMutableMap()?.apply {
                    updatedProperties.getProperties()?.let { putAll(it) }
                }
        )

        // Check with a screen event
        val fakeScreen = aVectorAnalyticsScreen("Screen", mutableMapOf("foo" to "bar"))
        defaultVectorAnalytics.screen(fakeScreen)

        fakePostHog.verifyScreenTracked(
                "Screen",
                fakeScreen.getProperties().clearNulls()?.toMutableMap()?.apply {
                    updatedProperties.getProperties()?.let { putAll(it) }
                }
        )
    }

    @Test
    fun `Super properties can be updated`() = runTest {
        fakeAnalyticsStore.givenUserContent(consent = true)

        val superProperties = SuperProperties(
                appPlatform = SuperProperties.AppPlatform.EA,
                cryptoSDKVersion = "0.0",
                cryptoSDK = SuperProperties.CryptoSDK.Rust
        )

        defaultVectorAnalytics.updateSuperProperties(superProperties)

        val fakeEvent = aVectorAnalyticsEvent("THE_NAME", mutableMapOf("foo" to "bar"))
        defaultVectorAnalytics.capture(fakeEvent)

        fakePostHog.verifyEventTracked(
                "THE_NAME",
                fakeEvent.getProperties().clearNulls()?.toMutableMap()?.apply {
                    superProperties.getProperties()?.let { putAll(it) }
                }
        )

        val superPropertiesUpdate = superProperties.copy(cryptoSDKVersion = "1.0")
        defaultVectorAnalytics.updateSuperProperties(superPropertiesUpdate)

        defaultVectorAnalytics.capture(fakeEvent)

        fakePostHog.verifyEventTracked(
                "THE_NAME",
                fakeEvent.getProperties().clearNulls()?.toMutableMap()?.apply {
                    superPropertiesUpdate.getProperties()?.let { putAll(it) }
                }
        )
    }

    @Test
    fun `Super properties should not override event property`() = runTest {
        fakeAnalyticsStore.givenUserContent(consent = true)

        val superProperties = SuperProperties(
                cryptoSDKVersion = "0.0",
        )

        defaultVectorAnalytics.updateSuperProperties(superProperties)

        val fakeEvent = aVectorAnalyticsEvent("THE_NAME", mutableMapOf("cryptoSDKVersion" to "XXX"))
        defaultVectorAnalytics.capture(fakeEvent)

        fakePostHog.verifyEventTracked(
                "THE_NAME",
                mapOf(
                        "cryptoSDKVersion" to "XXX"
                )
        )
    }

    @Test
    fun `Super properties should be added to event with no properties`() = runTest {
        fakeAnalyticsStore.givenUserContent(consent = true)

        val superProperties = SuperProperties(
                cryptoSDKVersion = "0.0",
        )

        defaultVectorAnalytics.updateSuperProperties(superProperties)

        val fakeEvent = aVectorAnalyticsEvent("THE_NAME", null)
        defaultVectorAnalytics.capture(fakeEvent)

        fakePostHog.verifyEventTracked(
                "THE_NAME",
                mapOf(
                        "cryptoSDKVersion" to "0.0"
                )
        )
    }

    private fun Map<String, Any?>?.clearNulls(): Map<String, Any>? {
        if (this == null) return null

        val nonNulls = HashMap<String, Any>()
        this.forEach { (key, value) ->
            if (value != null) {
                nonNulls[key] = value
            }
        }
        return nonNulls
    }
}
