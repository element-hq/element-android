package im.vector.matrix.android.internal.session.room.members

import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.room.model.Membership
import im.vector.matrix.android.api.session.room.model.RoomMember
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.database.model.EventEntityFields
import im.vector.matrix.android.internal.database.model.RoomSummaryEntity
import im.vector.matrix.android.internal.database.query.where
import io.realm.Realm

internal class RoomMembers(private val realm: Realm,
                           private val roomId: String
) {

    private val roomSummary: RoomSummaryEntity? by lazy {
        RoomSummaryEntity.where(realm, roomId).findFirst()
    }

    fun getLoaded(): Map<String, RoomMember> {
        return EventEntity
                .where(realm, roomId, EventType.STATE_ROOM_MEMBER)
                .sort(EventEntityFields.STATE_INDEX)
                .findAll()
                .map { it.asDomain() }
                .associateBy { it.stateKey!! }
                .mapValues { it.value.content<RoomMember>()!! }
    }


    fun getNumberOfJoinedMembers(): Int {
        return roomSummary?.joinedMembersCount
               ?: getLoaded().filterValues { it.membership == Membership.JOIN }.size
    }

    fun getNumberOfInvitedMembers(): Int {
        return roomSummary?.invitedMembersCount
               ?: getLoaded().filterValues { it.membership == Membership.INVITE }.size
    }

    fun getNumberOfMembers(): Int {
        return getNumberOfJoinedMembers() + getNumberOfInvitedMembers()
    }


}