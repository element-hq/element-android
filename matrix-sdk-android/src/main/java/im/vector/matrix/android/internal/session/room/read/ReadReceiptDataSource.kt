package im.vector.matrix.android.internal.session.room.read

import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import im.vector.matrix.android.api.session.room.model.ReadReceipt
import im.vector.matrix.android.api.util.Optional
import im.vector.matrix.android.internal.database.mapper.ReadReceiptMapper
import im.vector.matrix.android.internal.extensions.mapToOneOptionnal
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import im.vector.matrix.sqldelight.session.SessionDatabase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

internal class ReadReceiptDataSource @Inject constructor(private val sessionDatabase: SessionDatabase,
                                                private val coroutineDispatchers: MatrixCoroutineDispatchers,
                                                private val readReceiptMapper: ReadReceiptMapper) {

    fun getReadReceiptLive(roomId: String, userId: String): Flow<Optional<String>> {
        return sessionDatabase
                .readReceiptQueries.getEventIdForUser(roomId, userId)
                .asFlow()
                .mapToOneOptionnal(coroutineDispatchers.dbQuery)
    }

    fun getEventReadReceiptsLive(eventId: String): Flow<List<ReadReceipt>> {
        return sessionDatabase.readReceiptQueries.getAllForEvent(
                eventId = eventId,
                mapper = readReceiptMapper::map
        )
                .asFlow()
                .mapToList()
    }


}
