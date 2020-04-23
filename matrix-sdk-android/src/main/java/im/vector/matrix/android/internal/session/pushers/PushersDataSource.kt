package im.vector.matrix.android.internal.session.pushers

import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import im.vector.matrix.android.api.session.pushers.Pusher
import im.vector.matrix.android.internal.database.mapper.PushersMapper
import im.vector.matrix.sqldelight.session.SessionDatabase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

internal class PushersDataSource @Inject constructor(private val sessionDatabase: SessionDatabase,
                                                     private val pushersMapper: PushersMapper) {

    fun getPushersLive(): Flow<List<Pusher>> {
        return sessionDatabase.pushersQueries.getAll(pushersMapper::map)
                .asFlow()
                .mapToList()
    }

    fun getPushers(): List<Pusher> {
        return sessionDatabase.pushersQueries.getAll(pushersMapper::map).executeAsList()
    }


}
