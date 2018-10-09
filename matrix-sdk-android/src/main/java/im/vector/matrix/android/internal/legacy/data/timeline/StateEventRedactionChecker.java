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
import android.text.TextUtils;

import im.vector.matrix.android.internal.legacy.MXDataHandler;
import im.vector.matrix.android.internal.legacy.data.Room;
import im.vector.matrix.android.internal.legacy.data.RoomState;
import im.vector.matrix.android.internal.legacy.data.store.IMXStore;
import im.vector.matrix.android.internal.legacy.rest.callback.ApiCallback;
import im.vector.matrix.android.internal.legacy.rest.callback.SimpleApiCallback;
import im.vector.matrix.android.internal.legacy.rest.model.Event;
import im.vector.matrix.android.internal.legacy.rest.model.MatrixError;
import im.vector.matrix.android.internal.legacy.rest.model.RoomMember;
import im.vector.matrix.android.internal.legacy.util.Log;

import java.util.List;

import javax.annotation.Nonnull;

/**
 * This class is responsible of checking state events redaction.
 */
class StateEventRedactionChecker {

    private static final String LOG_TAG = StateEventRedactionChecker.class.getSimpleName();
    private final EventTimeline mEventTimeline;
    private final TimelineStateHolder mTimelineStateHolder;

    StateEventRedactionChecker(@NonNull final EventTimeline eventTimeline,
                               @NonNull final TimelineStateHolder timelineStateHolder) {
        mEventTimeline = eventTimeline;
        mTimelineStateHolder = timelineStateHolder;
    }

    /**
     * Redaction of a state event might require to reload the timeline
     * because the room states has to be updated.
     *
     * @param redactionEvent the redaction event
     */
    public void checkStateEventRedaction(@NonNull final Event redactionEvent) {
        final IMXStore store = mEventTimeline.getStore();
        final Room room = mEventTimeline.getRoom();
        final MXDataHandler dataHandler = room.getDataHandler();
        final String roomId = room.getRoomId();
        final String eventId = redactionEvent.getRedactedEventId();
        final RoomState state = mTimelineStateHolder.getState();
        Log.d(LOG_TAG, "checkStateEventRedaction of event " + eventId);
        // check if the state events is locally known
        state.getStateEvents(store, null, new SimpleApiCallback<List<Event>>() {
            @Override
            public void onSuccess(List<Event> stateEvents) {

                // Check whether the current room state depends on this redacted event.
                boolean isFound = false;
                for (int index = 0; index < stateEvents.size(); index++) {
                    Event stateEvent = stateEvents.get(index);

                    if (TextUtils.equals(stateEvent.eventId, eventId)) {

                        Log.d(LOG_TAG, "checkStateEventRedaction: the current room state has been modified by the event redaction");

                        // remove expected keys
                        stateEvent.prune(redactionEvent);
                        stateEvents.set(index, stateEvent);
                        // digest the updated state
                        mTimelineStateHolder.processStateEvent(stateEvent, EventTimeline.Direction.FORWARDS);
                        isFound = true;
                        break;
                    }
                }

                if (!isFound) {
                    // Else try to find the redacted event among members which
                    // are stored apart from other state events

                    // Reason: The membership events are not anymore stored in the application store
                    // until we have found a way to improve the way they are stored.
                    // It used to have many out of memory errors because they are too many stored small memory objects.
                    // see https://github.com/matrix-org/matrix-android-sdk/issues/196

                    // Note: if lazy loading is on, getMemberByEventId() can return null, but it is ok, because we just want to update our cache
                    RoomMember member = state.getMemberByEventId(eventId);
                    if (member != null) {
                        Log.d(LOG_TAG, "checkStateEventRedaction: the current room members list has been modified by the event redaction");

                        // the android SDK does not store stock member events but a representation of them, RoomMember.
                        // Prune this representation
                        member.prune();

                        isFound = true;
                    }
                }

                if (isFound) {
                    store.storeLiveStateForRoom(roomId);
                    // warn that there was a flush
                    mEventTimeline.initHistory();
                    dataHandler.onRoomFlush(roomId);
                } else {
                    Log.d(LOG_TAG, "checkStateEventRedaction: the redacted event is unknown. Fetch it from the homeserver");
                    checkStateEventRedactionWithHomeserver(dataHandler, roomId, eventId);
                }
            }
        });
    }

    /**
     * Check with the HS whether the redacted event impacts the room data we have locally.
     * If yes, local data must be pruned.
     *
     * @param eventId the redacted event id
     */
    private void checkStateEventRedactionWithHomeserver(@Nonnull final MXDataHandler dataHandler,
                                                        @Nonnull final String roomId,
                                                        @Nonnull final String eventId) {
        Log.d(LOG_TAG, "checkStateEventRedactionWithHomeserver on event Id " + eventId);

        // We need to figure out if this redacted event is a room state in the past.
        // If yes, we must prune the `prev_content` of the state event that replaced it.
        // Indeed, redacted information shouldn't spontaneously appear when you backpaginate...
        // TODO: This is no more implemented (see https://github.com/vector-im/riot-ios/issues/443).
        // The previous implementation based on a room initial sync was too heavy server side
        // and has been removed.
        if (!TextUtils.isEmpty(eventId)) {
            Log.d(LOG_TAG, "checkStateEventRedactionWithHomeserver : retrieving the event");
            dataHandler.getDataRetriever().getRoomsRestClient().getEvent(roomId, eventId, new ApiCallback<Event>() {
                @Override
                public void onSuccess(Event event) {
                    if (null != event && null != event.stateKey) {
                        Log.d(LOG_TAG, "checkStateEventRedactionWithHomeserver : the redacted event is a state event in the past." +
                                " TODO: prune prev_content of the new state event");

                    } else {
                        Log.d(LOG_TAG, "checkStateEventRedactionWithHomeserver : the redacted event is a not state event -> job is done");
                    }
                }

                @Override
                public void onNetworkError(Exception e) {
                    Log.e(LOG_TAG, "checkStateEventRedactionWithHomeserver : failed to retrieved the redacted event: onNetworkError " + e.getMessage(), e);
                }

                @Override
                public void onMatrixError(MatrixError e) {
                    Log.e(LOG_TAG, "checkStateEventRedactionWithHomeserver : failed to retrieved the redacted event: onNetworkError " + e.getMessage());
                }

                @Override
                public void onUnexpectedError(Exception e) {
                    Log.e(LOG_TAG, "checkStateEventRedactionWithHomeserver : failed to retrieved the redacted event: onNetworkError " + e.getMessage(), e);
                }
            });
        }
    }


}
