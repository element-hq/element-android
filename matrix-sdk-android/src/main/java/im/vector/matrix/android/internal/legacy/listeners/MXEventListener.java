/*
 * Copyright 2014 OpenMarket Ltd
 * Copyright 2017 Vector Creations Ltd
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

/**
 * A no-op class implementing {@link IMXEventListener} so listeners can just implement the methods
 * that they require.
 */
public class MXEventListener implements IMXEventListener {

    @Override
    public void onStoreReady() {
    }

    @Override
    public void onPresenceUpdate(Event event, User user) {
    }

    @Override
    public void onAccountInfoUpdate(MyUser myUser) {
    }

    @Override
    public void onLiveEvent(Event event, RoomState roomState) {

    }

    @Override
    public void onLiveEventsChunkProcessed(String fromToken, String toToken) {
    }

    @Override
    public void onBingEvent(Event event, RoomState roomState, BingRule bingRule) {
    }

    @Override
    public void onEventSent(final Event event, final String prevEventId) {
    }

    @Override
    public void onEventSentStateUpdated(Event event) {
    }

    @Override
    public void onEventDecrypted(Event event) {
    }

    @Override
    public void onBingRulesUpdate() {
    }

    @Override
    public void onInitialSyncComplete(String toToken) {
    }

    @Override
    public void onSyncError(MatrixError matrixError) {
    }

    @Override
    public void onCryptoSyncComplete() {
    }

    @Override
    public void onNewRoom(String roomId) {
    }

    @Override
    public void onJoinRoom(String roomId) {
    }

    @Override
    public void onRoomInternalUpdate(String roomId) {
    }

    @Override
    public void onNotificationCountUpdate(String roomId) {
    }

    @Override
    public void onLeaveRoom(String roomId) {
    }

    @Override
    public void onRoomKick(String roomId) {
    }

    @Override
    public void onReceiptEvent(String roomId, List<String> senderIds) {
    }

    @Override
    public void onRoomTagEvent(String roomId) {
    }

    @Override
    public void onReadMarkerEvent(String roomId) {
    }

    @Override
    public void onRoomFlush(String roomId) {
    }

    @Override
    public void onIgnoredUsersListUpdate() {
    }

    @Override
    public void onToDeviceEvent(Event event) {
    }

    @Override
    public void onDirectMessageChatRoomsListUpdate() {
    }

    @Override
    public void onNewGroupInvitation(String groupId) {
    }

    @Override
    public void onJoinGroup(String groupId) {
    }

    @Override
    public void onLeaveGroup(String groupId) {
    }

    @Override
    public void onGroupProfileUpdate(String groupId) {
    }

    @Override
    public void onGroupRoomsListUpdate(String groupId) {
    }

    @Override
    public void onGroupUsersListUpdate(String groupId) {
    }

    @Override
    public void onGroupInvitedUsersListUpdate(String groupId) {
    }
}
