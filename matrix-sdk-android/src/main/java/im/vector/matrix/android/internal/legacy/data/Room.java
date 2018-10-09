/*
 * Copyright 2014 OpenMarket Ltd
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

package im.vector.matrix.android.internal.legacy.data;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Pair;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import im.vector.matrix.android.R;
import im.vector.matrix.android.internal.legacy.MXDataHandler;
import im.vector.matrix.android.internal.legacy.MXPatterns;
import im.vector.matrix.android.internal.legacy.call.MXCallsManager;
import im.vector.matrix.android.internal.legacy.crypto.MXCryptoError;
import im.vector.matrix.android.internal.legacy.crypto.data.MXEncryptEventContentResult;
import im.vector.matrix.android.internal.legacy.data.store.IMXStore;
import im.vector.matrix.android.internal.legacy.data.timeline.EventTimeline;
import im.vector.matrix.android.internal.legacy.data.timeline.EventTimelineFactory;
import im.vector.matrix.android.internal.legacy.db.MXMediasCache;
import im.vector.matrix.android.internal.legacy.listeners.IMXEventListener;
import im.vector.matrix.android.internal.legacy.listeners.MXEventListener;
import im.vector.matrix.android.internal.legacy.listeners.MXRoomEventListener;
import im.vector.matrix.android.internal.legacy.rest.callback.ApiCallback;
import im.vector.matrix.android.internal.legacy.rest.callback.SimpleApiCallback;
import im.vector.matrix.android.internal.legacy.rest.client.AccountDataRestClient;
import im.vector.matrix.android.internal.legacy.rest.client.RoomsRestClient;
import im.vector.matrix.android.internal.legacy.rest.client.UrlPostTask;
import im.vector.matrix.android.internal.legacy.rest.model.BannedUser;
import im.vector.matrix.android.internal.legacy.rest.model.CreatedEvent;
import im.vector.matrix.android.internal.legacy.rest.model.Event;
import im.vector.matrix.android.internal.legacy.rest.model.MatrixError;
import im.vector.matrix.android.internal.legacy.rest.model.PowerLevels;
import im.vector.matrix.android.internal.legacy.rest.model.ReceiptData;
import im.vector.matrix.android.internal.legacy.rest.model.RoomDirectoryVisibility;
import im.vector.matrix.android.internal.legacy.rest.model.RoomMember;
import im.vector.matrix.android.internal.legacy.rest.model.TokensChunkEvents;
import im.vector.matrix.android.internal.legacy.rest.model.message.FileInfo;
import im.vector.matrix.android.internal.legacy.rest.model.message.FileMessage;
import im.vector.matrix.android.internal.legacy.rest.model.message.ImageInfo;
import im.vector.matrix.android.internal.legacy.rest.model.message.ImageMessage;
import im.vector.matrix.android.internal.legacy.rest.model.message.LocationMessage;
import im.vector.matrix.android.internal.legacy.rest.model.message.Message;
import im.vector.matrix.android.internal.legacy.rest.model.message.ThumbnailInfo;
import im.vector.matrix.android.internal.legacy.rest.model.message.VideoInfo;
import im.vector.matrix.android.internal.legacy.rest.model.message.VideoMessage;
import im.vector.matrix.android.internal.legacy.rest.model.sync.InvitedRoomSync;
import im.vector.matrix.android.internal.legacy.rest.model.sync.RoomResponse;
import im.vector.matrix.android.internal.legacy.rest.model.sync.RoomSync;
import im.vector.matrix.android.internal.legacy.util.ImageUtils;
import im.vector.matrix.android.internal.legacy.util.JsonUtils;
import im.vector.matrix.android.internal.legacy.util.Log;

/**
 * Class representing a room and the interactions we have with it.
 */
public class Room {

    private static final String LOG_TAG = Room.class.getSimpleName();

    // Account data
    private RoomAccountData mAccountData = new RoomAccountData();

    // handler
    private MXDataHandler mDataHandler;

    // store
    private IMXStore mStore;

    private String mMyUserId = null;

    // Map to keep track of the listeners the client adds vs. the ones we actually register to the global data handler.
    // This is needed to find the right one when removing the listener.
    private final Map<IMXEventListener, IMXEventListener> mEventListeners = new HashMap<>();

    // the user is leaving the room
    private boolean mIsLeaving = false;

    // the room is syncing
    private boolean mIsSyncing;

    // the unread messages count must be refreshed when the current sync is done.
    private boolean mRefreshUnreadAfterSync = false;

    // the time line
    private EventTimeline mTimeline;

    // initial sync callback.
    private ApiCallback<Void> mOnInitialSyncCallback;

    // This is used to block live events and history requests until the state is fully processed and ready
    private boolean mIsReady = false;

    // call conference user id
    private String mCallConferenceUserId;

    // true when the current room is a left one
    private boolean mIsLeft;

    /**
     * Constructor
     * FIXME All this @NonNull annotation must be also added to the class members and getters
     *
     * @param dataHandler the data handler
     * @param store       the store
     * @param roomId      the room id
     */
    public Room(@NonNull final MXDataHandler dataHandler, @NonNull final IMXStore store, @NonNull final String roomId) {
        mDataHandler = dataHandler;
        mStore = store;
        mMyUserId = mDataHandler.getUserId();
        mTimeline = EventTimelineFactory.liveTimeline(mDataHandler, this, roomId);
    }

    /**
     * @return the used data handler
     */
    public MXDataHandler getDataHandler() {
        return mDataHandler;
    }

    /**
     * @return the store in which the room is stored
     */
    public IMXStore getStore() {
        if (null == mStore) {
            if (null != mDataHandler) {
                mStore = mDataHandler.getStore(getRoomId());
            }

            if (null == mStore) {
                Log.e(LOG_TAG, "## getStore() : cannot retrieve the store of " + getRoomId());
            }
        }

        return mStore;
    }

    /**
     * Determine whether we should encrypt messages for invited users in this room.
     * <p>
     * Check here whether the invited members are allowed to read messages in the room history
     * from the point they were invited onwards.
     *
     * @return true if we should encrypt messages for invited users.
     */
    public boolean shouldEncryptForInvitedMembers() {
        String historyVisibility = getState().history_visibility;
        return !TextUtils.equals(historyVisibility, RoomState.HISTORY_VISIBILITY_JOINED);
    }

    /**
     * Tells if the room is a call conference one
     * i.e. this room has been created to manage the call conference
     *
     * @return true if it is a call conference room.
     */
    public boolean isConferenceUserRoom() {
        return getState().isConferenceUserRoom();
    }

    /**
     * Set this room as a conference user room
     *
     * @param isConferenceUserRoom true when it is an user conference room.
     */
    public void setIsConferenceUserRoom(boolean isConferenceUserRoom) {
        getState().setIsConferenceUserRoom(isConferenceUserRoom);
    }

    /**
     * Test if there is an ongoing conference call.
     *
     * @return true if there is one.
     */
    public boolean isOngoingConferenceCall() {
        RoomMember conferenceUser = getState().getMember(MXCallsManager.getConferenceUserId(getRoomId()));
        return (null != conferenceUser) && TextUtils.equals(conferenceUser.membership, RoomMember.MEMBERSHIP_JOIN);
    }

    /**
     * Defines that the current room is a left one
     *
     * @param isLeft true when the current room is a left one
     */
    public void setIsLeft(boolean isLeft) {
        mIsLeft = isLeft;
        mTimeline.setIsHistorical(isLeft);
    }

    /**
     * @return true if the current room is an left one
     */
    public boolean isLeft() {
        return mIsLeft;
    }

    //================================================================================
    // Sync events
    //================================================================================

    /**
     * Manage list of ephemeral events
     *
     * @param events the ephemeral events
     */
    private void handleEphemeralEvents(List<Event> events) {
        for (Event event : events) {

            // ensure that the room Id is defined
            event.roomId = getRoomId();

            try {
                if (Event.EVENT_TYPE_RECEIPT.equals(event.getType())) {
                    if (event.roomId != null) {
                        List<String> senders = handleReceiptEvent(event);

                        if (senders != null && !senders.isEmpty()) {
                            mDataHandler.onReceiptEvent(event.roomId, senders);
                        }
                    }
                } else if (Event.EVENT_TYPE_TYPING.equals(event.getType())) {
                    JsonObject eventContent = event.getContentAsJsonObject();

                    if (eventContent.has("user_ids")) {
                        synchronized (mTypingUsers) {
                            mTypingUsers.clear();

                            List<String> typingUsers = null;

                            try {
                                typingUsers = (new Gson()).fromJson(eventContent.get("user_ids"), new TypeToken<List<String>>() {
                                }.getType());
                            } catch (Exception e) {
                                Log.e(LOG_TAG, "## handleEphemeralEvents() : exception " + e.getMessage(), e);
                            }

                            if (typingUsers != null) {
                                mTypingUsers.addAll(typingUsers);
                            }
                        }
                    }

                    mDataHandler.onLiveEvent(event, getState());
                }
            } catch (Exception e) {
                Log.e(LOG_TAG, "ephemeral event failed " + e.getMessage(), e);
            }
        }
    }

    /**
     * Handle the events of a joined room.
     *
     * @param roomSync            the sync events list.
     * @param isGlobalInitialSync true if the room is initialized by a global initial sync.
     */
    public void handleJoinedRoomSync(RoomSync roomSync, boolean isGlobalInitialSync) {
        if (null != mOnInitialSyncCallback) {
            Log.d(LOG_TAG, "initial sync handleJoinedRoomSync " + getRoomId());
        } else {
            Log.d(LOG_TAG, "handleJoinedRoomSync " + getRoomId());
        }

        mIsSyncing = true;

        synchronized (this) {
            mTimeline.handleJoinedRoomSync(roomSync, isGlobalInitialSync);
            RoomSummary roomSummary = getRoomSummary();
            if (roomSummary != null) {
                roomSummary.setIsJoined();
            }
            // ephemeral events
            if ((null != roomSync.ephemeral) && (null != roomSync.ephemeral.events)) {
                handleEphemeralEvents(roomSync.ephemeral.events);
            }

            // Handle account data events (if any)
            if ((null != roomSync.accountData) && (null != roomSync.accountData.events) && (roomSync.accountData.events.size() > 0)) {
                if (isGlobalInitialSync) {
                    Log.d(LOG_TAG, "## handleJoinedRoomSync : received " + roomSync.accountData.events.size() + " account data events");
                }

                handleAccountDataEvents(roomSync.accountData.events);
            }
        }

        // the user joined the room
        // With V2 sync, the server sends the events to init the room.
        if ((null != mOnInitialSyncCallback) && isJoined()) {
            Log.d(LOG_TAG, "handleJoinedRoomSync " + getRoomId() + " :  the initial sync is done");
            final ApiCallback<Void> fOnInitialSyncCallback = mOnInitialSyncCallback;

            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    // to initialise the notification counters
                    markAllAsRead(null);

                    try {
                        fOnInitialSyncCallback.onSuccess(null);
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "handleJoinedRoomSync : onSuccess failed" + e.getMessage(), e);
                    }
                }
            });

            mOnInitialSyncCallback = null;
        }

        mIsSyncing = false;

        if (mRefreshUnreadAfterSync) {
            if (!isGlobalInitialSync) {
                refreshUnreadCounter();
            } // else -> it will be done at the end of the sync
            mRefreshUnreadAfterSync = false;
        }
    }

    /**
     * Handle the invitation room events
     *
     * @param invitedRoomSync the invitation room events.
     */
    public void handleInvitedRoomSync(InvitedRoomSync invitedRoomSync) {
        mTimeline.handleInvitedRoomSync(invitedRoomSync);

        RoomSummary roomSummary = getRoomSummary();

        if (roomSummary != null) {
            roomSummary.setIsInvited();
        }
    }

    /**
     * Store an outgoing event.
     *
     * @param event the event.
     */
    public void storeOutgoingEvent(Event event) {
        mTimeline.storeOutgoingEvent(event);
    }

    /**
     * Request events to the server. The local cache is not used.
     * The events will not be saved in the local storage.
     *
     * @param token           the token to go back from.
     * @param paginationCount the number of events to retrieve.
     * @param callback        the onComplete callback
     */
    public void requestServerRoomHistory(final String token,
                                         final int paginationCount,
                                         final ApiCallback<TokensChunkEvents> callback) {
        mDataHandler.getDataRetriever()
                .requestServerRoomHistory(getRoomId(), token, paginationCount, mDataHandler.isLazyLoadingEnabled(),
                        new SimpleApiCallback<TokensChunkEvents>(callback) {
                            @Override
                            public void onSuccess(TokensChunkEvents info) {
                                callback.onSuccess(info);
                            }
                        });
    }

    /**
     * cancel any remote request
     */
    public void cancelRemoteHistoryRequest() {
        mDataHandler.getDataRetriever().cancelRemoteHistoryRequest(getRoomId());
    }

    //================================================================================
    // Getters / setters
    //================================================================================

    public String getRoomId() {
        return getState().roomId;
    }

    public void setAccountData(RoomAccountData accountData) {
        mAccountData = accountData;
    }

    public RoomAccountData getAccountData() {
        return mAccountData;
    }

    public RoomState getState() {
        return mTimeline.getState();
    }

    public boolean isLeaving() {
        return mIsLeaving;
    }

    public void getMembersAsync(@NonNull final ApiCallback<List<RoomMember>> callback) {
        getState().getMembersAsync(callback);
    }

    public void getDisplayableMembersAsync(@NonNull final ApiCallback<List<RoomMember>> callback) {
        getState().getDisplayableMembersAsync(callback);
    }

    public EventTimeline getTimeline() {
        return mTimeline;
    }

    public void setTimeline(EventTimeline eventTimeline) {
        mTimeline = eventTimeline;
    }

    public void setReadyState(boolean isReady) {
        mIsReady = isReady;
    }

    public boolean isReady() {
        return mIsReady;
    }

    /**
     * @return the list of active members in a room ie joined or invited ones.
     */
    public void getActiveMembersAsync(@NonNull final ApiCallback<List<RoomMember>> callback) {
        getMembersAsync(new SimpleApiCallback<List<RoomMember>>(callback) {
            @Override
            public void onSuccess(List<RoomMember> members) {
                List<RoomMember> activeMembers = new ArrayList<>();
                String conferenceUserId = MXCallsManager.getConferenceUserId(getRoomId());

                for (RoomMember member : members) {
                    if (!TextUtils.equals(member.getUserId(), conferenceUserId)) {
                        if (TextUtils.equals(member.membership, RoomMember.MEMBERSHIP_JOIN)
                                || TextUtils.equals(member.membership, RoomMember.MEMBERSHIP_INVITE)) {
                            activeMembers.add(member);
                        }
                    }
                }

                callback.onSuccess(activeMembers);
            }
        });
    }

    /**
     * Get the list of the members who have joined the room.
     *
     * @return the list the joined members of the room.
     */
    public void getJoinedMembersAsync(final ApiCallback<List<RoomMember>> callback) {
        getMembersAsync(new SimpleApiCallback<List<RoomMember>>(callback) {
            @Override
            public void onSuccess(List<RoomMember> members) {
                List<RoomMember> joinedMembersList = new ArrayList<>();

                for (RoomMember member : members) {
                    if (TextUtils.equals(member.membership, RoomMember.MEMBERSHIP_JOIN)) {
                        joinedMembersList.add(member);
                    }
                }

                callback.onSuccess(joinedMembersList);
            }
        });
    }

    @Nullable
    public RoomMember getMember(String userId) {
        return getState().getMember(userId);
    }

    // member event caches
    private final Map<String, Event> mMemberEventByEventId = new HashMap<>();

    public void getMemberEvent(final String userId, final ApiCallback<Event> callback) {
        final Event event;
        final RoomMember member = getMember(userId);

        if ((null != member) && (null != member.getOriginalEventId())) {
            event = mMemberEventByEventId.get(member.getOriginalEventId());

            if (null == event) {
                mDataHandler.getDataRetriever().getRoomsRestClient().getEvent(getRoomId(), member.getOriginalEventId(), new SimpleApiCallback<Event>(callback) {
                    @Override
                    public void onSuccess(Event event) {
                        if (null != event) {
                            mMemberEventByEventId.put(event.eventId, event);
                        }
                        callback.onSuccess(event);
                    }
                });
                return;
            }
        } else {
            event = null;
        }

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                callback.onSuccess(event);
            }
        });
    }

    public String getTopic() {
        return getState().topic;
    }

    public String getVisibility() {
        return getState().visibility;
    }

    /**
     * @return true if the user is invited to the room
     */
    public boolean isInvited() {
        if (getRoomSummary() == null) {
            return false;
        }

        return getRoomSummary().isInvited();
    }

    /**
     * @return true if the user has joined the room
     */
    public boolean isJoined() {
        if (getRoomSummary() == null) {
            return false;
        }

        return getRoomSummary().isJoined();
    }

    /**
     * @return true is the user is a member of the room (invited or joined)
     */
    public boolean isMember() {
        return isJoined() || isInvited();
    }

    /**
     * @return true if the user is invited in a direct chat room
     */
    public boolean isDirectChatInvitation() {
        if (isInvited()) {
            // Is it an initial sync for this room ?
            RoomState state = getState();

            RoomMember selfMember = state.getMember(mMyUserId);

            if ((null != selfMember) && (null != selfMember.isDirect)) {
                return selfMember.isDirect;
            }
        }

        return false;
    }

    //================================================================================
    // Join
    //================================================================================

    /**
     * Defines the initial sync callback
     *
     * @param callback the new callback.
     */
    public void setOnInitialSyncCallback(ApiCallback<Void> callback) {
        mOnInitialSyncCallback = callback;
    }

    /**
     * Join a room with an url to post before joined the room.
     *
     * @param alias               the room alias
     * @param thirdPartySignedUrl the thirdPartySigned url
     * @param callback            the callback
     */
    public void joinWithThirdPartySigned(final String alias, final String thirdPartySignedUrl, final ApiCallback<Void> callback) {
        if (null == thirdPartySignedUrl) {
            join(alias, callback);
        } else {
            String url = thirdPartySignedUrl + "&mxid=" + mMyUserId;
            UrlPostTask task = new UrlPostTask();

            task.setListener(new UrlPostTask.IPostTaskListener() {
                @Override
                public void onSucceed(JsonObject object) {
                    Map<String, Object> map = null;

                    try {
                        map = new Gson().fromJson(object, new TypeToken<Map<String, Object>>() {
                        }.getType());
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "joinWithThirdPartySigned :  Gson().fromJson failed" + e.getMessage(), e);
                    }

                    if (null != map) {
                        Map<String, Object> joinMap = new HashMap<>();
                        joinMap.put("third_party_signed", map);
                        join(alias, joinMap, callback);
                    } else {
                        join(callback);
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    Log.d(LOG_TAG, "joinWithThirdPartySigned failed " + errorMessage);

                    // cannot validate the url
                    // try without validating the url
                    join(callback);
                }
            });

            // avoid crash if there are too many running task
            try {
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, url);
            } catch (final Exception e) {
                task.cancel(true);
                Log.e(LOG_TAG, "joinWithThirdPartySigned : task.executeOnExecutor failed" + e.getMessage(), e);

                (new android.os.Handler(Looper.getMainLooper())).post(new Runnable() {
                    @Override
                    public void run() {
                        if (null != callback) {
                            callback.onUnexpectedError(e);
                        }
                    }
                });
            }
        }
    }

    /**
     * Join the room. If successful, the room's current state will be loaded before calling back onComplete.
     *
     * @param callback the callback for when done
     */
    public void join(final ApiCallback<Void> callback) {
        join(null, null, callback);
    }

    /**
     * Join the room. If successful, the room's current state will be loaded before calling back onComplete.
     *
     * @param roomAlias the room alias
     * @param callback  the callback for when done
     */
    private void join(String roomAlias, ApiCallback<Void> callback) {
        join(roomAlias, null, callback);
    }

    /**
     * Join the room. If successful, the room's current state will be loaded before calling back onComplete.
     *
     * @param roomAlias   the room alias
     * @param extraParams the join extra params
     * @param callback    the callback for when done
     */
    private void join(final String roomAlias, final Map<String, Object> extraParams, final ApiCallback<Void> callback) {
        Log.d(LOG_TAG, "Join the room " + getRoomId() + " with alias " + roomAlias);

        mDataHandler.getDataRetriever().getRoomsRestClient()
                .joinRoom((null != roomAlias) ? roomAlias : getRoomId(), extraParams, new SimpleApiCallback<RoomResponse>(callback) {
                    @Override
                    public void onSuccess(final RoomResponse aResponse) {
                        try {
                            // the join request did not get the room initial history
                            if (!isJoined()) {
                                Log.d(LOG_TAG, "the room " + getRoomId() + " is joined but wait after initial sync");

                                // wait the server sends the events chunk before calling the callback
                                setOnInitialSyncCallback(callback);
                            } else {
                                Log.d(LOG_TAG, "the room " + getRoomId() + " is joined : the initial sync has been done");
                                // to initialise the notification counters
                                markAllAsRead(null);
                                // already got the initial sync
                                callback.onSuccess(null);
                            }
                        } catch (Exception e) {
                            Log.e(LOG_TAG, "join exception " + e.getMessage(), e);
                        }
                    }

                    @Override
                    public void onNetworkError(Exception e) {
                        Log.e(LOG_TAG, "join onNetworkError " + e.getMessage(), e);
                        callback.onNetworkError(e);
                    }

                    @Override
                    public void onMatrixError(MatrixError e) {
                        Log.e(LOG_TAG, "join onMatrixError " + e.getMessage());

                        if (MatrixError.UNKNOWN.equals(e.errcode) && TextUtils.equals("No known servers", e.error)) {
                            // minging kludge until https://matrix.org/jira/browse/SYN-678 is fixed
                            // 'Error when trying to join an empty room should be more explicit
                            e.error = getStore().getContext().getString(R.string.room_error_join_failed_empty_room);
                        }

                        // if the alias is not found
                        // try with the room id
                        if ((e.mStatus == 404) && !TextUtils.isEmpty(roomAlias)) {
                            Log.e(LOG_TAG, "Retry without the room alias");
                            join(null, extraParams, callback);
                            return;
                        }

                        callback.onMatrixError(e);
                    }

                    @Override
                    public void onUnexpectedError(Exception e) {
                        Log.e(LOG_TAG, "join onUnexpectedError " + e.getMessage(), e);
                        callback.onUnexpectedError(e);
                    }
                });
    }

    //================================================================================
    // Room info (liveState) update
    //================================================================================

    /**
     * This class dispatches the error to the dedicated callbacks.
     * If the operation succeeds, the room state is saved because calling the callback.
     */
    private class RoomInfoUpdateCallback<T> extends SimpleApiCallback<T> {
        private final ApiCallback<T> mCallback;

        /**
         * Constructor
         */
        public RoomInfoUpdateCallback(ApiCallback<T> callback) {
            super(callback);
            mCallback = callback;
        }

        @Override
        public void onSuccess(T info) {
            getStore().storeLiveStateForRoom(getRoomId());

            if (null != mCallback) {
                mCallback.onSuccess(info);
            }
        }
    }

    /**
     * Update the power level of the user userId
     *
     * @param userId     the user id
     * @param powerLevel the new power level
     * @param callback   the callback with the created event
     */
    public void updateUserPowerLevels(String userId, int powerLevel, ApiCallback<Void> callback) {
        PowerLevels powerLevels = getState().getPowerLevels().deepCopy();
        powerLevels.setUserPowerLevel(userId, powerLevel);
        mDataHandler.getDataRetriever().getRoomsRestClient().updatePowerLevels(getRoomId(), powerLevels, callback);
    }

    /**
     * Update the room's name.
     *
     * @param aRoomName the new name
     * @param callback  the async callback
     */
    public void updateName(final String aRoomName, final ApiCallback<Void> callback) {
        mDataHandler.getDataRetriever().getRoomsRestClient().updateRoomName(getRoomId(), aRoomName, new RoomInfoUpdateCallback<Void>(callback) {
            @Override
            public void onSuccess(Void info) {
                getState().name = aRoomName;
                super.onSuccess(info);
            }
        });
    }

    /**
     * Update the room's topic.
     *
     * @param aTopic   the new topic
     * @param callback the async callback
     */
    public void updateTopic(final String aTopic, final ApiCallback<Void> callback) {
        mDataHandler.getDataRetriever().getRoomsRestClient().updateTopic(getRoomId(), aTopic, new RoomInfoUpdateCallback<Void>(callback) {
            @Override
            public void onSuccess(Void info) {
                getState().topic = aTopic;
                super.onSuccess(info);
            }
        });
    }

    /**
     * Update the room's main alias.
     *
     * @param aCanonicalAlias the canonical alias
     * @param callback        the async callback
     */
    public void updateCanonicalAlias(final String aCanonicalAlias, final ApiCallback<Void> callback) {
        final String fCanonicalAlias = TextUtils.isEmpty(aCanonicalAlias) ? null : aCanonicalAlias;

        mDataHandler.getDataRetriever().getRoomsRestClient().updateCanonicalAlias(getRoomId(), fCanonicalAlias, new RoomInfoUpdateCallback<Void>(callback) {
            @Override
            public void onSuccess(Void info) {
                getState().setCanonicalAlias(aCanonicalAlias);
                super.onSuccess(info);
            }
        });
    }

    /**
     * Provides the room aliases list.
     * The result is never null.
     *
     * @return the room aliases list.
     */
    public List<String> getAliases() {
        return getState().getAliases();
    }

    /**
     * Remove a room alias.
     *
     * @param alias    the alias to remove
     * @param callback the async callback
     */
    public void removeAlias(final String alias, final ApiCallback<Void> callback) {
        final List<String> updatedAliasesList = new ArrayList<>(getAliases());

        // nothing to do
        if (TextUtils.isEmpty(alias) || (updatedAliasesList.indexOf(alias) < 0)) {
            if (null != callback) {
                callback.onSuccess(null);
            }
            return;
        }

        mDataHandler.getDataRetriever().getRoomsRestClient().removeRoomAlias(alias, new RoomInfoUpdateCallback<Void>(callback) {
            @Override
            public void onSuccess(Void info) {
                getState().removeAlias(alias);
                super.onSuccess(info);
            }
        });
    }

    /**
     * Try to add an alias to the aliases list.
     *
     * @param alias    the alias to add.
     * @param callback the the async callback
     */
    public void addAlias(final String alias, final ApiCallback<Void> callback) {
        final List<String> updatedAliasesList = new ArrayList<>(getAliases());

        // nothing to do
        if (TextUtils.isEmpty(alias) || (updatedAliasesList.indexOf(alias) >= 0)) {
            if (null != callback) {
                callback.onSuccess(null);
            }
            return;
        }

        mDataHandler.getDataRetriever().getRoomsRestClient().setRoomIdByAlias(getRoomId(), alias, new RoomInfoUpdateCallback<Void>(callback) {
            @Override
            public void onSuccess(Void info) {
                getState().addAlias(alias);
                super.onSuccess(info);
            }
        });
    }

    /**
     * Add a group to the related ones
     *
     * @param groupId  the group id to add
     * @param callback the asynchronous callback
     */
    public void addRelatedGroup(final String groupId, final ApiCallback<Void> callback) {
        List<String> nextGroupIdsList = new ArrayList<>(getState().getRelatedGroups());

        if (!nextGroupIdsList.contains(groupId)) {
            nextGroupIdsList.add(groupId);
        }

        updateRelatedGroups(nextGroupIdsList, callback);
    }

    /**
     * Remove a group id from the related ones.
     *
     * @param groupId  the group id
     * @param callback the asynchronous callback
     */
    public void removeRelatedGroup(final String groupId, final ApiCallback<Void> callback) {
        List<String> nextGroupIdsList = new ArrayList<>(getState().getRelatedGroups());
        nextGroupIdsList.remove(groupId);

        updateRelatedGroups(nextGroupIdsList, callback);
    }

    /**
     * Update the related group ids list
     *
     * @param groupIds the new related groups
     * @param callback the asynchronous callback
     */
    public void updateRelatedGroups(final List<String> groupIds, final ApiCallback<Void> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("groups", groupIds);

        mDataHandler.getDataRetriever().getRoomsRestClient()
                .sendStateEvent(getRoomId(), Event.EVENT_TYPE_STATE_RELATED_GROUPS, null, params, new SimpleApiCallback<Void>(callback) {
                    @Override
                    public void onSuccess(Void info) {
                        getState().groups = groupIds;
                        getDataHandler().getStore().storeLiveStateForRoom(getRoomId());

                        if (null != callback) {
                            callback.onSuccess(null);
                        }
                    }
                });
    }


    /**
     * @return the room avatar URL. If there is no defined one, use the members one (1:1 chat only).
     */
    @Nullable
    public String getAvatarUrl() {
        String res = getState().getAvatarUrl();

        // detect if it is a room with no more than 2 members (i.e. an alone or a 1:1 chat)
        if (null == res) {
            if (getNumberOfMembers() == 1 && !getState().getLoadedMembers().isEmpty()) {
                res = getState().getLoadedMembers().get(0).getAvatarUrl();
            } else if (getNumberOfMembers() == 2 && getState().getLoadedMembers().size() > 1) {
                RoomMember m1 = getState().getLoadedMembers().get(0);
                RoomMember m2 = getState().getLoadedMembers().get(1);

                res = TextUtils.equals(m1.getUserId(), mMyUserId) ? m2.getAvatarUrl() : m1.getAvatarUrl();
            }
        }

        return res;
    }

    /**
     * The call avatar is the same as the room avatar except there are only 2 JOINED members.
     * In this case, it returns the avtar of the other joined member.
     *
     * @return the call avatar URL.
     */
    @Nullable
    public String getCallAvatarUrl() {
        String avatarURL;

        if (getNumberOfMembers() == 2 && getState().getLoadedMembers().size() > 1) {
            RoomMember m1 = getState().getLoadedMembers().get(0);
            RoomMember m2 = getState().getLoadedMembers().get(1);

            // use other member avatar.
            if (TextUtils.equals(mMyUserId, m1.getUserId())) {
                avatarURL = m2.getAvatarUrl();
            } else {
                avatarURL = m1.getAvatarUrl();
            }
        } else {
            //
            avatarURL = getAvatarUrl();
        }

        return avatarURL;
    }

    /**
     * Update the room avatar URL.
     *
     * @param avatarUrl the new avatar URL
     * @param callback  the async callback
     */
    public void updateAvatarUrl(final String avatarUrl, final ApiCallback<Void> callback) {
        mDataHandler.getDataRetriever().getRoomsRestClient().updateAvatarUrl(getRoomId(), avatarUrl, new RoomInfoUpdateCallback<Void>(callback) {
            @Override
            public void onSuccess(Void info) {
                getState().url = avatarUrl;
                super.onSuccess(info);
            }
        });
    }

    /**
     * Update the room's history visibility
     *
     * @param historyVisibility the visibility (should be one of RoomState.HISTORY_VISIBILITY_XX values)
     * @param callback          the async callback
     */
    public void updateHistoryVisibility(final String historyVisibility, final ApiCallback<Void> callback) {
        mDataHandler.getDataRetriever().getRoomsRestClient()
                .updateHistoryVisibility(getRoomId(), historyVisibility, new RoomInfoUpdateCallback<Void>(callback) {
                    @Override
                    public void onSuccess(Void info) {
                        getState().history_visibility = historyVisibility;
                        super.onSuccess(info);
                    }
                });
    }

    /**
     * Update the directory's visibility
     *
     * @param visibility the visibility (should be one of RoomState.HISTORY_VISIBILITY_XX values)
     * @param callback   the async callback
     */
    public void updateDirectoryVisibility(final String visibility, final ApiCallback<Void> callback) {
        mDataHandler.getDataRetriever().getRoomsRestClient().updateDirectoryVisibility(getRoomId(), visibility, new RoomInfoUpdateCallback<Void>(callback) {
            @Override
            public void onSuccess(Void info) {
                getState().visibility = visibility;
                super.onSuccess(info);
            }
        });
    }

    /**
     * Get the directory visibility of the room (see {@link #updateDirectoryVisibility(String, ApiCallback)}).
     * The directory visibility indicates if the room is listed among the directory list.
     *
     * @param roomId   the user Id.
     * @param callback the callback returning the visibility response value.
     */
    public void getDirectoryVisibility(final String roomId, final ApiCallback<String> callback) {
        RoomsRestClient roomRestApi = mDataHandler.getDataRetriever().getRoomsRestClient();

        if (null != roomRestApi) {
            roomRestApi.getDirectoryVisibility(roomId, new SimpleApiCallback<RoomDirectoryVisibility>(callback) {
                @Override
                public void onSuccess(RoomDirectoryVisibility roomDirectoryVisibility) {
                    RoomState currentRoomState = getState();
                    if (null != currentRoomState) {
                        currentRoomState.visibility = roomDirectoryVisibility.visibility;
                    }

                    if (null != callback) {
                        callback.onSuccess(roomDirectoryVisibility.visibility);
                    }
                }
            });
        }
    }

    /**
     * Update the join rule of the room.
     *
     * @param aRule         the join rule: {@link RoomState#JOIN_RULE_PUBLIC} or {@link RoomState#JOIN_RULE_INVITE}
     * @param aCallBackResp the async callback
     */
    public void updateJoinRules(final String aRule, final ApiCallback<Void> aCallBackResp) {
        mDataHandler.getDataRetriever().getRoomsRestClient().updateJoinRules(getRoomId(), aRule, new RoomInfoUpdateCallback<Void>(aCallBackResp) {
            @Override
            public void onSuccess(Void info) {
                getState().join_rule = aRule;
                super.onSuccess(info);
            }
        });
    }

    /**
     * Update the guest access rule of the room.
     * To deny guest access to the room, aGuestAccessRule must be set to {@link RoomState#GUEST_ACCESS_FORBIDDEN}.
     *
     * @param aGuestAccessRule the guest access rule: {@link RoomState#GUEST_ACCESS_CAN_JOIN} or {@link RoomState#GUEST_ACCESS_FORBIDDEN}
     * @param callback         the async callback
     */
    public void updateGuestAccess(final String aGuestAccessRule, final ApiCallback<Void> callback) {
        mDataHandler.getDataRetriever().getRoomsRestClient().updateGuestAccess(getRoomId(), aGuestAccessRule, new RoomInfoUpdateCallback<Void>(callback) {
            @Override
            public void onSuccess(Void info) {
                getState().guest_access = aGuestAccessRule;
                super.onSuccess(info);
            }
        });
    }

    //================================================================================
    // Read receipts events
    //================================================================================

    /**
     * @return the call conference user id
     */
    private String getCallConferenceUserId() {
        if (null == mCallConferenceUserId) {
            mCallConferenceUserId = MXCallsManager.getConferenceUserId(getRoomId());
        }

        return mCallConferenceUserId;
    }

    /**
     * Handle a receiptData.
     *
     * @param receiptData the receiptData.
     * @return true if there a store update.
     */
    public boolean handleReceiptData(ReceiptData receiptData) {
        if (!TextUtils.equals(receiptData.userId, getCallConferenceUserId()) && (null != getStore())) {
            boolean isUpdated = getStore().storeReceipt(receiptData, getRoomId());

            // check oneself receipts
            // if there is an update, it means that the messages have been read from another client
            // it requires to update the summary to display valid information.
            if (isUpdated && TextUtils.equals(mMyUserId, receiptData.userId)) {
                RoomSummary summary = getStore().getSummary(getRoomId());

                if (null != summary) {
                    summary.setReadReceiptEventId(receiptData.eventId);
                    getStore().flushSummary(summary);
                }

                refreshUnreadCounter();
            }

            return isUpdated;
        } else {
            return false;
        }
    }

    /**
     * Handle receipt event.
     * Event content will contains the receipts dictionaries
     * <pre>
     * key   : $EventId
     * value : dict key @UserId
     *              value dict key "ts"
     *                    dict value ts value
     * </pre>
     * <p>
     * Example:
     * <pre>
     * {
     *     "$1535657109773196ZjoWE:matrix.org": {
     *         "m.read": {
     *             "@slash_benoit:matrix.org": {
     *                 "ts": 1535708570621
     *             },
     *             "@benoit.marty:matrix.org": {
     *                 "ts": 1535657109472
     *             }
     *         }
     *     }
     * },
     * </pre>
     *
     * @param event the event receipts.
     * @return the sender user IDs list.
     */
    private List<String> handleReceiptEvent(Event event) {
        List<String> senderIDs = new ArrayList<>();

        try {
            Type type = new TypeToken<Map<String, Map<String, Map<String, Map<String, Object>>>>>() {
            }.getType();
            Map<String, Map<String, Map<String, Map<String, Object>>>> receiptsDict = JsonUtils.getGson(false).fromJson(event.getContent(), type);

            for (String eventId : receiptsDict.keySet()) {
                Map<String, Map<String, Map<String, Object>>> receiptDict = receiptsDict.get(eventId);

                for (String receiptType : receiptDict.keySet()) {
                    // only the read receipts are managed
                    if (TextUtils.equals(receiptType, "m.read")) {
                        Map<String, Map<String, Object>> userIdsDict = receiptDict.get(receiptType);

                        for (String userID : userIdsDict.keySet()) {
                            Map<String, Object> paramsDict = userIdsDict.get(userID);

                            for (String paramName : paramsDict.keySet()) {
                                if (TextUtils.equals("ts", paramName)) {
                                    Double value = (Double) paramsDict.get(paramName);
                                    long ts = value.longValue();

                                    if (handleReceiptData(new ReceiptData(userID, eventId, ts))) {
                                        senderIDs.add(userID);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "handleReceiptEvent : failed" + e.getMessage(), e);
        }

        return senderIDs;
    }

    /**
     * Clear the unread message counters
     *
     * @param summary the room summary
     */
    private void clearUnreadCounters(RoomSummary summary) {
        Log.d(LOG_TAG, "## clearUnreadCounters " + getRoomId());

        // reset the notification count
        getState().setHighlightCount(0);
        getState().setNotificationCount(0);

        if (null != getStore()) {
            getStore().storeLiveStateForRoom(getRoomId());

            // flush the summary
            if (null != summary) {
                summary.setUnreadEventsCount(0);
                summary.setHighlightCount(0);
                summary.setNotificationCount(0);
                getStore().flushSummary(summary);
            }

            getStore().commit();
        }
    }

    /**
     * @return the read marker event id
     */
    public String getReadMarkerEventId() {
        if (null == getStore()) {
            return null;
        }

        RoomSummary summary = getStore().getSummary(getRoomId());

        if (null != summary) {
            return (null != summary.getReadMarkerEventId()) ? summary.getReadMarkerEventId() : summary.getReadReceiptEventId();
        } else {
            return null;
        }
    }

    /**
     * Mark all the messages as read.
     * It also move the read marker to the latest known messages
     *
     * @param aRespCallback the asynchronous callback
     * @return true if the request is sent, false otherwise
     */
    public boolean markAllAsRead(final ApiCallback<Void> aRespCallback) {
        return markAllAsRead(true, aRespCallback);
    }

    /**
     * Mark all the messages as read.
     * It also move the read marker to the latest known messages if updateReadMarker is set to true
     *
     * @param updateReadMarker true to move the read marker to the latest known event
     * @param aRespCallback    the asynchronous callback
     * @return true if the request is sent, false otherwise
     */
    private boolean markAllAsRead(boolean updateReadMarker, final ApiCallback<Void> aRespCallback) {
        final Event lastEvent = (null != getStore()) ? getStore().getLatestEvent(getRoomId()) : null;
        boolean res = sendReadMarkers(updateReadMarker ? ((null != lastEvent) ? lastEvent.eventId : null) : getReadMarkerEventId(), null, aRespCallback);

        if (!res) {
            RoomSummary summary = (null != getStore()) ? getStore().getSummary(getRoomId()) : null;

            if (null != summary) {
                if ((0 != summary.getUnreadEventsCount())
                        || (0 != summary.getHighlightCount())
                        || (0 != summary.getNotificationCount())) {
                    Log.e(LOG_TAG, "## markAllAsRead() : the summary events counters should be cleared for " + getRoomId());

                    Event latestEvent = getStore().getLatestEvent(getRoomId());
                    summary.setLatestReceivedEvent(latestEvent);

                    if (null != latestEvent) {
                        summary.setReadReceiptEventId(latestEvent.eventId);
                    } else {
                        summary.setReadReceiptEventId(null);
                    }

                    summary.setUnreadEventsCount(0);
                    summary.setHighlightCount(0);
                    summary.setNotificationCount(0);
                    getStore().flushSummary(summary);
                }
            } else {
                Log.e(LOG_TAG, "## sendReadReceipt() : no summary for " + getRoomId());
            }

            if ((0 != getState().getNotificationCount()) || (0 != getState().getHighlightCount())) {
                Log.e(LOG_TAG, "## markAllAsRead() : the notification messages count for " + getRoomId() + " should have been cleared");

                getState().setNotificationCount(0);
                getState().setHighlightCount(0);

                if (null != getStore()) {
                    getStore().storeLiveStateForRoom(getRoomId());
                }
            }
        }

        return res;
    }

    /**
     * Update the read marker event Id
     *
     * @param readMarkerEventId the read marker even id
     */
    public void setReadMakerEventId(final String readMarkerEventId) {
        RoomSummary summary = (null != getStore()) ? getStore().getSummary(getRoomId()) : null;
        if (summary != null && !readMarkerEventId.equals(summary.getReadMarkerEventId())) {
            sendReadMarkers(readMarkerEventId, summary.getReadReceiptEventId(), null);
        }
    }

    /**
     * Send a read receipt to the latest known event
     */
    public void sendReadReceipt() {
        markAllAsRead(false, null);
    }

    /**
     * Send the read receipt to the latest room message id.
     *
     * @param event         send a read receipt to a provided event
     * @param aRespCallback asynchronous response callback
     * @return true if the read receipt has been sent, false otherwise
     */
    public boolean sendReadReceipt(Event event, final ApiCallback<Void> aRespCallback) {
        String eventId = (null != event) ? event.eventId : null;
        Log.d(LOG_TAG, "## sendReadReceipt() : eventId " + eventId + " in room " + getRoomId());
        return sendReadMarkers(null, eventId, aRespCallback);
    }

    /**
     * Forget the current read marker
     * This will update the read marker to match the read receipt
     *
     * @param callback the asynchronous callback
     */
    public void forgetReadMarker(final ApiCallback<Void> callback) {
        final RoomSummary summary = (null != getStore()) ? getStore().getSummary(getRoomId()) : null;
        final String currentReadReceipt = (null != summary) ? summary.getReadReceiptEventId() : null;

        if (null != summary) {
            Log.d(LOG_TAG, "## forgetReadMarker() : update the read marker to " + currentReadReceipt + " in room " + getRoomId());
            summary.setReadMarkerEventId(currentReadReceipt);
            getStore().flushSummary(summary);
        }

        setReadMarkers(currentReadReceipt, currentReadReceipt, callback);
    }

    /**
     * Send the read markers
     *
     * @param aReadMarkerEventId  the new read marker event id (if null use the latest known event id)
     * @param aReadReceiptEventId the new read receipt event id (if null use the latest known event id)
     * @param aRespCallback       asynchronous response callback
     * @return true if the request is sent, false otherwise
     */
    public boolean sendReadMarkers(final String aReadMarkerEventId, final String aReadReceiptEventId, final ApiCallback<Void> aRespCallback) {
        final Event lastEvent = (null != getStore()) ? getStore().getLatestEvent(getRoomId()) : null;

        // reported by GA
        if (null == lastEvent) {
            Log.e(LOG_TAG, "## sendReadMarkers(): no last event");
            return false;
        }

        Log.d(LOG_TAG, "## sendReadMarkers(): readMarkerEventId " + aReadMarkerEventId + " readReceiptEventId " + aReadReceiptEventId
                + " in room " + getRoomId());

        boolean hasUpdate = false;

        String readMarkerEventId = aReadMarkerEventId;
        if (!TextUtils.isEmpty(aReadMarkerEventId)) {
            if (!MXPatterns.isEventId(aReadMarkerEventId)) {
                Log.e(LOG_TAG, "## sendReadMarkers() : invalid event id " + readMarkerEventId);
                // Read marker is invalid, ignore it
                readMarkerEventId = null;
            } else {
                // Check if the read marker is updated
                RoomSummary summary = getStore().getSummary(getRoomId());
                if ((null != summary) && !TextUtils.equals(readMarkerEventId, summary.getReadMarkerEventId())) {
                    // Make sure the new read marker event is newer than the current one
                    final Event newReadMarkerEvent = getStore().getEvent(readMarkerEventId, getRoomId());
                    final Event currentReadMarkerEvent = getStore().getEvent(summary.getReadMarkerEventId(), getRoomId());
                    if (newReadMarkerEvent == null || currentReadMarkerEvent == null
                            || newReadMarkerEvent.getOriginServerTs() > currentReadMarkerEvent.getOriginServerTs()) {
                        // Event is not in store (assume it is in the past), or is older than current one
                        Log.d(LOG_TAG, "## sendReadMarkers(): set new read marker event id " + readMarkerEventId + " in room " + getRoomId());
                        summary.setReadMarkerEventId(readMarkerEventId);
                        getStore().flushSummary(summary);
                        hasUpdate = true;
                    }
                }
            }
        }

        final String readReceiptEventId = (null == aReadReceiptEventId) ? lastEvent.eventId : aReadReceiptEventId;
        // check if the read receipt event id is already read
        if ((null != getStore()) && !getStore().isEventRead(getRoomId(), getDataHandler().getUserId(), readReceiptEventId)) {
            // check if the event id update is allowed
            if (handleReceiptData(new ReceiptData(mMyUserId, readReceiptEventId, System.currentTimeMillis()))) {
                // Clear the unread counters if the latest message is displayed
                // We don't try to compute the unread counters for oldest messages :
                // ---> it would require too much time.
                // The counters are cleared to avoid displaying invalid values
                // when the device is offline.
                // The read receipts will be sent later
                // (asap there is a valid network connection)
                if (TextUtils.equals(lastEvent.eventId, readReceiptEventId)) {
                    clearUnreadCounters(getStore().getSummary(getRoomId()));
                }
                hasUpdate = true;
            }
        }

        if (hasUpdate) {
            setReadMarkers(readMarkerEventId, readReceiptEventId, aRespCallback);
        }

        return hasUpdate;
    }

    /**
     * Send the request to update the read marker and read receipt.
     *
     * @param aReadMarkerEventId  the read marker event id
     * @param aReadReceiptEventId the read receipt event id
     * @param callback            the asynchronous callback
     */
    private void setReadMarkers(final String aReadMarkerEventId, final String aReadReceiptEventId, final ApiCallback<Void> callback) {
        Log.d(LOG_TAG, "## setReadMarkers(): readMarkerEventId " + aReadMarkerEventId + " readReceiptEventId " + aReadMarkerEventId);

        // check if the message ids are valid
        final String readMarkerEventId = MXPatterns.isEventId(aReadMarkerEventId) ? aReadMarkerEventId : null;
        final String readReceiptEventId = MXPatterns.isEventId(aReadReceiptEventId) ? aReadReceiptEventId : null;

        // if there is nothing to do
        if (TextUtils.isEmpty(readMarkerEventId) && TextUtils.isEmpty(readReceiptEventId)) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    if (null != callback) {
                        callback.onSuccess(null);
                    }
                }
            });
        } else {
            mDataHandler.getDataRetriever().getRoomsRestClient().sendReadMarker(getRoomId(), readMarkerEventId, readReceiptEventId,
                    new SimpleApiCallback<Void>(callback) {
                        @Override
                        public void onSuccess(Void info) {
                            if (null != callback) {
                                callback.onSuccess(info);
                            }
                        }
                    });
        }
    }

    /**
     * Check if an event has been read.
     *
     * @param eventId the event id
     * @return true if the message has been read
     */
    public boolean isEventRead(String eventId) {
        if (null != getStore()) {
            return getStore().isEventRead(getRoomId(), mMyUserId, eventId);
        } else {
            return false;
        }
    }

    //================================================================================
    // Unread event count management
    //================================================================================

    /**
     * @return the number of unread messages that match the push notification rules.
     */
    public int getNotificationCount() {
        return getState().getNotificationCount();
    }

    /**
     * @return the number of highlighted events.
     */
    public int getHighlightCount() {
        return getState().getHighlightCount();
    }

    /**
     * refresh the unread events counts.
     */
    public void refreshUnreadCounter() {
        // avoid refreshing the unread counter while processing a bunch of messages.
        if (!mIsSyncing) {
            RoomSummary summary = (null != getStore()) ? getStore().getSummary(getRoomId()) : null;

            if (null != summary) {
                int prevValue = summary.getUnreadEventsCount();
                int newValue = getStore().eventsCountAfter(getRoomId(), summary.getReadReceiptEventId());

                if (prevValue != newValue) {
                    summary.setUnreadEventsCount(newValue);
                    getStore().flushSummary(summary);
                }
            }
        } else {
            // wait the sync end before computing is again
            mRefreshUnreadAfterSync = true;
        }
    }

    //================================================================================
    // typing events
    //================================================================================

    // userIds list
    @NonNull
    private final List<String> mTypingUsers = new ArrayList<>();

    /**
     * Get typing users
     *
     * @return the userIds list
     */
    @NonNull
    public List<String> getTypingUsers() {
        List<String> typingUsers;

        synchronized (mTypingUsers) {
            typingUsers = new ArrayList<>(mTypingUsers);
        }

        return typingUsers;
    }

    /**
     * Send a typing notification
     *
     * @param isTyping typing status
     * @param timeout  the typing timeout
     * @param callback asynchronous callback
     */
    public void sendTypingNotification(boolean isTyping, int timeout, ApiCallback<Void> callback) {
        // send the event only if the user has joined the room.
        if (isJoined()) {
            mDataHandler.getDataRetriever().getRoomsRestClient().sendTypingNotification(getRoomId(), mMyUserId, isTyping, timeout, callback);
        }
    }

    //================================================================================
    // Medias events
    //================================================================================

    /**
     * Fill the locationInfo
     *
     * @param context         the context
     * @param locationMessage the location message
     * @param thumbnailUri    the thumbnail uri
     * @param thumbMimeType   the thumbnail mime type
     */
    public static void fillLocationInfo(Context context, LocationMessage locationMessage, Uri thumbnailUri, String thumbMimeType) {
        if (null != thumbnailUri) {
            try {
                locationMessage.thumbnail_url = thumbnailUri.toString();

                ThumbnailInfo thumbInfo = new ThumbnailInfo();
                File thumbnailFile = new File(thumbnailUri.getPath());

                ExifInterface exifMedia = new ExifInterface(thumbnailUri.getPath());
                String sWidth = exifMedia.getAttribute(ExifInterface.TAG_IMAGE_WIDTH);
                String sHeight = exifMedia.getAttribute(ExifInterface.TAG_IMAGE_LENGTH);

                if (null != sWidth) {
                    thumbInfo.w = Integer.parseInt(sWidth);
                }

                if (null != sHeight) {
                    thumbInfo.h = Integer.parseInt(sHeight);
                }

                thumbInfo.size = Long.valueOf(thumbnailFile.length());
                thumbInfo.mimetype = thumbMimeType;
                locationMessage.thumbnail_info = thumbInfo;
            } catch (Exception e) {
                Log.e(LOG_TAG, "fillLocationInfo : failed" + e.getMessage(), e);
            }
        }
    }

    /**
     * Fills the VideoMessage info.
     *
     * @param context       Application context for the content resolver.
     * @param videoMessage  The VideoMessage to fill.
     * @param fileUri       The file uri.
     * @param videoMimeType The mimeType
     * @param thumbnailUri  the thumbnail uri
     * @param thumbMimeType the thumbnail mime type
     */
    public static void fillVideoInfo(Context context, VideoMessage videoMessage, Uri fileUri, String videoMimeType, Uri thumbnailUri, String thumbMimeType) {
        try {
            VideoInfo videoInfo = new VideoInfo();
            File file = new File(fileUri.getPath());

            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(file.getAbsolutePath());

            Bitmap bmp = retriever.getFrameAtTime();
            videoInfo.h = bmp.getHeight();
            videoInfo.w = bmp.getWidth();
            videoInfo.mimetype = videoMimeType;

            try {
                MediaPlayer mp = MediaPlayer.create(context, fileUri);
                if (null != mp) {
                    videoInfo.duration = Long.valueOf(mp.getDuration());
                    mp.release();
                }
            } catch (Exception e) {
                Log.e(LOG_TAG, "fillVideoInfo : MediaPlayer.create failed" + e.getMessage(), e);
            }
            videoInfo.size = file.length();

            // thumbnail
            if (null != thumbnailUri) {
                videoInfo.thumbnail_url = thumbnailUri.toString();

                ThumbnailInfo thumbInfo = new ThumbnailInfo();
                File thumbnailFile = new File(thumbnailUri.getPath());

                ExifInterface exifMedia = new ExifInterface(thumbnailUri.getPath());
                String sWidth = exifMedia.getAttribute(ExifInterface.TAG_IMAGE_WIDTH);
                String sHeight = exifMedia.getAttribute(ExifInterface.TAG_IMAGE_LENGTH);

                if (null != sWidth) {
                    thumbInfo.w = Integer.parseInt(sWidth);
                }

                if (null != sHeight) {
                    thumbInfo.h = Integer.parseInt(sHeight);
                }

                thumbInfo.size = Long.valueOf(thumbnailFile.length());
                thumbInfo.mimetype = thumbMimeType;
                videoInfo.thumbnail_info = thumbInfo;
            }

            videoMessage.info = videoInfo;
        } catch (Exception e) {
            Log.e(LOG_TAG, "fillVideoInfo : failed" + e.getMessage(), e);
        }
    }

    /**
     * Fills the fileMessage fileInfo.
     *
     * @param context     Application context for the content resolver.
     * @param fileMessage The fileMessage to fill.
     * @param fileUri     The file uri.
     * @param mimeType    The mimeType
     */
    public static void fillFileInfo(Context context, FileMessage fileMessage, Uri fileUri, String mimeType) {
        try {
            FileInfo fileInfo = new FileInfo();

            String filename = fileUri.getPath();
            File file = new File(filename);

            fileInfo.mimetype = mimeType;
            fileInfo.size = file.length();

            fileMessage.info = fileInfo;

        } catch (Exception e) {
            Log.e(LOG_TAG, "fillFileInfo : failed" + e.getMessage(), e);
        }
    }


    /**
     * Update or create an ImageInfo for an image uri.
     *
     * @param context     Application context for the content resolver.
     * @param anImageInfo the imageInfo to fill, null to create a new one
     * @param imageUri    The full size image uri.
     * @param mimeType    The image mimeType
     * @return the filled image info
     */
    public static ImageInfo getImageInfo(Context context, ImageInfo anImageInfo, Uri imageUri, String mimeType) {
        ImageInfo imageInfo = (null == anImageInfo) ? new ImageInfo() : anImageInfo;

        try {
            String filename = imageUri.getPath();
            File file = new File(filename);

            ExifInterface exifMedia = new ExifInterface(filename);
            String sWidth = exifMedia.getAttribute(ExifInterface.TAG_IMAGE_WIDTH);
            String sHeight = exifMedia.getAttribute(ExifInterface.TAG_IMAGE_LENGTH);

            // the image rotation is replaced by orientation
            // imageInfo.rotation = ImageUtils.getRotationAngleForBitmap(context, imageUri);
            imageInfo.orientation = ImageUtils.getOrientationForBitmap(context, imageUri);

            int width = 0;
            int height = 0;

            // extract the Exif info
            if ((null != sWidth) && (null != sHeight)) {

                if ((imageInfo.orientation == ExifInterface.ORIENTATION_TRANSPOSE)
                        || (imageInfo.orientation == ExifInterface.ORIENTATION_ROTATE_90)
                        || (imageInfo.orientation == ExifInterface.ORIENTATION_TRANSVERSE)
                        || (imageInfo.orientation == ExifInterface.ORIENTATION_ROTATE_270)) {
                    height = Integer.parseInt(sWidth);
                    width = Integer.parseInt(sHeight);
                } else {
                    width = Integer.parseInt(sWidth);
                    height = Integer.parseInt(sHeight);
                }
            }

            // there is no exif info or the size is invalid
            if ((0 == width) || (0 == height)) {
                try {
                    BitmapFactory.Options opts = new BitmapFactory.Options();
                    opts.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(imageUri.getPath(), opts);

                    // don't need to load the bitmap in memory
                    if ((opts.outHeight > 0) && (opts.outWidth > 0)) {
                        width = opts.outWidth;
                        height = opts.outHeight;
                    }

                } catch (Exception e) {
                    Log.e(LOG_TAG, "fillImageInfo : failed" + e.getMessage(), e);
                } catch (OutOfMemoryError oom) {
                    Log.e(LOG_TAG, "fillImageInfo : oom", oom);
                }
            }

            // valid image size ?
            if ((0 != width) || (0 != height)) {
                imageInfo.w = width;
                imageInfo.h = height;
            }

            imageInfo.mimetype = mimeType;
            imageInfo.size = file.length();
        } catch (Exception e) {
            Log.e(LOG_TAG, "fillImageInfo : failed" + e.getMessage(), e);
            imageInfo = null;
        }

        return imageInfo;
    }

    /**
     * Fills the imageMessage imageInfo.
     *
     * @param context      Application context for the content resolver.
     * @param imageMessage The imageMessage to fill.
     * @param imageUri     The full size image uri.
     * @param mimeType     The image mimeType
     */
    public static void fillImageInfo(Context context, ImageMessage imageMessage, Uri imageUri, String mimeType) {
        imageMessage.info = getImageInfo(context, imageMessage.info, imageUri, mimeType);
    }

    /**
     * Fills the imageMessage imageInfo.
     *
     * @param context      Application context for the content resolver.
     * @param imageMessage The imageMessage to fill.
     * @param thumbUri     The thumbnail uri
     * @param mimeType     The image mimeType
     */
    public static void fillThumbnailInfo(Context context, ImageMessage imageMessage, Uri thumbUri, String mimeType) {
        ImageInfo imageInfo = getImageInfo(context, null, thumbUri, mimeType);

        if (null != imageInfo) {
            if (null == imageMessage.info) {
                imageMessage.info = new ImageInfo();
            }

            imageMessage.info.thumbnailInfo = new ThumbnailInfo();
            imageMessage.info.thumbnailInfo.w = imageInfo.w;
            imageMessage.info.thumbnailInfo.h = imageInfo.h;
            imageMessage.info.thumbnailInfo.size = imageInfo.size;
            imageMessage.info.thumbnailInfo.mimetype = imageInfo.mimetype;
        }
    }

    //================================================================================
    // Call
    //================================================================================

    /**
     * Test if a call can be performed in this room.
     *
     * @return true if a call can be performed.
     */
    public boolean canPerformCall() {
        return getNumberOfMembers() > 1;
    }

    /**
     * @return a list of callable members.
     */
    public void callees(final ApiCallback<List<RoomMember>> callback) {
        getMembersAsync(new SimpleApiCallback<List<RoomMember>>(callback) {
            @Override
            public void onSuccess(List<RoomMember> info) {
                List<RoomMember> res = new ArrayList<>();

                for (RoomMember m : info) {
                    if (RoomMember.MEMBERSHIP_JOIN.equals(m.membership) && !mMyUserId.equals(m.getUserId())) {
                        res.add(m);
                    }
                }

                callback.onSuccess(res);
            }
        });
    }

    //================================================================================
    // Account data management
    //================================================================================

    /**
     * Handle private user data events.
     *
     * @param accountDataEvents the account events.
     */
    private void handleAccountDataEvents(List<Event> accountDataEvents) {
        if ((null != accountDataEvents) && (accountDataEvents.size() > 0)) {
            // manage the account events
            for (Event accountDataEvent : accountDataEvents) {
                String eventType = accountDataEvent.getType();

                final RoomSummary summary = (null != getStore()) ? getStore().getSummary(getRoomId()) : null;
                if (eventType.equals(Event.EVENT_TYPE_READ_MARKER)) {
                    if (summary != null) {
                        final Event event = JsonUtils.toEvent(accountDataEvent.getContent());
                        if (null != event && !TextUtils.equals(event.eventId, summary.getReadMarkerEventId())) {
                            Log.d(LOG_TAG, "## handleAccountDataEvents() : update the read marker to " + event.eventId + " in room " + getRoomId());
                            if (TextUtils.isEmpty(event.eventId)) {
                                Log.e(LOG_TAG, "## handleAccountDataEvents() : null event id " + accountDataEvent.getContent());
                            }
                            summary.setReadMarkerEventId(event.eventId);
                            getStore().flushSummary(summary);
                            mDataHandler.onReadMarkerEvent(getRoomId());
                        }
                    }
                } else {
                    mAccountData.handleTagEvent(accountDataEvent);
                    if (Event.EVENT_TYPE_TAGS.equals(accountDataEvent.getType())) {
                        summary.setRoomTags(mAccountData.getKeys());
                        getStore().flushSummary(summary);
                        mDataHandler.onRoomTagEvent(getRoomId());
                    } else if (Event.EVENT_TYPE_URL_PREVIEW.equals(accountDataEvent.getType())) {
                        final JsonObject jsonObject = accountDataEvent.getContentAsJsonObject();
                        if (jsonObject.has(AccountDataRestClient.ACCOUNT_DATA_KEY_URL_PREVIEW_DISABLE)) {
                            final boolean disabled = jsonObject.get(AccountDataRestClient.ACCOUNT_DATA_KEY_URL_PREVIEW_DISABLE).getAsBoolean();
                            Set<String> roomIdsWithoutURLPreview = mDataHandler.getStore().getRoomsWithoutURLPreviews();
                            if (disabled) {
                                roomIdsWithoutURLPreview.add(getRoomId());
                            } else {
                                roomIdsWithoutURLPreview.remove(getRoomId());
                            }

                            mDataHandler.getStore().setRoomsWithoutURLPreview(roomIdsWithoutURLPreview);
                        }
                    }
                }
            }

            if (null != getStore()) {
                getStore().storeAccountData(getRoomId(), mAccountData);
            }
        }
    }

    /**
     * Add a tag to a room.
     * Use this method to update the order of an existing tag.
     *
     * @param tag      the new tag to add to the room.
     * @param order    the order.
     * @param callback the operation callback
     */
    private void addTag(String tag, Double order, final ApiCallback<Void> callback) {
        // sanity check
        if ((null != tag) && (null != order)) {
            mDataHandler.getDataRetriever().getRoomsRestClient().addTag(getRoomId(), tag, order, callback);
        } else {
            if (null != callback) {
                callback.onSuccess(null);
            }
        }
    }

    /**
     * Remove a tag to a room.
     *
     * @param tag      the new tag to add to the room.
     * @param callback the operation callback.
     */
    private void removeTag(String tag, final ApiCallback<Void> callback) {
        // sanity check
        if (null != tag) {
            mDataHandler.getDataRetriever().getRoomsRestClient().removeTag(getRoomId(), tag, callback);
        } else {
            if (null != callback) {
                callback.onSuccess(null);
            }
        }
    }

    /**
     * Remove a tag and add another one.
     *
     * @param oldTag      the tag to remove.
     * @param newTag      the new tag to add. Nil can be used. Then, no new tag will be added.
     * @param newTagOrder the order of the new tag.
     * @param callback    the operation callback.
     */
    public void replaceTag(final String oldTag, final String newTag, final Double newTagOrder, final ApiCallback<Void> callback) {
        // remove tag
        if ((null != oldTag) && (null == newTag)) {
            removeTag(oldTag, callback);
        }
        // define a tag or define a new order
        else if (((null == oldTag) && (null != newTag)) || TextUtils.equals(oldTag, newTag)) {
            addTag(newTag, newTagOrder, callback);
        } else {
            removeTag(oldTag, new SimpleApiCallback<Void>(callback) {
                @Override
                public void onSuccess(Void info) {
                    addTag(newTag, newTagOrder, callback);
                }
            });
        }
    }

    //==============================================================================================================
    // URL preview
    //==============================================================================================================

    /**
     * Tells if the URL preview has been allowed by the user.
     *
     * @return @return true if allowed.
     */
    public boolean isURLPreviewAllowedByUser() {
        return !getDataHandler().getStore().getRoomsWithoutURLPreviews().contains(getRoomId());
    }

    /**
     * Update the user enabled room url preview
     *
     * @param status   the new status
     * @param callback the asynchronous callback
     */
    public void setIsURLPreviewAllowedByUser(boolean status, ApiCallback<Void> callback) {
        mDataHandler.getDataRetriever().getRoomsRestClient().updateURLPreviewStatus(getRoomId(), status, callback);
    }

    //==============================================================================================================
    // Room events dispatcher
    //==============================================================================================================

    /**
     * Add an event listener to this room. Only events relative to the room will come down.
     *
     * @param eventListener the event listener to add
     */
    public void addEventListener(final IMXEventListener eventListener) {
        // sanity check
        if (null == eventListener) {
            Log.e(LOG_TAG, "addEventListener : eventListener is null");
            return;
        }

        // GA crash : should never happen but got it.
        if (null == mDataHandler) {
            Log.e(LOG_TAG, "addEventListener : mDataHandler is null");
            return;
        }

        // Create a global listener that we'll add to the data handler
        IMXEventListener globalListener = new MXRoomEventListener(this, eventListener);

        mEventListeners.put(eventListener, globalListener);

        // GA crash
        if (null != mDataHandler) {
            mDataHandler.addListener(globalListener);
        }
    }

    /**
     * Remove an event listener.
     *
     * @param eventListener the event listener to remove
     */
    public void removeEventListener(IMXEventListener eventListener) {
        // sanity check
        if ((null != eventListener) && (null != mDataHandler)) {
            mDataHandler.removeListener(mEventListeners.get(eventListener));
            mEventListeners.remove(eventListener);
        }
    }

    //==============================================================================================================
    // Send methods
    //==============================================================================================================

    /**
     * Send an event content to the room.
     * The event is updated with the data provided by the server
     * The provided event contains the error description.
     *
     * @param event    the message
     * @param callback the callback with the created event
     */
    public void sendEvent(final Event event, final ApiCallback<Void> callback) {
        // wait that the room is synced before sending messages
        if (!mIsReady || !isJoined()) {
            mDataHandler.updateEventState(event, Event.SentState.WAITING_RETRY);
            try {
                callback.onNetworkError(null);
            } catch (Exception e) {
                Log.e(LOG_TAG, "sendEvent exception " + e.getMessage(), e);
            }
            return;
        }

        final String prevEventId = event.eventId;

        final ApiCallback<CreatedEvent> localCB = new ApiCallback<CreatedEvent>() {
            @Override
            public void onSuccess(final CreatedEvent createdEvent) {
                if (null != getStore()) {
                    // remove the tmp event
                    getStore().deleteEvent(event);
                }

                // replace the tmp event id by the final one
                boolean isReadMarkerUpdated = TextUtils.equals(getReadMarkerEventId(), event.eventId);

                // update the event with the server response
                event.eventId = createdEvent.eventId;
                event.originServerTs = System.currentTimeMillis();
                mDataHandler.updateEventState(event, Event.SentState.SENT);

                // the message echo is not yet echoed
                if (null != getStore() && !getStore().doesEventExist(createdEvent.eventId, getRoomId())) {
                    getStore().storeLiveRoomEvent(event);
                }

                // send the dedicated read receipt asap
                markAllAsRead(isReadMarkerUpdated, null);

                if (null != getStore()) {
                    getStore().commit();
                }
                mDataHandler.onEventSent(event, prevEventId);

                try {
                    callback.onSuccess(null);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "sendEvent exception " + e.getMessage(), e);
                }
            }

            @Override
            public void onNetworkError(Exception e) {
                event.unsentException = e;
                mDataHandler.updateEventState(event, Event.SentState.UNDELIVERED);
                try {
                    callback.onNetworkError(e);
                } catch (Exception anException) {
                    Log.e(LOG_TAG, "sendEvent exception " + anException.getMessage(), anException);
                }
            }

            @Override
            public void onMatrixError(MatrixError e) {
                event.unsentMatrixError = e;
                mDataHandler.updateEventState(event, Event.SentState.UNDELIVERED);

                if (MatrixError.isConfigurationErrorCode(e.errcode)) {
                    mDataHandler.onConfigurationError(e.errcode);
                } else {
                    try {
                        callback.onMatrixError(e);
                    } catch (Exception anException) {
                        Log.e(LOG_TAG, "sendEvent exception " + anException.getMessage(), anException);
                    }
                }
            }

            @Override
            public void onUnexpectedError(Exception e) {
                event.unsentException = e;
                mDataHandler.updateEventState(event, Event.SentState.UNDELIVERED);
                try {
                    callback.onUnexpectedError(e);
                } catch (Exception anException) {
                    Log.e(LOG_TAG, "sendEvent exception " + anException.getMessage(), anException);
                }
            }
        };

        if (isEncrypted() && (null != mDataHandler.getCrypto())) {
            mDataHandler.updateEventState(event, Event.SentState.ENCRYPTING);

            // Store the "m.relates_to" data and remove them from event content before encrypting the event content
            final JsonElement relatesTo;

            JsonObject contentAsJsonObject = event.getContentAsJsonObject();

            if (contentAsJsonObject != null
                    && contentAsJsonObject.has("m.relates_to")) {
                // Get a copy of "m.relates_to" data...
                relatesTo = contentAsJsonObject.get("m.relates_to");

                // ... and remove "m.relates_to" data from the content before encrypting it
                contentAsJsonObject.remove("m.relates_to");
            } else {
                relatesTo = null;
            }

            // Encrypt the content before sending
            mDataHandler.getCrypto()
                    .encryptEventContent(contentAsJsonObject, event.getType(), this, new ApiCallback<MXEncryptEventContentResult>() {
                        @Override
                        public void onSuccess(MXEncryptEventContentResult encryptEventContentResult) {
                            // update the event content with the encrypted data
                            event.type = encryptEventContentResult.mEventType;

                            // Add the "m.relates_to" data to the encrypted event here
                            JsonObject encryptedContent = encryptEventContentResult.mEventContent.getAsJsonObject();
                            if (relatesTo != null) {
                                encryptedContent.add("m.relates_to", relatesTo);
                            }
                            event.updateContent(encryptedContent);
                            mDataHandler.decryptEvent(event, null);

                            // sending in progress
                            mDataHandler.updateEventState(event, Event.SentState.SENDING);
                            mDataHandler.getDataRetriever().getRoomsRestClient().sendEventToRoom(event.eventId, getRoomId(),
                                    encryptEventContentResult.mEventType, encryptEventContentResult.mEventContent.getAsJsonObject(), localCB);
                        }

                        @Override
                        public void onNetworkError(Exception e) {
                            event.unsentException = e;
                            mDataHandler.updateEventState(event, Event.SentState.UNDELIVERED);

                            if (null != callback) {
                                callback.onNetworkError(e);
                            }
                        }

                        @Override
                        public void onMatrixError(MatrixError e) {
                            // update the sent state if the message encryption failed because there are unknown devices.
                            if ((e instanceof MXCryptoError) && TextUtils.equals(((MXCryptoError) e).errcode, MXCryptoError.UNKNOWN_DEVICES_CODE)) {
                                event.mSentState = Event.SentState.FAILED_UNKNOWN_DEVICES;
                            } else {
                                event.mSentState = Event.SentState.UNDELIVERED;
                            }
                            event.unsentMatrixError = e;
                            mDataHandler.onEventSentStateUpdated(event);

                            if (null != callback) {
                                callback.onMatrixError(e);
                            }
                        }

                        @Override
                        public void onUnexpectedError(Exception e) {
                            event.unsentException = e;
                            mDataHandler.updateEventState(event, Event.SentState.UNDELIVERED);

                            if (null != callback) {
                                callback.onUnexpectedError(e);
                            }
                        }
                    });
        } else {
            mDataHandler.updateEventState(event, Event.SentState.SENDING);

            if (Event.EVENT_TYPE_MESSAGE.equals(event.getType())) {
                mDataHandler.getDataRetriever().getRoomsRestClient()
                        .sendMessage(event.eventId, getRoomId(), JsonUtils.toMessage(event.getContent()), localCB);
            } else {
                mDataHandler.getDataRetriever().getRoomsRestClient()
                        .sendEventToRoom(event.eventId, getRoomId(), event.getType(), event.getContentAsJsonObject(), localCB);
            }
        }
    }

    /**
     * Cancel the event sending.
     * Any media upload will be cancelled too.
     * The event becomes undeliverable.
     *
     * @param event the message
     */
    public void cancelEventSending(final Event event) {
        if (null != event) {
            if ((Event.SentState.UNSENT == event.mSentState)
                    || (Event.SentState.SENDING == event.mSentState)
                    || (Event.SentState.WAITING_RETRY == event.mSentState)
                    || (Event.SentState.ENCRYPTING == event.mSentState)) {

                // the message cannot be sent anymore
                mDataHandler.updateEventState(event, Event.SentState.UNDELIVERED);
            }

            List<String> urls = event.getMediaUrls();
            MXMediasCache cache = mDataHandler.getMediasCache();

            for (String url : urls) {
                cache.cancelUpload(url);
                cache.cancelDownload(cache.downloadIdFromUrl(url));
            }
        }
    }

    /**
     * Redact an event from the room.
     *
     * @param eventId  the event's id
     * @param callback the callback with the redacted event
     */
    public void redact(final String eventId, final ApiCallback<Event> callback) {
        mDataHandler.getDataRetriever().getRoomsRestClient().redactEvent(getRoomId(), eventId, new SimpleApiCallback<Event>(callback) {
            @Override
            public void onSuccess(Event event) {
                Event redactedEvent = (null != getStore()) ? getStore().getEvent(eventId, getRoomId()) : null;

                // test if the redacted event has been echoed
                // it it was not echoed, the event must be pruned to remove useless data
                // the room summary will be updated when the server will echo the redacted event
                if ((null != redactedEvent) && ((null == redactedEvent.unsigned) || (null == redactedEvent.unsigned.redacted_because))) {
                    redactedEvent.prune(null);
                    getStore().storeLiveRoomEvent(redactedEvent);
                    getStore().commit();
                }

                if (null != callback) {
                    callback.onSuccess(redactedEvent);
                }
            }
        });
    }

    /**
     * Redact an event from the room.
     *
     * @param eventId  the event's id
     * @param score    the score
     * @param reason   the redaction reason
     * @param callback the callback with the created event
     */
    public void report(String eventId, int score, String reason, ApiCallback<Void> callback) {
        mDataHandler.getDataRetriever().getRoomsRestClient().reportEvent(getRoomId(), eventId, score, reason, callback);
    }

    //================================================================================
    // Member actions
    //================================================================================

    /**
     * Invite an user to this room.
     *
     * @param userId   the user id
     * @param callback the callback for when done
     */
    public void invite(String userId, ApiCallback<Void> callback) {
        if (null != userId) {
            invite(Collections.singletonList(userId), callback);
        }
    }

    /**
     * Invite an user to a room based on their email address to this room.
     *
     * @param email    the email address
     * @param callback the callback for when done
     */
    public void inviteByEmail(String email, ApiCallback<Void> callback) {
        if (null != email) {
            invite(Collections.singletonList(email), callback);
        }
    }

    /**
     * Invite users to this room.
     * The identifiers are either ini Id or email address.
     *
     * @param identifiers the identifiers list
     * @param callback    the callback for when done
     */
    public void invite(List<String> identifiers, ApiCallback<Void> callback) {
        if (null != identifiers) {
            invite(identifiers.iterator(), callback);
        }
    }

    /**
     * Invite some users to this room.
     *
     * @param identifiers the identifiers iterator
     * @param callback    the callback for when done
     */
    private void invite(final Iterator<String> identifiers, final ApiCallback<Void> callback) {
        if (!identifiers.hasNext()) {
            callback.onSuccess(null);
            return;
        }

        final ApiCallback<Void> localCallback = new SimpleApiCallback<Void>(callback) {
            @Override
            public void onSuccess(Void info) {
                invite(identifiers, callback);
            }
        };

        String identifier = identifiers.next();

        if (android.util.Patterns.EMAIL_ADDRESS.matcher(identifier).matches()) {
            mDataHandler.getDataRetriever().getRoomsRestClient().inviteByEmailToRoom(getRoomId(), identifier, localCallback);
        } else {
            mDataHandler.getDataRetriever().getRoomsRestClient().inviteUserToRoom(getRoomId(), identifier, localCallback);
        }
    }

    /**
     * Leave the room.
     *
     * @param callback the callback for when done
     */
    public void leave(final ApiCallback<Void> callback) {
        mIsLeaving = true;
        mDataHandler.onRoomInternalUpdate(getRoomId());

        mDataHandler.getDataRetriever().getRoomsRestClient().leaveRoom(getRoomId(), new ApiCallback<Void>() {
            @Override
            public void onSuccess(Void info) {
                if (mDataHandler.isAlive()) {
                    mIsLeaving = false;

                    // delete references to the room
                    mDataHandler.deleteRoom(getRoomId());

                    if (null != getStore()) {
                        Log.d(LOG_TAG, "leave : commit");
                        getStore().commit();
                    }

                    try {
                        callback.onSuccess(info);
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "leave exception " + e.getMessage(), e);
                    }

                    mDataHandler.onLeaveRoom(getRoomId());
                }
            }

            @Override
            public void onNetworkError(Exception e) {
                mIsLeaving = false;

                try {
                    callback.onNetworkError(e);
                } catch (Exception anException) {
                    Log.e(LOG_TAG, "leave exception " + anException.getMessage(), anException);
                }

                mDataHandler.onRoomInternalUpdate(getRoomId());
            }

            @Override
            public void onMatrixError(MatrixError e) {
                // the room was not anymore defined server side
                // race condition ?
                if (e.mStatus == 404) {
                    onSuccess(null);
                } else {
                    mIsLeaving = false;

                    try {
                        callback.onMatrixError(e);
                    } catch (Exception anException) {
                        Log.e(LOG_TAG, "leave exception " + anException.getMessage(), anException);
                    }

                    mDataHandler.onRoomInternalUpdate(getRoomId());
                }
            }

            @Override
            public void onUnexpectedError(Exception e) {
                mIsLeaving = false;

                try {
                    callback.onUnexpectedError(e);
                } catch (Exception anException) {
                    Log.e(LOG_TAG, "leave exception " + anException.getMessage(), anException);
                }

                mDataHandler.onRoomInternalUpdate(getRoomId());
            }
        });
    }

    /**
     * Forget the room.
     *
     * @param callback the callback for when done
     */
    public void forget(final ApiCallback<Void> callback) {
        mDataHandler.getDataRetriever().getRoomsRestClient().forgetRoom(getRoomId(), new SimpleApiCallback<Void>(callback) {
            @Override
            public void onSuccess(Void info) {
                if (mDataHandler.isAlive()) {
                    // don't call onSuccess.deleteRoom because it moves an existing room to historical store
                    IMXStore store = mDataHandler.getStore(getRoomId());

                    if (null != store) {
                        store.deleteRoom(getRoomId());
                        store.commit();
                    }

                    try {
                        callback.onSuccess(info);
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "forget exception " + e.getMessage(), e);
                    }
                }
            }
        });
    }


    /**
     * Kick a user from the room.
     *
     * @param userId   the user id
     * @param callback the async callback
     */
    public void kick(String userId, ApiCallback<Void> callback) {
        mDataHandler.getDataRetriever().getRoomsRestClient().kickFromRoom(getRoomId(), userId, callback);
    }

    /**
     * Ban a user from the room.
     *
     * @param userId   the user id
     * @param reason   ban reason
     * @param callback the async callback
     */
    public void ban(String userId, String reason, ApiCallback<Void> callback) {
        BannedUser user = new BannedUser();
        user.userId = userId;
        if (!TextUtils.isEmpty(reason)) {
            user.reason = reason;
        }
        mDataHandler.getDataRetriever().getRoomsRestClient().banFromRoom(getRoomId(), user, callback);
    }

    /**
     * Unban a user.
     *
     * @param userId   the user id
     * @param callback the async callback
     */
    public void unban(String userId, ApiCallback<Void> callback) {
        BannedUser user = new BannedUser();
        user.userId = userId;

        mDataHandler.getDataRetriever().getRoomsRestClient().unbanFromRoom(getRoomId(), user, callback);
    }

    //================================================================================
    // Encryption
    //================================================================================

    private ApiCallback<Void> mRoomEncryptionCallback;

    private final MXEventListener mEncryptionListener = new MXEventListener() {
        @Override
        public void onLiveEvent(Event event, RoomState roomState) {
            if (TextUtils.equals(event.getType(), Event.EVENT_TYPE_MESSAGE_ENCRYPTION)) {
                if (null != mRoomEncryptionCallback) {
                    mRoomEncryptionCallback.onSuccess(null);
                    mRoomEncryptionCallback = null;
                }
            }
        }
    };

    /**
     * @return if the room content is encrypted
     */
    public boolean isEncrypted() {
        return getState().isEncrypted();
    }

    /**
     * Enable the encryption.
     *
     * @param algorithm the used algorithm
     * @param callback  the asynchronous callback
     */
    public void enableEncryptionWithAlgorithm(final String algorithm, final ApiCallback<Void> callback) {
        // ensure that the crypto has been update
        if (null != mDataHandler.getCrypto() && !TextUtils.isEmpty(algorithm)) {
            Map<String, Object> params = new HashMap<>();
            params.put("algorithm", algorithm);

            if (null != callback) {
                mRoomEncryptionCallback = callback;
                addEventListener(mEncryptionListener);
            }

            mDataHandler.getDataRetriever().getRoomsRestClient()
                    .sendStateEvent(getRoomId(), Event.EVENT_TYPE_MESSAGE_ENCRYPTION, null, params, new ApiCallback<Void>() {
                        @Override
                        public void onSuccess(Void info) {
                            // Wait for the event coming back from the hs
                        }

                        @Override
                        public void onNetworkError(Exception e) {
                            if (null != callback) {
                                callback.onNetworkError(e);
                                removeEventListener(mEncryptionListener);
                            }
                        }

                        @Override
                        public void onMatrixError(MatrixError e) {
                            if (null != callback) {
                                callback.onMatrixError(e);
                                removeEventListener(mEncryptionListener);
                            }
                        }

                        @Override
                        public void onUnexpectedError(Exception e) {
                            if (null != callback) {
                                callback.onUnexpectedError(e);
                                removeEventListener(mEncryptionListener);
                            }
                        }
                    });
        } else if (null != callback) {
            if (null == mDataHandler.getCrypto()) {
                callback.onMatrixError(new MXCryptoError(MXCryptoError.ENCRYPTING_NOT_ENABLED_ERROR_CODE,
                        MXCryptoError.ENCRYPTING_NOT_ENABLED_REASON, MXCryptoError.ENCRYPTING_NOT_ENABLED_REASON));
            } else {
                callback.onMatrixError(new MXCryptoError(MXCryptoError.MISSING_FIELDS_ERROR_CODE,
                        MXCryptoError.UNABLE_TO_ENCRYPT, MXCryptoError.MISSING_FIELDS_REASON));
            }
        }
    }

    //==============================================================================================================
    // Room events helper
    //==============================================================================================================

    private RoomMediaMessagesSender mRoomMediaMessagesSender;

    /**
     * Init the mRoomMediaMessagesSender instance
     */
    private void initRoomMediaMessagesSender() {
        if (null == mRoomMediaMessagesSender) {
            mRoomMediaMessagesSender = new RoomMediaMessagesSender(getStore().getContext(), mDataHandler, this);
        }
    }

    /**
     * Send a text message asynchronously.
     *
     * @param text              the unformatted text
     * @param htmlFormattedText the HTML formatted text
     * @param format            the formatted text format
     * @param listener          the event creation listener
     */
    public void sendTextMessage(String text,
                                String htmlFormattedText,
                                String format,
                                RoomMediaMessage.EventCreationListener listener) {
        sendTextMessage(text, htmlFormattedText, format, null, Message.MSGTYPE_TEXT, listener);
    }

    /**
     * Send a text message asynchronously.
     *
     * @param text              the unformatted text
     * @param htmlFormattedText the HTML formatted text
     * @param format            the formatted text format
     * @param replyToEvent      the event to reply to, or null
     * @param listener          the event creation listener
     */
    public void sendTextMessage(String text,
                                String htmlFormattedText,
                                String format,
                                @Nullable Event replyToEvent,
                                RoomMediaMessage.EventCreationListener listener) {
        sendTextMessage(text, htmlFormattedText, format, replyToEvent, Message.MSGTYPE_TEXT, listener);
    }

    /**
     * Send an emote message asynchronously.
     *
     * @param text              the unformatted text
     * @param htmlFormattedText the HTML formatted text
     * @param format            the formatted text format
     * @param listener          the event creation listener
     */
    public void sendEmoteMessage(String text,
                                 String htmlFormattedText,
                                 String format,
                                 final RoomMediaMessage.EventCreationListener listener) {
        sendTextMessage(text, htmlFormattedText, format, null, Message.MSGTYPE_EMOTE, listener);
    }

    /**
     * Send a text message asynchronously.
     *
     * @param text              the unformatted text
     * @param htmlFormattedText the HTML formatted text
     * @param format            the formatted text format
     * @param replyToEvent      the event to reply to (optional). Only event of type Event.EVENT_TYPE_MESSAGE are supported for the moment.
     * @param msgType           the message type
     * @param listener          the event creation listener
     */
    private void sendTextMessage(String text,
                                 String htmlFormattedText,
                                 String format,
                                 @Nullable Event replyToEvent,
                                 String msgType,
                                 final RoomMediaMessage.EventCreationListener listener) {
        initRoomMediaMessagesSender();

        RoomMediaMessage roomMediaMessage = new RoomMediaMessage(text, htmlFormattedText, format);
        roomMediaMessage.setMessageType(msgType);
        roomMediaMessage.setEventCreationListener(listener);

        if (canReplyTo(replyToEvent)) {
            roomMediaMessage.setReplyToEvent(replyToEvent);
        }

        mRoomMediaMessagesSender.send(roomMediaMessage);
    }

    /**
     * Indicate if replying to the provided event is supported.
     * Only event of type Event.EVENT_TYPE_MESSAGE are supported for the moment, and for certain msgtype.
     *
     * @param replyToEvent the event to reply to
     * @return true if it is possible to reply to this event
     */
    public boolean canReplyTo(@Nullable Event replyToEvent) {
        if (replyToEvent != null
                && Event.EVENT_TYPE_MESSAGE.equals(replyToEvent.getType())) {

            // Cf. https://docs.google.com/document/d/1BPd4lBrooZrWe_3s_lHw_e-Dydvc7bXbm02_sV2k6Sc
            String msgType = JsonUtils.getMessageMsgType(replyToEvent.getContentAsJsonObject());

            if (msgType != null) {
                switch (msgType) {
                    case Message.MSGTYPE_TEXT:
                    case Message.MSGTYPE_NOTICE:
                    case Message.MSGTYPE_EMOTE:
                    case Message.MSGTYPE_IMAGE:
                    case Message.MSGTYPE_VIDEO:
                    case Message.MSGTYPE_AUDIO:
                    case Message.MSGTYPE_FILE:
                        return true;
                }
            }
        }

        return false;
    }

    /**
     * Send an media message asynchronously.
     *
     * @param roomMediaMessage   the media message to send.
     * @param maxThumbnailWidth  the max thumbnail width
     * @param maxThumbnailHeight the max thumbnail height
     * @param listener           the event creation listener
     */
    public void sendMediaMessage(final RoomMediaMessage roomMediaMessage,
                                 final int maxThumbnailWidth,
                                 final int maxThumbnailHeight,
                                 final RoomMediaMessage.EventCreationListener listener) {
        initRoomMediaMessagesSender();

        roomMediaMessage.setThumbnailSize(new Pair<>(maxThumbnailWidth, maxThumbnailHeight));
        roomMediaMessage.setEventCreationListener(listener);

        mRoomMediaMessagesSender.send(roomMediaMessage);
    }

    /**
     * Send a sticker message.
     *
     * @param event
     * @param listener
     */
    public void sendStickerMessage(Event event, final RoomMediaMessage.EventCreationListener listener) {
        initRoomMediaMessagesSender();

        RoomMediaMessage roomMediaMessage = new RoomMediaMessage(event);
        roomMediaMessage.setMessageType(Event.EVENT_TYPE_STICKER);
        roomMediaMessage.setEventCreationListener(listener);

        mRoomMediaMessagesSender.send(roomMediaMessage);
    }

    //==============================================================================================================
    // Unsent events management
    //==============================================================================================================

    /**
     * Provides the unsent messages list.
     *
     * @return the unsent events list
     */
    public List<Event> getUnsentEvents() {
        List<Event> unsent = new ArrayList<>();

        if (null != getStore()) {
            List<Event> undeliverableEvents = getStore().getUndeliveredEvents(getRoomId());
            List<Event> unknownDeviceEvents = getStore().getUnknownDeviceEvents(getRoomId());

            if (null != undeliverableEvents) {
                unsent.addAll(undeliverableEvents);
            }

            if (null != unknownDeviceEvents) {
                unsent.addAll(unknownDeviceEvents);
            }
        }

        return unsent;
    }

    /**
     * Delete an events list.
     *
     * @param events the events list
     */
    public void deleteEvents(List<Event> events) {
        if ((null != getStore()) && (null != events) && events.size() > 0) {
            // reset the timestamp
            for (Event event : events) {
                getStore().deleteEvent(event);
            }

            // update the summary
            Event latestEvent = getStore().getLatestEvent(getRoomId());

            // if there is an oldest event, use it to set a summary
            if (latestEvent != null) {
                if (RoomSummary.isSupportedEvent(latestEvent)) {
                    RoomSummary summary = getStore().getSummary(getRoomId());

                    if (null != summary) {
                        summary.setLatestReceivedEvent(latestEvent, getState());
                    } else {
                        summary = new RoomSummary(null, latestEvent, getState(), mDataHandler.getUserId());
                    }

                    getStore().storeSummary(summary);
                }
            }

            getStore().commit();
        }
    }

    /**
     * Tell if room is Direct Chat
     *
     * @return true if is direct chat
     */
    public boolean isDirect() {
        return mDataHandler.getDirectChatRoomIdsList().contains(getRoomId());
    }

    @Nullable
    public RoomSummary getRoomSummary() {
        if (getDataHandler() == null) {
            return null;
        }

        if (getDataHandler().getStore() == null) {
            return null;
        }

        return getDataHandler().getStore().getSummary(getRoomId());
    }

    public int getNumberOfMembers() {
        if (getDataHandler().isLazyLoadingEnabled()) {
            return getNumberOfJoinedMembers() + getNumberOfInvitedMembers();
        } else {
            return getState().getLoadedMembers().size();
        }
    }

    public int getNumberOfJoinedMembers() {
        if (getDataHandler().isLazyLoadingEnabled()) {
            RoomSummary roomSummary = getRoomSummary();

            if (roomSummary != null) {
                return roomSummary.getNumberOfJoinedMembers();
            } else {
                // Should not happen, fallback to loaded members
                return getNumberOfLoadedJoinedMembers();
            }
        } else {
            return getNumberOfLoadedJoinedMembers();
        }
    }

    private int getNumberOfLoadedJoinedMembers() {
        int count = 0;

        for (RoomMember roomMember : getState().getLoadedMembers()) {
            if (RoomMember.MEMBERSHIP_JOIN.equals(roomMember.membership)) {
                count++;
            }
        }

        return count;
    }

    public int getNumberOfInvitedMembers() {
        if (getDataHandler().isLazyLoadingEnabled()) {
            RoomSummary roomSummary = getRoomSummary();

            if (roomSummary != null) {
                return roomSummary.getNumberOfInvitedMembers();
            } else {
                // Should not happen, fallback to loaded members
                return getNumberOfLoadedInvitedMembers();
            }
        } else {
            return getNumberOfLoadedInvitedMembers();
        }
    }

    private int getNumberOfLoadedInvitedMembers() {
        int count = 0;

        for (RoomMember roomMember : getState().getLoadedMembers()) {
            if (RoomMember.MEMBERSHIP_INVITE.equals(roomMember.membership)) {
                count++;
            }
        }

        return count;
    }
}
