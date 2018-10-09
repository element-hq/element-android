/* 
 * Copyright 2016 OpenMarket Ltd
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

import com.google.gson.annotations.SerializedName;

// RoomSync represents the response for a room during server sync v2.
public class RoomSync {
    /**
     * The state updates for the room.
     */
    public RoomSyncState state;

    /**
     * The timeline of messages and state changes in the room.
     */
    public RoomSyncTimeline timeline;

    /**
     * The ephemeral events in the room that aren't recorded in the timeline or state of the room (e.g. typing, receipts).
     */
    public RoomSyncEphemeral ephemeral;

    /**
     * The account data events for the room (e.g. tags).
     */
    public RoomSyncAccountData accountData;

    /**
     The notification counts for the room.
     */
    public RoomSyncUnreadNotifications unreadNotifications;

    /**
     * The room summary
     */
    @SerializedName("summary")
    public RoomSyncSummary roomSyncSummary;

}