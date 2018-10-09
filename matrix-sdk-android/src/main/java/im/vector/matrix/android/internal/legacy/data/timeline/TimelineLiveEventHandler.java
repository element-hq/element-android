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
import im.vector.matrix.android.internal.legacy.data.MyUser;
import im.vector.matrix.android.internal.legacy.data.Room;
import im.vector.matrix.android.internal.legacy.data.RoomState;
import im.vector.matrix.android.internal.legacy.data.RoomSummary;
import im.vector.matrix.android.internal.legacy.data.store.IMXStore;
import im.vector.matrix.android.internal.legacy.rest.model.Event;
import im.vector.matrix.android.internal.legacy.rest.model.EventContent;
import im.vector.matrix.android.internal.legacy.rest.model.RoomMember;
import im.vector.matrix.android.internal.legacy.util.EventDisplay;
import im.vector.matrix.android.internal.legacy.util.JsonUtils;
import im.vector.matrix.android.internal.legacy.util.Log;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

/**
 * This class is responsible for handling live event
 */
class TimelineLiveEventHandler {

    private static final String LOG_TAG = TimelineLiveEventHandler.class.getSimpleName();

    private final MXEventTimeline mEventTimeline;
    private final TimelineEventSaver mTimelineEventSaver;
    private final StateEventRedactionChecker mStateEventRedactionChecker;
    private final TimelinePushWorker mTimelinePushWorker;
    private final TimelineStateHolder mTimelineStateHolder;
    private final TimelineEventListeners mEventListeners;

    TimelineLiveEventHandler(@Nonnull final MXEventTimeline eventTimeline,
                             @Nonnull final TimelineEventSaver timelineEventSaver,
                             @Nonnull final StateEventRedactionChecker stateEventRedactionChecker,
                             @Nonnull final TimelinePushWorker timelinePushWorker,
                             @NonNull final TimelineStateHolder timelineStateHolder,
                             @NonNull final TimelineEventListeners eventListeners) {
        mEventTimeline = eventTimeline;
        mTimelineEventSaver = timelineEventSaver;
        mStateEventRedactionChecker = stateEventRedactionChecker;
        mTimelinePushWorker = timelinePushWorker;
        mTimelineStateHolder = timelineStateHolder;
        mEventListeners = eventListeners;
    }

    /**
     * Handle events coming down from the event stream.
     *
     * @param event                   the live event
     * @param checkRedactedStateEvent set to true to check if it triggers a state event redaction
     * @param withPush                set to true to trigger pushes when it is required
     */
    public void handleLiveEvent(@NonNull final Event event,
                                final boolean checkRedactedStateEvent,
                                final boolean withPush) {
        final IMXStore store = mEventTimeline.getStore();
        final Room room = mEventTimeline.getRoom();
        final MXDataHandler dataHandler = room.getDataHandler();
        final String timelineId = mEventTimeline.getTimelineId();
        final MyUser myUser = dataHandler.getMyUser();

        // Decrypt event if necessary
        dataHandler.decryptEvent(event, timelineId);

        // dispatch the call events to the calls manager
        if (event.isCallEvent()) {
            final RoomState roomState = mTimelineStateHolder.getState();
            dataHandler.getCallsManager().handleCallEvent(store, event);
            storeLiveRoomEvent(dataHandler, store, event, false);
            // the candidates events are not tracked
            // because the users don't need to see the peer exchanges.
            if (!TextUtils.equals(event.getType(), Event.EVENT_TYPE_CALL_CANDIDATES)) {
                // warn the listeners
                // general listeners
                dataHandler.onLiveEvent(event, roomState);
                // timeline listeners
                mEventListeners.onEvent(event, EventTimeline.Direction.FORWARDS, roomState);
            }

            // trigger pushes when it is required
            if (withPush) {
                mTimelinePushWorker.triggerPush(roomState, event);
            }

        } else {
            final Event storedEvent = store.getEvent(event.eventId, event.roomId);

            // avoid processing event twice
            if (storedEvent != null) {
                // an event has been echoed
                if (storedEvent.getAge() == Event.DUMMY_EVENT_AGE) {
                    store.deleteEvent(storedEvent);
                    store.storeLiveRoomEvent(event);
                    store.commit();
                    Log.d(LOG_TAG, "handleLiveEvent : the event " + event.eventId + " in " + event.roomId + " has been echoed");
                } else {
                    Log.d(LOG_TAG, "handleLiveEvent : the event " + event.eventId + " in " + event.roomId + " already exist.");
                    return;
                }
            }

            // Room event
            if (event.roomId != null) {
                // check if the room has been joined
                // the initial sync + the first requestHistory call is done here
                // instead of being done in the application
                if (Event.EVENT_TYPE_STATE_ROOM_MEMBER.equals(event.getType()) && TextUtils.equals(event.getSender(), dataHandler.getUserId())) {
                    EventContent eventContent = JsonUtils.toEventContent(event.getContentAsJsonObject());
                    EventContent prevEventContent = event.getPrevContent();

                    String prevMembership = null;

                    if (prevEventContent != null) {
                        prevMembership = prevEventContent.membership;
                    }

                    // if the membership keeps the same value "join".
                    // it should mean that the user profile has been updated.
                    if (!event.isRedacted() && TextUtils.equals(prevMembership, eventContent.membership)
                            && TextUtils.equals(RoomMember.MEMBERSHIP_JOIN, eventContent.membership)) {
                        // check if the user updates his profile from another device.

                        boolean hasAccountInfoUpdated = false;

                        if (!TextUtils.equals(eventContent.displayname, myUser.displayname)) {
                            hasAccountInfoUpdated = true;
                            myUser.displayname = eventContent.displayname;
                            store.setDisplayName(myUser.displayname, event.getOriginServerTs());
                        }

                        if (!TextUtils.equals(eventContent.avatar_url, myUser.getAvatarUrl())) {
                            hasAccountInfoUpdated = true;
                            myUser.setAvatarUrl(eventContent.avatar_url);
                            store.setAvatarURL(myUser.avatar_url, event.getOriginServerTs());
                        }

                        if (hasAccountInfoUpdated) {
                            dataHandler.onAccountInfoUpdate(myUser);
                        }
                    }
                }

                final RoomState previousState = mTimelineStateHolder.getState();
                if (event.stateKey != null) {
                    // copy the live state before applying any update
                    mTimelineStateHolder.deepCopyState(EventTimeline.Direction.FORWARDS);
                    // check if the event has been processed
                    if (!mTimelineStateHolder.processStateEvent(event, EventTimeline.Direction.FORWARDS)) {
                        // not processed -> do not warn the application
                        // assume that the event is a duplicated one.
                        return;
                    }
                }
                storeLiveRoomEvent(dataHandler, store, event, checkRedactedStateEvent);

                // warn the listeners
                // general listeners
                dataHandler.onLiveEvent(event, previousState);

                // timeline listeners
                mEventListeners.onEvent(event, EventTimeline.Direction.FORWARDS, previousState);

                // trigger pushes when it is required
                if (withPush) {
                    mTimelinePushWorker.triggerPush(mTimelineStateHolder.getState(), event);
                }
            } else {
                Log.e(LOG_TAG, "Unknown live event type: " + event.getType());
            }
        }
    }

    /**
     * Store a live room event.
     *
     * @param event                   The event to be stored.
     * @param checkRedactedStateEvent true to check if this event redacts a state event
     */
    private void storeLiveRoomEvent(@NonNull final MXDataHandler dataHandler,
                                    @NonNull final IMXStore store,
                                    @NonNull Event event,
                                    final boolean checkRedactedStateEvent) {
        boolean shouldBeSaved = false;
        String myUserId = dataHandler.getCredentials().userId;

        if (Event.EVENT_TYPE_REDACTION.equals(event.getType())) {
            if (event.getRedactedEventId() != null) {
                Event eventToPrune = store.getEvent(event.getRedactedEventId(), event.roomId);

                // when an event is redacted, some fields must be kept.
                if (eventToPrune != null) {
                    shouldBeSaved = true;
                    // remove expected keys
                    eventToPrune.prune(event);
                    // store the prune event
                    mTimelineEventSaver.storeEvent(eventToPrune);
                    // store the redaction event too (for the read markers management)
                    mTimelineEventSaver.storeEvent(event);
                    // the redaction check must not be done during an initial sync
                    // or the redacted event is received with roomSync.timeline.limited
                    if (checkRedactedStateEvent && eventToPrune.stateKey != null) {
                        mStateEventRedactionChecker.checkStateEventRedaction(event);
                    }
                    // search the latest displayable event
                    // to replace the summary text
                    final List<Event> events = new ArrayList<>(store.getRoomMessages(event.roomId));
                    for (int index = events.size() - 1; index >= 0; index--) {
                        final Event indexedEvent = events.get(index);
                        if (RoomSummary.isSupportedEvent(indexedEvent)) {
                            // Decrypt event if necessary
                            if (TextUtils.equals(indexedEvent.getType(), Event.EVENT_TYPE_MESSAGE_ENCRYPTED)) {
                                if (null != dataHandler.getCrypto()) {
                                    dataHandler.decryptEvent(indexedEvent, mEventTimeline.getTimelineId());
                                }
                            }
                            final RoomState state = mTimelineStateHolder.getState();
                            final EventDisplay eventDisplay = new EventDisplay(store.getContext(), indexedEvent, state);
                            // ensure that message can be displayed
                            if (!TextUtils.isEmpty(eventDisplay.getTextualDisplay())) {
                                event = indexedEvent;
                                break;
                            }
                        }

                    }
                } else if (checkRedactedStateEvent) {
                    // the redaction check must not be done during an initial sync
                    // or the redacted event is received with roomSync.timeline.limited
                    mStateEventRedactionChecker.checkStateEventRedaction(event);
                }
            }
        } else {
            // the candidate events are not stored.
            shouldBeSaved = !event.isCallEvent() || !Event.EVENT_TYPE_CALL_CANDIDATES.equals(event.getType());
            // thread issue
            // if the user leaves a room,
            if (Event.EVENT_TYPE_STATE_ROOM_MEMBER.equals(event.getType()) && myUserId.equals(event.stateKey)) {
                final String membership = event.getContentAsJsonObject().getAsJsonPrimitive("membership").getAsString();
                if (RoomMember.MEMBERSHIP_LEAVE.equals(membership) || RoomMember.MEMBERSHIP_BAN.equals(membership)) {
                    shouldBeSaved = mEventTimeline.isHistorical();
                    // delete the room and warn the listener of the leave event only at the end of the events chunk processing
                }
            }
        }
        if (shouldBeSaved) {
            mTimelineEventSaver.storeEvent(event);
        }
        // warn the listener that a new room has been created
        if (Event.EVENT_TYPE_STATE_ROOM_CREATE.equals(event.getType())) {
            dataHandler.onNewRoom(event.roomId);
        }
        // warn the listeners that a room has been joined
        if (Event.EVENT_TYPE_STATE_ROOM_MEMBER.equals(event.getType()) && myUserId.equals(event.stateKey)) {
            final String membership = event.getContentAsJsonObject().getAsJsonPrimitive("membership").getAsString();
            if (RoomMember.MEMBERSHIP_JOIN.equals(membership)) {
                dataHandler.onJoinRoom(event.roomId);
            } else if (RoomMember.MEMBERSHIP_INVITE.equals(membership)) {
                dataHandler.onNewRoom(event.roomId);
            }
        }
    }

}
