/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fixtures

import im.vector.app.features.analytics.itf.VectorAnalyticsEvent
import im.vector.app.features.analytics.itf.VectorAnalyticsScreen

fun aVectorAnalyticsScreen(
        name: String = "a-screen-name",
        properties: Map<String, Any>? = null
) = object : VectorAnalyticsScreen {
    override fun getName() = name
    override fun getProperties() = properties
}

fun aVectorAnalyticsEvent(
        name: String = "an-event-name",
        properties: Map<String, Any>? = null
) = object : VectorAnalyticsEvent {
    override fun getName() = name
    override fun getProperties() = properties
}
