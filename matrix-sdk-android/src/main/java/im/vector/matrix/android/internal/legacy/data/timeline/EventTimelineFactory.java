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

package im.vector.matrix.android.internal.legacy.data.timeline;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import im.vector.matrix.android.internal.legacy.MXDataHandler;
import im.vector.matrix.android.internal.legacy.data.Room;
import im.vector.matrix.android.internal.legacy.data.store.MXMemoryStore;

/**
 * This factory creates MXEventTimeline instances
 */
public class EventTimelineFactory {

    /**
     * Method to create a live timeline associated with the room.
     *
     * @param dataHandler the dataHandler
     * @param room        the linked room
     * @param roomId      the room id
     */
    public static EventTimeline liveTimeline(@NonNull final MXDataHandler dataHandler,
                                             @NonNull final Room room,
                                             @NonNull final String roomId) {
        return new MXEventTimeline(dataHandler.getStore(roomId), dataHandler, room, roomId, null, true);
    }

    /**
     * Method to create an in memory timeline for a room.
     *
     * @param dataHandler the data handler
     * @param roomId      the room id.
     */
    public static EventTimeline inMemoryTimeline(@NonNull final MXDataHandler dataHandler,
                                                 @NonNull final String roomId) {
        return inMemoryTimeline(dataHandler, roomId, null);
    }

    /**
     * Method to create a past timeline around an eventId.
     * It will create a memory store and a room
     *
     * @param dataHandler the data handler
     * @param roomId      the room id
     * @param eventId     the event id
     */
    public static EventTimeline pastTimeline(@NonNull final MXDataHandler dataHandler,
                                             @NonNull final String roomId,
                                             @NonNull final String eventId) {
        return inMemoryTimeline(dataHandler, roomId, eventId);
    }

    /* ==========================================================================================
     * Private
     * ========================================================================================== */

    /**
     * Method to create a in memory timeline.
     * It will create a memory store and a room
     *
     * @param dataHandler the data handler
     * @param roomId      the room id
     * @param eventId     the event id or null
     */
    private static EventTimeline inMemoryTimeline(@NonNull final MXDataHandler dataHandler,
                                                  @NonNull final String roomId,
                                                  @Nullable final String eventId) {
        final MXMemoryStore store = new MXMemoryStore(dataHandler.getCredentials(), null);
        final Room room = dataHandler.getRoom(store, roomId, true);
        final EventTimeline eventTimeline = new MXEventTimeline(store, dataHandler, room, roomId, eventId, false);
        room.setTimeline(eventTimeline);
        room.setReadyState(true);
        return eventTimeline;
    }
}
