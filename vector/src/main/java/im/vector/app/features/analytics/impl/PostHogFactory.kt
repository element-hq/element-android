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
import com.posthog.android.PostHog
import im.vector.app.BuildConfig
import im.vector.app.config.analyticsConfig
import javax.inject.Inject

class PostHogFactory @Inject constructor(private val context: Context) {

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
        return if (BuildConfig.DEBUG) {
            PostHog.LogLevel.DEBUG
        } else {
            PostHog.LogLevel.INFO
        }
    }
}
