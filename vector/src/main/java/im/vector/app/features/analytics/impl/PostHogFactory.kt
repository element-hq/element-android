/*
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.analytics.impl

import android.content.Context
import com.posthog.android.PostHog
import im.vector.app.core.resources.BuildMeta
import im.vector.app.features.analytics.AnalyticsConfig
import javax.inject.Inject

class PostHogFactory @Inject constructor(
        private val context: Context,
        private val analyticsConfig: AnalyticsConfig,
        private val buildMeta: BuildMeta,
) {

    fun createPosthog(): PostHog {
        return PostHog.Builder(context, analyticsConfig.postHogApiKey, analyticsConfig.postHogHost)
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
    }

    private fun getLogLevel(): PostHog.LogLevel {
        return if (buildMeta.isDebug) {
            PostHog.LogLevel.DEBUG
        } else {
            PostHog.LogLevel.INFO
        }
    }
}
