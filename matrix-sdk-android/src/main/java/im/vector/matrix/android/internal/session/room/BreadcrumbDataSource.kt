package im.vector.matrix.android.internal.session.room

import com.squareup.sqldelight.Query
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import im.vector.matrix.android.api.session.room.model.Breadcrumb
import im.vector.matrix.android.internal.database.mapper.BreadcrumbMapper
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import im.vector.matrix.sqldelight.session.SessionDatabase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

internal class BreadcrumbDataSource @Inject constructor(private val sessionDatabase: SessionDatabase,
                                                        private val coroutineDispatchers: MatrixCoroutineDispatchers,
                                                        private val breadcrumbMapper: BreadcrumbMapper) {

    fun getBreadcrumbs(): List<Breadcrumb> {
        return breadcrumbsQuery().executeAsList()
    }

    fun getBreadcrumbsLive(): Flow<List<Breadcrumb>> {
        return breadcrumbsQuery().asFlow().mapToList(coroutineDispatchers.dbQuery)
    }

    private fun breadcrumbsQuery(): Query<Breadcrumb> {
        return sessionDatabase.breadcrumbsQueries.getAllBreadcrumbs(breadcrumbMapper::map)
    }

}
