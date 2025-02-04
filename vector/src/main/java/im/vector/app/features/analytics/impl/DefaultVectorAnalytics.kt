/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.analytics.impl

import com.posthog.PostHogInterface
import im.vector.app.core.di.NamedGlobalScope
import im.vector.app.features.analytics.AnalyticsConfig
import im.vector.app.features.analytics.VectorAnalytics
import im.vector.app.features.analytics.itf.VectorAnalyticsEvent
import im.vector.app.features.analytics.itf.VectorAnalyticsScreen
import im.vector.app.features.analytics.log.analyticsTag
import im.vector.app.features.analytics.plan.SuperProperties
import im.vector.app.features.analytics.plan.UserProperties
import im.vector.app.features.analytics.store.AnalyticsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.matrix.android.sdk.api.extensions.orFalse
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultVectorAnalytics @Inject constructor(
        private val postHogFactory: PostHogFactory,
        private val sentryAnalytics: SentryAnalytics,
        private val analyticsConfig: AnalyticsConfig,
        private val analyticsStore: AnalyticsStore,
        private val lateInitUserPropertiesFactory: LateInitUserPropertiesFactory,
        @NamedGlobalScope private val globalScope: CoroutineScope
) : VectorAnalytics {

    private var posthog: PostHogInterface? = null

    private fun createPosthog(): PostHogInterface? {
        return when {
            analyticsConfig.isEnabled -> postHogFactory.createPosthog()
            else -> {
                Timber.tag(analyticsTag.value).w("Analytics is disabled")
                null
            }
        }
    }

    // Cache for the store values
    private var userConsent: Boolean? = null
    private var analyticsId: String? = null

    // Cache for the properties to send
    private var pendingUserProperties: UserProperties? = null

    private var superProperties: SuperProperties? = null

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

        // Close Sentry SDK.
        sentryAnalytics.stopSentry()
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
        if (!userConsent.orFalse()) return
        if (id.isEmpty()) {
            Timber.tag(analyticsTag.value).d("reset")
            posthog?.reset()
        } else {
            Timber.tag(analyticsTag.value).d("identify")
            posthog?.identify(id, lateInitUserPropertiesFactory.createUserProperties()?.getProperties()?.toPostHogUserProperties())
        }
    }

    private fun observeUserConsent() {
        getUserConsent()
                .onEach { consent ->
                    Timber.tag(analyticsTag.value).d("User consent updated to $consent")
                    userConsent = consent
                    initOrStopPostHog()
                    initOrStopSentry()
                }
                .launchIn(globalScope)
    }

    private fun initOrStopSentry() {
        userConsent?.let {
            when (it) {
                true -> sentryAnalytics.initSentry()
                false -> sentryAnalytics.stopSentry()
            }
        }
    }

    private suspend fun initOrStopPostHog() {
        userConsent?.let { _userConsent ->
            when (_userConsent) {
                true -> {
                    posthog = createPosthog()
                    posthog?.optIn()
                    identifyPostHog()
                    pendingUserProperties?.let { doUpdateUserProperties(it) }
                    pendingUserProperties = null
                }
                false -> {
                    // When opting out, ensure that the queue is flushed first, or it will be flushed later (after user has revoked consent)
                    posthog?.flush()
                    posthog?.optOut()
                    posthog?.close()
                    posthog = null
                }
            }
        }
    }

    override fun capture(event: VectorAnalyticsEvent) {
        Timber.tag(analyticsTag.value).d("capture($event)")
        posthog?.takeIf { userConsent == true }?.capture(
                        event.getName(), analyticsId, event.getProperties()?.toPostHogProperties().orEmpty().withSuperProperties()
                )
    }

    override fun screen(screen: VectorAnalyticsScreen) {
        Timber.tag(analyticsTag.value).d("screen($screen)")
        posthog?.takeIf { userConsent == true }?.screen(screen.getName(), screen.getProperties()?.toPostHogProperties().orEmpty().withSuperProperties())
    }

    override fun updateUserProperties(userProperties: UserProperties) {
        if (userConsent == true) {
            doUpdateUserProperties(userProperties)
        } else {
            pendingUserProperties = userProperties
        }
    }

    private fun doUpdateUserProperties(userProperties: UserProperties) {
        // we need a distinct id to set user properties
        val distinctId = analyticsId ?: return
        posthog?.takeIf { userConsent == true }?.identify(distinctId, userProperties.getProperties())
    }

    private fun Map<String, Any?>?.toPostHogProperties(): Map<String, Any>? {
        if (this == null) return null

        val nonNulls = HashMap<String, Any>()
        this.forEach { (key, value) ->
            if (value != null) {
                nonNulls[key] = value
            }
        }
        return nonNulls
    }

    /**
     * We avoid sending nulls as part of the UserProperties as this will reset the values across all devices.
     * The UserProperties event has nullable properties to allow for clients to opt in.
     */
    private fun Map<String, Any?>.toPostHogUserProperties(): Map<String, Any> {
        val nonNulls = HashMap<String, Any>()
        this.forEach { (key, value) ->
            if (value != null) {
                nonNulls[key] = value
            }
        }
        return nonNulls
    }

    /**
     * Adds super properties to the actual property set.
     * If a property of the same name is already on the reported event it will not be overwritten.
     */
    private fun Map<String, Any>.withSuperProperties(): Map<String, Any>? {
        val withSuperProperties = this.toMutableMap()
        val superProperties = this@DefaultVectorAnalytics.superProperties?.getProperties()
        superProperties?.forEach {
            if (!withSuperProperties.containsKey(it.key)) {
                withSuperProperties[it.key] = it.value
            }
        }
        return withSuperProperties.takeIf { it.isEmpty().not() }
    }

    override fun trackError(throwable: Throwable) {
        sentryAnalytics
                .takeIf { userConsent == true }
                ?.trackError(throwable)
    }

    override fun updateSuperProperties(updatedProperties: SuperProperties) {
        this.superProperties = SuperProperties(
                cryptoSDK = updatedProperties.cryptoSDK ?: this.superProperties?.cryptoSDK,
                appPlatform = updatedProperties.appPlatform ?: this.superProperties?.appPlatform,
                cryptoSDKVersion = updatedProperties.cryptoSDKVersion ?: superProperties?.cryptoSDKVersion
        )
    }
}
