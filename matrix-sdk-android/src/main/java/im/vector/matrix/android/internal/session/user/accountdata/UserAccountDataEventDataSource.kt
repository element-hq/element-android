package im.vector.matrix.android.internal.session.user.accountdata

import com.squareup.moshi.Moshi
import com.squareup.sqldelight.Query
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import im.vector.matrix.android.api.util.JSON_DICT_PARAMETERIZED_TYPE
import im.vector.matrix.android.api.util.Optional
import im.vector.matrix.android.internal.extensions.mapToOneOptionnal
import im.vector.matrix.android.internal.session.sync.model.accountdata.UserAccountDataEvent
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import im.vector.matrix.sqldelight.session.SessionDatabase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

internal class UserAccountDataEventDataSource @Inject constructor(private val sessionDatabase: SessionDatabase,
                                                                  private val coroutineDispatchers: MatrixCoroutineDispatchers,
                                                                  moshi: Moshi) {

    private val adapter = moshi.adapter<Map<String, Any>>(JSON_DICT_PARAMETERIZED_TYPE)


    fun getAccountDataEvent(type: String): UserAccountDataEvent? {
        return getAccountDataEvents(setOf(type)).firstOrNull()
    }

    fun getLiveAccountDataEvent(type: String): Flow<Optional<UserAccountDataEvent>> {
        return getAccountDataEventsQuery(setOf(type)).asFlow().mapToOneOptionnal(coroutineDispatchers.dbQuery)
    }

    fun getAccountDataEvents(types: Set<String>): List<UserAccountDataEvent> {
        return getAccountDataEventsQuery(types).executeAsList()
    }

    fun getLiveAccountDataEvents(types: Set<String>): Flow<List<UserAccountDataEvent>> {
        return getAccountDataEventsQuery(types).asFlow().mapToList(coroutineDispatchers.dbQuery)
    }

    private fun getAccountDataEventsQuery(types: Set<String> = emptySet()): Query<UserAccountDataEvent> {
        return if (types.isEmpty()) {
            sessionDatabase.userAccountDataQueries.getAll(::map)
        } else {
            sessionDatabase.userAccountDataQueries.getAllWithType(types, ::map)
        }
    }


    private fun map(type: String, content: String?): UserAccountDataEvent {
        return UserAccountDataEvent(
                type = type,
                content = content?.let { adapter.fromJson(it) } ?: emptyMap()
        )
    }
}
