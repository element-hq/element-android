package im.vector.matrix.android.api.rooms


import android.text.TextUtils
import im.vector.matrix.android.api.events.Event
import im.vector.matrix.android.api.events.EventContent
import im.vector.matrix.android.api.events.EventType
import im.vector.matrix.android.internal.legacy.call.MXCallsManager
import im.vector.matrix.android.internal.legacy.data.RoomState
import im.vector.matrix.android.internal.legacy.rest.model.RoomMember
import im.vector.matrix.android.internal.legacy.rest.model.message.Message
import im.vector.matrix.android.internal.legacy.rest.model.sync.RoomSyncSummary
import timber.log.Timber
import java.util.*

/**
 * Stores summarised information about the room.
 */
class RoomSummary() {

    private var mRoomId: String? = null
    /**
     * @return the topic.
     */
    var roomTopic: String? = null
        private set
    private var mLatestReceivedEvent: Event? = null

    // the room state is only used to check
    // 1- the invitation status
    // 2- the members display name
    @Transient private var mLatestRoomState: RoomState? = null

    // defines the latest read message
    /**
     * @return the read receipt event id
     */
    /**
     * Set the read receipt event Id
     *
     * @param eventId the read receipt event id.
     */
    var readReceiptEventId: String? = null
        set(eventId) {
            Timber.d("## setReadReceiptEventId() : " + eventId + " roomId " + getRoomId())
            field = eventId
        }

    // the read marker event id
    private var mReadMarkerEventId: String? = null

    /**
     * @return the room tags
     */
    /**
     * Update the room tags
     *
     * @param roomTags the room tags
     */
    // wraps the set into a serializable one
    var roomTags: Set<String>? = null
        set(roomTags) = if (roomTags != null) {
            field = HashSet(roomTags)
        } else {
            field = HashSet()
        }

    // counters
    var mUnreadEventsCount: Int = 0
    var mNotificationCount: Int = 0
    var mHighlightsCount: Int = 0

    // invitation status
    // retrieved at initial sync
    // the roomstate is not always known
    /**
     * @return the inviter user id.
     */
    var inviterUserId: String? = null
        private set

    // retrieved from the roomState
    private var mInviterName: String? = null

    /**
     * @return the user id
     */
    var userId: String? = null

    // Info from sync, depending on the room position in the sync
    private var mUserMembership: String? = null

    /**
     * Tell if the room is a user conference user one
     */
    private var mIsConferenceUserRoom: Boolean? = null

    /**
     * Data from RoomSyncSummary
     */
    private val mHeroes = ArrayList<String>()

    var numberOfJoinedMembers: Int = 0
        private set

    var numberOfInvitedMembers: Int = 0
        private set

    /**
     * @return true if the current user is invited
     */
    val isInvited: Boolean
        get() = RoomMember.MEMBERSHIP_INVITE == mUserMembership

    /**
     * @return true if the current user is invited
     */
    val isJoined: Boolean
        get() = RoomMember.MEMBERSHIP_JOIN == mUserMembership

    /**
     * @return the read receipt event id
     */
    /**
     * Set the read marker event Id
     *
     * @param eventId the read marker event id.
     */
    var readMarkerEventId: String?
        get() {
            if (TextUtils.isEmpty(mReadMarkerEventId)) {
                Timber.e("## getReadMarkerEventId') : null mReadMarkerEventId, in " + getRoomId()!!)
                mReadMarkerEventId = readReceiptEventId
            }

            return mReadMarkerEventId
        }
        set(eventId) {
            Timber.d("## setReadMarkerEventId() : " + eventId + " roomId " + getRoomId())

            if (TextUtils.isEmpty(eventId)) {
                Timber.e("## setReadMarkerEventId') : null mReadMarkerEventId, in " + getRoomId()!!)
            }

            mReadMarkerEventId = eventId
        }

    /**
     * @return the unread events count
     */
    /**
     * Update the unread message counter
     *
     * @param count the unread events count.
     */
    var unreadEventsCount: Int
        get() = mUnreadEventsCount
        set(count) {
            Timber.d("## setUnreadEventsCount() : " + count + " roomId " + getRoomId())
            mUnreadEventsCount = count
        }

    /**
     * @return the notification count
     */
    /**
     * Update the notification counter
     *
     * @param count the notification counter
     */
    var notificationCount: Int
        get() = mNotificationCount
        set(count) {
            Timber.d("## setNotificationCount() : " + count + " roomId " + getRoomId())
            mNotificationCount = count
        }

    /**
     * @return the highlight count
     */
    /**
     * Update the highlight counter
     *
     * @param count the highlight counter
     */
    var highlightCount: Int
        get() = mHighlightsCount
        set(count) {
            Timber.d("## setHighlightCount() : " + count + " roomId " + getRoomId())
            mHighlightsCount = count
        }

    // test if it is not yet initialized
    // FIXME LazyLoading Heroes does not contains me
    // FIXME I'ms not sure this code will work anymore
    // works only with 1:1 room
    var isConferenceUserRoom: Boolean
        get() {
            if (null == mIsConferenceUserRoom) {

                mIsConferenceUserRoom = false

                val membersId = heroes
                if (2 == membersId.size) {
                    for (userId in membersId) {
                        if (MXCallsManager.isConferenceUserId(userId)) {
                            mIsConferenceUserRoom = true
                            break
                        }
                    }
                }
            }

            return mIsConferenceUserRoom!!
        }
        set(isConferenceUserRoom) {
            mIsConferenceUserRoom = isConferenceUserRoom
        }

    val heroes: List<String>
        get() = mHeroes

    /**
     * Create a room summary
     *
     * @param fromSummary the summary source
     * @param event       the latest event of the room
     * @param roomState   the room state - used to display the event
     * @param userId      our own user id - used to display the room name
     */
    constructor(fromSummary: RoomSummary?,
                event: Event?,
                roomState: RoomState?,
                userId: String) : this() {
        this.userId = userId
        if (roomState != null) {
            setRoomId(roomState.roomId)
        }
        if (mRoomId == null) {
            event?.roomId?.let { setRoomId(it) }
        }
        setLatestReceivedEvent(event, roomState)

        // if no summary is provided
        if (fromSummary == null) {
            event?.let {
                readMarkerEventId = it.eventId
                readReceiptEventId = it.eventId
            }
            roomState?.let {
                highlightCount = it.highlightCount
                notificationCount = it.highlightCount
            }
            unreadEventsCount = Math.max(highlightCount, notificationCount)
        } else {
            // else use the provided summary data
            readMarkerEventId = fromSummary.readMarkerEventId
            readReceiptEventId = fromSummary.readReceiptEventId
            unreadEventsCount = fromSummary.unreadEventsCount
            highlightCount = fromSummary.highlightCount
            notificationCount = fromSummary.notificationCount
            mHeroes.addAll(fromSummary.mHeroes)
            numberOfJoinedMembers = fromSummary.numberOfJoinedMembers
            numberOfInvitedMembers = fromSummary.numberOfInvitedMembers
            mUserMembership = fromSummary.mUserMembership
        }
    }

    /**
     * @return the room id
     */
    fun getRoomId(): String? {
        return mRoomId
    }

    /**
     * @return the room summary event.
     */
    fun getLatestReceivedEvent(): Event? {
        return mLatestReceivedEvent
    }

    /**
     * @return the dedicated room state.
     */
    fun getLatestRoomState(): RoomState? {
        return mLatestRoomState
    }

    /**
     * To call when the room is in the invited section of the sync response
     */
    fun setIsInvited() {
        mUserMembership = RoomMember.MEMBERSHIP_INVITE
    }

    /**
     * To call when the room is in the joined section of the sync response
     */
    fun setIsJoined() {
        mUserMembership = RoomMember.MEMBERSHIP_JOIN
    }

    /**
     * Set the room's [org.matrix.androidsdk.rest.model.Event.EVENT_TYPE_STATE_ROOM_TOPIC].
     *
     * @param topic The topic
     * @return This summary for chaining calls.
     */
    fun setTopic(topic: String): RoomSummary {
        roomTopic = topic
        return this
    }

    /**
     * Set the room's ID..
     *
     * @param roomId The room ID
     * @return This summary for chaining calls.
     */
    fun setRoomId(roomId: String): RoomSummary {
        mRoomId = roomId
        return this
    }

    /**
     * Set the latest tracked event (e.g. the latest m.room.message)
     *
     * @param event     The most-recent event.
     * @param roomState The room state
     * @return This summary for chaining calls.
     */
    fun setLatestReceivedEvent(event: Event?, roomState: RoomState?): RoomSummary {
        setLatestReceivedEvent(event)
        setLatestRoomState(roomState)

        if (null != roomState) {
            setTopic(roomState.topic)
        }
        return this
    }

    /**
     * Set the latest tracked event (e.g. the latest m.room.message)
     *
     * @param event The most-recent event.
     * @return This summary for chaining calls.
     */
    fun setLatestReceivedEvent(event: Event?): RoomSummary {
        mLatestReceivedEvent = event
        return this
    }

    /**
     * Set the latest RoomState
     *
     * @param roomState The room state of the latest event.
     * @return This summary for chaining calls.
     */
    fun setLatestRoomState(roomState: RoomState?): RoomSummary {
        mLatestRoomState = roomState

        // Keep this code for compatibility?
        var isInvited = false

        // check for the invitation status
        if (null != mLatestRoomState) {
            val member = mLatestRoomState!!.getMember(userId)
            isInvited = null != member && RoomMember.MEMBERSHIP_INVITE == member.membership
        }
        // when invited, the only received message should be the invitation one
        if (isInvited) {
            mInviterName = null

            if (null != mLatestReceivedEvent) {
                inviterUserId = mLatestReceivedEvent!!.sender
                mInviterName = inviterUserId

                // try to retrieve a display name
                if (null != mLatestRoomState) {
                    mInviterName = mLatestRoomState!!.getMemberName(mLatestReceivedEvent!!.sender)
                }
            }
        } else {
            mInviterName = null
            inviterUserId = mInviterName
        }

        return this
    }

    fun setRoomSyncSummary(roomSyncSummary: RoomSyncSummary) {
        if (roomSyncSummary.heroes != null) {
            mHeroes.clear()
            mHeroes.addAll(roomSyncSummary.heroes)
        }

        if (roomSyncSummary.joinedMembersCount != null) {
            // Update the value
            numberOfJoinedMembers = roomSyncSummary.joinedMembersCount
        }

        if (roomSyncSummary.invitedMembersCount != null) {
            // Update the value
            numberOfInvitedMembers = roomSyncSummary.invitedMembersCount
        }
    }

    companion object {

        private const val serialVersionUID = -3683013938626566489L

        // list of supported types
        private val supportedType = Arrays.asList(
                EventType.STATE_ROOM_TOPIC,
                EventType.ENCRYPTED,
                EventType.ENCRYPTION,
                EventType.STATE_ROOM_NAME,
                EventType.STATE_ROOM_MEMBER,
                EventType.STATE_ROOM_CREATE,
                EventType.STATE_HISTORY_VISIBILITY,
                EventType.STATE_ROOM_THIRD_PARTY_INVITE,
                EventType.STICKER)

        // List of known unsupported types
        private val knownUnsupportedType = Arrays.asList(
                EventType.TYPING,
                EventType.STATE_ROOM_POWER_LEVELS,
                EventType.STATE_ROOM_JOIN_RULES,
                EventType.STATE_CANONICAL_ALIAS,
                EventType.STATE_ROOM_ALIASES,
                EventType.PREVIEW_URLS,
                EventType.STATE_RELATED_GROUPS,
                EventType.STATE_ROOM_GUEST_ACCESS,
                EventType.REDACTION)

        /**
         * Test if the event can be summarized.
         * Some event types are not yet supported.
         *
         * @param event the event to test.
         * @return true if the event can be summarized
         */
        fun isSupportedEvent(event: Event): Boolean {
            val type = event.type
            var isSupported = false

            // check if the msgtype is supported
            if (TextUtils.equals(EventType.MESSAGE, type)) {
                try {
                    val eventContent = event.contentAsJsonObject
                    var msgType = ""

                    val element = eventContent!!.get("msgtype")

                    if (null != element) {
                        msgType = element.asString
                    }

                    isSupported = (TextUtils.equals(msgType, Message.MSGTYPE_TEXT)
                            || TextUtils.equals(msgType, Message.MSGTYPE_EMOTE)
                            || TextUtils.equals(msgType, Message.MSGTYPE_NOTICE)
                            || TextUtils.equals(msgType, Message.MSGTYPE_IMAGE)
                            || TextUtils.equals(msgType, Message.MSGTYPE_AUDIO)
                            || TextUtils.equals(msgType, Message.MSGTYPE_VIDEO)
                            || TextUtils.equals(msgType, Message.MSGTYPE_FILE))

                    if (!isSupported && !TextUtils.isEmpty(msgType)) {
                        Timber.e("isSupportedEvent : Unsupported msg type $msgType")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "isSupportedEvent failed " + e.message)
                }

            } else if (EventType.ENCRYPTED == type) {
                isSupported = event.content?.isNotEmpty() ?: false
            } else if (EventType.STATE_ROOM_MEMBER == type) {
                val eventContentAsJsonObject = event.contentAsJsonObject
                if (eventContentAsJsonObject != null) {
                    if (eventContentAsJsonObject.entrySet().isEmpty()) {
                        Timber.d("isSupportedEvent : room member with no content is not supported")
                    } else {
                        // do not display the avatar / display name update
                        val prevEventContent = event.prevContent<EventContent>()
                        val eventContent = event.content<EventContent>()
                        var membership: String? = null
                        var preMembership: String? = null
                        if (eventContent != null) {
                            membership = eventContent.membership
                        }
                        if (prevEventContent != null) {
                            preMembership = prevEventContent.membership
                        }

                        isSupported = !TextUtils.equals(membership, preMembership)

                        if (!isSupported) {
                            Timber.d("isSupportedEvent : do not support avatar display name update")
                        }
                    }
                }
            } else {
                isSupported = supportedType.contains(type) || event.isCallEvent && !TextUtils.isEmpty(type) && EventType.CALL_CANDIDATES != type
            }

            if (!isSupported) {
                // some events are known to be never traced
                // avoid warning when it is not required.
                if (!knownUnsupportedType.contains(type)) {
                    Timber.e("isSupportedEvent :  Unsupported event type $type")
                }
            }

            return isSupported
        }
    }
}
