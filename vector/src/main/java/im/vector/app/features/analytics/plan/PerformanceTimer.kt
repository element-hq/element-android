/*
 * Copyright 2021-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.analytics.plan

import im.vector.app.features.analytics.itf.VectorAnalyticsEvent

// GENERATED FILE, DO NOT EDIT. FOR MORE INFORMATION VISIT
// https://github.com/matrix-org/matrix-analytics-events/

/**
 * Triggered after timing an operation in the app.
 */
data class PerformanceTimer(
        /**
         * Client defined, can be used for debugging.
         */
        val context: String? = null,
        /**
         * Client defined, an optional value to indicate how many items were
         * handled during the operation.
         */
        val itemCount: Int? = null,
        /**
         * The timer that is being reported.
         */
        val name: Name,
        /**
         * The time reported by the timer in milliseconds.
         */
        val timeMs: Int,
) : VectorAnalyticsEvent {

    enum class Name {
        /**
         * The time spent parsing the response from an initial /sync request. In
         * this case, `itemCount` should contain the number of joined rooms.
         */
        InitialSyncParsing,

        /**
         * The time spent waiting for a response to an initial /sync request. In
         * this case, `itemCount` should contain the number of joined rooms.
         */
        InitialSyncRequest,

        /**
         * The time taken to display an event in the timeline that was opened
         * from a notification.
         */
        NotificationsOpenEvent,

        /**
         * The duration of a regular /sync request when resuming the app. In
         * this case, `itemCount` should contain the number of joined rooms in
         * the response.
         */
        StartupIncrementalSync,

        /**
         * The duration of an initial /sync request during startup (if the store
         * has been wiped). In this case, `itemCount` should contain the number
         * of joined rooms.
         */
        StartupInitialSync,

        /**
         * How long the app launch screen is displayed for.
         */
        StartupLaunchScreen,

        /**
         * The time to preload data in the MXStore on iOS. In this case,
         * `itemCount` should contain the number of rooms in the store.
         */
        StartupStorePreload,

        /**
         * The time to load all data from the store (including
         * StartupStorePreload time). In this case, `itemCount` should contain
         * the number of rooms loaded into the session
         */
        StartupStoreReady,
    }

    override fun getName() = "PerformanceTimer"

    override fun getProperties(): Map<String, Any>? {
        return mutableMapOf<String, Any>().apply {
            context?.let { put("context", it) }
            itemCount?.let { put("itemCount", it) }
            put("name", name.name)
            put("timeMs", timeMs)
        }.takeIf { it.isNotEmpty() }
    }
}
