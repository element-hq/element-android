/*
 * Copyright (c) 2021 New Vector Ltd
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

import android.content.Context
import com.posthog.android.PostHog
import com.posthog.android.Properties
import im.vector.app.BuildConfig
import im.vector.app.config.analyticsConfig
import im.vector.app.features.analytics.VectorAnalytics
import im.vector.app.features.analytics.itf.VectorAnalyticsEvent
import im.vector.app.features.analytics.itf.VectorAnalyticsScreen
import im.vector.app.features.analytics.log.analyticsTag
import im.vector.app.features.analytics.store.AnalyticsStore
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultVectorAnalytics @Inject constructor(
        private val context: Context,
        private val analyticsStore: AnalyticsStore
) : VectorAnalytics {
    private var posthog: PostHog? = null

    // Cache for the store values
    private var userConsent: Boolean? = null
    private var analyticsId: String? = null

    override fun getUserConsent(): Flow<Boolean> {
        return analyticsStore.userConsentFlow
    }

    override suspend fun setUserConsent(userConsent: Boolean) {
        Timber.tag(analyticsTag.value).d("setUserConsent($userConsent)")
        analyticsStore.setUserConsent(userConsent)
    }

    override fun didAskUserConsent(): Flow<Boolean> {
        return analyticsStore.didAskUserConsentFlow
    }

    override suspend fun setDidAskUserConsent() {
        Timber.tag(analyticsTag.value).d("setDidAskUserConsent()")
        analyticsStore.setDidAskUserConsent()
    }

    override fun getAnalyticsId(): Flow<String> {
        return analyticsStore.analyticsIdFlow
    }

    override suspend fun setAnalyticsId(analyticsId: String) {
        Timber.tag(analyticsTag.value).d("setAnalyticsId($analyticsId)")
        analyticsStore.setAnalyticsId(analyticsId)
    }

    override suspend fun onSignOut() {
        // reset the analyticsId
        setAnalyticsId("")
    }

    override fun init() {
        observeUserConsent()
        observeAnalyticsId()
        createAnalyticsClient()
    }

    @Suppress("EXPERIMENTAL_API_USAGE")
    private fun observeAnalyticsId() {
        getAnalyticsId()
                .onEach { id ->
                    Timber.tag(analyticsTag.value).d("Analytics Id updated to '$id'")
                    analyticsId = id
                    identifyPostHog()
                }
                .launchIn(GlobalScope)
    }

    private fun identifyPostHog() {
        val id = analyticsId ?: return
        if (id.isEmpty()) {
            Timber.tag(analyticsTag.value).d("reset")
            posthog?.reset()
        } else {
            Timber.tag(analyticsTag.value).d("identify")
            posthog?.identify(id)
        }
    }

    @Suppress("EXPERIMENTAL_API_USAGE")
    private fun observeUserConsent() {
        getUserConsent()
                .onEach { consent ->
                    Timber.tag(analyticsTag.value).d("User consent updated to $consent")
                    userConsent = consent
                    optOutPostHog()
                }
                .launchIn(GlobalScope)
    }

    private fun optOutPostHog() {
        userConsent?.let { posthog?.optOut(!it) }
    }

    private fun createAnalyticsClient() {
        Timber.tag(analyticsTag.value).d("createAnalyticsClient()")

        if (analyticsConfig.isEnabled.not()) {
            Timber.tag(analyticsTag.value).w("Analytics is disabled")
            return
        }

        posthog = PostHog.Builder(context, analyticsConfig.postHogApiKey, analyticsConfig.postHogHost)
                // Record certain application events automatically! (off/false by default)
                // .captureApplicationLifecycleEvents()
                // Record screen views automatically! (off/false by default)
                // .recordScreenViews()
                // Capture deep links as part of the screen call. (off by default)
                // .captureDeepLinks()
                // Maximum number of events to keep in queue before flushing (default 20)
                // .flushQueueSize(20)
                // Max delay before flushing the queue (30 seconds)
                // .flushInterval(30, TimeUnit.SECONDS)
                // Enable or disable collection of ANDROID_ID (true)
                .collectDeviceId(false)
                .logLevel(getLogLevel())
                .build()

        optOutPostHog()
        identifyPostHog()
    }

    private fun getLogLevel(): PostHog.LogLevel {
        return if (BuildConfig.DEBUG) {
            PostHog.LogLevel.DEBUG
        } else {
            PostHog.LogLevel.INFO
        }
    }

    override fun capture(event: VectorAnalyticsEvent) {
        Timber.tag(analyticsTag.value).d("capture($event)")
        posthog
                ?.takeIf { userConsent == true }
                ?.capture(event.getName(), event.getProperties()?.toPostHogProperties())
    }

    override fun screen(screen: VectorAnalyticsScreen) {
        Timber.tag(analyticsTag.value).d("screen($screen)")
        posthog
                ?.takeIf { userConsent == true }
                ?.screen(screen.getName(), screen.getProperties()?.toPostHogProperties())
    }

    private fun Map<String, Any>?.toPostHogProperties(): Properties? {
        if (this == null) return null

        return Properties().apply {
            putAll(this@toPostHogProperties)
        }
    }
}
