package im.vector.matrix.android.internal.session.sync

import android.text.TextUtils
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.internal.session.sync.model.SyncResponse
import im.vector.matrix.android.internal.legacy.MXDataHandler
import im.vector.matrix.android.internal.legacy.data.Room
import im.vector.matrix.android.internal.legacy.data.store.IMXStore
import im.vector.matrix.android.internal.legacy.data.store.MXMemoryStore
import im.vector.matrix.android.internal.legacy.rest.client.AccountDataRestClient
import im.vector.matrix.android.internal.legacy.rest.model.bingrules.PushRulesResponse
import im.vector.matrix.android.internal.legacy.util.JsonUtils
import timber.log.Timber

class SyncResponseHandler(
        private val roomSyncHandler: RoomSyncHandler,
        private val dataHandler: MXDataHandler
) {

    private val store = dataHandler.store
    private val leftRoomsStore = MXMemoryStore()
    private var isStartingCryptoWithInitialSync = false
    private var areLeftRoomsSynced = false

    fun handleResponse(syncResponse: SyncResponse?, fromToken: String?, isCatchingUp: Boolean) {
        if (syncResponse == null) {
            return
        }
        Timber.v("Handle sync response")

        if (syncResponse.rooms != null) {
            // joined rooms events
            roomSyncHandler.handleRoomSync(RoomSyncHandler.HandlingStrategy.JOINED(syncResponse.rooms.join))
            roomSyncHandler.handleRoomSync(RoomSyncHandler.HandlingStrategy.INVITED(syncResponse.rooms.invite))
            roomSyncHandler.handleRoomSync(RoomSyncHandler.HandlingStrategy.LEFT(syncResponse.rooms.leave))
        }

        /*
        val isInitialSync = null == fromToken
        var isEmptyResponse = true

        // Handle the to device events before the room ones
        // to ensure to decrypt them properly
        if (syncResponse.toDevice?.events != null) {
            for (toDeviceEvent in syncResponse.toDevice.events) {
                handleToDeviceEvent(toDeviceEvent)
            }
        }
        // Handle account data before the room events
        // to be able to update direct chats dictionary during invites handling.
        if (syncResponse.accountData != null) {
            manageAccountData(syncResponse.accountData, isInitialSync)
        }


        if (syncResponse.rooms != null) {

            // invited room management
            if (syncResponse.rooms.invite.isNotEmpty()) {
                val roomIds = syncResponse.rooms.invite.keys

                var updatedDirectChatRoomsDict: MutableMap<String, List<String>>? = null
                var hasChanged = false

                for (roomId in roomIds) {
                    if (null != leftRoomsStore.getRoom(roomId)) {
                        leftRoomsStore.deleteRoom(roomId)
                    }
                    val room = getRoom(roomId)
                    val invitedRoomSync = syncResponse.rooms.invite[roomId]
                    // TODO handle invited room
                    //room.handleInvitedRoomSync(invitedRoomSync)

                    // Handle here the invites to a direct chat.
                    if (room.isDirectChatInvitation) {
                        // Retrieve the inviter user id.
                        var participantUserId: String? = null
                        for (event in invitedRoomSync?.inviteState?.events ?: emptyList()) {
                            if (event.sender != null) {
                                participantUserId = event.sender
                                break
                            }
                        }

                        if (participantUserId != null) {
                            // Prepare the updated dictionary.
                            if (updatedDirectChatRoomsDict == null) updatedDirectChatRoomsDict = if (null != store.directChatRoomsDict) {
                                // Consider the current dictionary.
                                HashMap(store.directChatRoomsDict)
                            } else {
                                java.util.HashMap()
                            }

                            val roomIdsList: MutableList<String> = if (updatedDirectChatRoomsDict.containsKey(participantUserId)) {
                                ArrayList(updatedDirectChatRoomsDict[participantUserId])
                            } else {
                                ArrayList()
                            }

                            // Check whether the room was not yet seen as direct chat
                            if (roomIdsList.indexOf(roomId) < 0) {

                                roomIdsList.add(roomId) // update room list with the new room
                                updatedDirectChatRoomsDict[participantUserId] = roomIdsList
                                hasChanged = true
                            }
                        }
                    }
                }
            }

            isEmptyResponse = false

            if (hasChanged) {
                // Update account data to add new direct chat room(s)
                /* mAccountDataRestClient.setAccountData(mCredentials.userId, AccountDataRestClient.ACCOUNT_DATA_TYPE_DIRECT_MESSAGES,
                         updatedDirectChatRoomsDict, object : ApiCallback<Void> {
                     override fun onSuccess(info: Void) {
                     }

                     override fun onNetworkError(e: Exception) {
                         // TODO: we should try again.
                     }

                     override fun onMatrixError(e: MatrixError) {
                     }

                     override fun onUnexpectedError(e: Exception) {
                     }
                 })*/
            }
        }

        // left room management
        // it should be done at the end but it seems there is a server issue
        // when inviting after leaving a room, the room is defined in the both leave & invite rooms list.
        if (syncResponse.rooms.leave.isNotEmpty()) {
            val roomIds = syncResponse.rooms.leave.keys
            for (roomId in roomIds) {
                // RoomSync leftRoomSync = syncResponse.rooms.leave.get(roomId);

                // Presently we remove the existing room from the rooms list.
                // FIXME SYNC V2 Archive/Display the left rooms!
                // For that create 'handleArchivedRoomSync' method

                var membership = RoomMember.MEMBERSHIP_LEAVE
                val room = getRoom(roomId)

                // Retrieve existing room
                // The room will then be able to notify its listeners.
                // TODO handle
                // room.handleJoinedRoomSync(syncResponse.rooms.leave[roomId], isInitialSync)

                val member = room.getMember(dataHandler.userId)
                if (null != member) {
                    membership = member.membership
                }
                if (!TextUtils.equals(membership, RoomMember.MEMBERSHIP_KICK) && !TextUtils.equals(membership, RoomMember.MEMBERSHIP_BAN)) {
                    // ensure that the room data are properly deleted
                    store.deleteRoom(roomId)
                    dataHandler.onLeaveRoom(roomId)
                } else {
                    dataHandler.onRoomKick(roomId)
                }
                // don't add to the left rooms if the user has been kicked / banned
                if (areLeftRoomsSynced && TextUtils.equals(membership, RoomMember.MEMBERSHIP_LEAVE)) {
                    val leftRoom = getRoom(leftRoomsStore, roomId, true)
                    //Todo handle
                    //leftRoom.handleJoinedRoomSync(syncResponse.rooms.leave[roomId], isInitialSync)
                }
            }
            isEmptyResponse = false
        }
    }

    // groups
    if (null != syncResponse.groups)
    {
        // Handle invited groups
        if (null != syncResponse.groups.invite && !syncResponse.groups.invite.isEmpty()) {
            // Handle invited groups
            for (groupId in syncResponse.groups.invite.keys) {
                val invitedGroupSync = syncResponse.groups.invite[groupId]
                dataHandler.groupsManager.onNewGroupInvitation(groupId, invitedGroupSync?.profile, invitedGroupSync?.inviter, !isInitialSync)
            }
        }

        // Handle joined groups
        if (null != syncResponse.groups.join && !syncResponse.groups.join.isEmpty()) {
            for (groupId in syncResponse.groups.join.keys) {
                dataHandler.groupsManager.onJoinGroup(groupId, !isInitialSync)
            }
        }
        // Handle left groups
        if (null != syncResponse.groups.leave && !syncResponse.groups.leave.isEmpty()) {
            // Handle joined groups
            for (groupId in syncResponse.groups.leave.keys) {
                dataHandler.groupsManager.onLeaveGroup(groupId, !isInitialSync)
            }
        }
    }

    // Handle presence of other users
    if (syncResponse.presence?.events != null)
    {
        for (presenceEvent in syncResponse.presence.events) {
            handlePresenceEvent(presenceEvent)
        }
    }
    dataHandler.crypto?.onSyncCompleted(syncResponse, fromToken, isCatchingUp)
    if (!isEmptyResponse)
    {
        store.eventStreamToken = syncResponse.nextBatch
        store.commit()
    }

    if (isInitialSync)
    {
        if (!isCatchingUp) {
            dataHandler.startCrypto(true)
        } else {
            // the events thread sends a dummy initial sync event
            // when the application is restarted.
            isStartingCryptoWithInitialSync = !isEmptyResponse
        }

        dataHandler.onInitialSyncComplete(syncResponse?.nextBatch)
    } else
    {

        if (!isCatchingUp) {
            dataHandler.startCrypto(isStartingCryptoWithInitialSync)
        }

        dataHandler.onLiveEventsChunkProcessed(fromToken, syncResponse.nextBatch)
        dataHandler.callsManager?.checkPendingIncomingCalls()

    }
    */
    }

    private fun manageAccountData(accountData: Map<String, Any>, isInitialSync: Boolean) {
        if (accountData.containsKey("events")) {
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
    }

    /**
     * Refresh the push rules from the account data events list
     *
     * @param events the account data events.
     */
    private fun managePushRulesUpdate(events: List<Map<String, Any>>) {
        for (event in events) {
            val type = event["type"] as String

            if (TextUtils.equals(type, "m.push_rules")) {
                if (event.containsKey("content")) {
                    val gson = JsonUtils.getGson(false)

                    // convert the data to PushRulesResponse
                    // because BingRulesManager supports only PushRulesResponse
                    val element = gson.toJsonTree(event["content"])
                    dataHandler.bingRulesManager?.buildRules(gson.fromJson(element, PushRulesResponse::class.java))

                    // warn the client that the push rules have been updated
                    dataHandler.onBingRulesUpdate()
                }

                return
            }
        }
    }

    /**
     * Check if the ignored users list is updated
     *
     * @param events the account data events list
     */
    private fun manageIgnoredUsers(events: List<Map<String, Any>>, isInitialSync: Boolean) {
        val newIgnoredUsers = dataHandler.ignoredUsers(events)

        if (null != newIgnoredUsers) {
            val curIgnoredUsers = dataHandler.ignoredUserIds
            // the both lists are not empty
            if (0 != newIgnoredUsers.size || 0 != curIgnoredUsers.size) {
                // check if the ignored users list has been updated
                if (newIgnoredUsers.size != curIgnoredUsers.size || !newIgnoredUsers.containsAll(curIgnoredUsers)) {
                    // update the store
                    store.setIgnoredUserIdsList(newIgnoredUsers)
                    if (!isInitialSync) {
                        // warn there is an update
                        dataHandler.onIgnoredUsersListUpdate()
                    }
                }
            }
        }
    }


    /**
     * Extract the direct chat rooms list from the dedicated events.
     *
     * @param events the account data events list.
     */
    private fun manageDirectChatRooms(events: List<Map<String, Any>>, isInitialSync: Boolean) {
        if (events.isNotEmpty()) {
            for (event in events) {
                val type = event["type"] as String

                if (TextUtils.equals(type, AccountDataRestClient.ACCOUNT_DATA_TYPE_DIRECT_MESSAGES)) {
                    if (event.containsKey("content")) {
                        val contentDict = event["content"] as Map<String, List<String>>
                        store.directChatRoomsDict = contentDict
                        // reset the current list of the direct chat roomIDs
                        // to update it
                        if (!isInitialSync) {
                            // warn there is an update
                            dataHandler.onDirectMessageChatRoomsListUpdate()
                        }
                    }
                }
            }
        }
    }

    /**
     * Manage the URL preview flag
     *
     * @param events the events list
     */
    private fun manageUrlPreview(events: List<Map<String, Any>>) {
        if (0 != events.size) {
            for (event in events) {
                val type = event["type"] as String

                if (TextUtils.equals(type, AccountDataRestClient.ACCOUNT_DATA_TYPE_PREVIEW_URLS)) {
                    if (event.containsKey("content")) {
                        val contentDict = event["content"] as Map<String, Any>
                        var enable = true
                        if (contentDict.containsKey(AccountDataRestClient.ACCOUNT_DATA_KEY_URL_PREVIEW_DISABLE)) {
                            enable = !(contentDict[AccountDataRestClient.ACCOUNT_DATA_KEY_URL_PREVIEW_DISABLE] as Boolean)
                        }

                        store.setURLPreviewEnabled(enable)
                    }
                }
            }
        }
    }

    /**
     * Manage the user widgets
     *
     * @param events the events list
     */
    private fun manageUserWidgets(events: List<Map<String, Any>>) {
        if (0 != events.size) {
            for (event in events) {
                val type = event["type"] as String

                if (TextUtils.equals(type, AccountDataRestClient.ACCOUNT_DATA_TYPE_WIDGETS)) {
                    if (event.containsKey("content")) {
                        val contentDict = event["content"] as Map<String, Any>
                        store.setUserWidgets(contentDict)
                    }
                }
            }
        }
    }

    /**
     * Handle a presence event.
     *
     * @param presenceEvent the presence event.
     */
    private fun handlePresenceEvent(presenceEvent: Event) {
        /* // Presence event
         if (EventType.PRESENCE == presenceEvent.type) {
             val userPresence = JsonUtils.toUser(presenceEvent.getContent())

             // use the sender by default
             if (!TextUtils.isEmpty(presenceEvent.getSender())) {
                 userPresence.user_id = presenceEvent.getSender()
             }
             var user: User? = store.getUser(userPresence.user_id)

             if (user == null) {
                 user = userPresence
                 user!!.setDataHandler(dataHandler)
             } else {
                 user.currently_active = userPresence.currently_active
                 user.presence = userPresence.presence
                 user.lastActiveAgo = userPresence.lastActiveAgo
             }
             user.latestPresenceTs = System.currentTimeMillis()
             // check if the current user has been updated
             if (mCredentials.userId == user.user_id) {
                 // always use the up-to-date information
                 getMyUser().displayname = user.displayname
                 getMyUser().avatar_url = user.avatarUrl

                 store.setAvatarURL(user.avatarUrl, presenceEvent.getOriginServerTs())
                 store.setDisplayName(user.displayname, presenceEvent.getOriginServerTs())
             }
             store.storeUser(user)
             onPresenceUpdate(presenceEvent, user)
         }*/
    }

    private fun handleToDeviceEvent(event: Event) {
        // Decrypt event if necessary
        /*
        decryptEvent(event, null)
        if (TextUtils.equals(event.getType(), Event.EVENT_TYPE_MESSAGE)
                && null != event.getContent()
                && TextUtils.equals(JsonUtils.getMessageMsgType(event.getContent()), "m.bad.encrypted")) {
            Timber.e("## handleToDeviceEvent() : Warning: Unable to decrypt to-device event : %s", event.getContent())
        } else {
            //onToDeviceEvent(event)
        }
        */
    }

    /**
     * Decrypt an encrypted event
     *
     * @param event      the event to decrypt
     * @param timelineId the timeline identifier
     * @return true if the event has been decrypted
     */
    fun decryptEvent(event: Event?, timelineId: String?): Boolean {
        /*
        if (null != event && TextUtils.equals(event.getType(), Event.EVENT_TYPE_MESSAGE_ENCRYPTED)) {
            if (null != getCrypto()) {
                var result: MXEventDecryptionResult? = null
                try {
                    result = getCrypto().decryptEvent(event, timelineId)
                } catch (exception: MXDecryptionException) {
                    event.cryptoError = exception.cryptoError
                }
                if (null != result) {
                    event.setClearData(result)
                    return true
                }
            } else {
                event.cryptoError = MXCryptoError(MXCryptoError.ENCRYPTING_NOT_ENABLED_ERROR_CODE, MXCryptoError.ENCRYPTING_NOT_ENABLED_REASON, null)
            }
        }
        */
        return false
    }

    /**
     * Get the room object for the corresponding room id. Creates and initializes the object if there is none.
     *
     * @param roomId the room id
     * @return the corresponding room
     */
    fun getRoom(roomId: String): Room {
        return getRoom(roomId, true)
    }

    /**
     * Get the room object for the corresponding room id.
     * The left rooms are not included.
     *
     * @param roomId the room id
     * @param create create the room it does not exist.
     * @return the corresponding room
     */
    fun getRoom(roomId: String, create: Boolean): Room {
        return getRoom(store, roomId, create)
    }

    /**
     * Get the room object for the corresponding room id.
     * By default, the left rooms are not included.
     *
     * @param roomId        the room id
     * @param testLeftRooms true to test if the room is a left room
     * @param create        create the room it does not exist.
     * @return the corresponding room
     */
    fun getRoom(roomId: String, testLeftRooms: Boolean, create: Boolean): Room {
        var room = store.getRoom(roomId)
        if (room == null && testLeftRooms) {
            room = leftRoomsStore.getRoom(roomId)
        }
        if (room == null && create) {
            room = getRoom(store, roomId, create)
        }
        return room
    }

    fun getRoom(store: IMXStore, roomId: String, create: Boolean): Room {
        var room = store.getRoom(roomId)
        if (room == null && create) {
            room = Room(dataHandler, store, roomId)
            store.storeRoom(room)
        }
        return room
    }


}