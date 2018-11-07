package im.vector.matrix.android.internal.session.events.prune

import arrow.core.Option
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.internal.database.RealmLiveEntityObserver
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.database.mapper.asEntity
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.database.query.where
import io.realm.Realm
import io.realm.RealmResults

internal class EventsPruner(monarchy: Monarchy) :
        RealmLiveEntityObserver<EventEntity>(monarchy) {

    override val query = Monarchy.Query<EventEntity> { EventEntity.where(it, type = EventType.REDACTION) }

    override fun process(results: RealmResults<EventEntity>, indexes: IntArray) {
        val redactionEvents = results.map { it.asDomain() }
        monarchy.writeAsync { realm ->
            indexes.forEach { index ->
                val data = redactionEvents[index]
                pruneEvent(realm, data)
            }
        }
    }

    private fun pruneEvent(realm: Realm, redactionEvent: Event?) {
        if (redactionEvent == null || redactionEvent.redacts.isNullOrEmpty()) {
            return
        }
        val eventToPrune = EventEntity.where(realm, eventId = redactionEvent.redacts).findFirst()?.asDomain()
                ?: return

        val allowedKeys = computeAllowedKeys(eventToPrune.type)
        val prunedContent = allowedKeys.fold(
                { eventToPrune.content },
                { eventToPrune.content?.filterKeys { key -> it.contains(key) } }
        )
        val eventToPruneEntity = eventToPrune.copy(content = prunedContent).asEntity()
        realm.insertOrUpdate(eventToPruneEntity)
    }

    private fun computeAllowedKeys(type: String): Option<List<String>> {
        // Add filtered content, allowed keys in content depends on the event type
        val result = when (type) {
            EventType.STATE_ROOM_MEMBER -> listOf("membership")
            EventType.STATE_ROOM_CREATE -> listOf("creator")
            EventType.STATE_ROOM_JOIN_RULES -> listOf("join_rule")
            EventType.STATE_ROOM_POWER_LEVELS -> listOf("users",
                    "users_default",
                    "events",
                    "events_default",
                    "state_default",
                    "ban",
                    "kick",
                    "redact",
                    "invite")
            EventType.STATE_ROOM_ALIASES -> listOf("aliases")
            EventType.STATE_CANONICAL_ALIAS -> listOf("alias")
            EventType.FEEDBACK -> listOf("type", "target_event_id")
            else -> null
        }
        return Option.fromNullable(result)
    }
}