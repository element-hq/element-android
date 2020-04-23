package im.vector.matrix.android.internal.database.repository

import com.squareup.sqldelight.runtime.coroutines.asFlow
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.util.Optional
import im.vector.matrix.android.api.util.toOptional
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.extensions.mapToOneOptionnal
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import im.vector.matrix.sqldelight.session.EventEntity
import im.vector.matrix.sqldelight.session.SessionDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class CurrentStateEventDataSource @Inject constructor(private val sessionDatabase: SessionDatabase,
                                                               private val coroutineDispatchers: MatrixCoroutineDispatchers ) {

    fun getCurrentMapped(roomId: String, type: String, stateKey: String): Event? {
        val entity = sessionDatabase.stateEventQueries.getCurrentStateEvent(roomId, type = type, stateKey = stateKey).executeAsOneOrNull()
        return entity?.asDomain()
    }

    fun getCurrentLiveMapped(roomId: String, type: String, stateKey: String): Flow<Optional<Event>> {
        return sessionDatabase.stateEventQueries
                .getCurrentStateEvent(roomId, type = type, stateKey = stateKey)
                .asFlow()
                .map {
                    it.executeAsOneOrNull()?.asDomain().toOptional()
                }
    }

    fun getCurrentLive(roomId: String, type: String, stateKey: String): Flow<Optional<EventEntity>> {
        return sessionDatabase.stateEventQueries
                .getCurrentStateEvent(roomId, type = type, stateKey = stateKey)
                .asFlow()
                .mapToOneOptionnal(coroutineDispatchers.dbQuery)
    }

    fun getCurrent(roomId: String, type: String, stateKey: String): EventEntity? {
        return sessionDatabase.stateEventQueries.getCurrentStateEvent(roomId, type = type, stateKey = stateKey).executeAsOneOrNull()
    }
}
