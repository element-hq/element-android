package im.vector.matrix.android.internal.session.room.members

import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.room.model.RoomMember
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.database.model.EventEntityFields
import im.vector.matrix.android.internal.database.query.findMostSuitableStateEvent
import im.vector.matrix.android.internal.database.query.last
import im.vector.matrix.android.internal.database.query.where
import io.realm.Realm
import io.realm.RealmQuery

internal class RoomMemberExtractor(private val realm: Realm,
                                   private val roomId: String) {

    fun extractFrom(event: EventEntity): RoomMember? {
        val stateIndex = event.stateIndex

        // First of all, try to get the most suitable state event coming from a chunk
        return buildQuery(realm, roomId, event.sender)
                .findMostSuitableStateEvent(stateIndex = stateIndex)
                ?.asDomain()
                ?.pickContent(stateIndex)

        // If the content is null, we try get the last state event, not coming from a chunk
                ?: buildQuery(realm, roomId, event.sender)
                        .last()
                        ?.asDomain()
                        ?.content()

    }

    private fun buildQuery(realm: Realm, roomId: String, sender: String?): RealmQuery<EventEntity> {
        return EventEntity
                .where(realm, roomId, EventType.STATE_ROOM_MEMBER)
                .equalTo(EventEntityFields.STATE_KEY, sender)
    }


}