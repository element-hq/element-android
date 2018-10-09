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

import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.gson.JsonObject;

import im.vector.matrix.android.internal.legacy.MXDataHandler;
import im.vector.matrix.android.internal.legacy.call.MXCallsManager;
import im.vector.matrix.android.internal.legacy.data.store.IMXStore;
import im.vector.matrix.android.internal.legacy.data.timeline.EventTimeline;
import im.vector.matrix.android.internal.legacy.rest.callback.ApiCallback;
import im.vector.matrix.android.internal.legacy.rest.callback.SimpleApiCallback;
import im.vector.matrix.android.internal.legacy.rest.model.Event;
import im.vector.matrix.android.internal.legacy.rest.model.PowerLevels;
import im.vector.matrix.android.internal.legacy.rest.model.RoomCreateContent;
import im.vector.matrix.android.internal.legacy.rest.model.RoomDirectoryVisibility;
import im.vector.matrix.android.internal.legacy.rest.model.RoomMember;
import im.vector.matrix.android.internal.legacy.rest.model.RoomPinnedEventsContent;
import im.vector.matrix.android.internal.legacy.rest.model.RoomTombstoneContent;
import im.vector.matrix.android.internal.legacy.rest.model.User;
import im.vector.matrix.android.internal.legacy.rest.model.pid.RoomThirdPartyInvite;
import im.vector.matrix.android.internal.legacy.util.JsonUtils;
import im.vector.matrix.android.internal.legacy.util.Log;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The state of a room.
 */
public class RoomState implements Externalizable {
    private static final String LOG_TAG = RoomState.class.getSimpleName();
    private static final long serialVersionUID = -6019932024524988201L;

    public static final String JOIN_RULE_PUBLIC = "public";
    public static final String JOIN_RULE_INVITE = "invite";

    /**
     * room access is granted to guests
     */
    public static final String GUEST_ACCESS_CAN_JOIN = "can_join";
    /**
     * room access is denied to guests
     */
    public static final String GUEST_ACCESS_FORBIDDEN = "forbidden";

    public static final String HISTORY_VISIBILITY_SHARED = "shared";
    public static final String HISTORY_VISIBILITY_INVITED = "invited";
    public static final String HISTORY_VISIBILITY_JOINED = "joined";
    public static final String HISTORY_VISIBILITY_WORLD_READABLE = "world_readable";


    // Public members used for JSON mapping

    // The room ID
    public String roomId;

    // The power level of room members
    private PowerLevels powerLevels;

    // The aliases
    public List<String> aliases;

    // The room aliases. The key is the domain.
    private Map<String, Event> mRoomAliases = new HashMap<>();

    // the aliases are defined for each home server url
    private Map<String, List<String>> mAliasesByDomain = new HashMap<>();

    // merged from mAliasesByHomeServerUrl
    private List<String> mMergedAliasesList;

    //
    private Map<String, List<Event>> mStateEvents = new HashMap<>();

    // The canonical alias of the room.
    private String canonicalAlias;

    // The name of the room as provided by the home server.
    public String name;

    // The topic of the room.
    public String topic;

    // The tombstone content if the room has been killed
    private RoomTombstoneContent mRoomTombstoneContent;

    // The avatar url of the room.
    public String url;
    public String avatar_url;

    // the room create content
    private RoomCreateContent mRoomCreateContent;

    // the room pinned events content
    @Nullable
    private RoomPinnedEventsContent mRoomPinnedEventsContent;

    // the join rule
    public String join_rule;

    /**
     * the guest access policy of the room
     **/
    public String guest_access;

    // SPEC-134
    public String history_visibility;

    /**
     * the room visibility in the directory list (i.e. public, private...)
     **/
    public String visibility;

    // the encryption algorithm
    public String algorithm;

    // group ids list which should be displayed
    public List<String> groups;

    /**
     * The number of unread messages that match the push notification rules.
     * It is based on the notificationCount field in /sync response.
     */
    private int mNotificationCount;

    /**
     * The number of highlighted unread messages (subset of notifications).
     * It is based on the notificationCount field in /sync response.
     */
    private int mHighlightCount;

    // the associated token
    private String token;

    // the room members. May be a partial list if all members are not loaded yet, due to lazy loading
    private final Map<String, RoomMember> mMembers = new HashMap<>();

    // true if all members are loaded
    private boolean mAllMembersAreLoaded;

    private final List<ApiCallback<List<RoomMember>>> mGetAllMembersCallbacks = new ArrayList<>();

    // the third party invite members
    private final Map<String, RoomThirdPartyInvite> mThirdPartyInvites = new HashMap<>();

    /**
     * Cache for [self memberWithThirdPartyInviteToken].
     * The key is the 3pid invite token.
     */
    private final Map<String, RoomMember> mMembersWithThirdPartyInviteTokenCache = new HashMap<>();

    /**
     * Tell if the roomstate if a live one.
     */
    private boolean mIsLive;

    // the unitary tests crash when MXDataHandler type is set.
    // TODO Try to avoid this ^^
    private transient Object mDataHandler = null;

    // member display cache
    private transient Map<String, String> mMemberDisplayNameByUserId = new HashMap<>();

    // get the guest access
    // avoid the null case
    public String getGuestAccess() {
        if (null != guest_access) {
            return guest_access;
        }

        // retro compliancy
        return RoomState.GUEST_ACCESS_FORBIDDEN;
    }

    // get the history visibility
    // avoid the null case
    public String getHistoryVisibility() {
        if (null != history_visibility) {
            return history_visibility;
        }

        // retro compliancy
        return RoomState.HISTORY_VISIBILITY_SHARED;
    }

    /**
     * @return the state token
     */
    public String getToken() {
        return token;
    }

    /**
     * Update the token.
     *
     * @param token the new token
     */
    public void setToken(String token) {
        this.token = token;
    }

    // avatar Url makes more sense than url.
    public String getAvatarUrl() {
        if (null != url) {
            return url;
        } else {
            return avatar_url;
        }
    }

    /**
     * @return the related group ids list (cannot be null)
     */
    public List<String> getRelatedGroups() {
        return (null == groups) ? new ArrayList<String>() : groups;
    }

    /**
     * @return a copy of the room members list. May be incomplete if the full list is not loaded yet
     */
    public List<RoomMember> getLoadedMembers() {
        List<RoomMember> res;

        synchronized (this) {
            // make a copy to avoid concurrency modifications
            res = new ArrayList<>(mMembers.values());
        }

        return res;
    }

    /**
     * Get the list of all the room members. Fetch from server if the full list is not loaded yet.
     *
     * @param callback The callback to get a copy of the room members list.
     */
    public void getMembersAsync(ApiCallback<List<RoomMember>> callback) {
        if (areAllMembersLoaded()) {
            List<RoomMember> res;

            synchronized (this) {
                // make a copy to avoid concurrency modifications
                res = new ArrayList<>(mMembers.values());
            }

            callback.onSuccess(res);
        } else {
            boolean doTheRequest;

            synchronized (mGetAllMembersCallbacks) {
                mGetAllMembersCallbacks.add(callback);

                doTheRequest = mGetAllMembersCallbacks.size() == 1;
            }

            if (doTheRequest) {
                // Load members from server
                getDataHandler().getMembersAsync(roomId, new SimpleApiCallback<List<RoomMember>>(callback) {
                    @Override
                    public void onSuccess(List<RoomMember> info) {
                        Log.d(LOG_TAG, "getMembers has returned " + info.size() + " users.");

                        IMXStore store = ((MXDataHandler) mDataHandler).getStore();
                        List<RoomMember> res;

                        for (RoomMember member : info) {
                            // Do not erase already known members form the sync
                            if (getMember(member.getUserId()) == null) {
                                setMember(member.getUserId(), member);

                                // Also create a User
                                if (store != null) {
                                    store.updateUserWithRoomMemberEvent(member);
                                }
                            }
                        }

                        synchronized (mGetAllMembersCallbacks) {
                            for (ApiCallback<List<RoomMember>> apiCallback : mGetAllMembersCallbacks) {
                                // make a copy to avoid concurrency modifications
                                res = new ArrayList<>(mMembers.values());

                                apiCallback.onSuccess(res);
                            }

                            mGetAllMembersCallbacks.clear();
                        }

                        mAllMembersAreLoaded = true;
                    }
                });
            }
        }
    }

    /**
     * Tell if all members has been loaded
     *
     * @return true if LazyLoading is Off, or if all members has been loaded
     */
    private boolean areAllMembersLoaded() {
        return mDataHandler != null
                && (!((MXDataHandler) mDataHandler).isLazyLoadingEnabled() || mAllMembersAreLoaded);
    }

    /**
     * Force a fetch of the loaded members the next time they will be requested
     */
    public void forceMembersRequest() {
        mAllMembersAreLoaded = false;
    }

    /**
     * Provides the loaded states event list.
     * The room member events are NOT included.
     *
     * @param types the allowed event types.
     * @return the filtered state events list.
     */
    public List<Event> getStateEvents(final Set<String> types) {
        final List<Event> filteredStateEvents = new ArrayList<>();
        final List<Event> stateEvents = new ArrayList<>();

        // merge the values lists
        Collection<List<Event>> currentStateEvents = mStateEvents.values();
        for (List<Event> eventsList : currentStateEvents) {
            stateEvents.addAll(eventsList);
        }

        if ((null != types) && !types.isEmpty()) {
            for (Event stateEvent : stateEvents) {
                if ((null != stateEvent.getType()) && types.contains(stateEvent.getType())) {
                    filteredStateEvents.add(stateEvent);
                }
            }
        } else {
            filteredStateEvents.addAll(stateEvents);
        }

        return filteredStateEvents;
    }


    /**
     * Provides the state events list.
     * It includes the room member creation events (they are not loaded in memory by default).
     *
     * @param store    the store in which the state events must be retrieved
     * @param types    the allowed event types.
     * @param callback the asynchronous callback.
     */
    public void getStateEvents(IMXStore store, final Set<String> types, final ApiCallback<List<Event>> callback) {
        if (null != store) {
            final List<Event> stateEvents = new ArrayList<>();

            Collection<List<Event>> currentStateEvents = mStateEvents.values();

            for (List<Event> eventsList : currentStateEvents) {
                stateEvents.addAll(eventsList);
            }

            // retrieve the roomMember creation events
            store.getRoomStateEvents(roomId, new SimpleApiCallback<List<Event>>() {
                @Override
                public void onSuccess(List<Event> events) {
                    stateEvents.addAll(events);

                    final List<Event> filteredStateEvents = new ArrayList<>();

                    if ((null != types) && !types.isEmpty()) {
                        for (Event stateEvent : stateEvents) {
                            if ((null != stateEvent.getType()) && types.contains(stateEvent.getType())) {
                                filteredStateEvents.add(stateEvent);
                            }
                        }
                    } else {
                        filteredStateEvents.addAll(stateEvents);
                    }

                    callback.onSuccess(filteredStateEvents);
                }
            });
        }
    }

    /**
     * @return a copy of the displayable members list. May be incomplete if the full list is not loaded yet
     */
    public List<RoomMember> getDisplayableLoadedMembers() {
        List<RoomMember> res = getLoadedMembers();

        RoomMember conferenceUserId = getMember(MXCallsManager.getConferenceUserId(roomId));

        if (null != conferenceUserId) {
            res.remove(conferenceUserId);
        }

        return res;
    }

    /**
     * Provides a list of displayable members.
     * Some dummy members are created to internal stuff.
     *
     * @param callback The callback to get a copy of the displayable room members list.
     */
    public void getDisplayableMembersAsync(final ApiCallback<List<RoomMember>> callback) {
        getMembersAsync(new SimpleApiCallback<List<RoomMember>>(callback) {
            @Override
            public void onSuccess(List<RoomMember> members) {
                RoomMember conferenceUserId = getMember(MXCallsManager.getConferenceUserId(roomId));

                if (null != conferenceUserId) {
                    List<RoomMember> membersList = new ArrayList<>(members);
                    membersList.remove(conferenceUserId);
                    callback.onSuccess(membersList);
                } else {
                    callback.onSuccess(members);
                }
            }
        });
    }

    /**
     * Tells if the room is a call conference one
     * i.e. this room has been created to manage the call conference
     *
     * @return true if it is a call conference room.
     */
    public boolean isConferenceUserRoom() {
        return getDataHandler().getStore().getSummary(roomId).isConferenceUserRoom();
    }

    /**
     * Set this room as a conference user room
     *
     * @param isConferenceUserRoom true when it is an user conference room.
     */
    public void setIsConferenceUserRoom(boolean isConferenceUserRoom) {
        getDataHandler().getStore().getSummary(roomId).setIsConferenceUserRoom(isConferenceUserRoom);
    }

    /**
     * Update the room member from its user id.
     *
     * @param userId the user id.
     * @param member the new member value.
     */
    private void setMember(String userId, RoomMember member) {
        // Populate a basic user object if there is none
        if (member.getUserId() == null) {
            member.setUserId(userId);
        }
        synchronized (this) {
            if (null != mMemberDisplayNameByUserId) {
                mMemberDisplayNameByUserId.remove(userId);
            }
            mMembers.put(userId, member);
        }
    }

    /**
     * Retrieve a room member from its user id.
     *
     * @param userId the user id.
     * @return the linked member it exists.
     */
    // TODO Change this? Can return null if all members are not loaded yet
    @Nullable
    public RoomMember getMember(String userId) {
        RoomMember member;

        synchronized (this) {
            member = mMembers.get(userId);
        }

        if (member == null) {
            // TODO LazyLoading
            Log.e(LOG_TAG, "!!!!!!!!!!!!!!!!!!!!!!!!!!!!! Null member '" + userId + "' !!!!!!!!!!!!!!!!!!!!!!!!!!!!!");

            if (TextUtils.equals(getDataHandler().getUserId(), userId)) {
                // This should never happen
                Log.e(LOG_TAG, "!!!!!!!!!!!!!!!!!!!!!!!!!!!!! Null current user '" + userId + "' !!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            }
        }

        return member;
    }

    /**
     * Retrieve a room member from its original event id.
     * It can return null if the lazy loading is enabled and if the member is not loaded yet.
     *
     * @param eventId the event id.
     * @return the linked member if it exists and if it is loaded.
     */
    @Nullable
    public RoomMember getMemberByEventId(String eventId) {
        RoomMember member = null;

        synchronized (this) {
            for (RoomMember aMember : mMembers.values()) {
                if (aMember.getOriginalEventId().equals(eventId)) {
                    member = aMember;
                    break;
                }
            }
        }

        return member;
    }

    /**
     * Remove a member defines by its user id.
     *
     * @param userId the user id.
     */
    public void removeMember(String userId) {
        synchronized (this) {
            mMembers.remove(userId);
            // remove the cached display name
            if (null != mMemberDisplayNameByUserId) {
                mMemberDisplayNameByUserId.remove(userId);
            }
        }
    }

    /**
     * Retrieve a member from an invitation token.
     *
     * @param thirdPartyInviteToken the third party invitation token.
     * @return the member it exists.
     */
    public RoomMember memberWithThirdPartyInviteToken(String thirdPartyInviteToken) {
        return mMembersWithThirdPartyInviteTokenCache.get(thirdPartyInviteToken);
    }

    /**
     * Retrieve a RoomThirdPartyInvite from its token.
     *
     * @param thirdPartyInviteToken the third party invitation token.
     * @return the linked RoomThirdPartyInvite if it exists
     */
    public RoomThirdPartyInvite thirdPartyInviteWithToken(String thirdPartyInviteToken) {
        return mThirdPartyInvites.get(thirdPartyInviteToken);
    }

    /**
     * @return the third party invite list.
     */
    public Collection<RoomThirdPartyInvite> thirdPartyInvites() {
        return mThirdPartyInvites.values();
    }

    /**
     * @return the power levels (it can be null).
     */
    public PowerLevels getPowerLevels() {
        if (null != powerLevels) {
            return powerLevels.deepCopy();
        } else {
            return null;
        }
    }

    /**
     * Update the power levels.
     *
     * @param powerLevels the new power levels
     */
    public void setPowerLevels(PowerLevels powerLevels) {
        this.powerLevels = powerLevels;
    }

    /**
     * Update the linked dataHandler.
     *
     * @param dataHandler the new dataHandler
     */
    public void setDataHandler(MXDataHandler dataHandler) {
        mDataHandler = dataHandler;
    }

    /**
     * @return the user dataHandler
     */
    public MXDataHandler getDataHandler() {
        return (MXDataHandler) mDataHandler;
    }

    /**
     * Update the notified messages count.
     *
     * @param notificationCount the new notified messages count.
     */
    public void setNotificationCount(int notificationCount) {
        Log.d(LOG_TAG, "## setNotificationCount() : " + notificationCount + " room id " + roomId);
        mNotificationCount = notificationCount;
    }

    /**
     * @return the notified messages count.
     */
    public int getNotificationCount() {
        return mNotificationCount;
    }

    /**
     * Update the highlighted messages count.
     *
     * @param highlightCount the new highlighted messages count.
     */
    public void setHighlightCount(int highlightCount) {
        Log.d(LOG_TAG, "## setHighlightCount() : " + highlightCount + " room id " + roomId);
        mHighlightCount = highlightCount;
    }

    /**
     * @return the highlighted messages count.
     */
    public int getHighlightCount() {
        return mHighlightCount;
    }

    /**
     * Check if the user userId can back paginate.
     *
     * @param isJoined  true is user is in the room
     * @param isInvited true is user is invited to the room
     * @return true if the user can back paginate.
     */
    public boolean canBackPaginate(boolean isJoined, boolean isInvited) {
        String visibility = TextUtils.isEmpty(history_visibility) ? HISTORY_VISIBILITY_SHARED : history_visibility;

        return isJoined
                || visibility.equals(HISTORY_VISIBILITY_WORLD_READABLE)
                || visibility.equals(HISTORY_VISIBILITY_SHARED)
                || (visibility.equals(HISTORY_VISIBILITY_INVITED) && isInvited);
    }

    /**
     * Make a deep copy of this room state object.
     *
     * @return the copy
     */
    public RoomState deepCopy() {
        RoomState copy = new RoomState();
        copy.roomId = roomId;
        copy.setPowerLevels((powerLevels == null) ? null : powerLevels.deepCopy());
        copy.aliases = (aliases == null) ? null : new ArrayList<>(aliases);
        copy.mAliasesByDomain = new HashMap<>(mAliasesByDomain);
        copy.canonicalAlias = canonicalAlias;
        copy.name = name;
        copy.topic = topic;
        copy.url = url;
        copy.mRoomCreateContent = mRoomCreateContent != null ? mRoomCreateContent.deepCopy() : null;
        copy.mRoomPinnedEventsContent = mRoomPinnedEventsContent != null ? mRoomPinnedEventsContent.deepCopy() : null;
        copy.join_rule = join_rule;
        copy.guest_access = guest_access;
        copy.history_visibility = history_visibility;
        copy.visibility = visibility;
        copy.token = token;
        copy.groups = groups;
        copy.mDataHandler = mDataHandler;
        copy.mIsLive = mIsLive;
        copy.mAllMembersAreLoaded = mAllMembersAreLoaded;
        copy.algorithm = algorithm;
        copy.mRoomAliases = new HashMap<>(mRoomAliases);
        copy.mStateEvents = new HashMap<>(mStateEvents);
        copy.mRoomTombstoneContent = mRoomTombstoneContent != null ? mRoomTombstoneContent.deepCopy() : null;
        synchronized (this) {
            Iterator it = mMembers.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, RoomMember> pair = (Map.Entry<String, RoomMember>) it.next();
                copy.setMember(pair.getKey(), pair.getValue().deepCopy());
            }

            Collection<String> keys = mThirdPartyInvites.keySet();
            for (String key : keys) {
                copy.mThirdPartyInvites.put(key, mThirdPartyInvites.get(key).deepCopy());
            }

            keys = mMembersWithThirdPartyInviteTokenCache.keySet();
            for (String key : keys) {
                copy.mMembersWithThirdPartyInviteTokenCache.put(key, mMembersWithThirdPartyInviteTokenCache.get(key).deepCopy());
            }
        }

        return copy;
    }

    /**
     * @return the room canonical alias
     */
    public String getCanonicalAlias() {
        return canonicalAlias;
    }

    /**
     * Update the canonical alias of a room
     *
     * @param newCanonicalAlias the new canonical alias
     */
    public void setCanonicalAlias(String newCanonicalAlias) {
        canonicalAlias = newCanonicalAlias;
    }

    /**
     * Provides the aliases for any known domains
     *
     * @return the aliases list
     */
    public List<String> getAliases() {
        if (null == mMergedAliasesList) {
            mMergedAliasesList = new ArrayList<>();

            for (String url : mAliasesByDomain.keySet()) {
                mMergedAliasesList.addAll(mAliasesByDomain.get(url));
            }

            // ensure that the current aliases have been added.
            // for example for the public rooms because there is no applystate call.
            if (null != aliases) {
                for (String anAlias : aliases) {
                    if (mMergedAliasesList.indexOf(anAlias) < 0) {
                        mMergedAliasesList.add(anAlias);
                    }
                }
            }
        }

        return mMergedAliasesList;
    }

    /**
     * Provides the aliases by domain
     *
     * @return the aliases list map
     */
    public Map<String, List<String>> getAliasesByDomain() {
        return new HashMap<>(mAliasesByDomain);
    }

    /**
     * Remove an alias.
     *
     * @param alias the alias to remove
     */
    public void removeAlias(String alias) {
        if (getAliases().indexOf(alias) >= 0) {
            if (null != aliases) {
                aliases.remove(alias);
            }

            for (String host : mAliasesByDomain.keySet()) {
                mAliasesByDomain.get(host).remove(alias);
            }

            mMergedAliasesList = null;
        }
    }

    /**
     * Add an alias.
     *
     * @param alias the alias to add
     */
    public void addAlias(String alias) {
        if (getAliases().indexOf(alias) < 0) {
            // patch until the server echoes the alias addition.
            mMergedAliasesList.add(alias);
        }
    }

    /**
     * @return true if the room is encrypted
     */
    public boolean isEncrypted() {
        // When a client receives an m.room.encryption event as above, it should set a flag to indicate that messages sent in the room should be encrypted.
        // This flag should not be cleared if a later m.room.encryption event changes the configuration. This is to avoid a situation where a MITM can simply
        // ask participants to disable encryption. In short: once encryption is enabled in a room, it can never be disabled.
        return null != algorithm;
    }

    /**
     * @return true if the room is versioned, it means that the room is obsolete.
     *         You can't interact with it anymore, but you can still browse the past messages.
     */
    public boolean isVersioned() {
        return mRoomTombstoneContent != null;
    }

    /**
     * @return the room tombstone content
     */
    public RoomTombstoneContent getRoomTombstoneContent() {
        return mRoomTombstoneContent;
    }

    /**
     * @return true if the room has a predecessor
     */
    public boolean hasPredecessor() {
        return mRoomCreateContent != null && mRoomCreateContent.hasPredecessor();
    }

    /**
     * @return the room create content
     */
    public RoomCreateContent getRoomCreateContent() {
        return mRoomCreateContent;
    }

    /**
     * @return the room pinned events content
     */
    @Nullable
    public RoomPinnedEventsContent getRoomPinnedEventsContent() {
        return mRoomPinnedEventsContent;
    }

    /**
     * @return the encryption algorithm
     */
    public String encryptionAlgorithm() {
        return TextUtils.isEmpty(algorithm) ? null : algorithm;
    }

    /**
     * Apply the given event (relevant for state changes) to our state.
     *
     * @param store     the store to use
     * @param event     the event
     * @param direction how the event should affect the state: Forwards for applying, backwards for un-applying (applying the previous state)
     * @return true if the event is managed
     */
    public boolean applyState(IMXStore store, Event event, EventTimeline.Direction direction) {
        if (event.stateKey == null) {
            return false;
        }

        JsonObject contentToConsider = (direction == EventTimeline.Direction.FORWARDS) ? event.getContentAsJsonObject() : event.getPrevContentAsJsonObject();
        String eventType = event.getType();

        try {
            if (Event.EVENT_TYPE_STATE_ROOM_NAME.equals(eventType)) {
                name = JsonUtils.toStateEvent(contentToConsider).name;
            } else if (Event.EVENT_TYPE_STATE_ROOM_TOPIC.equals(eventType)) {
                topic = JsonUtils.toStateEvent(contentToConsider).topic;
            } else if (Event.EVENT_TYPE_STATE_ROOM_CREATE.equals(eventType)) {
                mRoomCreateContent = JsonUtils.toRoomCreateContent(contentToConsider);
            } else if (Event.EVENT_TYPE_STATE_ROOM_JOIN_RULES.equals(eventType)) {
                join_rule = JsonUtils.toStateEvent(contentToConsider).joinRule;
            } else if (Event.EVENT_TYPE_STATE_ROOM_GUEST_ACCESS.equals(eventType)) {
                guest_access = JsonUtils.toStateEvent(contentToConsider).guestAccess;
            } else if (Event.EVENT_TYPE_STATE_ROOM_ALIASES.equals(eventType)) {
                if (!TextUtils.isEmpty(event.stateKey)) {
                    // backward compatibility
                    aliases = JsonUtils.toStateEvent(contentToConsider).aliases;

                    // sanity check
                    if (null != aliases) {
                        mAliasesByDomain.put(event.stateKey, aliases);
                        mRoomAliases.put(event.stateKey, event);
                    } else {
                        mAliasesByDomain.put(event.stateKey, new ArrayList<String>());
                    }
                }
            } else if (Event.EVENT_TYPE_MESSAGE_ENCRYPTION.equals(eventType)) {
                algorithm = JsonUtils.toStateEvent(contentToConsider).algorithm;

                // When a client receives an m.room.encryption event as above, it should set a flag to indicate that messages sent
                // in the room should be encrypted.
                // This flag should not be cleared if a later m.room.encryption event changes the configuration. This is to avoid
                // a situation where a MITM can simply ask participants to disable encryption. In short: once encryption is enabled
                // in a room, it can never be disabled.
                if (null == algorithm) {
                    algorithm = "";
                }
            } else if (Event.EVENT_TYPE_STATE_CANONICAL_ALIAS.equals(eventType)) {
                // SPEC-125
                canonicalAlias = JsonUtils.toStateEvent(contentToConsider).canonicalAlias;
            } else if (Event.EVENT_TYPE_STATE_HISTORY_VISIBILITY.equals(eventType)) {
                // SPEC-134
                history_visibility = JsonUtils.toStateEvent(contentToConsider).historyVisibility;
            } else if (Event.EVENT_TYPE_STATE_ROOM_AVATAR.equals(eventType)) {
                url = JsonUtils.toStateEvent(contentToConsider).url;
            } else if (Event.EVENT_TYPE_STATE_RELATED_GROUPS.equals(eventType)) {
                groups = JsonUtils.toStateEvent(contentToConsider).groups;
            } else if (Event.EVENT_TYPE_STATE_ROOM_MEMBER.equals(eventType)) {
                RoomMember member = JsonUtils.toRoomMember(contentToConsider);
                String userId = event.stateKey;

                if (null == userId) {
                    Log.e(LOG_TAG, "## applyState() : null stateKey in " + roomId);
                } else if (null == member) {
                    // the member has already been removed
                    if (null == getMember(userId)) {
                        Log.e(LOG_TAG, "## applyState() : the user " + userId + " is not anymore a member of " + roomId);
                        return false;
                    }
                    removeMember(userId);
                } else {
                    try {
                        member.setUserId(userId);
                        member.setOriginServerTs(event.getOriginServerTs());
                        member.setOriginalEventId(event.eventId);
                        member.mSender = event.getSender();

                        if ((null != store) && (direction == EventTimeline.Direction.FORWARDS)) {
                            store.storeRoomStateEvent(roomId, event);
                        }

                        RoomMember currentMember = getMember(userId);

                        // check if the member is the same
                        // duplicated message ?
                        if (member.equals(currentMember)) {
                            Log.e(LOG_TAG, "## applyState() : seems being a duplicated event for " + userId + " in room " + roomId);
                            return false;
                        }

                        // when a member leaves a room, his avatar / display name is not anymore provided
                        if (null != currentMember) {
                            if (TextUtils.equals(member.membership, RoomMember.MEMBERSHIP_LEAVE)
                                    || TextUtils.equals(member.membership, (RoomMember.MEMBERSHIP_BAN))) {
                                if (null == member.getAvatarUrl()) {
                                    member.setAvatarUrl(currentMember.getAvatarUrl());
                                }

                                if (null == member.displayname) {
                                    member.displayname = currentMember.displayname;
                                }

                                // remove the cached display name
                                if (null != mMemberDisplayNameByUserId) {
                                    mMemberDisplayNameByUserId.remove(userId);
                                }

                                // test if the user has been kicked
                                if (!TextUtils.equals(event.getSender(), event.stateKey)
                                        && TextUtils.equals(currentMember.membership, RoomMember.MEMBERSHIP_JOIN)
                                        && TextUtils.equals(member.membership, RoomMember.MEMBERSHIP_LEAVE)) {
                                    member.membership = RoomMember.MEMBERSHIP_KICK;
                                }
                            }
                        }

                        if ((direction == EventTimeline.Direction.FORWARDS) && (null != store)) {
                            store.updateUserWithRoomMemberEvent(member);
                        }

                        // Cache room member event that is successor of a third party invite event
                        if (!TextUtils.isEmpty(member.getThirdPartyInviteToken())) {
                            mMembersWithThirdPartyInviteTokenCache.put(member.getThirdPartyInviteToken(), member);
                        }
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "## applyState() - EVENT_TYPE_STATE_ROOM_MEMBER failed " + e.getMessage(), e);
                    }

                    setMember(userId, member);
                }
            } else if (Event.EVENT_TYPE_STATE_ROOM_POWER_LEVELS.equals(eventType)) {
                powerLevels = JsonUtils.toPowerLevels(contentToConsider);
            } else if (Event.EVENT_TYPE_STATE_ROOM_THIRD_PARTY_INVITE.equals(event.getType())) {
                if (null != contentToConsider) {
                    RoomThirdPartyInvite thirdPartyInvite = JsonUtils.toRoomThirdPartyInvite(contentToConsider);

                    thirdPartyInvite.token = event.stateKey;

                    if ((direction == EventTimeline.Direction.FORWARDS) && (null != store)) {
                        store.storeRoomStateEvent(roomId, event);
                    }

                    if (!TextUtils.isEmpty(thirdPartyInvite.token)) {
                        mThirdPartyInvites.put(thirdPartyInvite.token, thirdPartyInvite);
                    }
                }
            } else if (Event.EVENT_TYPE_STATE_ROOM_TOMBSTONE.equals(eventType)) {
                mRoomTombstoneContent = JsonUtils.toRoomTombstoneContent(contentToConsider);
            } else if (Event.EVENT_TYPE_STATE_PINNED_EVENT.equals(eventType)) {
                mRoomPinnedEventsContent = JsonUtils.toRoomPinnedEventsContent(contentToConsider);
            }
            // same the latest room state events
            // excepts the membership ones
            // they are saved elsewhere
            if (!TextUtils.isEmpty(eventType) && !Event.EVENT_TYPE_STATE_ROOM_MEMBER.equals(eventType)) {
                List<Event> eventsList = mStateEvents.get(eventType);

                if (null == eventsList) {
                    eventsList = new ArrayList<>();
                    mStateEvents.put(eventType, eventsList);
                }

                eventsList.add(event);
            }

        } catch (Exception e) {
            Log.e(LOG_TAG, "applyState failed with error " + e.getMessage(), e);
        }

        return true;
    }

    /**
     * @return true if the room is a public one
     */
    public boolean isPublic() {
        return TextUtils.equals((null != visibility) ? visibility : join_rule, RoomDirectoryVisibility.DIRECTORY_VISIBILITY_PUBLIC);
    }

    /**
     * Return an unique display name of the member userId.
     *
     * @param userId the user id
     * @return unique display name
     */
    public String getMemberName(String userId) {
        // sanity check
        if (null == userId) {
            return null;
        }

        String displayName;

        synchronized (this) {
            if (null == mMemberDisplayNameByUserId) {
                mMemberDisplayNameByUserId = new HashMap<>();
            }
            displayName = mMemberDisplayNameByUserId.get(userId);
        }

        if (null != displayName) {
            return displayName;
        }

        // Get the user display name from the member list of the room
        RoomMember member = getMember(userId);

        // Do not consider null display name
        if ((null != member) && !TextUtils.isEmpty(member.displayname)) {
            displayName = member.displayname;

            synchronized (this) {
                List<String> matrixIds = new ArrayList<>();

                // Disambiguate users who have the same display name in the room
                for (RoomMember aMember : mMembers.values()) {
                    if (displayName.equals(aMember.displayname)) {
                        matrixIds.add(aMember.getUserId());
                    }
                }

                // if several users have the same display name
                // index it i.e bob (<Matrix id>)
                if (matrixIds.size() > 1) {
                    displayName += " (" + userId + ")";
                }
            }
        } else if ((null != member) && TextUtils.equals(member.membership, RoomMember.MEMBERSHIP_INVITE)) {
            User user = ((MXDataHandler) mDataHandler).getUser(userId);

            if (null != user) {
                displayName = user.displayname;
            }
        }

        if (null == displayName) {
            // By default, use the user ID
            displayName = userId;
        }

        mMemberDisplayNameByUserId.put(userId, displayName);

        return displayName;
    }

    @Override
    public void readExternal(ObjectInput input) throws IOException, ClassNotFoundException {
        if (input.readBoolean()) {
            roomId = input.readUTF();
        }

        if (input.readBoolean()) {
            powerLevels = (PowerLevels) input.readObject();
        }

        if (input.readBoolean()) {
            aliases = (List<String>) input.readObject();
        }

        List<Event> roomAliasesEvents = (List<Event>) input.readObject();
        for (Event e : roomAliasesEvents) {
            mRoomAliases.put(e.stateKey, e);
        }

        mAliasesByDomain = (Map<String, List<String>>) input.readObject();

        if (input.readBoolean()) {
            mMergedAliasesList = (List<String>) input.readObject();
        }

        Map<String, List<Event>> stateEvents = (Map<String, List<Event>>) input.readObject();
        if (null != stateEvents) {
            mStateEvents = new HashMap<>(stateEvents);
        }

        if (input.readBoolean()) {
            canonicalAlias = input.readUTF();
        }

        if (input.readBoolean()) {
            name = input.readUTF();
        }

        if (input.readBoolean()) {
            topic = input.readUTF();
        }

        if (input.readBoolean()) {
            url = input.readUTF();
        }

        if (input.readBoolean()) {
            avatar_url = input.readUTF();
        }

        if (input.readBoolean()) {
            mRoomCreateContent = (RoomCreateContent) input.readObject();
        }

        if (input.readBoolean()) {
            mRoomPinnedEventsContent = (RoomPinnedEventsContent) input.readObject();
        }

        if (input.readBoolean()) {
            join_rule = input.readUTF();
        }

        if (input.readBoolean()) {
            guest_access = input.readUTF();
        }

        if (input.readBoolean()) {
            history_visibility = input.readUTF();
        }

        if (input.readBoolean()) {
            visibility = input.readUTF();
        }

        if (input.readBoolean()) {
            algorithm = input.readUTF();
        }

        mNotificationCount = input.readInt();
        mHighlightCount = input.readInt();

        if (input.readBoolean()) {
            token = input.readUTF();
        }

        List<RoomMember> members = (List<RoomMember>) input.readObject();
        for (RoomMember r : members) {
            mMembers.put(r.getUserId(), r);
        }

        List<RoomThirdPartyInvite> invites = (List<RoomThirdPartyInvite>) input.readObject();
        for (RoomThirdPartyInvite i : invites) {
            mThirdPartyInvites.put(i.token, i);
        }

        List<RoomMember> inviteTokens = (List<RoomMember>) input.readObject();
        for (RoomMember r : inviteTokens) {
            mMembersWithThirdPartyInviteTokenCache.put(r.getThirdPartyInviteToken(), r);
        }

        mIsLive = input.readBoolean();

        mAllMembersAreLoaded = input.readBoolean();

        if (input.readBoolean()) {
            groups = (List<String>) input.readObject();
        }

        if (input.readBoolean()) {
            mRoomTombstoneContent = (RoomTombstoneContent) input.readObject();
        }
    }

    @Override
    public void writeExternal(ObjectOutput output) throws IOException {
        output.writeBoolean(null != roomId);
        if (null != roomId) {
            output.writeUTF(roomId);
        }

        output.writeBoolean(null != powerLevels);
        if (null != powerLevels) {
            output.writeObject(powerLevels);
        }

        output.writeBoolean(null != aliases);
        if (null != aliases) {
            output.writeObject(aliases);
        }

        output.writeObject(new ArrayList<>(mRoomAliases.values()));

        output.writeObject(mAliasesByDomain);

        output.writeBoolean(null != mMergedAliasesList);
        if (null != mMergedAliasesList) {
            output.writeObject(mMergedAliasesList);
        }

        output.writeObject(mStateEvents);

        output.writeBoolean(null != canonicalAlias);
        if (null != canonicalAlias) {
            output.writeUTF(canonicalAlias);
        }

        output.writeBoolean(null != name);
        if (null != name) {
            output.writeUTF(name);
        }

        output.writeBoolean(null != topic);
        if (null != topic) {
            output.writeUTF(topic);
        }

        output.writeBoolean(null != url);
        if (null != url) {
            output.writeUTF(url);
        }

        output.writeBoolean(null != avatar_url);
        if (null != avatar_url) {
            output.writeUTF(avatar_url);
        }

        output.writeBoolean(null != mRoomCreateContent);
        if (null != mRoomCreateContent) {
            output.writeObject(mRoomCreateContent);
        }

        output.writeBoolean(null != mRoomPinnedEventsContent);
        if (null != mRoomPinnedEventsContent) {
            output.writeObject(mRoomPinnedEventsContent);
        }

        output.writeBoolean(null != join_rule);
        if (null != join_rule) {
            output.writeUTF(join_rule);
        }

        output.writeBoolean(null != guest_access);
        if (null != guest_access) {
            output.writeUTF(guest_access);
        }

        output.writeBoolean(null != history_visibility);
        if (null != history_visibility) {
            output.writeUTF(history_visibility);
        }

        output.writeBoolean(null != visibility);
        if (null != visibility) {
            output.writeUTF(visibility);
        }

        output.writeBoolean(null != algorithm);
        if (null != algorithm) {
            output.writeUTF(algorithm);
        }

        output.writeInt(mNotificationCount);
        output.writeInt(mHighlightCount);

        output.writeBoolean(null != token);
        if (null != token) {
            output.writeUTF(token);
        }

        output.writeObject(new ArrayList<>(mMembers.values()));
        output.writeObject(new ArrayList<>(mThirdPartyInvites.values()));
        output.writeObject(new ArrayList<>(mMembersWithThirdPartyInviteTokenCache.values()));

        output.writeBoolean(mIsLive);

        output.writeBoolean(mAllMembersAreLoaded);

        output.writeBoolean(null != groups);
        if (null != groups) {
            output.writeObject(groups);
        }

        output.writeBoolean(null != mRoomTombstoneContent);
        if (null != mRoomTombstoneContent) {
            output.writeObject(mRoomTombstoneContent);
        }
    }
}