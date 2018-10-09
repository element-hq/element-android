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
package im.vector.matrix.android.internal.legacy.listeners;

import im.vector.matrix.android.internal.legacy.data.MyUser;
import im.vector.matrix.android.internal.legacy.data.RoomState;
import im.vector.matrix.android.internal.legacy.rest.model.Event;
import im.vector.matrix.android.internal.legacy.rest.model.MatrixError;
import im.vector.matrix.android.internal.legacy.rest.model.User;
import im.vector.matrix.android.internal.legacy.rest.model.bingrules.BingRule;

import java.util.List;

public interface IMXEventListener {
    /**
     * The store is ready.
     */
    void onStoreReady();

    /**
     * User presence was updated.
     *
     * @param event The presence event.
     * @param user  The new user value.
     */
    void onPresenceUpdate(Event event, User user);

    /**
     * The self user has been updated (display name, avatar url...).
     *
     * @param myUser The updated myUser
     */
    void onAccountInfoUpdate(MyUser myUser);

    /**
     * The ignored users list has been updated.
     */
    void onIgnoredUsersListUpdate();

    /**
     * The direct chat rooms list have been updated.
     */
    void onDirectMessageChatRoomsListUpdate();

    /**
     * A live room event was received.
     *
     * @param event     the event
     * @param roomState the room state right before the event
     */
    void onLiveEvent(Event event, RoomState roomState);

    /**
     * The live events from a chunk are performed.
     *
     * @param fromToken the start sync token
     * @param toToken   the up-to sync token
     */
    void onLiveEventsChunkProcessed(String fromToken, String toToken);

    /**
     * A received event fulfills the bing rules
     * The first matched bing rule is provided in paramater to perform
     * dedicated action like playing a notification sound.
     *
     * @param event     the event
     * @param roomState the room state right before the event
     * @param bingRule  the bing rule
     */
    void onBingEvent(Event event, RoomState roomState, BingRule bingRule);

    /**
     * The state of an event has been updated.
     *
     * @param event the event
     */
    void onEventSentStateUpdated(Event event);

    /**
     * An event has been sent.
     * prevEventId defines the event id set before getting the server new one.
     *
     * @param event       the event
     * @param prevEventId the previous eventId
     */
    void onEventSent(Event event, String prevEventId);

    /**
     * An event has been decrypted
     *
     * @param event the decrypted event
     */
    void onEventDecrypted(Event event);

    /**
     * The bing rules have been updated
     */
    void onBingRulesUpdate();

    /**
     * The initial sync is complete and the store can be queried for current state.
     *
     * @param toToken the up-to sync token
     */
    void onInitialSyncComplete(String toToken);

    /**
     * The sync has encountered an error
     *
     * @param matrixError the error
     */
    void onSyncError(MatrixError matrixError);

    /**
     * The crypto sync is complete
     */
    void onCryptoSyncComplete();

    /**
     * A new room has been created.
     *
     * @param roomId the roomID
     */
    void onNewRoom(String roomId);

    /**
     * The user joined a room.
     *
     * @param roomId the roomID
     */
    void onJoinRoom(String roomId);

    /**
     * The messages of an existing room has been flushed during server sync.
     * This flush may be due to a limited timeline in the room sync, or the redaction of a state event.
     *
     * @param roomId the room Id
     */
    void onRoomFlush(String roomId);

    /**
     * The room data has been internally updated.
     * It could be triggered when a request failed.
     *
     * @param roomId the roomID
     */
    void onRoomInternalUpdate(String roomId);

    /**
     * The notification count of a dedicated room
     * has been updated.
     *
     * @param roomId the room ID
     */
    void onNotificationCountUpdate(String roomId);

    /**
     * The user left the room.
     *
     * @param roomId the roomID
     */
    void onLeaveRoom(String roomId);

    /**
     * The user has been kicked or banned.
     *
     * @param roomId the roomID
     */
    void onRoomKick(String roomId);

    /**
     * A receipt event has been received.
     * It could be triggered when a request failed.
     *
     * @param roomId    the roomID
     * @param senderIds the list of the
     */
    void onReceiptEvent(String roomId, List<String> senderIds);

    /**
     * A Room Tag event has been received.
     *
     * @param roomId the roomID
     */
    void onRoomTagEvent(String roomId);

    /**
     * A read marker has been updated
     *
     * @param roomId thr room id.
     */
    void onReadMarkerEvent(String roomId);

    /**
     * An event was sent to the current device.
     *
     * @param event the event
     */
    void onToDeviceEvent(Event event);

    /**
     * The user has been invited to a new group.
     *
     * @param groupId the group id
     */
    void onNewGroupInvitation(String groupId);

    /**
     * A group has been joined.
     *
     * @param groupId the group id
     */
    void onJoinGroup(String groupId);

    /**
     * A group has been left.
     *
     * @param groupId the group id
     */
    void onLeaveGroup(String groupId);

    /**
     * The group file has been updated.
     *
     * @param groupId the group id
     */
    void onGroupProfileUpdate(String groupId);

    /**
     * The group rooms list has been updated.
     *
     * @param groupId the group id
     */
    void onGroupRoomsListUpdate(String groupId);

    /**
     * The group users id list has been updated.
     *
     * @param groupId the group id
     */
    void onGroupUsersListUpdate(String groupId);

    /**
     * The group invited users id list has been updated.
     *
     * @param groupId the group id
     */
    void onGroupInvitedUsersListUpdate(String groupId);
}

