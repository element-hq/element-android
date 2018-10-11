package im.vector.matrix.android.api.rooms

import android.text.TextUtils
import im.vector.matrix.android.api.events.Event
import im.vector.matrix.android.api.events.EventType
import im.vector.matrix.android.api.rooms.timeline.EventTimeline
import im.vector.matrix.android.internal.legacy.MXDataHandler
import im.vector.matrix.android.internal.legacy.call.MXCallsManager
import im.vector.matrix.android.internal.legacy.data.store.IMXStore
import im.vector.matrix.android.internal.legacy.rest.callback.ApiCallback
import im.vector.matrix.android.internal.legacy.rest.callback.SimpleApiCallback
import im.vector.matrix.android.internal.legacy.rest.model.*
import im.vector.matrix.android.internal.legacy.rest.model.RoomDirectoryVisibility
import im.vector.matrix.android.internal.legacy.rest.model.RoomMember
import im.vector.matrix.android.internal.legacy.rest.model.pid.RoomThirdPartyInvite
import im.vector.matrix.android.internal.legacy.util.JsonUtils
import im.vector.matrix.android.internal.legacy.util.Log
import timber.log.Timber
import java.util.*
import kotlin.collections.ArrayList

/**
 * The state of a room.
 */
data class RoomState(
        var roomId: String? = null,
        var powerLevels: PowerLevels? = null,
        var canonicalAlias: String? = null,
        var name: String? = null,
        var topic: String? = null,
        var roomTombstoneContent: RoomTombstoneContent? = null,
        var url: String? = null,
        var avatarUrl: String? = null,
        var roomCreateContent: RoomCreateContent? = null,
        var roomPinnedEventsContent: RoomPinnedEventsContent? = null,
        var joinRule: String? = null,
        var guestAccess: String = RoomState.GUEST_ACCESS_FORBIDDEN,
        var historyVisibility: String? = RoomState.HISTORY_VISIBILITY_SHARED,
        var visibility: String? = null,
        var algorithm: String? = null,
        var groups: List<String> = emptyList(),
        var token: String? = null,

        private var mergedAliasesList: MutableList<String>? = null,
        private var currentAliases: MutableList<String> = ArrayList(),
        private var roomAliases: MutableMap<String, Event> = HashMap(),
        private var aliasesByDomain_: MutableMap<String, MutableList<String>> = HashMap(),
        private var stateEvents: MutableMap<String, MutableList<Event>> = HashMap(),
        private var notificationCount_: Int = 0,
        private var highlightCount_: Int = 0,
        private val members: HashMap<String, RoomMember> = HashMap(),
        private var allMembersAreLoaded: Boolean = false,
        private val getAllMembersCallbacks: ArrayList<ApiCallback<List<RoomMember>>> = ArrayList(),
        private val thirdPartyInvites: HashMap<String, RoomThirdPartyInvite> = HashMap(),
        private val membersWithThirdPartyInviteTokenCache: HashMap<String, RoomMember> = HashMap(),
        private var isLive: Boolean = false,
        private var memberDisplayNameByUserId: MutableMap<String, String> = HashMap()
) {

    lateinit var dataHandler: MXDataHandler

    /**
     * @return a copy of the room members list. May be incomplete if the full list is not loaded yet
     */
    // make a copy to avoid concurrency modifications
    val loadedMembers: List<RoomMember>
        get() {
            val res: List<RoomMember>
            synchronized(this) {
                res = ArrayList(members.values)
            }
            return res
        }

    /**
     * @return a copy of the displayable members list. May be incomplete if the full list is not loaded yet
     */
    val displayableLoadedMembers: List<RoomMember>
        get() {
            val conferenceUserId = getMember(MXCallsManager.getConferenceUserId(roomId))
            return loadedMembers.filter { it != conferenceUserId }
        }

    /**
     * Tells if the room is a call conference one
     * i.e. this room has been created to manage the call conference
     *
     * @return true if it is a call conference room.
     */
    /**
     * Set this room as a conference user room
     *
     * @param isConferenceUserRoom true when it is an user conference room.
     */
    var isConferenceUserRoom: Boolean
        get() = dataHandler.store.getSummary(roomId)?.isConferenceUserRoom ?: false
        set(isConferenceUserRoom) = dataHandler.store!!.getSummary(roomId)!!.setIsConferenceUserRoom(isConferenceUserRoom)

    /**
     * @return the notified messages count.
     */
    /**
     * Update the notified messages count.
     *
     * @param notificationCount the new notified messages count.
     */
    var notificationCount: Int
        get() = notificationCount_
        set(notificationCount) {
            Timber.d("## setNotificationCount() : $notificationCount room id $roomId")
            notificationCount_ = notificationCount
        }

    /**
     * @return the highlighted messages count.
     */
    /**
     * Update the highlighted messages count.
     *
     * @param highlightCount the new highlighted messages count.
     */
    var highlightCount: Int
        get() = highlightCount_
        set(highlightCount) {
            Timber.d("## setHighlightCount() : $highlightCount room id $roomId")
            highlightCount_ = highlightCount
        }

    /**
     * Provides the currentAliases by domain
     *
     * @return the currentAliases list map
     */
    val aliasesByDomain: Map<String, List<String>>
        get() = HashMap(aliasesByDomain_)

    /**
     * @return true if the room is encrypted
     */
    // When a client receives an m.room.encryption event as above, it should set a flag to indicate that messages sent in the room should be encrypted.
    // This flag should not be cleared if a later m.room.encryption event changes the configuration. This is to avoid a situation where a MITM can simply
    // ask participants to disable encryption. In short: once encryption is enabled in a room, it can never be disabled.
    val isEncrypted: Boolean
        get() = null != algorithm

    /**
     * @return true if the room is versioned, it means that the room is obsolete.
     * You can't interact with it anymore, but you can still browse the past messages.
     */
    val isVersioned: Boolean
        get() = roomTombstoneContent != null

    /**
     * @return true if the room is a public one
     */
    val isPublic: Boolean
        get() = TextUtils.equals(if (null != visibility) visibility else joinRule, RoomDirectoryVisibility.DIRECTORY_VISIBILITY_PUBLIC)

    /**
     * Get the list of all the room members. Fetch from server if the full list is not loaded yet.
     *
     * @param callback The callback to get a copy of the room members list.
     */
    fun getMembersAsync(callback: ApiCallback<List<RoomMember>>) {
        if (areAllMembersLoaded()) {
            val res: List<RoomMember>

            synchronized(this) {
                // make a copy to avoid concurrency modifications
                res = ArrayList(members.values)
            }

            callback.onSuccess(res)
        } else {
            val doTheRequest: Boolean

            synchronized(getAllMembersCallbacks) {
                getAllMembersCallbacks.add(callback)

                doTheRequest = getAllMembersCallbacks.size == 1
            }

            if (doTheRequest) {
                // Load members from server
                dataHandler.getMembersAsync(roomId, object : SimpleApiCallback<List<RoomMember>>(callback) {
                    override fun onSuccess(info: List<RoomMember>) {
                        Timber.d("getMembers has returned " + info.size + " users.")

                        val store = (dataHandler as MXDataHandler).store
                        var res: List<RoomMember>

                        for (member in info) {
                            // Do not erase already known members form the sync
                            if (getMember(member.userId) == null) {
                                setMember(member.userId, member)

                                // Also create a User
                                store?.updateUserWithRoomMemberEvent(member)
                            }
                        }

                        synchronized(getAllMembersCallbacks) {
                            for (apiCallback in getAllMembersCallbacks) {
                                // make a copy to avoid concurrency modifications
                                res = ArrayList(members.values)

                                apiCallback.onSuccess(res)
                            }

                            getAllMembersCallbacks.clear()
                        }

                        allMembersAreLoaded = true
                    }
                })
            }
        }
    }

    /**
     * Tell if all members has been loaded
     *
     * @return true if LazyLoading is Off, or if all members has been loaded
     */
    private fun areAllMembersLoaded(): Boolean {
        return !dataHandler.isLazyLoadingEnabled || allMembersAreLoaded
    }

    /**
     * Force a fetch of the loaded members the next time they will be requested
     */
    fun forceMembersRequest() {
        allMembersAreLoaded = false
    }

    /**
     * Provides the loaded states event list.
     * The room member events are NOT included.
     *
     * @param types the allowed event types.
     * @return the filtered state events list.
     */
    fun getStateEvents(types: Set<String>?): List<Event> {
        val filteredStateEvents = ArrayList<Event>()
        val stateEvents = ArrayList<Event>()

        // merge the values lists
        val currentStateEvents = this.stateEvents.values
        for (eventsList in currentStateEvents) {
            stateEvents.addAll(eventsList)
        }

        if (null != types && !types.isEmpty()) {
            for (stateEvent in stateEvents) {
                if (types.contains(stateEvent.type)) {
                    filteredStateEvents.add(stateEvent)
                }
            }
        } else {
            filteredStateEvents.addAll(stateEvents)
        }

        return filteredStateEvents
    }


    /**
     * Provides the state events list.
     * It includes the room member creation events (they are not loaded in memory by default).
     *
     * @param store    the store in which the state events must be retrieved
     * @param types    the allowed event types.
     * @param callback the asynchronous callback.
     */
    fun getStateEvents(store: IMXStore?, types: Set<String>?, callback: ApiCallback<List<Event>>) {
        if (null != store) {
            val stateEvents = ArrayList<Event>()

            val currentStateEvents = this.stateEvents.values

            for (eventsList in currentStateEvents) {
                stateEvents.addAll(eventsList)
            }

            // retrieve the roomMember creation events
            store.getRoomStateEvents(roomId, object : SimpleApiCallback<List<Event>>() {
                override fun onSuccess(events: List<Event>) {
                    stateEvents.addAll(events)

                    val filteredStateEvents = ArrayList<Event>()

                    if (null != types && !types.isEmpty()) {
                        for (stateEvent in stateEvents) {
                            if (types.contains(stateEvent.type)) {
                                filteredStateEvents.add(stateEvent)
                            }
                        }
                    } else {
                        filteredStateEvents.addAll(stateEvents)
                    }

                    callback.onSuccess(filteredStateEvents)
                }
            })
        }
    }

    /**
     * Provides a list of displayable members.
     * Some dummy members are created to internal stuff.
     *
     * @param callback The callback to get a copy of the displayable room members list.
     */
    fun getDisplayableMembersAsync(callback: ApiCallback<List<RoomMember>>) {
        getMembersAsync(object : SimpleApiCallback<List<RoomMember>>(callback) {
            override fun onSuccess(members: List<RoomMember>) {
                val conferenceUserId = getMember(MXCallsManager.getConferenceUserId(roomId))

                if (null != conferenceUserId) {
                    val membersList = ArrayList(members)
                    membersList.remove(conferenceUserId)
                    callback.onSuccess(membersList)
                } else {
                    callback.onSuccess(members)
                }
            }
        })
    }

    /**
     * Update the room member from its user id.
     *
     * @param userId the user id.
     * @param member the new member value.
     */
    private fun setMember(userId: String, member: RoomMember) {
        // Populate a basic user object if there is none
        if (member.userId == null) {
            member.userId = userId
        }
        synchronized(this) {
            if (null != memberDisplayNameByUserId) {
                memberDisplayNameByUserId!!.remove(userId)
            }
            members.put(userId, member)
        }
    }

    /**
     * Retrieve a room member from its user id.
     *
     * @param userId the user id.
     * @return the linked member it exists.
     */
    // TODO Change this? Can return null if all members are not loaded yet
    fun getMember(userId: String): RoomMember? {
        val member: RoomMember?

        synchronized(this) {
            member = members[userId]
        }

        if (member == null) {
            // TODO LazyLoading
            Log.e(LOG_TAG, "!!!!!!!!!!!!!!!!!!!!!!!!!!!!! Null member '$userId' !!!!!!!!!!!!!!!!!!!!!!!!!!!!!")

            if (TextUtils.equals(dataHandler.userId, userId)) {
                // This should never happen
                Log.e(LOG_TAG, "!!!!!!!!!!!!!!!!!!!!!!!!!!!!! Null current user '$userId' !!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
            }
        }

        return member
    }

    /**
     * Retrieve a room member from its original event id.
     * It can return null if the lazy loading is enabled and if the member is not loaded yet.
     *
     * @param eventId the event id.
     * @return the linked member if it exists and if it is loaded.
     */
    fun getMemberByEventId(eventId: String): RoomMember? {
        var member: RoomMember? = null

        synchronized(this) {
            for (aMember in members.values) {
                if (aMember.originalEventId == eventId) {
                    member = aMember
                    break
                }
            }
        }

        return member
    }

    /**
     * Remove a member defines by its user id.
     *
     * @param userId the user id.
     */
    fun removeMember(userId: String) {
        synchronized(this) {
            members.remove(userId)
            // remove the cached display name
            if (null != memberDisplayNameByUserId) {
                memberDisplayNameByUserId!!.remove(userId)
            }
        }
    }

    /**
     * Retrieve a member from an invitation token.
     *
     * @param thirdPartyInviteToken the third party invitation token.
     * @return the member it exists.
     */
    fun memberWithThirdPartyInviteToken(thirdPartyInviteToken: String): RoomMember? {
        return membersWithThirdPartyInviteTokenCache[thirdPartyInviteToken]
    }

    /**
     * Retrieve a RoomThirdPartyInvite from its token.
     *
     * @param thirdPartyInviteToken the third party invitation token.
     * @return the linked RoomThirdPartyInvite if it exists
     */
    fun thirdPartyInviteWithToken(thirdPartyInviteToken: String): RoomThirdPartyInvite? {
        return thirdPartyInvites[thirdPartyInviteToken]
    }

    /**
     * @return the third party invite list.
     */
    fun thirdPartyInvites(): Collection<RoomThirdPartyInvite> {
        return thirdPartyInvites.values
    }

    /**
     * Check if the user userId can back paginate.
     *
     * @param isJoined  true is user is in the room
     * @param isInvited true is user is invited to the room
     * @return true if the user can back paginate.
     */
    fun canBackPaginate(isJoined: Boolean, isInvited: Boolean): Boolean {
        val visibility = if (TextUtils.isEmpty(historyVisibility)) HISTORY_VISIBILITY_SHARED else historyVisibility
        return (isJoined
                || visibility == HISTORY_VISIBILITY_WORLD_READABLE
                || visibility == HISTORY_VISIBILITY_SHARED
                || visibility == HISTORY_VISIBILITY_INVITED && isInvited)
    }

    /**
     * Provides the currentAliases for any known domains
     *
     * @return the currentAliases list
     */
    fun getAliases(): List<String> {
        if (mergedAliasesList != null) {
            return mergedAliasesList as List<String>
        }
        val merged = ArrayList<String>()
        for (url in aliasesByDomain.keys) {
            merged.addAll(aliasesByDomain[url] ?: emptyList())
        }
        // ensure that the current currentAliases have been added.
        // for example for the public rooms because there is no applystate call.
        for (anAlias in currentAliases) {
            if (merged.indexOf(anAlias) < 0) {
                merged.add(anAlias)
            }
        }
        mergedAliasesList = merged
        return merged
    }

    /**
     * Remove an alias.
     *
     * @param alias the alias to remove
     */
    fun removeAlias(alias: String) {
        if (getAliases().indexOf(alias) >= 0) {
            currentAliases.remove(alias)
            for (host in aliasesByDomain.keys) {
                aliasesByDomain_[host]?.remove(alias)
            }
        }
        mergedAliasesList = null
    }

    /**
     * Add an alias.
     *
     * @param alias the alias to add
     */
    fun addAlias(alias: String) {
        if (getAliases().indexOf(alias) < 0) {
            // patch until the server echoes the alias addition.
            mergedAliasesList?.add(alias)
        }
    }

    /**
     * @return true if the room has a predecessor
     */
    fun hasPredecessor(): Boolean {
        return roomCreateContent != null && roomCreateContent!!.hasPredecessor()
    }

    /**
     * @return the encryption algorithm
     */
    fun encryptionAlgorithm(): String? {
        return if (TextUtils.isEmpty(algorithm)) null else algorithm
    }

    /**
     * Apply the given event (relevant for state changes) to our state.
     *
     * @param store     the store to use
     * @param event     the event
     * @param direction how the event should affect the state: Forwards for applying, backwards for un-applying (applying the previous state)
     * @return true if the event is managed
     */
    fun applyState(store: IMXStore?, event: Event, direction: EventTimeline.Direction): Boolean {
        if (event.stateKey == null) {
            return false
        }
        val contentToConsider = if (direction == EventTimeline.Direction.FORWARDS) event.contentAsJsonObject else event.prevContentAsJsonObject
        val dataToConsider = if (direction == EventTimeline.Direction.FORWARDS) event.content else event.prevContent


        val eventType = event.type
        try {
            if (EventType.STATE_ROOM_NAME == eventType) {
                name = JsonUtils.toStateEvent(contentToConsider).name
            } else if (EventType.STATE_ROOM_TOPIC == eventType) {
                topic = JsonUtils.toStateEvent(contentToConsider).topic
            } else if (EventType.STATE_ROOM_CREATE == eventType) {
                roomCreateContent = JsonUtils.toRoomCreateContent(contentToConsider)
            } else if (EventType.STATE_ROOM_JOIN_RULES == eventType) {
                joinRule = JsonUtils.toStateEvent(contentToConsider).joinRule
            } else if (EventType.STATE_ROOM_GUEST_ACCESS == eventType) {
                guestAccess = JsonUtils.toStateEvent(contentToConsider).guestAccess
            } else if (EventType.STATE_ROOM_ALIASES == eventType) {
                if (!TextUtils.isEmpty(event.stateKey)) {
                    // backward compatibility
                    currentAliases = JsonUtils.toStateEvent(contentToConsider).aliases
                    // sanity check
                    if (null != currentAliases) {
                        aliasesByDomain_[event.stateKey] = currentAliases
                        roomAliases[event.stateKey] = event
                    } else {
                        aliasesByDomain_[event.stateKey] = ArrayList()
                    }
                }
            } else if (EventType.ENCRYPTION == eventType) {
                algorithm = JsonUtils.toStateEvent(contentToConsider).algorithm
                // When a client receives an m.room.encryption event as above, it should set a flag to indicate that messages sent
                // in the room should be encrypted.
                // This flag should not be cleared if a later m.room.encryption event changes the configuration. This is to avoid
                // a situation where a MITM can simply ask participants to disable encryption. In short: once encryption is enabled
                // in a room, it can never be disabled.
                if (algorithm == null) {
                    algorithm = ""
                }
            } else if (EventType.STATE_CANONICAL_ALIAS == eventType) {
                // SPEC-125
                canonicalAlias = JsonUtils.toStateEvent(contentToConsider).canonicalAlias
            } else if (EventType.STATE_HISTORY_VISIBILITY == eventType) {
                // SPEC-134
                historyVisibility = JsonUtils.toStateEvent(contentToConsider).historyVisibility
            } else if (EventType.STATE_ROOM_AVATAR == eventType) {
                url = JsonUtils.toStateEvent(contentToConsider).url
            } else if (EventType.STATE_RELATED_GROUPS == eventType) {
                groups = JsonUtils.toStateEvent(contentToConsider).groups
            } else if (EventType.STATE_ROOM_MEMBER == eventType) {
                val member = JsonUtils.toRoomMember(contentToConsider)
                val userId = event.stateKey
                if (member == null) {
                    // the member has already been removed
                    if (getMember(userId) == null) {
                        Log.e(LOG_TAG, "## applyState() : the user $userId is not anymore a member of $roomId")
                        return false
                    }
                    removeMember(userId)
                } else {
                    try {
                        member.userId = userId
                        member.originServerTs = event.originServerTs ?: -1
                        member.originalEventId = event.eventId
                        member.mSender = event.sender
                        if (null != store && direction == EventTimeline.Direction.FORWARDS) {
                            store.storeRoomStateEvent(roomId, event)
                        }
                        val currentMember = getMember(userId)
                        // check if the member is the same
                        // duplicated message ?
                        if (member.equals(currentMember)) {
                            Log.e(LOG_TAG, "## applyState() : seems being a duplicated event for $userId in room $roomId")
                            return false
                        }

                        // when a member leaves a room, his avatar / display name is not anymore provided
                        if (currentMember != null && (TextUtils.equals(member.membership, RoomMember.MEMBERSHIP_LEAVE) || TextUtils.equals(member.membership, RoomMember.MEMBERSHIP_BAN))) {
                            if (member.getAvatarUrl() == null) {
                                member.setAvatarUrl(currentMember.getAvatarUrl())
                            }
                            if (member.displayname == null) {
                                member.displayname = currentMember.displayname
                            }
                            // remove the cached display name
                            memberDisplayNameByUserId.remove(userId)

                            // test if the user has been kicked
                            if (!TextUtils.equals(event.sender, event.stateKey)
                                    && TextUtils.equals(currentMember.membership, RoomMember.MEMBERSHIP_JOIN)
                                    && TextUtils.equals(member.membership, RoomMember.MEMBERSHIP_LEAVE)) {
                                member.membership = RoomMember.MEMBERSHIP_KICK
                            }
                        }

                        if (direction == EventTimeline.Direction.FORWARDS && null != store) {
                            store.updateUserWithRoomMemberEvent(member)
                        }

                        // Cache room member event that is successor of a third party invite event
                        if (!TextUtils.isEmpty(member.thirdPartyInviteToken)) {
                            membersWithThirdPartyInviteTokenCache[member.thirdPartyInviteToken] = member
                        }
                    } catch (e: Exception) {
                        Log.e(LOG_TAG, "## applyState() - EVENT_TYPE_STATE_ROOM_MEMBER failed " + e.message, e)
                    }

                    setMember(userId, member)
                }
            } else if (EventType.STATE_ROOM_POWER_LEVELS == eventType) {
                powerLevels = event.toModel(dataToConsider)
            } else if (EventType.STATE_ROOM_THIRD_PARTY_INVITE == eventType) {
                val thirdPartyInvite = JsonUtils.toRoomThirdPartyInvite(contentToConsider)
                thirdPartyInvite.token = event.stateKey
                if (direction == EventTimeline.Direction.FORWARDS && null != store) {
                    store.storeRoomStateEvent(roomId, event)
                }
                if (!TextUtils.isEmpty(thirdPartyInvite.token)) {
                    thirdPartyInvites[thirdPartyInvite.token] = thirdPartyInvite
                }
            } else if (EventType.STATE_ROOM_TOMBSTONE == eventType) {
                roomTombstoneContent = JsonUtils.toRoomTombstoneContent(contentToConsider)
            } else if (EventType.STATE_PINNED_EVENT == eventType) {
                roomPinnedEventsContent = JsonUtils.toRoomPinnedEventsContent(contentToConsider)
            }
            // same the latest room state events
            // excepts the membership ones
            // they are saved elsewhere
            if (!TextUtils.isEmpty(eventType) && EventType.STATE_ROOM_MEMBER != eventType) {
                var eventsList: MutableList<Event>? = stateEvents[eventType]
                if (eventsList == null) {
                    eventsList = ArrayList()
                    stateEvents[eventType] = eventsList
                }
                eventsList.add(event)
            }

        } catch (e: Exception) {
            Log.e(LOG_TAG, "applyState failed with error " + e.message, e)
        }

        return true
    }

    /**
     * Return an unique display name of the member userId.
     *
     * @param userId the user id
     * @return unique display name
     */
    fun getMemberName(userId: String?): String? {
        // sanity check
        if (userId == null) {
            return null
        }
        var displayName: String?
        synchronized(this) {
            displayName = memberDisplayNameByUserId[userId]
        }
        if (displayName != null) {
            return displayName
        }
        // Get the user display name from the member list of the room
        val member = getMember(userId)
        // Do not consider null display name
        if (null != member && !TextUtils.isEmpty(member.displayname)) {
            displayName = member.displayname
            synchronized(this) {
                val matrixIds = ArrayList<String>()
                // Disambiguate users who have the same display name in the room
                for (aMember in members.values) {
                    if (displayName == aMember.displayname) {
                        matrixIds.add(aMember.userId)
                    }
                }
                // if several users have the same display name
                // index it i.e bob (<Matrix id>)
                if (matrixIds.size > 1) {
                    displayName += " ($userId)"
                }
            }
        } else if (null != member && TextUtils.equals(member.membership, RoomMember.MEMBERSHIP_INVITE)) {
            val user = dataHandler.getUser(userId)
            if (null != user) {
                displayName = user.displayname
            }
        }
        if (displayName == null) {
            // By default, use the user ID
            displayName = userId
        }
        displayName?.let {
            memberDisplayNameByUserId[userId] = it
        }
        return displayName
    }

    companion object {
        private val LOG_TAG = RoomState::class.java.simpleName
        private val serialVersionUID = -6019932024524988201L

        val JOIN_RULE_PUBLIC = "public"
        val JOIN_RULE_INVITE = "invite"

        /**
         * room access is granted to guests
         */
        val GUEST_ACCESS_CAN_JOIN = "can_join"
        /**
         * room access is denied to guests
         */
        val GUEST_ACCESS_FORBIDDEN = "forbidden"

        val HISTORY_VISIBILITY_SHARED = "shared"
        val HISTORY_VISIBILITY_INVITED = "invited"
        val HISTORY_VISIBILITY_JOINED = "joined"
        val HISTORY_VISIBILITY_WORLD_READABLE = "world_readable"
    }
}