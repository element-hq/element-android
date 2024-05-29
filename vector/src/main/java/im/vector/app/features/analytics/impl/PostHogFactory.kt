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
