/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.matrix.android.internal.session.room.prune

import com.squareup.sqldelight.Query
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.internal.database.SqlLiveEntityObserver
import im.vector.matrix.android.internal.database.awaitTransaction
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import im.vector.matrix.sqldelight.session.EventInsertNotification
import im.vector.matrix.sqldelight.session.SessionDatabase
import javax.inject.Inject

/**
 * Listens to the database for the insertion of any redaction event.
 * As it will actually delete the content, it should be called last in the list of listener.
 */
internal class EventsPruner @Inject constructor(sessionDatabase: SessionDatabase,
                                                private val coroutineDispatchers: MatrixCoroutineDispatchers,
                                                private val pruneEventTask: PruneEventTask) :
        SqlLiveEntityObserver<EventInsertNotification>(sessionDatabase) {

    override val query: Query<EventInsertNotification>
        get() = sessionDatabase.observerTriggerQueries.getAllEventInsertNotifications(types = listOf(EventType.REDACTION))

    override suspend fun handleChanges(results: List<EventInsertNotification>) {
        pruneEventTask.execute(PruneEventTask.Params(results))
        sessionDatabase.awaitTransaction(coroutineDispatchers) {
            val notificationIds = results.map { it.event_id }
            sessionDatabase.observerTriggerQueries.deleteEventInsertNotifications(notificationIds)
        }
    }
}
