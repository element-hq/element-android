/* 
 * Copyright 2016 OpenMarket Ltd
 * Copyright 2017 Vector Creations Ltd
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

package im.vector.matrix.android.internal.legacy.rest.model.sync;

import im.vector.matrix.android.internal.legacy.rest.model.group.GroupsSyncResponse;

import java.util.Map;

// SyncResponse represents the request response for server sync v2.
public class SyncResponse {

    /**
     * The user private data.
     */
    public Map<String, Object> accountData;

    /**
     * The opaque token for the end.
     */
    public String nextBatch;

    /**
     * The updates to the presence status of other users.
     */
    public PresenceSyncResponse presence;

    /*
     * Data directly sent to one of user's devices.
     */
    public ToDeviceSyncResponse toDevice;

    /**
     * List of rooms.
     */
    public RoomsSyncResponse rooms;

    /**
     * Devices list update
     */
    public DeviceListResponse deviceLists;

    /**
     * One time keys management
     */
    public DeviceOneTimeKeysCountSyncResponse deviceOneTimeKeysCount;

    /**
     * List of groups.
     */
    public GroupsSyncResponse groups;
}