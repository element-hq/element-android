package im.vector.matrix.android.internal.session.room

import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.room.Room
import im.vector.matrix.android.api.session.room.model.MyMembership
import im.vector.matrix.android.api.session.room.model.RoomAvatarContent
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.database.query.last
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.session.room.members.RoomMembers

internal class RoomAvatarResolver(private val monarchy: Monarchy,
                                  private val credentials: Credentials) {

    /**
     * Compute the room avatar url
     *
     * @return the room avatar url, can be a fallback to a room member avatar or null
     */
    fun resolve(room: Room): String? {
        var res: String? = null
        monarchy.doWithRealm { realm ->
            val roomName = EventEntity.where(realm, room.roomId, EventType.STATE_ROOM_AVATAR).last()?.asDomain()
            res = roomName?.content<RoomAvatarContent>()?.avatarUrl
            if (!res.isNullOrEmpty()) {
                return@doWithRealm
            }
            val roomMembers = RoomMembers(realm, room.roomId)
            val members = roomMembers.getLoaded()
            if (room.myMembership == MyMembership.INVITED) {
                if (members.size == 1) {
                    res = members.entries.first().value.avatarUrl
                } else if (members.size > 1) {
                    val firstOtherMember = members.filterKeys { it != credentials.userId }.values.firstOrNull()
                    res = firstOtherMember?.avatarUrl
                }
            } else {
                // detect if it is a room with no more than 2 members (i.e. an alone or a 1:1 chat)
                if (roomMembers.getNumberOfJoinedMembers() == 1 && members.isNotEmpty()) {
                    res = members.entries.first().value.avatarUrl
                } else if (roomMembers.getNumberOfMembers() == 2 && members.size > 1) {
                    val firstOtherMember = members.filterKeys { it != credentials.userId }.values.firstOrNull()
                    res = firstOtherMember?.avatarUrl
                }
            }

        }
        return res
    }
}
