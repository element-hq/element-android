/*
 * Copyright 2014 OpenMarket Ltd
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

import im.vector.matrix.android.internal.legacy.rest.model.Event;
import im.vector.matrix.android.internal.legacy.rest.model.TokensChunkEvents;

import java.util.List;

/**
 * Class representing a room from a JSON response from room or global initial sync.
 */
public class RoomResponse {
    // The room identifier.
    public String roomId;

    // The last recent messages of the room.
    public TokensChunkEvents messages;

    // The state events.
    public List<Event> state;

    // The private data that this user has attached to this room.
    public List<Event> accountData;

    // The current user membership in this room.
    public String membership;

    // The room visibility (public/private).
    public String visibility;

    // The matrix id of the inviter in case of pending invitation.
    public String inviter;

    // The invite event if membership is invite.
    public Event invite;

    // The presence status of other users (Provided in case of room initial sync @see http://matrix.org/docs/api/client-server/#!/-rooms/get_room_sync_data)).
    public List<Event> presence;

    // The read receipts (Provided in case of room initial sync).
    public List<Event> receipts;
}
