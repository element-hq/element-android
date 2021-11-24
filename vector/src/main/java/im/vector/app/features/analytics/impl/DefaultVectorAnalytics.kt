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
import im.vector.app.features.analytics.AnalyticsConfig
import im.vector.app.features.analytics.VectorAnalytics
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

    private var userConsent: Boolean = false

    override fun getUserConsent(): Flow<Boolean> {
        return analyticsStore.userConsentFlow
    }

    override suspend fun setUserConsent(userConsent: Boolean) {
        analyticsStore.setUserConsent(userConsent)
    }

    override fun didAskUserConsent(): Flow<Boolean> {
        return analyticsStore.didAskUserConsentFlow
    }

    override suspend fun setDidAskUserConsent() {
        analyticsStore.setDidAskUserConsent()
    }

    override fun getAnalyticsId(): Flow<String> {
        return analyticsStore.analyticsIdFlow
    }

    override suspend fun setAnalyticsId(analyticsId: String) {
        analyticsStore.setAnalyticsId(analyticsId)
    }

    override suspend fun onSignOut() {
        // reset the analyticsId
        setAnalyticsId("")
    }

    override fun init() {
        observeUserConsent()
        observeAnalyticsId()
    }

    @Suppress("EXPERIMENTAL_API_USAGE")
    private fun observeAnalyticsId() {
        getAnalyticsId()
                .onEach { id ->
                    if (id.isEmpty()) {
                        posthog?.reset()
                    } else {
                        posthog?.identify(id)
                    }
                }
                .launchIn(GlobalScope)
    }

    @Suppress("EXPERIMENTAL_API_USAGE")
    private fun observeUserConsent() {
        getUserConsent()
                .onEach { consent ->
                    userConsent = consent
                    if (consent) {
                        createAnalyticsClient()
                    }
                    posthog?.optOut(!consent)
                }
                .launchIn(GlobalScope)
    }

    private fun createAnalyticsClient() {
        val config: AnalyticsConfig = AnalyticsConfig.getConfig()
                ?: return Unit.also { Timber.w("Analytics is disabled") }

        posthog = PostHog.Builder(context, config.postHogApiKey, config.postHogHost)
                // Record certain application events automatically! (off/false by default)
                // .captureApplicationLifecycleEvents()

                // Record screen views automatically! (off/false by default)
                // .recordScreenViews()

                // Capture deep links as part of the screen call. (off by default)
                // .captureDeepLinks()

                // Maximum number of events to keep in queue before flushing (20)
                // .flushQueueSize(20)

                // Max delay before flushing the queue (30 seconds)
                // .flushInterval(30, TimeUnit.SECONDS)

                // Enable or disable collection of ANDROID_ID (true)
                .collectDeviceId(false)
                .logLevel(getLogLevel())
                .build()
    }

    private fun getLogLevel(): PostHog.LogLevel {
        return if (BuildConfig.DEBUG) {
            PostHog.LogLevel.DEBUG
        } else {
            PostHog.LogLevel.INFO
        }
    }

    override fun capture(event: String, properties: Map<String, Any>?) {
        posthog
                ?.takeIf { userConsent }
                ?.capture(event, properties.toPostHogProperties())
    }

    override fun screen(name: String, properties: Map<String, Any>?) {
        posthog
                ?.takeIf { userConsent }
                ?.screen(name, properties.toPostHogProperties())
    }

    private fun Map<String, Any>?.toPostHogProperties(): Properties? {
        if (this == null) return null

        return Properties().apply {
            this@toPostHogProperties.forEach { putValue(it.key, it.value) }
        }
    }
}
