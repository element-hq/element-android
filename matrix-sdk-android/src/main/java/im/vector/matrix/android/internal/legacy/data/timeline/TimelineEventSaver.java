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

import im.vector.matrix.android.internal.legacy.MXDataHandler;
import im.vector.matrix.android.internal.legacy.data.Room;
import im.vector.matrix.android.internal.legacy.data.RoomState;
import im.vector.matrix.android.internal.legacy.data.RoomSummary;
import im.vector.matrix.android.internal.legacy.data.store.IMXStore;
import im.vector.matrix.android.internal.legacy.rest.model.Event;
import im.vector.matrix.android.internal.legacy.rest.model.ReceiptData;

/**
 * This class handles storing a live room event in a dedicated store.
 */
class TimelineEventSaver {

    private final IMXStore mStore;
    private final Room mRoom;
    private final TimelineStateHolder mTimelineStateHolder;

    TimelineEventSaver(@NonNull final IMXStore store,
                       @NonNull final Room room,
                       @NonNull final TimelineStateHolder timelineStateHolder) {
        mStore = store;
        mRoom = room;
        mTimelineStateHolder = timelineStateHolder;
    }

    /**
     * * Store a live room event.
     *
     * @param event the event to be stored.
     */

    public void storeEvent(@NonNull final Event event) {
        final MXDataHandler dataHandler = mRoom.getDataHandler();
        final String myUserId = dataHandler.getCredentials().userId;

        // create dummy read receipt for any incoming event
        // to avoid not synchronized read receipt and event
        if (event.getSender() != null && event.eventId != null) {
            mRoom.handleReceiptData(new ReceiptData(event.getSender(), event.eventId, event.originServerTs));
        }
        mStore.storeLiveRoomEvent(event);
        if (RoomSummary.isSupportedEvent(event)) {
            final RoomState roomState = mTimelineStateHolder.getState();
            RoomSummary summary = mStore.getSummary(event.roomId);
            if (summary == null) {
                summary = new RoomSummary(summary, event, roomState, myUserId);
            } else {
                summary.setLatestReceivedEvent(event, roomState);
            }
            mStore.storeSummary(summary);
        }
    }

}
