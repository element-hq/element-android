package im.vector.matrix.android.internal.session.room

import com.squareup.sqldelight.Query
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import com.squareup.sqldelight.runtime.coroutines.mapToOne
import im.vector.matrix.android.api.session.room.RoomSummaryQueryParams
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.matrix.android.api.util.Optional
import im.vector.matrix.android.api.util.toOptional
import im.vector.matrix.android.internal.database.mapper.RoomSummaryMapper
import im.vector.matrix.android.internal.database.mapper.map
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import im.vector.matrix.sqldelight.session.RoomSummaryWithTimeline
import im.vector.matrix.sqldelight.session.SessionDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

internal class RoomSummaryDataSource @Inject constructor(private val roomSummaryMapper: RoomSummaryMapper,
                                                         private val roomTagMapper: RoomTagMapper,
                                                         private val coroutineDispatchers: MatrixCoroutineDispatchers,
                                                         private val sessionDatabase: SessionDatabase) {

    fun getRoomSummary(roomIdOrAlias: String): RoomSummary? {
        val roomId = (if (roomIdOrAlias.startsWith("!")) {
            // It's a roomId
            roomIdOrAlias
        } else {
            // Assume it's a room alias
            sessionDatabase.roomSummaryQueries.getRoomIdWithAlias(roomIdOrAlias).executeAsOneOrNull()
        })
                ?: return null

        val tags = sessionDatabase.roomTagQueries.getAllForRoom(roomId, roomTagMapper::map).executeAsList()
        return sessionDatabase.roomSummaryQueries
                .get(roomId)
                .executeAsOneOrNull()
                ?.let {
                    roomSummaryMapper.map(it, tags)
                }
    }

    fun getRoomSummaryLive(roomId: String): Flow<Optional<RoomSummary>> {
        val tagsFlow = sessionDatabase.roomTagQueries.getAllForRoom(roomId, roomTagMapper::map).asFlow().mapToList(coroutineDispatchers.dbQuery)
        val roomSummaryFlow = sessionDatabase.roomSummaryQueries.get(roomId).asFlow().mapToOne(coroutineDispatchers.dbQuery)
        return tagsFlow.combine(roomSummaryFlow) { tags, roomSummaryWithTimeline ->
            roomSummaryMapper.map(roomSummaryWithTimeline, tags).toOptional()
        }
    }

    fun getRoomSummaries(queryParams: RoomSummaryQueryParams): List<RoomSummary> {
        return roomSummariesQuery(queryParams)
                .executeAsList()
                .map {
                    val tags = sessionDatabase.roomTagQueries.getAllForRoom(it.summary_room_id, roomTagMapper::map).executeAsList()
                    roomSummaryMapper.map(it, tags)
                }
    }

    fun getRoomSummariesLive(queryParams: RoomSummaryQueryParams): Flow<List<RoomSummary>> {
        val tagsFlow = sessionDatabase.roomTagQueries.getAll().asFlow().mapToList(coroutineDispatchers.dbQuery)
        val roomSummaryFlow = roomSummariesQuery(queryParams).asFlow().mapToList(coroutineDispatchers.dbQuery)
        return tagsFlow.combine(roomSummaryFlow) { tags, summaries ->
            val groupedTags = tags.groupBy { it.room_id }
            summaries.map {
                val tagsForRoom = groupedTags
                        .getOrElse(it.summary_room_id, { emptyList() })
                        .map { tagEntity ->
                            roomTagMapper.map(tagEntity)
                        }
                roomSummaryMapper.map(it, tagsForRoom)
            }
        }

    }

    private fun roomSummariesQuery(queryParams: RoomSummaryQueryParams): Query<RoomSummaryWithTimeline> {
        val memberships = queryParams.memberships.map()
        val (fromGroupIdFlag, groupId) = if (queryParams.fromGroupId != null) {
            Pair(1L, queryParams.fromGroupId)
        } else {
            Pair(0L, "")
        }
        return sessionDatabase.roomSummaryQueries.selectWithParams(
                fromGroupIdFlag = fromGroupIdFlag,
                groupId = groupId,
                memberships = memberships
        )
    }

}
