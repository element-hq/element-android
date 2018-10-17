/*
 * Copyright 2015 OpenMarket Ltd
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

package im.vector.matrix.android.internal.legacy.data.store;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import im.vector.matrix.android.internal.auth.data.Credentials;
import im.vector.matrix.android.internal.legacy.MXDataHandler;
import im.vector.matrix.android.internal.legacy.data.Room;
import im.vector.matrix.android.internal.legacy.data.RoomAccountData;
import im.vector.matrix.android.internal.legacy.data.RoomSummary;
import im.vector.matrix.android.internal.legacy.data.comparator.Comparators;
import im.vector.matrix.android.internal.legacy.data.metrics.MetricsListener;
import im.vector.matrix.android.internal.legacy.data.timeline.EventTimeline;
import im.vector.matrix.android.internal.legacy.rest.callback.ApiCallback;
import im.vector.matrix.android.internal.legacy.rest.model.Event;
import im.vector.matrix.android.internal.legacy.rest.model.ReceiptData;
import im.vector.matrix.android.internal.legacy.rest.model.RoomMember;
import im.vector.matrix.android.internal.legacy.rest.model.TokensChunkEvents;
import im.vector.matrix.android.internal.legacy.rest.model.User;
import im.vector.matrix.android.internal.legacy.rest.model.group.Group;
import im.vector.matrix.android.internal.legacy.rest.model.pid.ThirdPartyIdentifier;
import im.vector.matrix.android.internal.legacy.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An in-memory IMXStore.
 */
public class MXMemoryStore implements IMXStore {

    private static final String LOG_TAG = MXMemoryStore.class.getSimpleName();

    protected Map<String, Room> mRooms;
    protected Map<String, User> mUsers;

    protected static final Object mRoomEventsLock = new Object();

    // room id -> map of (event_id -> event) events for this room (linked so insertion order is preserved)
    protected Map<String, LinkedHashMap<String, Event>> mRoomEvents;

    protected Map<String, String> mRoomTokens;

    protected Map<String, RoomSummary> mRoomSummaries;
    protected Map<String, RoomAccountData> mRoomAccountData;

    // dict of dict of MXReceiptData indexed by userId
    protected final Object mReceiptsByRoomIdLock = new Object();
    protected Map<String, Map<String, ReceiptData>> mReceiptsByRoomId;

    protected Map<String, Group> mGroups;

    // room state events
    //protected final Map<String, Map<String, Event>> mRoomStateEventsByRoomId = new HashMap<>();

    // common context
    private static Context mSharedContext = null;

    // the context
    protected Context mContext;

    //
    private final Map<String, Event> mTemporaryEventsList = new HashMap<>();
    protected MetricsListener mMetricsListener;

    protected Credentials mCredentials;

    protected String mEventStreamToken = null;

    protected final List<IMXStoreListener> mListeners = new ArrayList<>();

    // Meta data about the store. It is defined only if the passed MXCredentials contains all information.
    // When nil, nothing is stored on the file system.
    protected MXFileStoreMetaData mMetadata = null;

    // last time the avatar / displayname was updated
    protected long mUserDisplayNameTs;
    protected long mUserAvatarUrlTs;

    // DataHandler -- added waiting to be refactored
    private MXDataHandler mDataHandler;

    /**
     * Initialization method.
     */
    protected void initCommon() {
        mRooms = new ConcurrentHashMap<>();
        mUsers = new ConcurrentHashMap<>();
        mRoomEvents = new ConcurrentHashMap<>();
        mRoomTokens = new ConcurrentHashMap<>();
        mRoomSummaries = new ConcurrentHashMap<>();
        mReceiptsByRoomId = new ConcurrentHashMap<>();
        mRoomAccountData = new ConcurrentHashMap<>();
        mGroups = new ConcurrentHashMap<>();
        mEventStreamToken = null;
    }

    public MXMemoryStore() {
        initCommon();
    }

    /**
     * Set the application context
     *
     * @param context the context
     */
    protected void setContext(Context context) {
        if (null == mSharedContext) {
            if (null != context) {
                mSharedContext = context.getApplicationContext();
            } else {
                throw new RuntimeException("MXMemoryStore : context cannot be null");
            }
        }

        mContext = mSharedContext;
    }

    /**
     * Default constructor
     *
     * @param credentials the expected getCredentials
     * @param context     the context
     */
    public MXMemoryStore(Credentials credentials, Context context) {
        initCommon();

        setContext(context);
        mCredentials = credentials;

        mMetadata = new MXFileStoreMetaData();
    }

    @Override
    public Context getContext() {
        return mContext;
    }

    /**
     * Save changes in the store.
     * If the store uses permanent storage like database or file, it is the optimised time
     * to commit the last changes.
     */
    @Override
    public void commit() {
    }

    /**
     * Open the store.
     */
    public void open() {
    }

    /**
     * Close the store.
     * Any pending operation must be complete in this call.
     */
    @Override
    public void close() {
    }

    /**
     * Clear the store.
     * Any pending operation must be complete in this call.
     */
    @Override
    public void clear() {
        initCommon();
    }

    /**
     * Indicate if the MXStore implementation stores data permanently.
     * Permanent storage allows the SDK to make less requests at the startup.
     *
     * @return true if permanent.
     */
    @Override
    public boolean isPermanent() {
        return false;
    }

    /**
     * Check if the initial load is performed.
     *
     * @return true if it is ready.
     */
    @Override
    public boolean isReady() {
        return true;
    }

    /**
     * Check if the read receipts are ready to be used.
     *
     * @return true if they are ready.
     */
    @Override
    public boolean areReceiptsReady() {
        return true;
    }

    /**
     * @return true if the store is corrupted.
     */
    @Override
    public boolean isCorrupted() {
        return false;
    }

    /**
     * Warn that the store data are corrupted.
     * It might append if an update request failed.
     *
     * @param reason the corruption reason
     */
    @Override
    public void setCorrupted(String reason) {
        dispatchOnStoreCorrupted(mCredentials.getUserId(), reason);
    }

    /**
     * Returns to disk usage size in bytes.
     *
     * @return disk usage size
     */
    @Override
    public long diskUsage() {
        return 0;
    }

    /**
     * Returns the latest known event stream token
     *
     * @return the event stream token
     */
    @Override
    public String getEventStreamToken() {
        return mEventStreamToken;
    }

    /**
     * Set the event stream token.
     *
     * @param token the event stream token
     */
    @Override
    public void setEventStreamToken(String token) {
        if (null != mMetadata) {
            mMetadata.mEventStreamToken = token;
        }
        mEventStreamToken = token;
    }

    @Override
    public void addMXStoreListener(IMXStoreListener listener) {
        synchronized (this) {
            if ((null != listener) && (mListeners.indexOf(listener) < 0)) {
                mListeners.add(listener);
            }
        }
    }

    @Override
    public void removeMXStoreListener(IMXStoreListener listener) {
        synchronized (this) {
            if (null != listener) {
                mListeners.remove(listener);
            }
        }
    }

    /**
     * profile information
     */
    @Override
    public String displayName() {
        if (null != mMetadata) {
            return mMetadata.mUserDisplayName;
        } else {
            return null;
        }
    }

    @Override
    public boolean setDisplayName(String displayName, long ts) {
        boolean isUpdated;

        synchronized (LOG_TAG) {
            if (null != mMetadata) {
                Log.d(LOG_TAG, "## setDisplayName() : from " + mMetadata.mUserDisplayName + " to " + displayName + " ts " + ts);
            }

            isUpdated = (null != mMetadata)
                    && !TextUtils.equals(mMetadata.mUserDisplayName, displayName)
                    && (mUserDisplayNameTs < ts)
                    && (ts != 0)
                    && (ts <= System.currentTimeMillis());

            if (isUpdated) {
                mMetadata.mUserDisplayName = (null != displayName) ? displayName.trim() : null;
                mUserDisplayNameTs = ts;

                // update the cached oneself User
                User myUser = getUser(mMetadata.mUserId);

                if (null != myUser) {
                    myUser.displayname = mMetadata.mUserDisplayName;
                }

                Log.d(LOG_TAG, "## setDisplayName() : updated");
                commit();
            }
        }

        return isUpdated;
    }

    @Override
    public String avatarURL() {
        if (null != mMetadata) {
            return mMetadata.mUserAvatarUrl;
        } else {
            return null;
        }
    }

    @Override
    public boolean setAvatarURL(String avatarURL, long ts) {
        boolean isUpdated = false;

        synchronized (LOG_TAG) {
            if (null != mMetadata) {
                Log.d(LOG_TAG, "## setAvatarURL() : from " + mMetadata.mUserAvatarUrl + " to " + avatarURL + " ts " + ts);
            }

            isUpdated = (null != mMetadata) && !TextUtils.equals(mMetadata.mUserAvatarUrl, avatarURL)
                    && (mUserAvatarUrlTs < ts) && (ts != 0) && (ts <= System.currentTimeMillis());

            if (isUpdated) {
                mMetadata.mUserAvatarUrl = avatarURL;
                mUserAvatarUrlTs = ts;

                // update the cached oneself User
                User myUser = getUser(mMetadata.mUserId);

                if (null != myUser) {
                    myUser.setAvatarUrl(avatarURL);
                }

                Log.d(LOG_TAG, "## setAvatarURL() : updated");
                commit();
            }
        }

        return isUpdated;
    }

    @Override
    public List<ThirdPartyIdentifier> thirdPartyIdentifiers() {
        if (null != mMetadata) {
            return mMetadata.mThirdPartyIdentifiers;
        } else {
            return new ArrayList<>();
        }
    }

    @Override
    public void setThirdPartyIdentifiers(List<ThirdPartyIdentifier> identifiers) {
        if (null != mMetadata) {
            mMetadata.mThirdPartyIdentifiers = identifiers;

            Log.d(LOG_TAG, "setThirdPartyIdentifiers : commit");
            commit();
        }
    }

    @Override
    public List<String> getIgnoredUserIdsList() {
        if (null != mMetadata) {
            return mMetadata.mIgnoredUsers;
        } else {
            return new ArrayList<>();
        }
    }

    @Override
    public void setIgnoredUserIdsList(List<String> users) {
        if (null != mMetadata) {
            mMetadata.mIgnoredUsers = users;
            Log.d(LOG_TAG, "setIgnoredUserIdsList : commit");
            commit();
        }
    }

    @Override
    public Map<String, List<String>> getDirectChatRoomsDict() {
        return mMetadata.mDirectChatRoomsMap;
    }

    @Override
    public void setDirectChatRoomsDict(Map<String, List<String>> directChatRoomsDict) {
        if (null != mMetadata) {
            mMetadata.mDirectChatRoomsMap = directChatRoomsDict;
            Log.d(LOG_TAG, "setDirectChatRoomsDict : commit");
            commit();
        }
    }

    @Override
    public Collection<Room> getRooms() {
        return new ArrayList<>(mRooms.values());
    }

    @Override
    public Collection<User> getUsers() {
        Collection<User> users;

        synchronized (mUsers) {
            users = new ArrayList<>(mUsers.values());
        }

        return users;
    }

    @Override
    public Room getRoom(String roomId) {
        if (null != roomId) {
            return mRooms.get(roomId);
        } else {
            return null;
        }
    }

    @Override
    public User getUser(String userId) {
        if (null != userId) {
            User user;

            synchronized (mUsers) {
                user = mUsers.get(userId);
            }

            return user;
        } else {
            return null;
        }
    }

    @Override
    public void storeUser(User user) {
        if ((null != user) && (null != user.user_id)) {
            try {
                synchronized (mUsers) {
                    mUsers.put(user.user_id, user);
                }
            } catch (OutOfMemoryError e) {
                dispatchOOM(e);
            }
        }
    }

    /**
     * Update the user information from a room member.
     *
     * @param roomMember the room member.
     */
    @Override
    public void updateUserWithRoomMemberEvent(RoomMember roomMember) {
        try {
            if (null != roomMember) {
                User user = getUser(roomMember.getUserId());

                // if the user does not exist, create it
                if (null == user) {
                    user = new User();
                    user.user_id = roomMember.getUserId();
                    user.setRetrievedFromRoomMember();
                    storeUser(user);
                }

                // update the display name and the avatar url.
                // the leave and ban events have no displayname and no avatar url.
                if (TextUtils.equals(roomMember.membership, RoomMember.MEMBERSHIP_JOIN)) {
                    boolean hasUpdates = !TextUtils.equals(user.displayname, roomMember.displayname)
                            || !TextUtils.equals(user.getAvatarUrl(), roomMember.getAvatarUrl());

                    if (hasUpdates) {
                        // invite event does not imply that the user uses the application.
                        // but if the presence is set to 0, it means that the user information is not initialized
                        if (user.getLatestPresenceTs() < roomMember.getOriginServerTs()) {
                            // if the user joined the room, it implies that he used the application
                            user.displayname = roomMember.displayname;
                            user.setAvatarUrl(roomMember.getAvatarUrl());
                            user.setLatestPresenceTs(roomMember.getOriginServerTs());
                            user.setRetrievedFromRoomMember();
                        }
                    }
                }
            }
        } catch (OutOfMemoryError oom) {
            dispatchOOM(oom);
            Log.e(LOG_TAG, "## updateUserWithRoomMemberEvent() failed " + oom.getMessage(), oom);
        } catch (Exception e) {
            Log.e(LOG_TAG, "## updateUserWithRoomMemberEvent() failed " + e.getMessage(), e);
        }
    }

    @Override
    public void storeRoom(Room room) {
        if ((null != room) && (null != room.getRoomId())) {
            mRooms.put(room.getRoomId(), room);

            // defines a default back token
            if (!mRoomTokens.containsKey(room.getRoomId())) {
                storeBackToken(room.getRoomId(), "");
            }
        }
    }

    @Override
    public Event getOldestEvent(String roomId) {
        Event event = null;

        if (null != roomId) {
            synchronized (mRoomEventsLock) {
                LinkedHashMap<String, Event> events = mRoomEvents.get(roomId);

                if (events != null) {
                    Iterator<Event> it = events.values().iterator();
                    if (it.hasNext()) {
                        event = it.next();
                    }
                }
            }
        }

        return event;
    }

    /**
     * Get the latest event from the given room (to update summary for example)
     *
     * @param roomId the room id
     * @return the event
     */
    @Override
    public Event getLatestEvent(String roomId) {
        Event event = null;

        if (null != roomId) {
            synchronized (mRoomEventsLock) {
                LinkedHashMap<String, Event> events = mRoomEvents.get(roomId);

                if (events != null) {
                    Iterator<Event> it = events.values().iterator();
                    if (it.hasNext()) {
                        Event lastEvent = null;

                        while (it.hasNext()) {
                            lastEvent = it.next();
                        }

                        event = lastEvent;
                    }
                }
            }
        }
        return event;
    }

    /**
     * Count the number of events after the provided events id
     *
     * @param roomId  the room id.
     * @param eventId the event id to find.
     * @return the events count after this event if
     */
    @Override
    public int eventsCountAfter(String roomId, String eventId) {
        return eventsAfter(roomId, eventId, mCredentials.getUserId(), null).size();
    }

    @Override
    public void storeLiveRoomEvent(Event event) {
        try {
            if ((null != event) && (null != event.roomId) && (null != event.eventId)) {
                synchronized (mRoomEventsLock) {
                    LinkedHashMap<String, Event> events = mRoomEvents.get(event.roomId);

                    // create the list it does not exist
                    if (null == events) {
                        events = new LinkedHashMap<>();
                        mRoomEvents.put(event.roomId, events);
                    } else if (events.containsKey(event.eventId)) {
                        // the event is already define
                        return;
                    } else if (!event.isDummyEvent() && (mTemporaryEventsList.size() > 0)) {
                        // remove any waiting echo event
                        String dummyKey = null;

                        for (String key : mTemporaryEventsList.keySet()) {
                            Event eventToCheck = mTemporaryEventsList.get(key);
                            if (TextUtils.equals(eventToCheck.eventId, event.eventId)) {
                                dummyKey = key;
                                break;
                            }
                        }

                        if (null != dummyKey) {
                            events.remove(dummyKey);
                            mTemporaryEventsList.remove(dummyKey);
                        }
                    }

                    // If we don't have any information on this room - a pagination token, namely - we don't store the event but instead
                    // wait for the first pagination request to set things right
                    events.put(event.eventId, event);

                    if (event.isDummyEvent()) {
                        mTemporaryEventsList.put(event.eventId, event);
                    }
                }
            }
        } catch (OutOfMemoryError e) {
            dispatchOOM(e);
        }
    }

    @Override
    public boolean doesEventExist(String eventId, String roomId) {
        boolean res = false;

        if (!TextUtils.isEmpty(eventId) && !TextUtils.isEmpty(roomId)) {
            synchronized (mRoomEventsLock) {
                res = mRoomEvents.containsKey(roomId) && mRoomEvents.get(roomId).containsKey(eventId);
            }
        }

        return res;
    }

    @Override
    public Event getEvent(String eventId, String roomId) {
        Event event = null;

        if (!TextUtils.isEmpty(eventId) && !TextUtils.isEmpty(roomId)) {
            synchronized (mRoomEventsLock) {
                LinkedHashMap<String, Event> events = mRoomEvents.get(roomId);

                if (events != null) {
                    event = events.get(eventId);
                }
            }
        }

        return event;
    }

    @Override
    public void deleteEvent(Event event) {
        if ((null != event) && (null != event.roomId) && (event.eventId != null)) {
            synchronized (mRoomEventsLock) {
                LinkedHashMap<String, Event> events = mRoomEvents.get(event.roomId);
                if (events != null) {
                    events.remove(event.eventId);
                }
            }
        }
    }

    @Override
    public void deleteRoom(String roomId) {
        // sanity check
        if (null != roomId) {
            deleteRoomData(roomId);
            synchronized (mRoomEventsLock) {
                mRooms.remove(roomId);
            }
        }
    }

    @Override
    public void deleteRoomData(String roomId) {
        // sanity check
        if (null != roomId) {
            synchronized (mRoomEventsLock) {
                mRoomEvents.remove(roomId);
                mRoomTokens.remove(roomId);
                mRoomSummaries.remove(roomId);
                mRoomAccountData.remove(roomId);
                mReceiptsByRoomId.remove(roomId);
            }
        }
    }

    /**
     * Remove all sent messages in a room.
     *
     * @param roomId     the id of the room.
     * @param keepUnsent set to true to do not delete the unsent message
     */
    @Override
    public void deleteAllRoomMessages(String roomId, boolean keepUnsent) {
        // sanity check
        if (null != roomId) {
            synchronized (mRoomEventsLock) {

                if (keepUnsent) {
                    LinkedHashMap<String, Event> eventMap = mRoomEvents.get(roomId);

                    if (null != eventMap) {
                        List<Event> events = new ArrayList<>(eventMap.values());

                        for (Event event : events) {
                            if (event.mSentState == Event.SentState.SENT) {
                                if (null != event.eventId) {
                                    eventMap.remove(event.eventId);
                                }
                            }
                        }
                    }
                } else {
                    mRoomEvents.remove(roomId);
                }

                mRoomSummaries.remove(roomId);
            }
        }
    }

    @Override
    public void flushRoomEvents(String roomId) {
        // NOP
    }

    @Override
    public void storeRoomEvents(String roomId, TokensChunkEvents tokensChunkEvents, EventTimeline.Direction direction) {
        try {
            if (null != roomId) {
                synchronized (mRoomEventsLock) {
                    LinkedHashMap<String, Event> events = mRoomEvents.get(roomId);
                    if (events == null) {
                        events = new LinkedHashMap<>();
                        mRoomEvents.put(roomId, events);
                    }

                    if (direction == EventTimeline.Direction.FORWARDS) {
                        mRoomTokens.put(roomId, tokensChunkEvents.start);

                        for (Event event : tokensChunkEvents.chunk) {
                            events.put(event.eventId, event);
                        }
                    } else { // BACKWARD
                        Collection<Event> eventsList = events.values();

                        // no stored events
                        if (events.size() == 0) {
                            // insert the catchup events in reverse order
                            for (int index = tokensChunkEvents.chunk.size() - 1; index >= 0; index--) {
                                Event backEvent = tokensChunkEvents.chunk.get(index);
                                events.put(backEvent.eventId, backEvent);
                            }

                            // define a token
                            mRoomTokens.put(roomId, tokensChunkEvents.start);
                        } else {
                            LinkedHashMap<String, Event> events2 = new LinkedHashMap<>();

                            // insert the catchup events in reverse order
                            for (int index = tokensChunkEvents.chunk.size() - 1; index >= 0; index--) {
                                Event backEvent = tokensChunkEvents.chunk.get(index);
                                events2.put(backEvent.eventId, backEvent);
                            }

                            // add the previous added Events
                            for (Event event : eventsList) {
                                events2.put(event.eventId, event);
                            }

                            // store the new list
                            mRoomEvents.put(roomId, events2);
                        }
                    }
                }
            }
        } catch (OutOfMemoryError e) {
            dispatchOOM(e);
        }
    }

    /**
     * Store the back token of a room.
     *
     * @param roomId    the room id.
     * @param backToken the back token
     */
    @Override
    public void storeBackToken(String roomId, String backToken) {
        if ((null != roomId) && (null != backToken)) {
            mRoomTokens.put(roomId, backToken);
        }
    }

    @Override
    public void flushSummary(RoomSummary summary) {
    }

    @Override
    public void flushSummaries() {
    }

    @Override
    public void storeSummary(RoomSummary summary) {
        try {
            if ((null != summary) && (null != summary.getRoomId())) {
                mRoomSummaries.put(summary.getRoomId(), summary);
            }
        } catch (OutOfMemoryError e) {
            dispatchOOM(e);
        }
    }

    @Override
    public void storeAccountData(String roomId, RoomAccountData accountData) {
        try {
            if (null != roomId) {
                Room room = mRooms.get(roomId);

                // sanity checks
                if ((room != null) && (null != accountData)) {
                    mRoomAccountData.put(roomId, accountData);
                }
            }
        } catch (OutOfMemoryError e) {
            dispatchOOM(e);
        }
    }

    @Override
    public void storeLiveStateForRoom(String roomId) {
    }

    @Override
    public void storeRoomStateEvent(String roomId, im.vector.matrix.android.api.session.events.model.Event event) {
        /*synchronized (mRoomStateEventsByRoomId) {
            Map<String, Event> events = mRoomStateEventsByRoomId.get(roomId);

            if (null == events) {
                events = new HashMap<>();
                mRoomStateEventsByRoomId.put(roomId, events);
            }

            // keeps the latest state events
            if (null != event.stateKey) {
                events.put(event.stateKey, event);
            }
        }*/
    }

    @Override
    public void getRoomStateEvents(final String roomId, final ApiCallback<List<im.vector.matrix.android.api.session.events.model.Event>> callback) {
        final List<im.vector.matrix.android.api.session.events.model.Event> events = new ArrayList<>();

        /*synchronized (mRoomStateEventsByRoomId) {
            if (mRoomStateEventsByRoomId.containsKey(roomId)) {
                events.addAll(mRoomStateEventsByRoomId.get(roomId).values());
            }
        }*/

        (new Handler(Looper.getMainLooper())).post(new Runnable() {
            @Override
            public void run() {
                callback.onSuccess(events);
            }
        });
    }

    /**
     * Retrieve all non-state room events for this room.
     *
     * @param roomId The room ID
     * @return A collection of events. null if there is no cached event.
     */
    @Override
    public Collection<Event> getRoomMessages(final String roomId) {
        // sanity check
        if (null == roomId) {
            return null;
        }

        Collection<Event> collection = null;

        synchronized (mRoomEventsLock) {
            LinkedHashMap<String, Event> events = mRoomEvents.get(roomId);

            if (null != events) {
                collection = new ArrayList<>(events.values());
            }
        }

        return collection;
    }

    @Override
    public TokensChunkEvents getEarlierMessages(final String roomId, final String fromToken, final int limit) {
        // For now, we return everything we have for the original null token request
        // For older requests (providing a token), returning null for now
        if (null != roomId) {
            List<Event> eventsList;

            synchronized (mRoomEventsLock) {
                LinkedHashMap<String, Event> events = mRoomEvents.get(roomId);
                if ((events == null) || (events.size() == 0)) {
                    return null;
                }

                // reach the end of the stored items
                if (TextUtils.equals(mRoomTokens.get(roomId), fromToken)) {
                    return null;
                }

                // check if the token is known in the sublist
                eventsList = new ArrayList<>(events.values());
            }


            List<Event> subEventsList = new ArrayList<>();

            // search from the latest to the oldest events
            Collections.reverse(eventsList);

            TokensChunkEvents response = new TokensChunkEvents();

            // start the latest event and there is enough events to provide to the caller ?
            if ((null == fromToken) && (eventsList.size() <= limit)) {
                subEventsList = eventsList;
            } else {
                int index = 0;

                if (null != fromToken) {
                    // search if token is one of the stored events
                    for (; (index < eventsList.size()) && (!TextUtils.equals(fromToken, eventsList.get(index).mToken)); index++)
                        ;

                    index++;
                }

                // found it ?
                if (index < eventsList.size()) {
                    for (; index < eventsList.size(); index++) {
                        Event event = eventsList.get(index);
                        subEventsList.add(event);

                        // loop until to find an event with a token
                        if ((subEventsList.size() >= limit) && (event.mToken != null)) {
                            break;
                        }
                    }
                }
            }

            // unknown token
            if (subEventsList.size() == 0) {
                return null;
            }

            response.chunk = subEventsList;

            Event firstEvent = subEventsList.get(0);
            Event lastEvent = subEventsList.get(subEventsList.size() - 1);

            response.start = firstEvent.mToken;

            // unknown last event token, use the latest known one
            if ((null == lastEvent.mToken) && !TextUtils.isEmpty(mRoomTokens.get(roomId))) {
                lastEvent.mToken = mRoomTokens.get(roomId);
            }

            response.end = lastEvent.mToken;

            return response;
        }
        return null;
    }

    @Override
    public Collection<RoomSummary> getSummaries() {
        List<RoomSummary> summaries = new ArrayList<>();

        for (String roomId : mRoomSummaries.keySet()) {
            Room room = mRooms.get(roomId);
            if (null != room) {
                if (!room.isJoined() && !room.isInvited()) {
                    Log.e(LOG_TAG, "## getSummaries() : a summary exists for the roomId " + roomId + " but the user is not anymore a member");
                } else {
                    summaries.add(mRoomSummaries.get(roomId));
                }
            } else {
                Log.e(LOG_TAG, "## getSummaries() : a summary exists for the roomId " + roomId + " but it does not exist in the room list");
            }
        }

        return summaries;
    }

    @Nullable
    @Override
    public RoomSummary getSummary(String roomId) {
        // sanity check
        if (null == roomId) {
            return null;
        }

        Room room = mRooms.get(roomId);
        if (null != room) {
            return mRoomSummaries.get(roomId);
        } else {
            Log.e(LOG_TAG, "## getSummary() : a summary exists for the roomId " + roomId + " but it does not exist in the room list");
        }

        return null;
    }

    @Override
    public List<Event> getLatestUnsentEvents(String roomId) {
        if (null == roomId) {
            return null;
        }

        List<Event> unsentRoomEvents = new ArrayList<>();

        synchronized (mRoomEventsLock) {
            LinkedHashMap<String, Event> events = mRoomEvents.get(roomId);

            // contain some events
            if ((null != events) && (events.size() > 0)) {
                List<Event> eventsList = new ArrayList<>(events.values());

                for (int index = events.size() - 1; index >= 0; index--) {
                    Event event = eventsList.get(index);

                    if (event.mSentState == Event.SentState.WAITING_RETRY) {
                        unsentRoomEvents.add(event);
                    } else {
                        //break;
                    }
                }

                Collections.reverse(unsentRoomEvents);
            }
        }

        return unsentRoomEvents;
    }

    @Override
    public List<Event> getUndeliveredEvents(String roomId) {
        if (null == roomId) {
            return null;
        }

        List<Event> undeliveredEvents = new ArrayList<>();

        synchronized (mRoomEventsLock) {
            LinkedHashMap<String, Event> events = mRoomEvents.get(roomId);

            // contain some events
            if ((null != events) && (events.size() > 0)) {
                List<Event> eventsList = new ArrayList<>(events.values());

                for (int index = 0; index < events.size(); index++) {
                    Event event = eventsList.get(index);

                    if (event.isUndelivered()) {
                        undeliveredEvents.add(event);
                    }
                }
            }
        }

        return undeliveredEvents;
    }

    @Override
    public List<Event> getUnknownDeviceEvents(String roomId) {
        if (null == roomId) {
            return null;
        }

        List<Event> unknownDeviceEvents = new ArrayList<>();

        synchronized (mRoomEventsLock) {
            LinkedHashMap<String, Event> events = mRoomEvents.get(roomId);

            // contain some events
            if ((null != events) && (events.size() > 0)) {
                List<Event> eventsList = new ArrayList<>(events.values());

                for (int index = 0; index < events.size(); index++) {
                    Event event = eventsList.get(index);

                    if (event.isUnknownDevice()) {
                        unknownDeviceEvents.add(event);
                    }
                }
            }
        }

        return unknownDeviceEvents;
    }

    /**
     * Returns the receipts list for an event in a dedicated room.
     * if sort is set to YES, they are sorted from the latest to the oldest ones.
     *
     * @param roomId      The room Id.
     * @param eventId     The event Id. (null to retrieve all existing receipts)
     * @param excludeSelf exclude the oneself read receipts.
     * @param sort        to sort them from the latest to the oldest
     * @return the receipts for an event in a dedicated room.
     */
    @Override
    public List<ReceiptData> getEventReceipts(String roomId, String eventId, boolean excludeSelf, boolean sort) {
        List<ReceiptData> receipts = new ArrayList<>();

        synchronized (mReceiptsByRoomIdLock) {
            if (mReceiptsByRoomId.containsKey(roomId)) {
                String myUserID = mCredentials.getUserId();

                Map<String, ReceiptData> receiptsByUserId = mReceiptsByRoomId.get(roomId);
                // copy the user id list to avoid having update while looping
                List<String> userIds = new ArrayList<>(receiptsByUserId.keySet());

                if (null == eventId) {
                    receipts.addAll(receiptsByUserId.values());
                } else {
                    for (String userId : userIds) {
                        if (receiptsByUserId.containsKey(userId) && (!excludeSelf || !TextUtils.equals(myUserID, userId))) {
                            ReceiptData receipt = receiptsByUserId.get(userId);

                            if (TextUtils.equals(receipt.eventId, eventId)) {
                                receipts.add(receipt);
                            }
                        }
                    }
                }
            }
        }

        if (sort && (receipts.size() > 0)) {
            Collections.sort(receipts, Comparators.descComparator);
        }

        return receipts;
    }

    /**
     * Store the receipt for an user in a room.
     * The receipt validity is checked i.e the receipt is not for an already read message.
     *
     * @param receipt The event
     * @param roomId  The roomId
     * @return true if the receipt has been stored
     */
    @Override
    public boolean storeReceipt(ReceiptData receipt, String roomId) {
        try {
            // sanity check
            if (TextUtils.isEmpty(roomId) || (null == receipt)) {
                return false;
            }

            Map<String, ReceiptData> receiptsByUserId;

            //Log.d(LOG_TAG, "## storeReceipt() : roomId " + roomId + " userId " + receipt.userId + " eventId " + receipt.eventId
            // + " originServerTs " + receipt.originServerTs);

            synchronized (mReceiptsByRoomIdLock) {
                if (!mReceiptsByRoomId.containsKey(roomId)) {
                    receiptsByUserId = new HashMap<>();
                    mReceiptsByRoomId.put(roomId, receiptsByUserId);
                } else {
                    receiptsByUserId = mReceiptsByRoomId.get(roomId);
                }
            }

            ReceiptData curReceipt = null;

            if (receiptsByUserId.containsKey(receipt.userId)) {
                curReceipt = receiptsByUserId.get(receipt.userId);
            }

            if (null == curReceipt) {
                //Log.d(LOG_TAG, "## storeReceipt() : there was no receipt from this user");
                receiptsByUserId.put(receipt.userId, receipt);
                return true;
            }

            if (TextUtils.equals(receipt.eventId, curReceipt.eventId)) {
                //Log.d(LOG_TAG, "## storeReceipt() : receipt for the same event");
                return false;
            }

            if (receipt.originServerTs < curReceipt.originServerTs) {
                //Log.d(LOG_TAG, "## storeReceipt() : the receipt is older that the current one");
                return false;
            }

            // check if the read receipt is not for an already read message
            if (TextUtils.equals(receipt.userId, mCredentials.getUserId())) {
                synchronized (mReceiptsByRoomIdLock) {
                    LinkedHashMap<String, Event> eventsMap = mRoomEvents.get(roomId);

                    // test if the event is know
                    if ((null != eventsMap) && eventsMap.containsKey(receipt.eventId)) {
                        List<String> eventIds = new ArrayList<>(eventsMap.keySet());

                        int curEventPos = eventIds.indexOf(curReceipt.eventId);
                        int newEventPos = eventIds.indexOf(receipt.eventId);

                        if (curEventPos >= newEventPos) {
                            Log.d(LOG_TAG, "## storeReceipt() : the read message is already read (cur pos " + curEventPos
                                    + " receipt event pos " + newEventPos + ")");
                            return false;
                        }
                    }
                }
            }

            //Log.d(LOG_TAG, "## storeReceipt() : updated");
            receiptsByUserId.put(receipt.userId, receipt);
        } catch (OutOfMemoryError e) {
            dispatchOOM(e);
        }

        return true;
    }

    /**
     * Get the receipt for an user in a dedicated room.
     *
     * @param roomId the room id.
     * @param userId the user id.
     * @return the dedicated receipt
     */
    @Override
    public ReceiptData getReceipt(String roomId, String userId) {
        ReceiptData res = null;

        // sanity checks
        if (!TextUtils.isEmpty(roomId) && !TextUtils.isEmpty(userId)) {
            synchronized (mReceiptsByRoomIdLock) {
                if (mReceiptsByRoomId.containsKey(roomId)) {
                    Map<String, ReceiptData> receipts = mReceiptsByRoomId.get(roomId);
                    res = receipts.get(userId);
                }
            }
        }

        return res;
    }

    /**
     * Return a list of stored events after the parameter one.
     * It could the ones sent by the user excludedUserId.
     * A filter can be applied to ignore some event (Event.EVENT_TYPE_...).
     *
     * @param roomId         the roomId
     * @param eventId        the start event Id.
     * @param excludedUserId the excluded user id
     * @param allowedTypes   the filtered event type (null to allow anyone)
     * @return the evnts list
     */
    private List<Event> eventsAfter(String roomId, String eventId, String excludedUserId, List<String> allowedTypes) {
        // events list
        List<Event> events = new ArrayList<>();

        // sanity check
        if (null != roomId) {
            synchronized (mRoomEventsLock) {
                LinkedHashMap<String, Event> roomEvents = mRoomEvents.get(roomId);

                if (roomEvents != null) {
                    List<Event> linkedEvents = new ArrayList<>(roomEvents.values());

                    // Check messages from the most recent
                    for (int i = linkedEvents.size() - 1; i >= 0; i--) {
                        Event event = linkedEvents.get(i);

                        if ((null == eventId) || !TextUtils.equals(event.eventId, eventId)) {
                            // Keep events matching filters
                            if ((null == allowedTypes || (allowedTypes.indexOf(event.getType()) >= 0))
                                    && !TextUtils.equals(event.getSender(), excludedUserId)) {
                                events.add(event);
                            }
                        } else {
                            // We are done
                            break;
                        }
                    }

                    // filter the unread messages
                    // some messages are not defined as unreadable
                    for (int index = 0; index < events.size(); index++) {
                        Event event = events.get(index);

                        if (TextUtils.equals(event.getSender(), mCredentials.getUserId()) || TextUtils.equals(event.getType(), Event.EVENT_TYPE_STATE_ROOM_MEMBER)) {
                            events.remove(index);
                            index--;
                        }
                    }

                    Collections.reverse(events);
                }
            }
        }

        return events;
    }

    /**
     * Check if an event has been read by an user.
     *
     * @param roomId        the room Id
     * @param userId        the user id
     * @param eventIdTotest the event id
     * @return true if the user has read the message.
     */
    @Override
    public boolean isEventRead(String roomId, String userId, String eventIdTotest) {
        boolean res = false;

        // sanity check
        if ((null != roomId) && (null != userId)) {
            synchronized (mReceiptsByRoomIdLock) {
                synchronized (mRoomEventsLock) {
                    if (mReceiptsByRoomId.containsKey(roomId) && mRoomEvents.containsKey(roomId)) {
                        Map<String, ReceiptData> receiptsByUserId = mReceiptsByRoomId.get(roomId);
                        LinkedHashMap<String, Event> eventsMap = mRoomEvents.get(roomId);

                        // check if the event is known
                        if (eventsMap.containsKey(eventIdTotest) && receiptsByUserId.containsKey(userId)) {
                            ReceiptData data = receiptsByUserId.get(userId);
                            List<String> eventIds = new ArrayList<>(eventsMap.keySet());

                            // the message has been read if it was sent before the latest read one
                            res = eventIds.indexOf(eventIdTotest) <= eventIds.indexOf(data.eventId);
                        } else if (receiptsByUserId.containsKey(userId)) {
                            // the event is not known so assume it is has been flushed
                            res = true;
                        }
                    }
                }
            }
        }

        return res;
    }

    /**
     * Provides the unread events list.
     *
     * @param roomId the room id.
     * @param types  an array of event types strings (Event.EVENT_TYPE_XXX).
     * @return the unread events list.
     */
    @Override
    public List<Event> unreadEvents(String roomId, List<String> types) {
        List<Event> res = null;

        synchronized (mReceiptsByRoomIdLock) {
            if (mReceiptsByRoomId.containsKey(roomId)) {
                Map<String, ReceiptData> receiptsByUserId = mReceiptsByRoomId.get(roomId);

                if (receiptsByUserId.containsKey(mCredentials.getUserId())) {
                    ReceiptData data = receiptsByUserId.get(mCredentials.getUserId());

                    res = eventsAfter(roomId, data.eventId, mCredentials.getUserId(), types);
                }
            }
        }

        if (null == res) {
            res = new ArrayList<>();
        }

        return res;
    }

    /**
     * @return the current listeners
     */
    private List<IMXStoreListener> getListeners() {
        List<IMXStoreListener> listeners;

        synchronized (this) {
            listeners = new ArrayList<>(mListeners);
        }

        return listeners;
    }

    /**
     * Dispatch postProcess
     *
     * @param accountId the account id
     */
    protected void dispatchPostProcess(String accountId) {
        List<IMXStoreListener> listeners = getListeners();

        for (IMXStoreListener listener : listeners) {
            listener.postProcess(accountId);
        }
    }

    /**
     * Dispatch store ready
     *
     * @param accountId the account id
     */
    protected void dispatchOnStoreReady(String accountId) {
        List<IMXStoreListener> listeners = getListeners();

        for (IMXStoreListener listener : listeners) {
            listener.onStoreReady(accountId);
        }
    }

    /**
     * Dispatch that the store is corrupted
     *
     * @param accountId   the account id
     * @param description the error description
     */
    protected void dispatchOnStoreCorrupted(String accountId, String description) {
        List<IMXStoreListener> listeners = getListeners();

        for (IMXStoreListener listener : listeners) {
            listener.onStoreCorrupted(accountId, description);
        }
    }

    /**
     * Dispatch an out of memory error.
     *
     * @param e the out of memory error
     */
    protected void dispatchOOM(OutOfMemoryError e) {
        List<IMXStoreListener> listeners = getListeners();

        for (IMXStoreListener listener : listeners) {
            listener.onStoreOOM(mCredentials.getUserId(), e.getMessage());
        }
    }

    /**
     * Dispatch the read receipts loading.
     *
     * @param roomId the room id.
     */
    protected void dispatchOnReadReceiptsLoaded(String roomId) {
        List<IMXStoreListener> listeners = getListeners();

        for (IMXStoreListener listener : listeners) {
            listener.onReadReceiptsLoaded(roomId);
        }
    }

    /**
     * Provides the store preload time in milliseconds.
     *
     * @return the store preload time in milliseconds.
     */
    @Override
    public long getPreloadTime() {
        return 0;
    }

    /**
     * Provides some store stats
     *
     * @return the store stats
     */
    @Override
    public Map<String, Long> getStats() {
        return new HashMap<>();
    }

    /**
     * Start a runnable from the store thread
     *
     * @param runnable the runnable to call
     */
    @Override
    public void post(Runnable runnable) {
        new Handler(Looper.getMainLooper()).post(runnable);
    }

    /**
     * Store a group
     *
     * @param group the group to store
     */
    @Override
    public void storeGroup(Group group) {
        if ((null != group) && !TextUtils.isEmpty(group.getGroupId())) {
            synchronized (mGroups) {
                mGroups.put(group.getGroupId(), group);
            }
        }
    }

    /**
     * Flush a group
     *
     * @param group the group to store
     */
    @Override
    public void flushGroup(Group group) {
    }

    /**
     * Delete a group
     *
     * @param groupId the groupId to delete
     */
    @Override
    public void deleteGroup(String groupId) {
        if (!TextUtils.isEmpty(groupId)) {
            synchronized (mGroups) {
                mGroups.remove(groupId);
            }
        }
    }

    /**
     * Retrieve a group from its id.
     *
     * @param groupId the group id
     * @return the group if it exists
     */
    @Override
    public Group getGroup(String groupId) {
        synchronized (mGroups) {
            return (null != groupId) ? mGroups.get(groupId) : null;
        }
    }

    /**
     * @return the stored groups
     */
    @Override
    public Collection<Group> getGroups() {
        synchronized (mGroups) {
            return mGroups.values();
        }
    }

    @Override
    public void setURLPreviewEnabled(boolean value) {
        mMetadata.mIsUrlPreviewEnabled = value;
    }

    @Override
    public boolean isURLPreviewEnabled() {
        return mMetadata.mIsUrlPreviewEnabled;
    }

    @Override
    public void setRoomsWithoutURLPreview(Set<String> roomIds) {
        mMetadata.mRoomsListWithoutURLPrevew = roomIds;
    }

    @Override
    public void setUserWidgets(Map<String, Object> contentDict) {
        mMetadata.mUserWidgets = contentDict;
    }

    @Override
    public Map<String, Object> getUserWidgets() {
        return mMetadata.mUserWidgets;
    }

    @Override
    public Set<String> getRoomsWithoutURLPreviews() {
        return (null != mMetadata.mRoomsListWithoutURLPrevew) ? mMetadata.mRoomsListWithoutURLPrevew : new HashSet<String>();
    }

    @Override
    public void addFilter(String jsonFilter, String filterId) {
        mMetadata.mKnownFilters.put(jsonFilter, filterId);
    }

    @Override
    public Map<String, String> getFilters() {
        return new HashMap<>(mMetadata.mKnownFilters);
    }

    @Override
    public void setAntivirusServerPublicKey(@Nullable String key) {
        mMetadata.mAntivirusServerPublicKey = key;
    }

    @Override
    @Nullable
    public String getAntivirusServerPublicKey() {
        return mMetadata.mAntivirusServerPublicKey;
    }

    /**
     * Update the metrics listener
     *
     * @param metricsListener the metrics listener
     */
    public void setMetricsListener(MetricsListener metricsListener) {
        mMetricsListener = metricsListener;
    }

    /**
     * Get the associated dataHandler
     *
     * @return the associated dataHandler
     */
    protected MXDataHandler getDataHandler() {
        return mDataHandler;
    }

    /**
     * Update the associated dataHandler
     *
     * @param dataHandler the dataHandler
     */
    public void setDataHandler(final MXDataHandler dataHandler) {
        mDataHandler = dataHandler;
    }
}
