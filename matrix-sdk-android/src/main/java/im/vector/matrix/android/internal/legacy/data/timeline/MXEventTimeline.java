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

package im.vector.matrix.android.internal.legacy.data.timeline;

import android.os.AsyncTask;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import im.vector.matrix.android.internal.legacy.MXDataHandler;
import im.vector.matrix.android.internal.legacy.data.Room;
import im.vector.matrix.android.internal.legacy.data.RoomState;
import im.vector.matrix.android.internal.legacy.data.RoomSummary;
import im.vector.matrix.android.internal.legacy.data.store.IMXStore;
import im.vector.matrix.android.internal.legacy.rest.callback.ApiCallback;
import im.vector.matrix.android.internal.legacy.rest.callback.SimpleApiCallback;
import im.vector.matrix.android.internal.legacy.rest.model.Event;
import im.vector.matrix.android.internal.legacy.rest.model.EventContext;
import im.vector.matrix.android.internal.legacy.rest.model.MatrixError;
import im.vector.matrix.android.internal.legacy.rest.model.TokensChunkEvents;
import im.vector.matrix.android.internal.legacy.rest.model.sync.InvitedRoomSync;
import im.vector.matrix.android.internal.legacy.rest.model.sync.RoomSync;
import im.vector.matrix.android.internal.legacy.util.FilterUtil;
import im.vector.matrix.android.internal.legacy.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A private implementation of EventTimeline interface. It's not exposed as you don't have to directly instantiate it.
 * Should be instantiated through EventTimelineFactory.
 */
class MXEventTimeline implements EventTimeline {
    private static final String LOG_TAG = MXEventTimeline.class.getSimpleName();

    /**
     * The initial event id used to initialise the timeline.
     * null in case of live timeline.
     */
    private String mInitialEventId;

    /**
     * Indicate if this timeline is a live one.
     */
    private boolean mIsLiveTimeline;

    /**
     * The associated room.
     */
    private final Room mRoom;

    /**
     * the room Id
     */
    private String mRoomId;

    /**
     * The store.
     */
    private IMXStore mStore;

    /**
     * MXStore does only back pagination. So, the forward pagination token for
     * past timelines is managed locally.
     */
    private String mForwardsPaginationToken;
    private boolean mHasReachedHomeServerForwardsPaginationEnd;

    /**
     * The data handler : used to retrieve data from the store or to trigger REST requests.
     */
    private MXDataHandler mDataHandler;

    /**
     * Pending request statuses
     */
    private boolean mIsBackPaginating = false;
    private boolean mIsForwardPaginating = false;

    /**
     * true if the back history has been retrieved.
     */
    public boolean mCanBackPaginate = true;

    /**
     * true if the last back chunck has been received
     */
    private boolean mIsLastBackChunk;

    /**
     * the server provides a token even for the first room message (which should never change it is the creator message).
     * so requestHistory always triggers a remote request which returns an empty json.
     * try to avoid such behaviour
     */
    private String mBackwardTopToken = "not yet found";

    // true when the current timeline is an historical one
    private boolean mIsHistorical;

    /**
     * Unique identifier
     */
    private final String mTimelineId = System.currentTimeMillis() + "";

    /**
     * * This class handles storing a live room event in a dedicated store.
     */
    private final TimelineEventSaver mTimelineEventSaver;

    /**
     * This class is responsible for holding the state and backState of a room timeline
     */
    private final TimelineStateHolder mStateHolder;

    /**
     * This class handle the timeline event listeners
     */
    private final TimelineEventListeners mEventListeners;

    /**
     * This class is responsible for handling events coming down from the event stream.
     */
    private final TimelineLiveEventHandler mLiveEventHandler;

    /**
     * Constructor with package visibility. Creation should be done through EventTimelineFactory
     *
     * @param store       the store associated (in case of past timeline, the store is memory only)
     * @param dataHandler the dataHandler
     * @param room        the room
     * @param roomId      the room id
     * @param eventId     the eventId
     * @param isLive      true if the timeline is a live one
     */
    MXEventTimeline(@NonNull final IMXStore store,
                    @NonNull final MXDataHandler dataHandler,
                    @NonNull final Room room,
                    @NonNull final String roomId,
                    @Nullable final String eventId,
                    final boolean isLive) {
        mIsLiveTimeline = isLive;
        mInitialEventId = eventId;
        mDataHandler = dataHandler;
        mRoom = room;
        mRoomId = roomId;
        mStore = store;
        mEventListeners = new TimelineEventListeners();
        mStateHolder = new TimelineStateHolder(mDataHandler, mStore, roomId);
        final StateEventRedactionChecker stateEventRedactionChecker = new StateEventRedactionChecker(this, mStateHolder);
        mTimelineEventSaver = new TimelineEventSaver(mStore, mRoom, mStateHolder);
        final TimelinePushWorker timelinePushWorker = new TimelinePushWorker(mDataHandler);
        mLiveEventHandler = new TimelineLiveEventHandler(this,
                mTimelineEventSaver,
                stateEventRedactionChecker,
                timelinePushWorker,
                mStateHolder,
                mEventListeners);
    }

    /**
     * Defines that the current timeline is an historical one
     *
     * @param isHistorical true when the current timeline is an historical one
     */
    @Override
    public void setIsHistorical(boolean isHistorical) {
        mIsHistorical = isHistorical;
    }

    /**
     * Returns true if the current timeline is an historical one
     */
    @Override
    public boolean isHistorical() {
        return mIsHistorical;
    }

    /*
     * @return the unique identifier
     */
    @Override
    public String getTimelineId() {
        return mTimelineId;
    }

    /**
     * @return the dedicated room
     */
    @Override
    public Room getRoom() {
        return mRoom;
    }

    /**
     * @return the used store
     */
    @Override
    public IMXStore getStore() {
        return mStore;
    }

    /**
     * @return the initial event id.
     */
    @Override
    public String getInitialEventId() {
        return mInitialEventId;
    }

    /**
     * @return true if this timeline is the live one
     */
    @Override
    public boolean isLiveTimeline() {
        return mIsLiveTimeline;
    }

    /**
     * Get whether we are at the end of the message stream
     *
     * @return true if end has been reached
     */
    @Override
    public boolean hasReachedHomeServerForwardsPaginationEnd() {
        return mHasReachedHomeServerForwardsPaginationEnd;
    }


    /**
     * Reset the back state so that future history requests start over from live.
     * Must be called when opening a room if interested in history.
     */
    @Override
    public void initHistory() {
        final RoomState backState = getState().deepCopy();
        setBackState(backState);
        mCanBackPaginate = true;

        mIsBackPaginating = false;
        mIsForwardPaginating = false;

        // sanity check
        if (null != mDataHandler && null != mDataHandler.getDataRetriever()) {
            mDataHandler.resetReplayAttackCheckInTimeline(getTimelineId());
            mDataHandler.getDataRetriever().cancelHistoryRequests(mRoomId);
        }
    }

    /**
     * @return The state of the room at the top most recent event of the timeline.
     */
    @Override
    public RoomState getState() {
        return mStateHolder.getState();
    }

    /**
     * Update the state.
     *
     * @param state the new state.
     */
    @Override
    public void setState(RoomState state) {
        mStateHolder.setState(state);
    }

    /**
     * Update the backState.
     *
     * @param state the new backState.
     */
    private void setBackState(RoomState state) {
        mStateHolder.setBackState(state);
    }

    /**
     * @return the backState.
     */
    private RoomState getBackState() {
        return mStateHolder.getBackState();
    }

    /**
     * Lock over the backPaginate process
     *
     * @param canBackPaginate the state of the lock (true/false)
     */
    protected void setCanBackPaginate(final boolean canBackPaginate) {
        mCanBackPaginate = canBackPaginate;
    }

    /**
     * Make a deep copy or the dedicated state.
     *
     * @param direction the room state direction to deep copy.
     */
    private void deepCopyState(Direction direction) {
        mStateHolder.deepCopyState(direction);
    }

    /**
     * Process a state event to keep the internal live and back states up to date.
     *
     * @param event     the state event
     * @param direction the direction; ie. forwards for live state, backwards for back state
     * @return true if the event has been processed.
     */
    private boolean processStateEvent(Event event, Direction direction) {
        return mStateHolder.processStateEvent(event, direction);
    }

    /**
     * Handle the invitation room events
     *
     * @param invitedRoomSync the invitation room events.
     */
    @Override
    public void handleInvitedRoomSync(InvitedRoomSync invitedRoomSync) {
        final TimelineInvitedRoomSyncHandler invitedRoomSyncHandler = new TimelineInvitedRoomSyncHandler(mRoom, mLiveEventHandler, invitedRoomSync);
        invitedRoomSyncHandler.handle();
    }

    /**
     * Manage the joined room events.
     *
     * @param roomSync            the roomSync.
     * @param isGlobalInitialSync true if the sync has been triggered by a global initial sync
     */
    @Override
    public void handleJoinedRoomSync(@NonNull final RoomSync roomSync, final boolean isGlobalInitialSync) {
        final TimelineJoinRoomSyncHandler joinRoomSyncHandler = new TimelineJoinRoomSyncHandler(this,
                roomSync,
                mStateHolder,
                mLiveEventHandler,
                isGlobalInitialSync);
        joinRoomSyncHandler.handle();
    }

    /**
     * Store an outgoing event.
     *
     * @param event the event to store
     */
    @Override
    public void storeOutgoingEvent(Event event) {
        if (mIsLiveTimeline) {
            storeEvent(event);
        }
    }

    /**
     * Store the event and update the dedicated room summary
     *
     * @param event the event to store
     */
    private void storeEvent(Event event) {
        mTimelineEventSaver.storeEvent(event);
    }

    //================================================================================
    // History request
    //================================================================================

    private static final int MAX_EVENT_COUNT_PER_PAGINATION = 30;

    // the storage events are buffered to provide a small bunch of events
    // the storage can provide a big bunch which slows down the UI.
    public class SnapshotEvent {
        public final Event mEvent;
        public final RoomState mState;

        public SnapshotEvent(Event event, RoomState state) {
            mEvent = event;
            mState = state;
        }
    }

    // avoid adding to many events
    // the room history request can provide more than expected event.
    private final List<SnapshotEvent> mSnapshotEvents = new ArrayList<>();

    /**
     * Send MAX_EVENT_COUNT_PER_PAGINATION events to the caller.
     *
     * @param maxEventCount the max event count
     * @param callback      the callback.
     */
    private void manageBackEvents(int maxEventCount, final ApiCallback<Integer> callback) {
        // check if the SDK was not logged out
        if (!mDataHandler.isAlive()) {
            Log.d(LOG_TAG, "manageEvents : mDataHandler is not anymore active.");

            return;
        }

        int count = Math.min(mSnapshotEvents.size(), maxEventCount);

        Event latestSupportedEvent = null;

        for (int i = 0; i < count; i++) {
            SnapshotEvent snapshotedEvent = mSnapshotEvents.get(0);

            // in some cases, there is no displayed summary
            // https://github.com/vector-im/vector-android/pull/354
            if (null == latestSupportedEvent && RoomSummary.isSupportedEvent(snapshotedEvent.mEvent)) {
                latestSupportedEvent = snapshotedEvent.mEvent;
            }

            mSnapshotEvents.remove(0);
            mEventListeners.onEvent(snapshotedEvent.mEvent, Direction.BACKWARDS, snapshotedEvent.mState);
        }

        // https://github.com/vector-im/vector-android/pull/354
        // defines a new summary if the known is not supported
        RoomSummary summary = mStore.getSummary(mRoomId);

        if (null != latestSupportedEvent && (null == summary || !RoomSummary.isSupportedEvent(summary.getLatestReceivedEvent()))) {
            mStore.storeSummary(new RoomSummary(null, latestSupportedEvent, getState(), mDataHandler.getUserId()));
        }

        Log.d(LOG_TAG, "manageEvents : commit");
        mStore.commit();

        if (mSnapshotEvents.size() < MAX_EVENT_COUNT_PER_PAGINATION && mIsLastBackChunk) {
            mCanBackPaginate = false;
        }
        mIsBackPaginating = false;
        if (callback != null) {
            try {
                callback.onSuccess(count);
            } catch (Exception e) {
                Log.e(LOG_TAG, "requestHistory exception " + e.getMessage(), e);
            }
        }
    }

    /**
     * Add some events in a dedicated direction.
     *
     * @param events      the events list
     * @param stateEvents the received state events (in case of lazy loading of room members)
     * @param direction   the direction
     */
    private void addPaginationEvents(List<Event> events,
                                     @Nullable List<Event> stateEvents,
                                     Direction direction) {
        RoomSummary summary = mStore.getSummary(mRoomId);
        boolean shouldCommitStore = false;

        // Process additional state events (this happens in case of lazy loading)
        if (stateEvents != null) {
            for (Event stateEvent : stateEvents) {
                if (direction == Direction.BACKWARDS) {
                    // Enrich the timeline root state with the additional state events observed during back pagination
                    processStateEvent(stateEvent, Direction.FORWARDS);
                }

                processStateEvent(stateEvent, direction);
            }
        }

        // the backward events have a dedicated management to avoid providing too many events for each request
        for (Event event : events) {
            boolean processedEvent = true;

            if (event.stateKey != null) {
                deepCopyState(direction);
                processedEvent = processStateEvent(event, direction);
            }

            // Decrypt event if necessary
            mDataHandler.decryptEvent(event, getTimelineId());

            if (processedEvent) {
                // warn the listener only if the message is processed.
                // it should avoid duplicated events.
                if (direction == Direction.BACKWARDS) {
                    if (mIsLiveTimeline) {
                        // update the summary is the event has been received after the oldest known event
                        // it might happen after a timeline update (hole in the chat history)
                        if (null != summary
                                && (null == summary.getLatestReceivedEvent()
                                || event.isValidOriginServerTs()
                                && summary.getLatestReceivedEvent().originServerTs < event.originServerTs
                                && RoomSummary.isSupportedEvent(event))) {
                            summary.setLatestReceivedEvent(event, getState());
                            mStore.storeSummary(summary);
                            shouldCommitStore = true;
                        }
                    }
                    mSnapshotEvents.add(new SnapshotEvent(event, getBackState()));
                    // onEvent will be called in manageBackEvents
                }
            }
        }

        if (shouldCommitStore) {
            mStore.commit();
        }
    }

    /**
     * Add some events in a dedicated direction.
     *
     * @param events      the events list
     * @param stateEvents the received state events (in case of lazy loading of room members)
     * @param direction   the direction
     * @param callback    the callback.
     */
    private void addPaginationEvents(final List<Event> events,
                                     @Nullable final List<Event> stateEvents,
                                     final Direction direction,
                                     final ApiCallback<Integer> callback) {
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                addPaginationEvents(events, stateEvents, direction);
                return null;
            }

            @Override
            protected void onPostExecute(Void args) {
                if (direction == Direction.BACKWARDS) {
                    manageBackEvents(MAX_EVENT_COUNT_PER_PAGINATION, callback);
                } else {
                    for (Event event : events) {
                        mEventListeners.onEvent(event, Direction.FORWARDS, getState());
                    }

                    if (null != callback) {
                        callback.onSuccess(events.size());
                    }
                }
            }
        };

        try {
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } catch (final Exception e) {
            Log.e(LOG_TAG, "## addPaginationEvents() failed " + e.getMessage(), e);
            task.cancel(true);

            new android.os.Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    if (null != callback) {
                        callback.onUnexpectedError(e);
                    }
                }
            });
        }
    }

    /**
     * Tells if a back pagination can be triggered.
     *
     * @return true if a back pagination can be triggered.
     */
    @Override
    public boolean canBackPaginate() {
        // One at a time please
        return !mIsBackPaginating
                // history_visibility flag management
                && getState().canBackPaginate(mRoom.isJoined(), mRoom.isInvited())
                // If we have already reached the end of history
                && mCanBackPaginate
                // If the room is not finished being set up
                && mRoom.isReady();
    }

    /**
     * Request older messages.
     *
     * @param callback the asynchronous callback
     * @return true if request starts
     */
    @Override
    public boolean backPaginate(final ApiCallback<Integer> callback) {
        return backPaginate(MAX_EVENT_COUNT_PER_PAGINATION, callback);
    }

    /**
     * Request older messages.
     *
     * @param eventCount number of events we want to retrieve
     * @param callback   callback to implement to be informed that the pagination request has been completed. Can be null.
     * @return true if request starts
     */
    @Override
    public boolean backPaginate(final int eventCount, final ApiCallback<Integer> callback) {
        return backPaginate(eventCount, false, callback);
    }

    /**
     * Request older messages.
     *
     * @param eventCount    number of events we want to retrieve
     * @param useCachedOnly to use the cached events list only (i.e no request will be triggered)
     * @param callback      callback to implement to be informed that the pagination request has been completed. Can be null.
     * @return true if request starts
     */
    @Override
    public boolean backPaginate(final int eventCount, final boolean useCachedOnly, final ApiCallback<Integer> callback) {
        if (!canBackPaginate()) {
            Log.d(LOG_TAG, "cannot requestHistory " + mIsBackPaginating + " " + !getState().canBackPaginate(mRoom.isJoined(), mRoom.isInvited())
                    + " " + !mCanBackPaginate + " " + !mRoom.isReady());
            return false;
        }

        Log.d(LOG_TAG, "backPaginate starts");

        // restart the pagination
        if (null == getBackState().getToken()) {
            mSnapshotEvents.clear();
        }

        final String fromBackToken = getBackState().getToken();

        mIsBackPaginating = true;

        // enough buffered data
        if (useCachedOnly
                || mSnapshotEvents.size() >= eventCount
                || TextUtils.equals(fromBackToken, mBackwardTopToken)
                || TextUtils.equals(fromBackToken, Event.PAGINATE_BACK_TOKEN_END)) {

            mIsLastBackChunk = TextUtils.equals(fromBackToken, mBackwardTopToken) || TextUtils.equals(fromBackToken, Event.PAGINATE_BACK_TOKEN_END);

            final android.os.Handler handler = new android.os.Handler(Looper.getMainLooper());
            final int maxEventsCount;

            if (useCachedOnly) {
                Log.d(LOG_TAG, "backPaginate : load " + mSnapshotEvents.size() + "cached events list");
                maxEventsCount = Math.min(mSnapshotEvents.size(), eventCount);
            } else if (mSnapshotEvents.size() >= eventCount) {
                Log.d(LOG_TAG, "backPaginate : the events are already loaded.");
                maxEventsCount = eventCount;
            } else {
                Log.d(LOG_TAG, "backPaginate : reach the history top");
                maxEventsCount = eventCount;
            }

            // call the callback with a delay
            // to reproduce the same behaviour as a network request.
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    handler.postDelayed(new Runnable() {
                        public void run() {
                            manageBackEvents(maxEventsCount, callback);
                        }
                    }, 0);
                }
            };

            Thread t = new Thread(r);
            t.start();

            return true;
        }

        mDataHandler.getDataRetriever().backPaginate(mStore, mRoomId, getBackState().getToken(), eventCount, mDataHandler.isLazyLoadingEnabled(),
                new SimpleApiCallback<TokensChunkEvents>(callback) {
                    @Override
                    public void onSuccess(TokensChunkEvents response) {
                        if (mDataHandler.isAlive()) {

                            if (null != response.chunk) {
                                Log.d(LOG_TAG, "backPaginate : " + response.chunk.size() + " events are retrieved.");
                            } else {
                                Log.d(LOG_TAG, "backPaginate : there is no event");
                            }

                            mIsLastBackChunk = null != response.chunk
                                    && 0 == response.chunk.size()
                                    && TextUtils.equals(response.end, response.start)
                                    || null == response.end;

                            if (mIsLastBackChunk && null != response.end) {
                                // save its token to avoid useless request
                                mBackwardTopToken = fromBackToken;
                            } else {
                                // the server returns a null pagination token when there is no more available data
                                if (null == response.end) {
                                    getBackState().setToken(Event.PAGINATE_BACK_TOKEN_END);
                                } else {
                                    getBackState().setToken(response.end);
                                }
                            }

                            addPaginationEvents(null == response.chunk ? new ArrayList<Event>() : response.chunk,
                                    response.stateEvents,
                                    Direction.BACKWARDS,
                                    callback);

                        } else {
                            Log.d(LOG_TAG, "mDataHandler is not active.");
                        }
                    }

                    @Override
                    public void onMatrixError(MatrixError e) {
                        Log.d(LOG_TAG, "backPaginate onMatrixError");

                        // When we've retrieved all the messages from a room, the pagination token is some invalid value
                        if (MatrixError.UNKNOWN.equals(e.errcode)) {
                            mCanBackPaginate = false;
                        }
                        mIsBackPaginating = false;

                        super.onMatrixError(e);
                    }

                    @Override
                    public void onNetworkError(Exception e) {
                        Log.d(LOG_TAG, "backPaginate onNetworkError");

                        mIsBackPaginating = false;

                        super.onNetworkError(e);
                    }

                    @Override
                    public void onUnexpectedError(Exception e) {
                        Log.d(LOG_TAG, "backPaginate onUnexpectedError");

                        mIsBackPaginating = false;

                        super.onUnexpectedError(e);
                    }
                });

        return true;
    }

    /**
     * Request newer messages.
     *
     * @param callback callback to implement to be informed that the pagination request has been completed. Can be null.
     * @return true if request starts
     */
    @Override
    public boolean forwardPaginate(final ApiCallback<Integer> callback) {
        if (mIsLiveTimeline) {
            Log.d(LOG_TAG, "Cannot forward paginate on Live timeline");
            return false;
        }

        if (mIsForwardPaginating || mHasReachedHomeServerForwardsPaginationEnd) {
            Log.d(LOG_TAG, "forwardPaginate " + mIsForwardPaginating
                    + " mHasReachedHomeServerForwardsPaginationEnd " + mHasReachedHomeServerForwardsPaginationEnd);
            return false;
        }

        mIsForwardPaginating = true;

        mDataHandler.getDataRetriever().paginate(mStore, mRoomId, mForwardsPaginationToken, Direction.FORWARDS, mDataHandler.isLazyLoadingEnabled(),
                new SimpleApiCallback<TokensChunkEvents>(callback) {
                    @Override
                    public void onSuccess(TokensChunkEvents response) {
                        if (mDataHandler.isAlive()) {
                            Log.d(LOG_TAG, "forwardPaginate : " + response.chunk.size() + " are retrieved.");

                            mHasReachedHomeServerForwardsPaginationEnd = 0 == response.chunk.size() && TextUtils.equals(response.end, response.start);
                            mForwardsPaginationToken = response.end;

                            addPaginationEvents(response.chunk,
                                    response.stateEvents,
                                    Direction.FORWARDS,
                                    callback);

                            mIsForwardPaginating = false;
                        } else {
                            Log.d(LOG_TAG, "mDataHandler is not active.");
                        }
                    }

                    @Override
                    public void onMatrixError(MatrixError e) {
                        mIsForwardPaginating = false;

                        super.onMatrixError(e);
                    }

                    @Override
                    public void onNetworkError(Exception e) {
                        mIsForwardPaginating = false;

                        super.onNetworkError(e);
                    }

                    @Override
                    public void onUnexpectedError(Exception e) {
                        mIsForwardPaginating = false;

                        super.onUnexpectedError(e);
                    }
                });

        return true;
    }

    /**
     * Trigger a pagination in the expected direction.
     *
     * @param direction the direction.
     * @param callback  the callback.
     * @return true if the operation succeeds
     */
    @Override
    public boolean paginate(Direction direction, final ApiCallback<Integer> callback) {
        if (Direction.BACKWARDS == direction) {
            return backPaginate(callback);
        } else {
            return forwardPaginate(callback);
        }
    }

    /**
     * Cancel any pending pagination requests
     */
    @Override
    public void cancelPaginationRequests() {
        mDataHandler.getDataRetriever().cancelHistoryRequests(mRoomId);
        mIsBackPaginating = false;
        mIsForwardPaginating = false;
    }

    //==============================================================================================================
    // pagination methods
    //==============================================================================================================

    /**
     * Reset the pagination timeline and start loading the context around its `initialEventId`.
     * The retrieved (backwards and forwards) events will be sent to registered listeners.
     *
     * @param limit    the maximum number of messages to get around the initial event.
     * @param callback the operation callback
     */
    @Override
    public void resetPaginationAroundInitialEvent(final int limit, final ApiCallback<Void> callback) {
        // Reset the store
        mStore.deleteRoomData(mRoomId);

        mDataHandler.resetReplayAttackCheckInTimeline(getTimelineId());

        mForwardsPaginationToken = null;
        mHasReachedHomeServerForwardsPaginationEnd = false;

        mDataHandler.getDataRetriever()
                .getRoomsRestClient()
                .getContextOfEvent(mRoomId, mInitialEventId, limit, FilterUtil.createRoomEventFilter(mDataHandler.isLazyLoadingEnabled()),
                        new SimpleApiCallback<EventContext>(callback) {
                            @Override
                            public void onSuccess(final EventContext eventContext) {

                                AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
                                    @Override
                                    protected Void doInBackground(Void... params) {
                                        // the state is the one after the latest event of the chunk i.e. the last message of eventContext.eventsAfter
                                        for (Event event : eventContext.state) {
                                            processStateEvent(event, Direction.FORWARDS);
                                        }

                                        // init the room states
                                        initHistory();

                                        // build the events list
                                        List<Event> events = new ArrayList<>();

                                        Collections.reverse(eventContext.eventsAfter);
                                        events.addAll(eventContext.eventsAfter);
                                        events.add(eventContext.event);
                                        events.addAll(eventContext.eventsBefore);

                                        // add events after
                                        addPaginationEvents(events, null, Direction.BACKWARDS);

                                        return null;
                                    }

                                    @Override
                                    protected void onPostExecute(Void args) {
                                        // create dummy forward events list
                                        // to center the selected event id
                                        // else if might be out of screen
                                        List<SnapshotEvent> nextSnapshotEvents = new ArrayList<>(mSnapshotEvents.subList(0, (mSnapshotEvents.size() + 1) / 2));

                                        // put in the right order
                                        Collections.reverse(nextSnapshotEvents);

                                        // send them one by one
                                        for (SnapshotEvent snapshotEvent : nextSnapshotEvents) {
                                            mSnapshotEvents.remove(snapshotEvent);
                                            mEventListeners.onEvent(snapshotEvent.mEvent, Direction.FORWARDS, snapshotEvent.mState);
                                        }

                                        // init the tokens
                                        getBackState().setToken(eventContext.start);
                                        mForwardsPaginationToken = eventContext.end;

                                        // send the back events to complete pagination
                                        manageBackEvents(MAX_EVENT_COUNT_PER_PAGINATION, new ApiCallback<Integer>() {
                                            @Override
                                            public void onSuccess(Integer info) {
                                                Log.d(LOG_TAG, "addPaginationEvents succeeds");
                                            }

                                            @Override
                                            public void onNetworkError(Exception e) {
                                                Log.e(LOG_TAG, "addPaginationEvents failed " + e.getMessage(), e);
                                            }

                                            @Override
                                            public void onMatrixError(MatrixError e) {
                                                Log.e(LOG_TAG, "addPaginationEvents failed " + e.getMessage());
                                            }

                                            @Override
                                            public void onUnexpectedError(Exception e) {
                                                Log.e(LOG_TAG, "addPaginationEvents failed " + e.getMessage(), e);
                                            }
                                        });

                                        // everything is done
                                        callback.onSuccess(null);
                                    }
                                };

                                try {
                                    task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                                } catch (final Exception e) {
                                    Log.e(LOG_TAG, "## resetPaginationAroundInitialEvent() failed " + e.getMessage(), e);
                                    task.cancel(true);

                                    new android.os.Handler(Looper.getMainLooper()).post(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (callback != null) {
                                                callback.onUnexpectedError(e);
                                            }
                                        }
                                    });
                                }
                            }
                        });
    }

    //==============================================================================================================
    // onEvent listener management.
    //==============================================================================================================

    /**
     * Add an events listener.
     *
     * @param listener the listener to add.
     */
    @Override
    public void addEventTimelineListener(@Nullable final Listener listener) {
        mEventListeners.add(listener);
    }

    /**
     * Remove an events listener.
     *
     * @param listener the listener to remove.
     */
    @Override
    public void removeEventTimelineListener(@Nullable final Listener listener) {
        mEventListeners.remove(listener);
    }
}
