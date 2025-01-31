/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.sync

sealed interface SyncRequestState {
    /**
     * For initial sync.
     */
    interface InitialSyncRequestState : SyncRequestState

    object Idle : InitialSyncRequestState
    data class InitialSyncProgressing(
            val initialSyncStep: InitialSyncStep,
            val percentProgress: Int = 0
    ) : InitialSyncRequestState

    /**
     * For incremental sync.
     */
    interface IncrementalSyncRequestState : SyncRequestState

    object IncrementalSyncIdle : IncrementalSyncRequestState
    data class IncrementalSyncParsing(
            val rooms: Int,
            val toDevice: Int
    ) : IncrementalSyncRequestState

    object IncrementalSyncError : IncrementalSyncRequestState
    object IncrementalSyncDone : IncrementalSyncRequestState
}
