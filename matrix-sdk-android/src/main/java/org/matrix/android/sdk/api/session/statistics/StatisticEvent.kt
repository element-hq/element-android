/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.statistics

/**
 * Statistic Events. You can subscribe to received such events using [Session.Listener].
 */
sealed interface StatisticEvent {
    /**
     * Initial sync request, response downloading, and treatment (parsing and storage) of response.
     */
    data class InitialSyncRequest(
            val requestDurationMs: Int,
            val downloadDurationMs: Int,
            val treatmentDurationMs: Int,
            val nbOfJoinedRooms: Int
    ) : StatisticEvent

    /**
     * Incremental sync event.
     */
    data class SyncTreatment(
            val durationMs: Int,
            val afterPause: Boolean,
            val nbOfJoinedRooms: Int
    ) : StatisticEvent
}
