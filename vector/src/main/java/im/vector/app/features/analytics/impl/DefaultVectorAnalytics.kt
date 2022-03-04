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

import com.posthog.android.Options
import com.posthog.android.PostHog
import com.posthog.android.Properties
import im.vector.app.core.di.NamedGlobalScope
import im.vector.app.features.analytics.AnalyticsConfig
import im.vector.app.features.analytics.VectorAnalytics
import im.vector.app.features.analytics.itf.VectorAnalyticsEvent
import im.vector.app.features.analytics.itf.VectorAnalyticsScreen
import im.vector.app.features.analytics.log.analyticsTag
import im.vector.app.features.analytics.plan.UserProperties
import im.vector.app.features.analytics.store.AnalyticsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private val REUSE_EXISTING_ID: String? = null
private val IGNORED_OPTIONS: Options? = null

@Singleton
class DefaultVectorAnalytics @Inject constructor(
        postHogFactory: PostHogFactory,
        analyticsConfig: AnalyticsConfig,
        private val analyticsStore: AnalyticsStore,
        private val lateInitUserPropertiesFactory: LateInitUserPropertiesFactory,
        @NamedGlobalScope private val globalScope: CoroutineScope
) : VectorAnalytics {

    private val posthog: PostHog? = when {
        analyticsConfig.isEnabled -> postHogFactory.createPosthog()
        else                      -> {
            Timber.tag(analyticsTag.value).w("Analytics is disabled")
            null
        }
    }

    // Cache for the store values
    private var userConsent: Boolean? = null
    private var analyticsId: String? = null

    override fun init() {
        observeUserConsent()
        observeAnalyticsId()
    }

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

    private fun observeAnalyticsId() {
        getAnalyticsId()
                .onEach { id ->
                    Timber.tag(analyticsTag.value).d("Analytics Id updated to '$id'")
                    analyticsId = id
                    identifyPostHog()
                }
                .launchIn(globalScope)
    }

    private suspend fun identifyPostHog() {
        val id = analyticsId ?: return
        if (id.isEmpty()) {
            Timber.tag(analyticsTag.value).d("reset")
            posthog?.reset()
        } else {
            Timber.tag(analyticsTag.value).d("identify")
            posthog?.identify(id, lateInitUserPropertiesFactory.createUserProperties()?.getProperties()?.toPostHogUserProperties(), IGNORED_OPTIONS)
        }
    }

    private fun observeUserConsent() {
        getUserConsent()
                .onEach { consent ->
                    Timber.tag(analyticsTag.value).d("User consent updated to $consent")
                    userConsent = consent
                    optOutPostHog()
                }
                .launchIn(globalScope)
    }

    private fun optOutPostHog() {
        userConsent?.let { posthog?.optOut(!it) }
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

    override fun updateUserProperties(userProperties: UserProperties) {
        posthog?.identify(REUSE_EXISTING_ID, userProperties.getProperties()?.toPostHogUserProperties(), IGNORED_OPTIONS)
    }

    private fun Map<String, Any?>?.toPostHogProperties(): Properties? {
        if (this == null) return null

        return Properties().apply {
            putAll(this@toPostHogProperties)
        }
    }

    /**
     * We avoid sending nulls as part of the UserProperties as this will reset the values across all devices
     * The UserProperties event has nullable properties to allow for clients to opt in
     */
    private fun Map<String, Any?>.toPostHogUserProperties(): Properties {
        return Properties().apply {
            putAll(this@toPostHogUserProperties.filter { it.value != null })
        }
    }
}
