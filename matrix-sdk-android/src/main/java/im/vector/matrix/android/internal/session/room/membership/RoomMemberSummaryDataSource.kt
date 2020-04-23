package im.vector.matrix.android.internal.session.room.membership

import com.squareup.sqldelight.Query
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import im.vector.matrix.android.api.session.room.members.RoomMemberQueryParams
import im.vector.matrix.android.api.session.room.model.RoomMemberSummary
import im.vector.matrix.android.api.util.Optional
import im.vector.matrix.android.internal.database.mapper.RoomMemberSummaryMapper
import im.vector.matrix.android.internal.database.mapper.map
import im.vector.matrix.android.internal.di.UserId
import im.vector.matrix.android.internal.extensions.mapToOneOptionnal
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import im.vector.matrix.sqldelight.session.SessionDatabase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

internal class RoomMemberSummaryDataSource @Inject constructor(@UserId private val userId: String,
                                                               private val sessionDatabase: SessionDatabase,
                                                               private val coroutineDispatchers: MatrixCoroutineDispatchers,
                                                               private val roomMemberSummaryMapper: RoomMemberSummaryMapper) {


    fun getRoomMember(roomId: String, userId: String): RoomMemberSummary? {
        return getRoomMemberQuery(roomId, userId).executeAsOneOrNull()
    }

    fun getRoomMemberLive(roomId: String, userId: String): Flow<Optional<RoomMemberSummary>> {
        return getRoomMemberQuery(roomId, userId).asFlow().mapToOneOptionnal(coroutineDispatchers.dbQuery)
    }

    private fun getRoomMemberQuery(roomId: String, userId: String): Query<RoomMemberSummary> {
        return sessionDatabase.roomMemberSummaryQueries.get(
                userId = userId,
                roomId = roomId,
                mapper = roomMemberSummaryMapper::map
        )
    }

    fun getRoomMembers(roomId: String, queryParams: RoomMemberQueryParams): List<RoomMemberSummary> {
        return getRoomMembersQuery(roomId, queryParams).executeAsList()
    }

    fun getRoomMembersLive(roomId: String, queryParams: RoomMemberQueryParams): Flow<List<RoomMemberSummary>> {
        return getRoomMembersQuery(roomId, queryParams)
                .asFlow()
                .mapToList()
    }

    private fun getRoomMembersQuery(roomId: String, queryParams: RoomMemberQueryParams): Query<RoomMemberSummary> {
        val excluded = if (queryParams.excludeSelf) {
            listOf(userId)
        } else {
            emptyList()
        }
        return sessionDatabase.roomMemberSummaryQueries.getAllFromRoom(
                roomId = roomId,
                memberships = queryParams.memberships.map(),
                excludedIds = excluded,
                mapper = roomMemberSummaryMapper::map
        )
    }

    fun getNumberOfJoinedMembers(roomId: String): Int {
        return RoomMemberHelper(sessionDatabase, roomId).getNumberOfJoinedMembers()
    }


}
