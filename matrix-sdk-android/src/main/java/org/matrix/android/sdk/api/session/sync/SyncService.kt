/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
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
