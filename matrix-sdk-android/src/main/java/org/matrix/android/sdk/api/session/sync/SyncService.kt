/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.sync

import androidx.lifecycle.LiveData
import kotlinx.coroutines.flow.SharedFlow
import org.matrix.android.sdk.api.session.sync.model.SyncResponse

interface SyncService {
    /**
     * This method start the sync thread.
     */
    fun startSync(fromForeground: Boolean)

    /**
     * This method stop the sync thread.
     */
    fun stopSync()

    /**
     * Requires a one time background sync.
     */
    fun requireBackgroundSync()

    /**
     * Launches infinite self rescheduling background syncs via the WorkManager.
     *
     * While dozing, syncs will only occur during maintenance windows.
     * For reliability it's recommended to also start a long running foreground service
     * along with disabling battery optimizations.
     */
    fun startAutomaticBackgroundSync(timeOutInSeconds: Long, repeatDelayInSeconds: Long)

    fun stopAnyBackgroundSync()

    /**
     * This method returns the current sync state.
     * @return the current [SyncState].
     */
    fun getSyncState(): SyncState

    /**
     * This method returns true if the sync thread is alive, i.e. started.
     */
    fun isSyncThreadAlive(): Boolean

    /**
     * This method allows to listen the sync state.
     * @return a [LiveData] of [SyncState].
     */
    fun getSyncStateLive(): LiveData<SyncState>

    /**
     * Get the [SyncRequestState] as a SharedFlow.
     */
    fun getSyncRequestStateFlow(): SharedFlow<SyncRequestState>

    /**
     * This method returns a flow of SyncResponse. New value will be pushed through the sync thread.
     */
    fun syncFlow(): SharedFlow<SyncResponse>

    /**
     * This methods return true if an initial sync has been processed.
     */
    fun hasAlreadySynced(): Boolean
}
