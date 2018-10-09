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

package im.vector.matrix.android.internal.legacy.data;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import im.vector.matrix.android.internal.legacy.call.MXCallsManager;
import im.vector.matrix.android.internal.legacy.rest.model.Event;
import im.vector.matrix.android.internal.legacy.rest.model.EventContent;
import im.vector.matrix.android.internal.legacy.rest.model.RoomMember;
import im.vector.matrix.android.internal.legacy.rest.model.message.Message;
import im.vector.matrix.android.internal.legacy.rest.model.sync.RoomSyncSummary;
import im.vector.matrix.android.internal.legacy.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Stores summarised information about the room.
 */
public class RoomSummary implements java.io.Serializable {
    private static final String LOG_TAG = RoomSummary.class.getSimpleName();

    private static final long serialVersionUID = -3683013938626566489L;

    // list of supported types
    private static final List<String> sSupportedType = Arrays.asList(
            Event.EVENT_TYPE_STATE_ROOM_TOPIC,
            Event.EVENT_TYPE_MESSAGE_ENCRYPTED,
            Event.EVENT_TYPE_MESSAGE_ENCRYPTION,
            Event.EVENT_TYPE_STATE_ROOM_NAME,
            Event.EVENT_TYPE_STATE_ROOM_MEMBER,
            Event.EVENT_TYPE_STATE_ROOM_CREATE,
            Event.EVENT_TYPE_STATE_HISTORY_VISIBILITY,
            Event.EVENT_TYPE_STATE_ROOM_THIRD_PARTY_INVITE,
            Event.EVENT_TYPE_STICKER);

    // List of known unsupported types
    private static final List<String> sKnownUnsupportedType = Arrays.asList(
            Event.EVENT_TYPE_TYPING,
            Event.EVENT_TYPE_STATE_ROOM_POWER_LEVELS,
            Event.EVENT_TYPE_STATE_ROOM_JOIN_RULES,
            Event.EVENT_TYPE_STATE_CANONICAL_ALIAS,
            Event.EVENT_TYPE_STATE_ROOM_ALIASES,
            Event.EVENT_TYPE_URL_PREVIEW,
            Event.EVENT_TYPE_STATE_RELATED_GROUPS,
            Event.EVENT_TYPE_STATE_ROOM_GUEST_ACCESS,
            Event.EVENT_TYPE_REDACTION);

    private String mRoomId = null;
    private String mTopic = null;
    private Event mLatestReceivedEvent = null;

    // the room state is only used to check
    // 1- the invitation status
    // 2- the members display name
    private transient RoomState mLatestRoomState = null;

    // defines the latest read message
    private String mReadReceiptEventId;

    // the read marker event id
    private String mReadMarkerEventId;

    private Set<String> mRoomTags;

    // counters
    public int mUnreadEventsCount;
    public int mNotificationCount;
    public int mHighlightsCount;

    // invitation status
    // retrieved at initial sync
    // the roomstate is not always known
    private String mInviterUserId = null;

    // retrieved from the roomState
    private String mInviterName = null;

    private String mUserId = null;

    // Info from sync, depending on the room position in the sync
    private String mUserMembership;

    /**
     * Tell if the room is a user conference user one
     */
    private Boolean mIsConferenceUserRoom = null;

    /**
     * Data from RoomSyncSummary
     */
    private List<String> mHeroes = new ArrayList<>();

    private int mJoinedMembersCountFromSyncRoomSummary;

    private int mInvitedMembersCountFromSyncRoomSummary;

    public RoomSummary() {
    }

    /**
     * Create a room summary
     *
     * @param fromSummary the summary source
     * @param event       the latest event of the room
     * @param roomState   the room state - used to display the event
     * @param userId      our own user id - used to display the room name
     */
    public RoomSummary(@Nullable RoomSummary fromSummary,
                       Event event,
                       RoomState roomState,
                       String userId) {
        mUserId = userId;

        if (null != roomState) {
            setRoomId(roomState.roomId);
        }

        if ((null == getRoomId()) && (null != event)) {
            setRoomId(event.roomId);
        }

        setLatestReceivedEvent(event, roomState);

        // if no summary is provided
        if (null == fromSummary) {
            if (null != event) {
                setReadMarkerEventId(event.eventId);
                setReadReceiptEventId(event.eventId);
            }

            if (null != roomState) {
                setHighlightCount(roomState.getHighlightCount());
                setNotificationCount(roomState.getHighlightCount());
            }
            setUnreadEventsCount(Math.max(getHighlightCount(), getNotificationCount()));
        } else {
            // else use the provided summary data
            setReadMarkerEventId(fromSummary.getReadMarkerEventId());
            setReadReceiptEventId(fromSummary.getReadReceiptEventId());
            setUnreadEventsCount(fromSummary.getUnreadEventsCount());
            setHighlightCount(fromSummary.getHighlightCount());
            setNotificationCount(fromSummary.getNotificationCount());

            mHeroes.addAll(fromSummary.mHeroes);
            mJoinedMembersCountFromSyncRoomSummary = fromSummary.mJoinedMembersCountFromSyncRoomSummary;
            mInvitedMembersCountFromSyncRoomSummary = fromSummary.mInvitedMembersCountFromSyncRoomSummary;

            mUserMembership = fromSummary.mUserMembership;
        }
    }

    /**
     * Test if the event can be summarized.
     * Some event types are not yet supported.
     *
     * @param event the event to test.
     * @return true if the event can be summarized
     */
    public static boolean isSupportedEvent(Event event) {
        String type = event.getType();
        boolean isSupported = false;

        // check if the msgtype is supported
        if (TextUtils.equals(Event.EVENT_TYPE_MESSAGE, type)) {
            try {
                JsonObject eventContent = event.getContentAsJsonObject();
                String msgType = "";

                JsonElement element = eventContent.get("msgtype");

                if (null != element) {
                    msgType = element.getAsString();
                }

                isSupported = TextUtils.equals(msgType, Message.MSGTYPE_TEXT)
                        || TextUtils.equals(msgType, Message.MSGTYPE_EMOTE)
                        || TextUtils.equals(msgType, Message.MSGTYPE_NOTICE)
                        || TextUtils.equals(msgType, Message.MSGTYPE_IMAGE)
                        || TextUtils.equals(msgType, Message.MSGTYPE_AUDIO)
                        || TextUtils.equals(msgType, Message.MSGTYPE_VIDEO)
                        || TextUtils.equals(msgType, Message.MSGTYPE_FILE);

                if (!isSupported && !TextUtils.isEmpty(msgType)) {
                    Log.e(LOG_TAG, "isSupportedEvent : Unsupported msg type " + msgType);
                }
            } catch (Exception e) {
                Log.e(LOG_TAG, "isSupportedEvent failed " + e.getMessage(), e);
            }
        } else if (TextUtils.equals(Event.EVENT_TYPE_MESSAGE_ENCRYPTED, type)) {
            isSupported = event.hasContentFields();
        } else if (TextUtils.equals(Event.EVENT_TYPE_STATE_ROOM_MEMBER, type)) {
            JsonObject eventContentAsJsonObject = event.getContentAsJsonObject();

            if (null != eventContentAsJsonObject) {
                if (eventContentAsJsonObject.entrySet().isEmpty()) {
                    Log.d(LOG_TAG, "isSupportedEvent : room member with no content is not supported");
                } else {
                    // do not display the avatar / display name update
                    EventContent prevEventContent = event.getPrevContent();
                    EventContent eventContent = event.getEventContent();

                    String membership = null;
                    String preMembership = null;

                    if (eventContent != null) {
                        membership = eventContent.membership;
                    }

                    if (prevEventContent != null) {
                        preMembership = prevEventContent.membership;
                    }

                    isSupported = !TextUtils.equals(membership, preMembership);

                    if (!isSupported) {
                        Log.d(LOG_TAG, "isSupportedEvent : do not support avatar display name update");
                    }
                }
            }
        } else {
            isSupported = sSupportedType.contains(type)
                    || (event.isCallEvent() && !TextUtils.isEmpty(type) && !Event.EVENT_TYPE_CALL_CANDIDATES.equals(type));
        }

        if (!isSupported) {
            // some events are known to be never traced
            // avoid warning when it is not required.
            if (!sKnownUnsupportedType.contains(type)) {
                Log.e(LOG_TAG, "isSupportedEvent :  Unsupported event type " + type);
            }
        }

        return isSupported;
    }

    /**
     * @return the user id
     */
    public String getUserId() {
        return mUserId;
    }

    /**
     * @return the room id
     */
    public String getRoomId() {
        return mRoomId;
    }

    /**
     * @return the topic.
     */
    public String getRoomTopic() {
        return mTopic;
    }

    /**
     * @return the room summary event.
     */
    public Event getLatestReceivedEvent() {
        return mLatestReceivedEvent;
    }

    /**
     * @return the dedicated room state.
     */
    public RoomState getLatestRoomState() {
        return mLatestRoomState;
    }

    /**
     * @return true if the current user is invited
     */
    public boolean isInvited() {
        return RoomMember.MEMBERSHIP_INVITE.equals(mUserMembership);
    }

    /**
     * To call when the room is in the invited section of the sync response
     */
    public void setIsInvited() {
        mUserMembership = RoomMember.MEMBERSHIP_INVITE;
    }

    /**
     * To call when the room is in the joined section of the sync response
     */
    public void setIsJoined() {
        mUserMembership = RoomMember.MEMBERSHIP_JOIN;
    }

    /**
     * @return true if the current user is invited
     */
    public boolean isJoined() {
        return RoomMember.MEMBERSHIP_JOIN.equals(mUserMembership);
    }

    /**
     * @return the inviter user id.
     */
    public String getInviterUserId() {
        return mInviterUserId;
    }

    /**
     * Set the room's {@link org.matrix.androidsdk.rest.model.Event#EVENT_TYPE_STATE_ROOM_TOPIC}.
     *
     * @param topic The topic
     * @return This summary for chaining calls.
     */
    public RoomSummary setTopic(String topic) {
        mTopic = topic;
        return this;
    }

    /**
     * Set the room's ID..
     *
     * @param roomId The room ID
     * @return This summary for chaining calls.
     */
    public RoomSummary setRoomId(String roomId) {
        mRoomId = roomId;
        return this;
    }

    /**
     * Set the latest tracked event (e.g. the latest m.room.message)
     *
     * @param event     The most-recent event.
     * @param roomState The room state
     * @return This summary for chaining calls.
     */
    public RoomSummary setLatestReceivedEvent(Event event, RoomState roomState) {
        setLatestReceivedEvent(event);
        setLatestRoomState(roomState);

        if (null != roomState) {
            setTopic(roomState.topic);
        }
        return this;
    }

    /**
     * Set the latest tracked event (e.g. the latest m.room.message)
     *
     * @param event The most-recent event.
     * @return This summary for chaining calls.
     */
    public RoomSummary setLatestReceivedEvent(Event event) {
        mLatestReceivedEvent = event;
        return this;
    }

    /**
     * Set the latest RoomState
     *
     * @param roomState The room state of the latest event.
     * @return This summary for chaining calls.
     */
    public RoomSummary setLatestRoomState(RoomState roomState) {
        mLatestRoomState = roomState;

        // Keep this code for compatibility?
        boolean isInvited = false;

        // check for the invitation status
        if (null != mLatestRoomState) {
            RoomMember member = mLatestRoomState.getMember(mUserId);
            isInvited = (null != member) && RoomMember.MEMBERSHIP_INVITE.equals(member.membership);
        }
        // when invited, the only received message should be the invitation one
        if (isInvited) {
            mInviterName = null;

            if (null != mLatestReceivedEvent) {
                mInviterName = mInviterUserId = mLatestReceivedEvent.getSender();

                // try to retrieve a display name
                if (null != mLatestRoomState) {
                    mInviterName = mLatestRoomState.getMemberName(mLatestReceivedEvent.getSender());
                }
            }
        } else {
            mInviterUserId = mInviterName = null;
        }

        return this;
    }

    /**
     * Set the read receipt event Id
     *
     * @param eventId the read receipt event id.
     */
    public void setReadReceiptEventId(String eventId) {
        Log.d(LOG_TAG, "## setReadReceiptEventId() : " + eventId + " roomId " + getRoomId());
        mReadReceiptEventId = eventId;
    }

    /**
     * @return the read receipt event id
     */
    public String getReadReceiptEventId() {
        return mReadReceiptEventId;
    }

    /**
     * Set the read marker event Id
     *
     * @param eventId the read marker event id.
     */
    public void setReadMarkerEventId(String eventId) {
        Log.d(LOG_TAG, "## setReadMarkerEventId() : " + eventId + " roomId " + getRoomId());

        if (TextUtils.isEmpty(eventId)) {
            Log.e(LOG_TAG, "## setReadMarkerEventId') : null mReadMarkerEventId, in " + getRoomId());
        }

        mReadMarkerEventId = eventId;
    }

    /**
     * @return the read receipt event id
     */
    public String getReadMarkerEventId() {
        if (TextUtils.isEmpty(mReadMarkerEventId)) {
            Log.e(LOG_TAG, "## getReadMarkerEventId') : null mReadMarkerEventId, in " + getRoomId());
            mReadMarkerEventId = getReadReceiptEventId();
        }

        return mReadMarkerEventId;
    }

    /**
     * Update the unread message counter
     *
     * @param count the unread events count.
     */
    public void setUnreadEventsCount(int count) {
        Log.d(LOG_TAG, "## setUnreadEventsCount() : " + count + " roomId " + getRoomId());
        mUnreadEventsCount = count;
    }

    /**
     * @return the unread events count
     */
    public int getUnreadEventsCount() {
        return mUnreadEventsCount;
    }

    /**
     * Update the notification counter
     *
     * @param count the notification counter
     */
    public void setNotificationCount(int count) {
        Log.d(LOG_TAG, "## setNotificationCount() : " + count + " roomId " + getRoomId());
        mNotificationCount = count;
    }

    /**
     * @return the notification count
     */
    public int getNotificationCount() {
        return mNotificationCount;
    }

    /**
     * Update the highlight counter
     *
     * @param count the highlight counter
     */
    public void setHighlightCount(int count) {
        Log.d(LOG_TAG, "## setHighlightCount() : " + count + " roomId " + getRoomId());
        mHighlightsCount = count;
    }

    /**
     * @return the highlight count
     */
    public int getHighlightCount() {
        return mHighlightsCount;
    }

    /**
     * @return the room tags
     */
    public Set<String> getRoomTags() {
        return mRoomTags;
    }

    /**
     * Update the room tags
     *
     * @param roomTags the room tags
     */
    public void setRoomTags(final Set<String> roomTags) {
        if (roomTags != null) {
            // wraps the set into a serializable one
            mRoomTags = new HashSet<>(roomTags);
        } else {
            mRoomTags = new HashSet<>();
        }
    }

    public boolean isConferenceUserRoom() {
        // test if it is not yet initialized
        if (null == mIsConferenceUserRoom) {

            mIsConferenceUserRoom = false;

            // FIXME LazyLoading Heroes does not contains me
            // FIXME I'ms not sure this code will work anymore

            Collection<String> membersId = getHeroes();

            // works only with 1:1 room
            if (2 == membersId.size()) {
                for (String userId : membersId) {
                    if (MXCallsManager.isConferenceUserId(userId)) {
                        mIsConferenceUserRoom = true;
                        break;
                    }
                }
            }
        }

        return mIsConferenceUserRoom;
    }

    public void setIsConferenceUserRoom(boolean isConferenceUserRoom) {
        mIsConferenceUserRoom = isConferenceUserRoom;
    }

    public void setRoomSyncSummary(@NonNull RoomSyncSummary roomSyncSummary) {
        if (roomSyncSummary.heroes != null) {
            mHeroes.clear();
            mHeroes.addAll(roomSyncSummary.heroes);
        }

        if (roomSyncSummary.joinedMembersCount != null) {
            // Update the value
            mJoinedMembersCountFromSyncRoomSummary = roomSyncSummary.joinedMembersCount;
        }

        if (roomSyncSummary.invitedMembersCount != null) {
            // Update the value
            mInvitedMembersCountFromSyncRoomSummary = roomSyncSummary.invitedMembersCount;
        }
    }

    @NonNull
    public List<String> getHeroes() {
        return mHeroes;
    }

    public int getNumberOfJoinedMembers() {
        return mJoinedMembersCountFromSyncRoomSummary;
    }

    public int getNumberOfInvitedMembers() {
        return mInvitedMembersCountFromSyncRoomSummary;
    }
}
