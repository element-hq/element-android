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
import im.vector.matrix.android.internal.legacy.data.RoomState;
import im.vector.matrix.android.internal.legacy.data.store.IMXStore;
import im.vector.matrix.android.internal.legacy.rest.callback.ApiCallback;
import im.vector.matrix.android.internal.legacy.rest.model.Event;
import im.vector.matrix.android.internal.legacy.rest.model.sync.InvitedRoomSync;
import im.vector.matrix.android.internal.legacy.rest.model.sync.RoomSync;

/**
 * A `EventTimeline` instance represents a contiguous sequence of events in a room.
 * <p>
 * There are two kinds of timeline:
 * <p>
 * - live timelines: they receive live events from the events stream. You can paginate
 * backwards but not forwards.
 * All (live or backwards) events they receive are stored in the store of the current
 * MXSession.
 * <p>
 * - past timelines: they start in the past from an `initialEventId`. They are filled
 * with events on calls of [MXEventTimeline paginate] in backwards or forwards direction.
 * Events are stored in a in-memory store (MXMemoryStore).
 */
public interface EventTimeline {
    /**
     * Defines that the current timeline is an historical one
     *
     * @param isHistorical true when the current timeline is an historical one
     */
    void setIsHistorical(boolean isHistorical);

    /**
     * Returns true if the current timeline is an historical one
     */
    boolean isHistorical();

    /**
     * @return the unique identifier
     */
    String getTimelineId();

    /**
     * @return the dedicated room
     */
    Room getRoom();

    /**
     * @return the used store
     */
    IMXStore getStore();

    /**
     * @return the initial event id.
     */
    String getInitialEventId();

    /**
     * @return true if this timeline is the live one
     */
    boolean isLiveTimeline();

    /**
     * Get whether we are at the end of the message stream
     *
     * @return true if end has been reached
     */
    boolean hasReachedHomeServerForwardsPaginationEnd();

    /**
     * Reset the back state so that future history requests start over from live.
     * Must be called when opening a room if interested in history.
     */
    void initHistory();

    /**
     * @return The state of the room at the top most recent event of the timeline.
     */
    RoomState getState();

    /**
     * Update the state.
     *
     * @param state the new state.
     */
    void setState(RoomState state);

    /**
     * Handle the invitation room events
     *
     * @param invitedRoomSync the invitation room events.
     */
    void handleInvitedRoomSync(InvitedRoomSync invitedRoomSync);

    /**
     * Manage the joined room events.
     *
     * @param roomSync            the roomSync.
     * @param isGlobalInitialSync true if the sync has been triggered by a global initial sync
     */
    void handleJoinedRoomSync(@NonNull RoomSync roomSync, boolean isGlobalInitialSync);

    /**
     * Store an outgoing event.
     *
     * @param event the event to store
     */
    void storeOutgoingEvent(Event event);

    /**
     * Tells if a back pagination can be triggered.
     *
     * @return true if a back pagination can be triggered.
     */
    boolean canBackPaginate();

    /**
     * Request older messages.
     *
     * @param callback the asynchronous callback
     * @return true if request starts
     */
    boolean backPaginate(ApiCallback<Integer> callback);

    /**
     * Request older messages.
     *
     * @param eventCount number of events we want to retrieve
     * @param callback   callback to implement to be informed that the pagination request has been completed. Can be null.
     * @return true if request starts
     */
    boolean backPaginate(int eventCount, ApiCallback<Integer> callback);

    /**
     * Request older messages.
     *
     * @param eventCount    number of events we want to retrieve
     * @param useCachedOnly to use the cached events list only (i.e no request will be triggered)
     * @param callback      callback to implement to be informed that the pagination request has been completed. Can be null.
     * @return true if request starts
     */
    boolean backPaginate(int eventCount, boolean useCachedOnly, ApiCallback<Integer> callback);

    /**
     * Request newer messages.
     *
     * @param callback callback to implement to be informed that the pagination request has been completed. Can be null.
     * @return true if request starts
     */
    boolean forwardPaginate(ApiCallback<Integer> callback);

    /**
     * Trigger a pagination in the expected direction.
     *
     * @param direction the direction.
     * @param callback  the callback.
     * @return true if the operation succeeds
     */
    boolean paginate(Direction direction, ApiCallback<Integer> callback);

    /**
     * Cancel any pending pagination requests
     */
    void cancelPaginationRequests();

    /**
     * Reset the pagination timeline and start loading the context around its `initialEventId`.
     * The retrieved (backwards and forwards) events will be sent to registered listeners.
     *
     * @param limit    the maximum number of messages to get around the initial event.
     * @param callback the operation callback
     */
    void resetPaginationAroundInitialEvent(int limit, ApiCallback<Void> callback);

    /**
     * Add an events listener.
     *
     * @param listener the listener to add.
     */
    void addEventTimelineListener(Listener listener);

    /**
     * Remove an events listener.
     *
     * @param listener the listener to remove.
     */
    void removeEventTimelineListener(Listener listener);

    /**
     * The direction from which an incoming event is considered.
     */
    enum Direction {
        /**
         * Forwards when the event is added to the end of the timeline.
         * These events come from the /sync stream or from forwards pagination.
         */
        FORWARDS,

        /**
         * Backwards when the event is added to the start of the timeline.
         * These events come from a back pagination.
         */
        BACKWARDS
    }

    interface Listener {

        /**
         * Call when an event has been handled in the timeline.
         *
         * @param event     the event.
         * @param direction the direction.
         * @param roomState the room state
         */
        void onEvent(Event event, Direction direction, RoomState roomState);
    }
}
