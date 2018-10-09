package im.vector.matrix.android.internal.legacy.data.timeline;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import im.vector.matrix.android.internal.legacy.MXDataHandler;
import im.vector.matrix.android.internal.legacy.data.Room;
import im.vector.matrix.android.internal.legacy.data.RoomState;
import im.vector.matrix.android.internal.legacy.data.RoomSummary;
import im.vector.matrix.android.internal.legacy.data.store.IMXStore;
import im.vector.matrix.android.internal.legacy.rest.model.Event;
import im.vector.matrix.android.internal.legacy.rest.model.RoomMember;
import im.vector.matrix.android.internal.legacy.rest.model.sync.RoomSync;
import im.vector.matrix.android.internal.legacy.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class is responsible for handling a Join RoomSync
 */
class TimelineJoinRoomSyncHandler {

    private static final String LOG_TAG = TimelineJoinRoomSyncHandler.class.getSimpleName();

    private final MXEventTimeline mEventTimeline;
    private final RoomSync mRoomSync;
    private final TimelineStateHolder mTimelineStateHolder;
    private final TimelineLiveEventHandler mTimelineLiveEventHandler;
    private final boolean mIsGlobalInitialSync;

    TimelineJoinRoomSyncHandler(@NonNull final MXEventTimeline eventTimeline,
                                @NonNull final RoomSync roomSync,
                                @NonNull final TimelineStateHolder timelineStateHolder,
                                @NonNull final TimelineLiveEventHandler timelineLiveEventHandler,
                                final boolean isGlobalInitialSync) {
        mEventTimeline = eventTimeline;
        mRoomSync = roomSync;
        mTimelineStateHolder = timelineStateHolder;
        mTimelineLiveEventHandler = timelineLiveEventHandler;
        mIsGlobalInitialSync = isGlobalInitialSync;
    }


    public void handle() {
        final IMXStore store = mEventTimeline.getStore();
        final Room room = mEventTimeline.getRoom();
        final MXDataHandler dataHandler = room.getDataHandler();
        final String roomId = room.getRoomId();
        final String myUserId = dataHandler.getMyUser().user_id;
        final RoomMember selfMember = mTimelineStateHolder.getState().getMember(myUserId);
        final RoomSummary currentSummary = store.getSummary(roomId);

        final String membership = selfMember != null ? selfMember.membership : null;
        final boolean isRoomInitialSync = membership == null || TextUtils.equals(membership, RoomMember.MEMBERSHIP_INVITE);

        // Check whether the room was pending on an invitation.
        if (RoomMember.MEMBERSHIP_INVITE.equals(membership)) {
            // Reset the storage of this room. An initial sync of the room will be done with the provided 'roomSync'.
            cleanInvitedRoom(store, roomId);
        }
        if (mRoomSync.state != null && mRoomSync.state.events != null && mRoomSync.state.events.size() > 0) {
            handleRoomSyncState(room, store, isRoomInitialSync);
        }
        // Handle now timeline.events, the room state is updated during this step too (Note: timeline events are in chronological order)
        if (mRoomSync.timeline != null) {
            handleRoomSyncTimeline(store, myUserId, roomId, currentSummary, isRoomInitialSync);
        }
        if (isRoomInitialSync) {
            // any request history can be triggered by now.
            room.setReadyState(true);
        } else if (mRoomSync.timeline != null && mRoomSync.timeline.limited) {
            // Finalize initial sync
            // The room has been synced with a limited timeline
            dataHandler.onRoomFlush(roomId);
        }
        // the EventTimeLine is used when displaying a room preview
        // so, the following items should only be called when it is a live one.
        if (mEventTimeline.isLiveTimeline()) {
            handleLiveTimeline(dataHandler, store, roomId, myUserId, currentSummary);
        }
    }

    private void handleRoomSyncState(@NonNull final Room room,
                                     @NonNull final IMXStore store,
                                     final boolean isRoomInitialSync) {
        if (isRoomInitialSync) {
            Log.d(LOG_TAG, "##" + mRoomSync.state.events.size() + " events "
                    + "for room " + room.getRoomId()
                    + "in store " + store
            );
        }

        // Build/Update first the room state corresponding to the 'start' of the timeline.
        // Note: We consider it is not required to clone the existing room state here, because no notification is posted for these events.
        if (room.getDataHandler().isAlive()) {
            for (Event event : mRoomSync.state.events) {
                try {
                    mTimelineStateHolder.processStateEvent(event, EventTimeline.Direction.FORWARDS);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "processStateEvent failed " + e.getMessage(), e);
                }
            }

            room.setReadyState(true);
        } else {
            Log.e(LOG_TAG, "## mDataHandler.isAlive() is false");
        }
        // if it is an initial sync, the live state is initialized here
        // so the back state must also be initialized
        if (isRoomInitialSync) {
            final RoomState state = mTimelineStateHolder.getState();
            Log.d(LOG_TAG, "## handleJoinedRoomSync() : retrieve X " + state.getLoadedMembers().size() + " members for room " + room.getRoomId());
            mTimelineStateHolder.setBackState(state.deepCopy());
        }
    }

    private void cleanInvitedRoom(@NonNull final IMXStore store,
                                  @NonNull final String roomId) {
        Log.d(LOG_TAG, "clean invited room from the store " + roomId);
        store.deleteRoomData(roomId);
        mTimelineStateHolder.clear();
    }

    private void handleRoomSyncTimeline(@NonNull final IMXStore store,
                                        @NonNull final String myUserId,
                                        @NonNull final String roomId,
                                        @Nullable final RoomSummary currentSummary,
                                        final boolean isRoomInitialSync) {
        if (mRoomSync.timeline.limited) {
            if (!isRoomInitialSync) {
                final RoomState state = mTimelineStateHolder.getState();
                // There is a gap between known events and received events in this incremental sync.
                // define a summary if some messages are left
                // the unsent messages are often displayed messages.
                final Event oldestEvent = store.getOldestEvent(roomId);
                // Flush the existing messages for this room by keeping state events.
                store.deleteAllRoomMessages(roomId, true);
                if (oldestEvent != null) {
                    if (RoomSummary.isSupportedEvent(oldestEvent)) {
                        if (currentSummary != null) {
                            currentSummary.setLatestReceivedEvent(oldestEvent, state);
                            store.storeSummary(currentSummary);
                        } else {
                            store.storeSummary(new RoomSummary(null, oldestEvent, state, myUserId));
                        }
                    }
                }
                // Force a fetch of the loaded members the next time they will be requested
                state.forceMembersRequest();
            }

            // if the prev batch is set to null
            // it implies there is no more data on server side.
            if (mRoomSync.timeline.prevBatch == null) {
                mRoomSync.timeline.prevBatch = Event.PAGINATE_BACK_TOKEN_END;
            }

            // In case of limited timeline, update token where to start back pagination
            store.storeBackToken(roomId, mRoomSync.timeline.prevBatch);
            // reset the state back token
            // because it does not make anymore sense
            // by setting at null, the events cache will be cleared when a requesthistory will be called
            mTimelineStateHolder.getBackState().setToken(null);
            // reset the back paginate lock
            mEventTimeline.setCanBackPaginate(true);
        }

        // any event ?
        if (mRoomSync.timeline.events != null && !mRoomSync.timeline.events.isEmpty()) {
            final List<Event> events = mRoomSync.timeline.events;
            // save the back token
            events.get(0).mToken = mRoomSync.timeline.prevBatch;

            // Here the events are handled in forward direction (see [handleLiveEvent:]).
            // They will be added at the end of the stored events, so we keep the chronological order.
            for (Event event : events) {
                // the roomId is not defined.
                event.roomId = roomId;
                try {
                    boolean isLimited = mRoomSync.timeline != null && mRoomSync.timeline.limited;

                    // digest the forward event
                    mTimelineLiveEventHandler.handleLiveEvent(event, !isLimited && !mIsGlobalInitialSync, !mIsGlobalInitialSync && !isRoomInitialSync);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "timeline event failed " + e.getMessage(), e);
                }
            }
        }
    }

    private void handleLiveTimeline(@NonNull final MXDataHandler dataHandler,
                                    @NonNull final IMXStore store,
                                    @NonNull final String roomId,
                                    @NonNull final String myUserId,
                                    @Nullable final RoomSummary currentSummary) {
        final RoomState state = mTimelineStateHolder.getState();
        // check if the summary is defined
        // after a sync, the room summary might not be defined because the latest message did not generate a room summary/
        if (null != store.getRoom(roomId)) {
            RoomSummary summary = store.getSummary(roomId);
            // if there is no defined summary
            // we have to create a new one
            if (summary == null) {
                // define a summary if some messages are left
                // the unsent messages are often displayed messages.
                final Event oldestEvent = store.getOldestEvent(roomId);

                // if there is an oldest event, use it to set a summary
                if (oldestEvent != null) {
                    // always defined a room summary else the room won't be displayed in the recents
                    store.storeSummary(new RoomSummary(null, oldestEvent, state, myUserId));
                    store.commit();

                    // if the event is not displayable
                    // back paginate until to find a valid one
                    if (!RoomSummary.isSupportedEvent(oldestEvent)) {
                        Log.e(LOG_TAG, "the room " + roomId + " has no valid summary, back paginate once to find a valid one");
                    }
                }
                // use the latest known event
                else if (currentSummary != null) {
                    currentSummary.setLatestReceivedEvent(currentSummary.getLatestReceivedEvent(), state);
                    store.storeSummary(currentSummary);
                    store.commit();
                }
                // try to build a summary from the state events
                else if (mRoomSync.state != null && mRoomSync.state.events != null && mRoomSync.state.events.size() > 0) {
                    final List<Event> events = new ArrayList<>(mRoomSync.state.events);
                    Collections.reverse(events);

                    for (Event event : events) {
                        event.roomId = roomId;
                        if (RoomSummary.isSupportedEvent(event)) {
                            summary = new RoomSummary(store.getSummary(roomId), event, state, myUserId);
                            store.storeSummary(summary);
                            store.commit();
                            break;
                        }
                    }
                }
            }
        }

        if (null != mRoomSync.unreadNotifications) {
            int notifCount = 0;
            int highlightCount = 0;

            if (null != mRoomSync.unreadNotifications.highlightCount) {
                highlightCount = mRoomSync.unreadNotifications.highlightCount;
            }

            if (null != mRoomSync.unreadNotifications.notificationCount) {
                notifCount = mRoomSync.unreadNotifications.notificationCount;
            }

            if (notifCount != state.getNotificationCount() || state.getHighlightCount() != highlightCount) {
                Log.d(LOG_TAG, "## handleJoinedRoomSync() : update room state notifs count for room id " + roomId
                        + ": highlightCount " + highlightCount + " - notifCount " + notifCount);

                state.setNotificationCount(notifCount);
                state.setHighlightCount(highlightCount);
                store.storeLiveStateForRoom(roomId);
                dataHandler.onNotificationCountUpdate(roomId);
            }

            // some users reported that the summary notification counts were sometimes invalid
            // so check roomstates and summaries separately
            final RoomSummary summary = store.getSummary(roomId);
            if (summary != null && (notifCount != summary.getNotificationCount() || summary.getHighlightCount() != highlightCount)) {
                Log.d(LOG_TAG, "## handleJoinedRoomSync() : update room summary notifs count for room id " + roomId
                        + ": highlightCount " + highlightCount + " - notifCount " + notifCount);

                summary.setNotificationCount(notifCount);
                summary.setHighlightCount(highlightCount);
                store.flushSummary(summary);
                dataHandler.onNotificationCountUpdate(roomId);
            }
        }

        // TODO LazyLoading, maybe this should be done earlier, because nb of members can be usefull in the instruction above.
        if (mRoomSync.roomSyncSummary != null) {
            RoomSummary summary = store.getSummary(roomId);

            if (summary == null) {
                // Should never happen here
                Log.e(LOG_TAG, "!!!!!!!!!!!!!!!!!!!!! RoomSummary is null !!!!!!!!!!!!!!!!!!!!!");
            } else {
                summary.setRoomSyncSummary(mRoomSync.roomSyncSummary);

                store.flushSummary(summary);
            }
        }
    }

}
