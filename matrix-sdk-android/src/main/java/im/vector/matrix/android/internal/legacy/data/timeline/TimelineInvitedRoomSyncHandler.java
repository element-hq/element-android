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

import im.vector.matrix.android.internal.legacy.data.Room;
import im.vector.matrix.android.internal.legacy.rest.model.Event;
import im.vector.matrix.android.internal.legacy.rest.model.sync.InvitedRoomSync;

import javax.annotation.Nullable;

/**
 * This class is responsible for handling the invitation room events from the SyncResponse
 */
class TimelineInvitedRoomSyncHandler {

    private final Room mRoom;
    private final TimelineLiveEventHandler mLiveEventHandler;
    private final InvitedRoomSync mInvitedRoomSync;

    TimelineInvitedRoomSyncHandler(@NonNull final Room room,
                                   @NonNull final TimelineLiveEventHandler liveEventHandler,
                                   @Nullable final InvitedRoomSync invitedRoomSync) {
        mRoom = room;
        mLiveEventHandler = liveEventHandler;
        mInvitedRoomSync = invitedRoomSync;
    }

    /**
     * Handle the invitation room events
     */
    public void handle() {
        // Handle the state events as live events (the room state will be updated, and the listeners (if any) will be notified).
        if (mInvitedRoomSync != null && mInvitedRoomSync.inviteState != null && mInvitedRoomSync.inviteState.events != null) {
            final String roomId = mRoom.getRoomId();

            for (Event event : mInvitedRoomSync.inviteState.events) {
                // Add a fake event id if none in order to be able to store the event
                if (event.eventId == null) {
                    event.eventId = roomId + "-" + System.currentTimeMillis() + "-" + event.hashCode();
                }

                // The roomId is not defined.
                event.roomId = roomId;
                mLiveEventHandler.handleLiveEvent(event, false, true);
            }
            // The room related to the pending invite can be considered as ready from now
            mRoom.setReadyState(true);
        }
    }


}
