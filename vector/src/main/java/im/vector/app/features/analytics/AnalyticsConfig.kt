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

package im.vector.app.features.analytics

import im.vector.app.BuildConfig
import timber.log.Timber

data class AnalyticsConfig(
        val postHogHost: String,
        val postHogApiKey: String
) {
    companion object {
        /**
         * Read the analytics config from the Build config
         */
        fun getConfig(): AnalyticsConfig? {
            val postHogHost = BuildConfig.ANALYTICS_POSTHOG_HOST.takeIf { it.isNotEmpty() }
                    ?: return null.also { Timber.w("Analytics is disabled, ANALYTICS_POSTHOG_HOST is empty") }
            val postHogApiKey = BuildConfig.ANALYTICS_POSTHOG_API_KEY.takeIf { it.isNotEmpty() }
                    ?: return null.also { Timber.w("Analytics is disabled, ANALYTICS_POSTHOG_API_KEY is empty") }

            return AnalyticsConfig(
                    postHogHost = postHogHost,
                    postHogApiKey = postHogApiKey
            )
        }

        fun isAnalyticsEnabled(): Boolean {
            return getConfig() != null
        }
    }
}
