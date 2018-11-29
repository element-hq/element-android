package im.vector.matrix.android.internal.session.room.members

import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.room.model.RoomMember
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.database.model.EventEntityFields
import im.vector.matrix.android.internal.database.query.last
import im.vector.matrix.android.internal.database.query.next
import im.vector.matrix.android.internal.database.query.where
import io.realm.Realm
import io.realm.RealmQuery

internal class RoomMemberExtractor(private val realm: Realm,
                                   private val roomId: String) {

    fun extractFrom(event: EventEntity): RoomMember? {
        val sender = event.sender ?: return null
        // If the event is unlinked we want to fetch unlinked state events
        val unlinked = event.isUnlinked
        // When stateIndex is negative, we try to get the next stateEvent prevContent()
        // If prevContent is null we fallback to the Int.MIN state events content()
        return if (event.stateIndex <= 0) {
            baseQuery(realm, roomId, sender, unlinked).next(from = event.stateIndex)?.asDomain()?.prevContent()
                    ?: baseQuery(realm, roomId, sender, unlinked).last(since = event.stateIndex)?.asDomain()?.content()
        } else {
            baseQuery(realm, roomId, sender, unlinked).last(since = event.stateIndex)?.asDomain()?.content()
        }
    }

    private fun baseQuery(realm: Realm,
                          roomId: String,
                          sender: String,
                          isUnlinked: Boolean): RealmQuery<EventEntity> {
        val filterMode = if (isUnlinked) EventEntity.LinkFilterMode.UNLINKED_ONLY else EventEntity.LinkFilterMode.LINKED_ONLY

        return EventEntity
                .where(realm, roomId = roomId, type = EventType.STATE_ROOM_MEMBER, linkFilterMode = filterMode)
                .equalTo(EventEntityFields.STATE_KEY, sender)

    }


}