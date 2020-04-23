package im.vector.matrix.android.internal.session.room.read

import com.squareup.sqldelight.runtime.coroutines.asFlow
import im.vector.matrix.android.api.util.Optional
import im.vector.matrix.android.internal.extensions.mapToOneOptionnal
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import im.vector.matrix.sqldelight.session.SessionDatabase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

internal class ReadMarkerDataSource @Inject constructor(private val sessionDatabase: SessionDatabase,
                                               private val coroutineDispatchers: MatrixCoroutineDispatchers) {

    fun getReadMarkerLive(roomId: String): Flow<Optional<String>> {
        return sessionDatabase.readMarkerQueries.get(roomId = roomId)
                .asFlow()
                .mapToOneOptionnal(coroutineDispatchers.dbQuery)
    }


}
