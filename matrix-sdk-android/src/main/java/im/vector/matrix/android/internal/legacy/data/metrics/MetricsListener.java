/*
 * Copyright 2018 New Vector Ltd
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
package im.vector.matrix.android.internal.legacy.data.metrics;

/**
 * This interface defines methods for collecting metrics data associated with startup times and stats
 * Those callbacks can be called from any threads
 */
public interface MetricsListener {

    /**
     * Called when the initial sync is finished
     *
     * @param duration of the sync
     */
    void onInitialSyncFinished(long duration);

    /**
     * Called when the incremental sync is finished
     *
     * @param duration of the sync
     */
    void onIncrementalSyncFinished(long duration);

    /**
     * Called when a store is preloaded
     *
     * @param duration of the preload
     */
    void onStorePreloaded(long duration);

    /**
     * Called when a sync is complete
     *
     * @param nbOfRooms loaded in the @SyncResponse
     */
    void onRoomsLoaded(int nbOfRooms);

}
