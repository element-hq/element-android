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
import im.vector.matrix.android.internal.legacy.data.RoomState;
import im.vector.matrix.android.internal.legacy.data.store.IMXStore;
import im.vector.matrix.android.internal.legacy.rest.model.Event;

/**
 * This class is responsible for holding the state and backState of a room timeline
 */
class TimelineStateHolder {

    private final MXDataHandler mDataHandler;
    private final IMXStore mStore;
    private String mRoomId;

    /**
     * The state of the room at the top most recent event of the timeline.
     */
    private RoomState mState;

    /**
     * The historical state of the room when paginating back.
     */
    private RoomState mBackState;

    TimelineStateHolder(@NonNull final MXDataHandler dataHandler,
                        @NonNull final IMXStore store,
                        @NonNull final String roomId) {
        mDataHandler = dataHandler;
        mStore = store;
        mRoomId = roomId;
        initStates();
    }

    /**
     * Clear the states
     */
    public void clear() {
        initStates();
    }

    /**
     * @return The state of the room at the top most recent event of the timeline.
     */
    @NonNull
    public RoomState getState() {
        return mState;
    }

    /**
     * Update the state.
     *
     * @param state the new state.
     */
    public void setState(@NonNull final RoomState state) {
        mState = state;
    }

    /**
     * @return the backState.
     */
    @NonNull
    public RoomState getBackState() {
        return mBackState;
    }

    /**
     * Update the backState.
     *
     * @param state the new backState.
     */
    public void setBackState(@NonNull final RoomState state) {
        mBackState = state;
    }

    /**
     * Make a deep copy or the dedicated state.
     *
     * @param direction the room state direction to deep copy.
     */
    public void deepCopyState(final EventTimeline.Direction direction) {
        if (direction == EventTimeline.Direction.FORWARDS) {
            mState = mState.deepCopy();
        } else {
            mBackState = mBackState.deepCopy();
        }
    }

    /**
     * Process a state event to keep the internal live and back states up to date.
     *
     * @param event     the state event
     * @param direction the direction; ie. forwards for live state, backwards for back state
     * @return true if the event has been processed.
     */
    public boolean processStateEvent(@NonNull final Event event,
                                     @NonNull final EventTimeline.Direction direction) {
        final RoomState affectedState = direction == EventTimeline.Direction.FORWARDS ? mState : mBackState;
        final boolean isProcessed = affectedState.applyState(mStore, event, direction);
        if (isProcessed && direction == EventTimeline.Direction.FORWARDS) {
            mStore.storeLiveStateForRoom(mRoomId);
        }
        return isProcessed;
    }

    /**
     * Set the room Id
     *
     * @param roomId the new room id.
     */
    public void setRoomId(@NonNull final String roomId) {
        mRoomId = roomId;
        mState.roomId = roomId;
        mBackState.roomId = roomId;
    }

    /**
     * Initialize the state and backState to default, with roomId and dataHandler
     */
    private void initStates() {
        mBackState = new RoomState();
        mBackState.setDataHandler(mDataHandler);
        mBackState.roomId = mRoomId;
        mState = new RoomState();
        mState.setDataHandler(mDataHandler);
        mState.roomId = mRoomId;
    }


}
