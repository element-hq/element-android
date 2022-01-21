/*
 * Copyright (c) 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.api.session.statistics

/**
 * Statistic Events. You can subscribe to received such events using [Session.Listener]
 */
sealed interface StatisticEvent {
    /**
     * Initial sync request and response downloading, not including parsing and storage of response
     */
    data class InitialSyncRequest(val durationMs: Int, val nbOfRooms: Int) : StatisticEvent

    /**
     * Initial sync treatment: parsing and storage of response
     */
    data class InitialSyncTreatment(val durationMs: Int, val nbOfRooms: Int) : StatisticEvent

    /**
     * Incremental sync event
     */
    data class SyncTreatment(val durationMs: Int, val afterPause: Boolean, val nbOfRooms: Int) : StatisticEvent
}
