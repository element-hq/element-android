package im.vector.matrix.android.internal.session.events.prune

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import arrow.core.Option
import com.squareup.moshi.JsonClass
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.internal.database.DatabaseInstances
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.database.mapper.asEntity
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.util.WorkerParamsFactory
import im.vector.matrix.android.internal.util.tryTransactionAsync
import io.realm.Realm
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

internal class PruneEventWorker(context: Context,
                                workerParameters: WorkerParameters
) : Worker(context, workerParameters), KoinComponent {

    @JsonClass(generateAdapter = true)
    internal data class Params(
            val redactionEvents: List<Event>,
            val updateIndexes: List<Int>,
            val deletionIndexes: List<Int>
    )

    private val dbInstances by inject<DatabaseInstances>()

    override fun doWork(): Result {
        val params = WorkerParamsFactory.fromData<Params>(inputData)
                     ?: return Result.FAILURE

        val result = dbInstances.disk.tryTransactionAsync { realm ->
            params.updateIndexes.forEach { index ->
                val data = params.redactionEvents[index]
                pruneEvent(realm, data)
            }
        }
        return result.fold({ Result.RETRY }, { Result.SUCCESS })
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
            EventType.STATE_ROOM_MEMBER       -> listOf("membership")
            EventType.STATE_ROOM_CREATE       -> listOf("creator")
            EventType.STATE_ROOM_JOIN_RULES   -> listOf("join_rule")
            EventType.STATE_ROOM_POWER_LEVELS -> listOf("users",
                                                        "users_default",
                                                        "events",
                                                        "events_default",
                                                        "state_default",
                                                        "ban",
                                                        "kick",
                                                        "redact",
                                                        "invite")
            EventType.STATE_ROOM_ALIASES      -> listOf("aliases")
            EventType.STATE_CANONICAL_ALIAS   -> listOf("alias")
            EventType.FEEDBACK                -> listOf("type", "target_event_id")
            else                              -> null
        }
        return Option.fromNullable(result)
    }

}