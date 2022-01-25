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

package im.vector.app.features.analytics.screen

import android.os.SystemClock
import im.vector.app.features.analytics.AnalyticsTracker
import im.vector.app.features.analytics.plan.Screen
import timber.log.Timber

/**
 * Track a screen display. Unique usage.
 */
class ScreenEvent(val screenName: Screen.ScreenName) {
    private val startTime = SystemClock.elapsedRealtime()

    // Protection to avoid multiple sending
    private var isSent = false

    /**
     * @param screenNameOverride can be used to override the screen name passed in constructor parameter
     */
    fun send(analyticsTracker: AnalyticsTracker,
             screenNameOverride: Screen.ScreenName? = null) {
        if (isSent) {
            Timber.w("Event $screenName Already sent!")
            return
        }
        isSent = true
        analyticsTracker.screen(
                Screen(
                        screenName = screenNameOverride ?: screenName,
                        durationMs = (SystemClock.elapsedRealtime() - startTime).toInt()
                )
        )
    }
}
