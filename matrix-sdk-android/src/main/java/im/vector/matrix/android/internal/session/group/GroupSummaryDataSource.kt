package im.vector.matrix.android.internal.session.group

import com.squareup.sqldelight.Query
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import im.vector.matrix.android.api.session.group.GroupSummaryQueryParams
import im.vector.matrix.android.api.session.group.model.GroupSummary
import im.vector.matrix.android.internal.database.mapper.GroupSummaryMapper
import im.vector.matrix.android.internal.database.mapper.map
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import im.vector.matrix.sqldelight.session.SessionDatabase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

internal class GroupSummaryDataSource @Inject constructor(private val sessionDatabase: SessionDatabase,
                                                          private val coroutineDispatchers: MatrixCoroutineDispatchers,
                                                          private val groupSummaryMapper: GroupSummaryMapper) {

    fun getGroupSummary(groupId: String): GroupSummary? {
        return sessionDatabase.groupSummaryQueries.getGroupSummary(groupId, groupSummaryMapper::map).executeAsOneOrNull()
    }

    fun getGroupSummaries(queryParams: GroupSummaryQueryParams): List<GroupSummary> {
        return groupSummariesQuery(queryParams).executeAsList()
    }

    fun getGroupSummariesLive(queryParams: GroupSummaryQueryParams): Flow<List<GroupSummary>> {
        return groupSummariesQuery(queryParams).asFlow().mapToList(coroutineDispatchers.dbQuery)
    }

    private fun groupSummariesQuery(queryParams: GroupSummaryQueryParams): Query<GroupSummary> {
        return sessionDatabase.groupSummaryQueries.getAllWithMemberships(
                queryParams.memberships.map(), groupSummaryMapper::map
        )
    }


}
