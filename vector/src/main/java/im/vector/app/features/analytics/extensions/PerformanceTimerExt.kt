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
        is StatisticEvent.SyncTreatment      ->
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
