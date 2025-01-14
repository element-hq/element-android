/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.analytics.impl

import android.content.Context
import com.posthog.PostHogInterface
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig
import im.vector.app.core.resources.BuildMeta
import im.vector.app.features.analytics.AnalyticsConfig
import javax.inject.Inject

class PostHogFactory @Inject constructor(
        private val context: Context,
        private val analyticsConfig: AnalyticsConfig,
        private val buildMeta: BuildMeta,
) {

    fun createPosthog(): PostHogInterface {
        val config = PostHogAndroidConfig(
                apiKey = analyticsConfig.postHogApiKey,
                host = analyticsConfig.postHogHost,
                // we do that manually
                captureScreenViews = false,
        ).also {
            if (buildMeta.isDebug) {
                it.debug = true
            }
        }
        return PostHogAndroid.with(context, config)
    }
}
