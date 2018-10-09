/*
 * Copyright 2015 OpenMarket Ltd
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

package im.vector.matrix.android.internal.legacy.data.store;

/**
 * An interface for listening the store events
 */
public interface IMXStoreListener {
    /**
     * The store has loaded its internal data.
     * Let any post processing data management.
     * It is called in the store thread before calling onStoreReady.
     *
     * @param accountId the account id
     */
    void postProcess(String accountId);

    /**
     * Called when the store is initialized
     *
     * @param accountId the account identifier
     */
    void onStoreReady(String accountId);

    /**
     * Called when the store initialization fails.
     *
     * @param accountId   the account identifier
     * @param description the corruption error messages
     */
    void onStoreCorrupted(String accountId, String description);

    /**
     * Called when the store has no more memory
     *
     * @param accountId   the account identifier
     * @param description the corruption error messages
     */
    void onStoreOOM(String accountId, String description);

    /**
     * The read receipts of a room is loaded are loaded
     *
     * @param roomId the room id
     */
    void onReadReceiptsLoaded(String roomId);
}
