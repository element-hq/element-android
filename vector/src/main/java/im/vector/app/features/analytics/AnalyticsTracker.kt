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

import im.vector.app.features.analytics.itf.VectorAnalyticsEvent
import im.vector.app.features.analytics.itf.VectorAnalyticsScreen
import im.vector.app.features.analytics.plan.UserProperties

interface AnalyticsTracker {
    /**
     * Capture an Event.
     *
     * @param event The event to capture.
     * @param extraProperties Some extra properties to attach to the event, that are not part of the events definition
     * (https://github.com/matrix-org/matrix-analytics-events/) and specific to this platform.
     */
    fun capture(event: VectorAnalyticsEvent, extraProperties: Map<String, String>? = null)

    /**
     * Track a displayed screen.
     */
    fun screen(screen: VectorAnalyticsScreen)

    /**
     * Update user specific properties.
     */
    fun updateUserProperties(userProperties: UserProperties)
}
