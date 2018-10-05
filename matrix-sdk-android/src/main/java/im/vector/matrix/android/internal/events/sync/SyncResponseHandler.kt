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
 *//*

package im.vector.matrix.android.internal.events.sync

import android.text.TextUtils
import android.util.Log
import im.vector.matrix.android.api.events.Event
import im.vector.matrix.android.api.events.EventType
import im.vector.matrix.android.internal.events.sync.data.SyncResponse
import java.util.*

class SyncResponseHandler {

    */
/**
     * Manage the sync accountData field
     *
     * @param accountData   the account data
     * @param isInitialSync true if it is an initial sync response
     *//*

    private fun manageAccountData(accountData: Map<String, Any>?, isInitialSync: Boolean) {
        try {
            if (accountData!!.containsKey("events")) {
                val events = accountData["events"] as List<Map<String, Any>>

                if (!events.isEmpty()) {
                    // ignored users list
                    manageIgnoredUsers(events, isInitialSync)
                    // push rules
                    managePushRulesUpdate(events)
                    // direct messages rooms
                    manageDirectChatRooms(events, isInitialSync)
                    // URL preview
                    manageUrlPreview(events)
                    // User widgets
                    manageUserWidgets(events)
                }
            }
        } catch (e: Exception) {

        }

    }

    */
/**
     * Refresh the push rules from the account data events list
     *
     * @param events the account data events.
     *//*

    private fun managePushRulesUpdate(events: List<Map<String, Any>>) {
        for (event in events) {
            val type = event["type"] as String
            if (TextUtils.equals(type, "m.push_rules")) {
                if (event.containsKey("content")) {
                    val gson = JsonUtils.getGson(false)
                    // convert the data to PushRulesResponse
                    // because BingRulesManager supports only PushRulesResponse
                    val element = gson.toJsonTree(event["content"])
                    getBingRulesManager().buildRules(gson.fromJson(element, PushRulesResponse::class.java))
                    // warn the client that the push rules have been updated
                    onBingRulesUpdate()
                }
                return
            }
        }
    }

    */
/**
     * Check if the ignored users list is updated
     *
     * @param events the account data events list
     *//*

    private fun manageIgnoredUsers(events: List<Map<String, Any>>, isInitialSync: Boolean) {
        val newIgnoredUsers = ignoredUsers(events)

        if (null != newIgnoredUsers) {
            val curIgnoredUsers = getIgnoredUserIds()

            // the both lists are not empty
            if (0 != newIgnoredUsers.size || 0 != curIgnoredUsers.size) {
                // check if the ignored users list has been updated
                if (newIgnoredUsers.size != curIgnoredUsers.size || !newIgnoredUsers.containsAll(curIgnoredUsers)) {
                    // update the store
                    mStore.setIgnoredUserIdsList(newIgnoredUsers)
                    mIgnoredUserIdsList = newIgnoredUsers

                    if (!isInitialSync) {
                        // warn there is an update
                        onIgnoredUsersListUpdate()
                    }
                }
            }
        }
    }

    */
/**
     * Extract the ignored users list from the account data events list..
     *
     * @param events the account data events list.
     * @return the ignored users list. null means that there is no defined user ids list.
     *//*

    private fun ignoredUsers(events: List<Map<String, Any>>): List<String>? {
        var ignoredUsers: List<String>? = null

        if (0 != events.size) {
            for (event in events) {
                val type = event["type"] as String

                if (TextUtils.equals(type, AccountDataRestClient.ACCOUNT_DATA_TYPE_IGNORED_USER_LIST)) {
                    if (event.containsKey("content")) {
                        val contentDict = event["content"] as Map<String, Any>

                        if (contentDict.containsKey(AccountDataRestClient.ACCOUNT_DATA_KEY_IGNORED_USERS)) {
                            val ignored_users = contentDict[AccountDataRestClient.ACCOUNT_DATA_KEY_IGNORED_USERS] as Map<String, Any>

                            if (null != ignored_users) {
                                ignoredUsers = ArrayList(ignored_users.keys)
                            }
                        }
                    }
                }
            }

        }

        return ignoredUsers
    }


    */
/**
     * Extract the direct chat rooms list from the dedicated events.
     *
     * @param events the account data events list.
     *//*

    private fun manageDirectChatRooms(events: List<Map<String, Any>>, isInitialSync: Boolean) {
        if (0 != events.size) {
            for (event in events) {
                val type = event["type"] as String

                if (TextUtils.equals(type, AccountDataRestClient.ACCOUNT_DATA_TYPE_DIRECT_MESSAGES)) {
                    if (event.containsKey("content")) {
                        val contentDict = event["content"] as Map<String, List<String>>

                        Log.d(LOG_TAG, "## manageDirectChatRooms() : update direct chats map$contentDict")

                        mStore.setDirectChatRoomsDict(contentDict)

                        // reset the current list of the direct chat roomIDs
                        // to update it
                        mLocalDirectChatRoomIdsList = null

                        if (!isInitialSync) {
                            // warn there is an update
                            onDirectMessageChatRoomsListUpdate()
                        }
                    }
                }
            }
        }
    }

    */
/**
     * Manage the URL preview flag
     *
     * @param events the events list
     *//*

    private fun manageUrlPreview(events: List<Map<String, Any>>) {
        if (0 != events.size) {
            for (event in events) {
                val type = event["type"] as String

                if (TextUtils.equals(type, AccountDataRestClient.ACCOUNT_DATA_TYPE_PREVIEW_URLS)) {
                    if (event.containsKey("content")) {
                        val contentDict = event["content"] as Map<String, Any>

                        Log.d(LOG_TAG, "## manageUrlPreview() : $contentDict")
                        var enable = true
                        if (contentDict.containsKey(AccountDataRestClient.ACCOUNT_DATA_KEY_URL_PREVIEW_DISABLE)) {
                            enable = !(contentDict[AccountDataRestClient.ACCOUNT_DATA_KEY_URL_PREVIEW_DISABLE] as Boolean)
                        }

                        mStore.setURLPreviewEnabled(enable)
                    }
                }
            }
        }
    }

    */
/**
     * Manage the user widgets
     *
     * @param events the events list
     *//*

    private fun manageUserWidgets(events: List<Map<String, Any>>) {
        if (0 != events.size) {
            for (event in events) {
                val type = event["type"] as String

                if (TextUtils.equals(type, AccountDataRestClient.ACCOUNT_DATA_TYPE_WIDGETS)) {
                    if (event.containsKey("content")) {
                        val contentDict = event["content"] as Map<String, Any>

                        Log.d(LOG_TAG, "## manageUserWidgets() : $contentDict")

                        mStore.setUserWidgets(contentDict)
                    }
                }
            }
        }
    }

    //================================================================================
    // Sync V2
    //================================================================================

    */
/**
     * Handle a presence event.
     *
     * @param presenceEvent the presence event.
     *//*

    private fun handlePresenceEvent(presenceEvent: Event) {
        // Presence event
        if (presenceEvent.type == EventType.PRESENCE) {
            val userPresence = presenceEvent.content<>()
            // use the sender by default
            if (!TextUtils.isEmpty(presenceEvent.sender)) {
                userPresence.user_id = presenceEvent.sender
            }
            var user = mStore.getUser(userPresence.user_id)

            if (user == null) {
                user = userPresence
                user!!.setDataHandler(this)
            } else {
                user!!.currently_active = userPresence.currently_active
                user!!.presence = userPresence.presence
                user!!.lastActiveAgo = userPresence.lastActiveAgo
            }

            user!!.setLatestPresenceTs(System.currentTimeMillis())

            // check if the current user has been updated
            if (mCredentials.userId.equals(user!!.user_id)) {
                // always use the up-to-date information
                getMyUser().displayname = user!!.displayname
                getMyUser().avatar_url = user!!.getAvatarUrl()

                mStore.setAvatarURL(user!!.getAvatarUrl(), presenceEvent.getOriginServerTs())
                mStore.setDisplayName(user!!.displayname, presenceEvent.getOriginServerTs())
            }

            mStore.storeUser(user)
            onPresenceUpdate(presenceEvent, user)
        }
    }


    private fun manageResponse(syncResponse: SyncResponse?, fromToken: String?, isCatchingUp: Boolean) {
        val isInitialSync = fromToken == null
        var isEmptyResponse = true
        if (syncResponse == null) {
            return
        }
        // Handle the to device events before the room ones
        // to ensure to decrypt them properly
        if (syncResponse.toDevice?.events?.isNotEmpty() == true) {
            for (toDeviceEvent in syncResponse.toDevice.events) {
                handleToDeviceEvent(toDeviceEvent)
            }
        }
        // Handle account data before the room events
        // to be able to update direct chats dictionary during invites handling.
        manageAccountData(syncResponse.accountData, isInitialSync)
        // joined rooms events
        if (syncResponse.rooms?.join?.isNotEmpty() == true) {
            Log.d(LOG_TAG, "Received " + syncResponse.rooms.join.size + " joined rooms")
            val roomIds = syncResponse.rooms.join.keys
            // Handle first joined rooms
            for (roomId in roomIds) {
                try {
                    if (null != mLeftRoomsStore.getRoom(roomId)) {
                        Log.d(LOG_TAG, "the room $roomId moves from left to the joined ones")
                        mLeftRoomsStore.deleteRoom(roomId)
                    }

                    getRoom(roomId).handleJoinedRoomSync(syncResponse.rooms.join[roomId], isInitialSync)
                } catch (e: Exception) {
                    Log.e(LOG_TAG, "## manageResponse() : handleJoinedRoomSync failed " + e.message + " for room " + roomId, e)
                }

            }

            isEmptyResponse = false
        }

        // invited room management
        if (syncResponse.rooms?.invite?.isNotEmpty() == true) {
            Log.d(LOG_TAG, "Received " + syncResponse.rooms.invite.size + " invited rooms")

            val roomIds = syncResponse.rooms.invite.keys

            var updatedDirectChatRoomsDict: MutableMap<String, List<String>>? = null
            var hasChanged = false

            for (roomId in roomIds) {
                try {
                    Log.d(LOG_TAG, "## manageResponse() : the user has been invited to $roomId")
                    val room = getRoom(roomId)
                    val invitedRoomSync = syncResponse.rooms.invite[roomId]
                    room.handleInvitedRoomSync(invitedRoomSync)
                    // Handle here the invites to a direct chat.
                    if (room.isDirectChatInvitation()) {
                        // Retrieve the inviter user id.
                        var participantUserId: String? = null
                        for (event in invitedRoomSync.inviteState.events) {
                            if (null != event.sender) {
                                participantUserId = event.sender
                                break
                            }
                        }
                        if (null != participantUserId) {
                            // Prepare the updated dictionary.
                            if (null == updatedDirectChatRoomsDict) {
                                if (null != getStore().getDirectChatRoomsDict()) {
                                    // Consider the current dictionary.
                                    updatedDirectChatRoomsDict = HashMap(getStore().getDirectChatRoomsDict())
                                } else {
                                    updatedDirectChatRoomsDict = HashMap()
                                }
                            }

                            val roomIdsList: MutableList<String>
                            if (updatedDirectChatRoomsDict!!.containsKey(participantUserId)) {
                                roomIdsList = ArrayList(updatedDirectChatRoomsDict[participantUserId])
                            } else {
                                roomIdsList = ArrayList()
                            }

                            // Check whether the room was not yet seen as direct chat
                            if (roomIdsList.indexOf(roomId) < 0) {
                                Log.d(LOG_TAG, "## manageResponse() : add this new invite in direct chats")

                                roomIdsList.add(roomId) // update room list with the new room
                                updatedDirectChatRoomsDict[participantUserId] = roomIdsList
                                hasChanged = true
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(LOG_TAG, "## manageResponse() : handleInvitedRoomSync failed " + e.message + " for room " + roomId, e)
                }

            }

            isEmptyResponse = false

            if (hasChanged) {
                // Update account data to add new direct chat room(s)
                mAccountDataRestClient.setAccountData(mCredentials.userId, AccountDataRestClient.ACCOUNT_DATA_TYPE_DIRECT_MESSAGES,
                        updatedDirectChatRoomsDict, object : ApiCallback<Void>() {
                    fun onSuccess(info: Void) {
                        Log.d(LOG_TAG, "## manageResponse() : succeeds")
                    }

                    fun onNetworkError(e: Exception) {
                        Log.e(LOG_TAG, "## manageResponse() : update account data failed " + e.message, e)
                        // TODO: we should try again.
                    }

                    fun onMatrixError(e: MatrixError) {
                        Log.e(LOG_TAG, "## manageResponse() : update account data failed " + e.getMessage())
                    }

                    fun onUnexpectedError(e: Exception) {
                        Log.e(LOG_TAG, "## manageResponse() : update account data failed " + e.message, e)
                    }
                })
            }

            // left room management
            // it should be done at the end but it seems there is a server issue
            // when inviting after leaving a room, the room is defined in the both leave & invite rooms list.
            if (null != syncResponse.rooms.leave && syncResponse.rooms.leave.size > 0) {
                Log.d(LOG_TAG, "Received " + syncResponse.rooms.leave.size + " left rooms")

                val roomIds = syncResponse.rooms.leave.keys

                for (roomId in roomIds) {
                    // RoomSync leftRoomSync = syncResponse.rooms.leave.get(roomId);

                    // Presently we remove the existing room from the rooms list.
                    // FIXME SYNC V2 Archive/Display the left rooms!
                    // For that create 'handleArchivedRoomSync' method

                    var membership = RoomMember.MEMBERSHIP_LEAVE
                    val room = getRoom(roomId)

                    // Retrieve existing room
                    // check if the room still exists.
                    if (null != room) {
                        // use 'handleJoinedRoomSync' to pass the last events to the room before leaving it.
                        // The room will then be able to notify its listeners.
                        room!!.handleJoinedRoomSync(syncResponse.rooms.leave[roomId], isInitialSync)

                        val member = room!!.getMember(getUserId())
                        if (null != member) {
                            membership = member!!.membership
                        }

                        Log.d(LOG_TAG, "## manageResponse() : leave the room $roomId")
                    }

                    if (!TextUtils.equals(membership, RoomMember.MEMBERSHIP_KICK) && !TextUtils.equals(membership, RoomMember.MEMBERSHIP_BAN)) {
                        // ensure that the room data are properly deleted
                        getStore().deleteRoom(roomId)
                        onLeaveRoom(roomId)
                    } else {
                        onRoomKick(roomId)
                    }

                    // don't add to the left rooms if the user has been kicked / banned
                    if (mAreLeftRoomsSynced && TextUtils.equals(membership, RoomMember.MEMBERSHIP_LEAVE)) {
                        val leftRoom = getRoom(mLeftRoomsStore, roomId, true)
                        leftRoom.handleJoinedRoomSync(syncResponse.rooms.leave[roomId], isInitialSync)
                    }
                }

                isEmptyResponse = false
            }
        }

        // groups
        if (null != syncResponse.groups) {
            // Handle invited groups
            if (null != syncResponse.groups.invite && !syncResponse.groups.invite.isEmpty()) {
                // Handle invited groups
                for (groupId in syncResponse.groups.invite.keySet()) {
                    val invitedGroupSync = syncResponse.groups.invite.get(groupId)
                    mGroupsManager.onNewGroupInvitation(groupId, invitedGroupSync.profile, invitedGroupSync.inviter, !isInitialSync)
                }
            }

            // Handle joined groups
            if (null != syncResponse.groups.join && !syncResponse.groups.join.isEmpty()) {
                for (groupId in syncResponse.groups.join.keySet()) {
                    mGroupsManager.onJoinGroup(groupId, !isInitialSync)
                }
            }
            // Handle left groups
            if (null != syncResponse.groups.leave && !syncResponse.groups.leave.isEmpty()) {
                // Handle joined groups
                for (groupId in syncResponse.groups.leave.keySet()) {
                    mGroupsManager.onLeaveGroup(groupId, !isInitialSync)
                }
            }
        }

        // Handle presence of other users
        if (null != syncResponse.presence && null != syncResponse.presence.events) {
            Log.d(LOG_TAG, "Received " + syncResponse.presence.events.size + " presence events")
            for (presenceEvent in syncResponse.presence.events) {
                handlePresenceEvent(presenceEvent)
            }
        }

        if (null != mCrypto) {
            mCrypto.onSyncCompleted(syncResponse, fromToken, isCatchingUp)
        }

        val store = getStore()

        if (!isEmptyResponse && null != store) {
            store!!.setEventStreamToken(syncResponse.nextBatch)
            store!!.commit()
        }

    }

    */
/*
     * Handle a 'toDevice' event
     * @param event the event
     *//*

    private fun handleToDeviceEvent(event: Event) {
        // Decrypt event if necessary
        decryptEvent(event, null)

        if (TextUtils.equals(event.getType(), Event.EVENT_TYPE_MESSAGE)
                && null != event.getContent()
                && TextUtils.equals(JsonUtils.getMessageMsgType(event.getContent()), "m.bad.encrypted")) {
            Log.e(LOG_TAG, "## handleToDeviceEvent() : Warning: Unable to decrypt to-device event : " + event.getContent())
        } else {
            onToDeviceEvent(event)
        }
    }

    companion object {

        private val LOG_TAG = MXDataHandler::class.java!!.getSimpleName()

        private val LEFT_ROOMS_FILTER = "{\"room\":{\"timeline\":{\"limit\":1},\"include_leave\":true}}"
    }


}
*/
