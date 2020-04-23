package im.vector.matrix.android.internal.session.user

import androidx.paging.PagedList
import com.squareup.sqldelight.android.paging.QueryDataSourceFactory
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import im.vector.matrix.android.api.session.user.model.User
import im.vector.matrix.android.api.util.Optional
import im.vector.matrix.android.internal.database.mapper.UserMapper
import im.vector.matrix.android.internal.extensions.mapToOneOptionnal
import im.vector.matrix.android.internal.util.FlowPagedListBuilder
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import im.vector.matrix.sqldelight.session.SessionDatabase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

internal class UserDataSource @Inject constructor(private val sessionDatabase: SessionDatabase,
                                                  private val coroutineDispatchers: MatrixCoroutineDispatchers,
                                                  private val userMapper: UserMapper) {

    fun getUser(userId: String): User? {
        return sessionDatabase.userQueries.get(userId, userMapper::map).executeAsOneOrNull()
    }

    fun getUserLive(userId: String): Flow<Optional<User>> {
        return sessionDatabase.userQueries.get(userId, userMapper::map)
                .asFlow()
                .mapToOneOptionnal(coroutineDispatchers.dbQuery)
    }

    fun getUsersLive(): Flow<List<User>> {
        return sessionDatabase.userQueries.getAllUsers(userMapper::map)
                .asFlow()
                .mapToList(coroutineDispatchers.dbQuery)
    }

    fun getPagedUsersLive(filter: String?): Flow<PagedList<User>> {
        val queryDataSourceFactory = QueryDataSourceFactory(
                queryProvider = sessionDatabase.userQueries::getAllUsersPaged,
                countQuery = sessionDatabase.userQueries.countUsers()
        ).map {
            userMapper.map(it)
        }
        val config = PagedList.Config.Builder().setPageSize(30).build()
        return FlowPagedListBuilder(queryDataSourceFactory, config).buildFlow()
    }

    fun getAllIgnoredIds(): List<String> {
        return sessionDatabase.userQueries.getAllIgnoredIds().executeAsList()
    }

    fun getIgnoredUsersLive(): Flow<List<User>> {
        return sessionDatabase.userQueries.getAllIgnored(userMapper::map).asFlow().mapToList(coroutineDispatchers.dbQuery)
    }

}
