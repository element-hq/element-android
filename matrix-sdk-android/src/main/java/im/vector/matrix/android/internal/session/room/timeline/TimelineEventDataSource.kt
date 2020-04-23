package im.vector.matrix.android.internal.session.room.timeline

import com.squareup.sqldelight.runtime.coroutines.asFlow
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.matrix.android.api.util.Optional
import im.vector.matrix.android.internal.database.mapper.TimelineEventMapper
import im.vector.matrix.android.internal.extensions.mapToOneOptionnal
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import im.vector.matrix.sqldelight.session.SessionDatabase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

internal class TimelineEventDataSource @Inject constructor(private val sessionDatabase: SessionDatabase,
                                                           private val coroutineDispatchers: MatrixCoroutineDispatchers,
                                                           private val timelineEventMapper: TimelineEventMapper) {

    fun getTimeLineEvent(eventId: String): TimelineEvent? {
        return sessionDatabase.timelineEventQueries.get(eventId, timelineEventMapper::map).executeAsOneOrNull()
    }

    fun getTimeLineEventLive(eventId: String): Flow<Optional<TimelineEvent>> {
        return sessionDatabase.timelineEventQueries.get(eventId, timelineEventMapper::map)
                .asFlow()
                .mapToOneOptionnal(coroutineDispatchers.dbQuery)
    }

}
