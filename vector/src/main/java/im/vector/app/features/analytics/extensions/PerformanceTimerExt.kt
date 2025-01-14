/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.analytics.extensions

import im.vector.app.features.analytics.plan.PerformanceTimer
import org.matrix.android.sdk.api.session.statistics.StatisticEvent

fun StatisticEvent.toListOfPerformanceTimer(): List<PerformanceTimer> {
    return when (this) {
        is StatisticEvent.InitialSyncRequest ->
            listOf(
                    PerformanceTimer(
                            name = PerformanceTimer.Name.InitialSyncRequest,
                            timeMs = requestDurationMs + downloadDurationMs,
                            itemCount = nbOfJoinedRooms
                    ),
                    PerformanceTimer(
                            name = PerformanceTimer.Name.InitialSyncParsing,
                            timeMs = treatmentDurationMs,
                            itemCount = nbOfJoinedRooms
                    )
            )
        is StatisticEvent.SyncTreatment ->
            if (afterPause) {
                listOf(
                        PerformanceTimer(
                                name = PerformanceTimer.Name.StartupIncrementalSync,
                                timeMs = durationMs,
                                itemCount = nbOfJoinedRooms
                        )
                )
            } else {
                // We do not report
                emptyList()
            }
    }
}
