/*
 * Copyright 2020 New Vector Ltd
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
 *
 */

package im.vector.matrix.android.internal.auth.sqlite

import im.vector.matrix.android.internal.auth.PendingSessionStore
import im.vector.matrix.android.internal.auth.registration.PendingSessionData
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import im.vector.matrix.sqldelight.auth.PendingSessionQueries
import im.vector.matrix.sqldelight.auth.AuthDatabase
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class SqlitePendingSessionStore @Inject constructor(database: AuthDatabase,
                                                             private val mapper: SqlitePendingSessionMapper,
                                                             private val coroutineDispatchers: MatrixCoroutineDispatchers) : PendingSessionStore {

    private val pendingSessionQueries: PendingSessionQueries = database.pendingSessionQueries

    override suspend fun savePendingSessionData(pendingSessionData: PendingSessionData) = withContext(coroutineDispatchers.dbTransaction) {
        val pendingSessionEntity = mapper.map(pendingSessionData)
        if (pendingSessionEntity != null) {
            pendingSessionQueries.transaction {
                pendingSessionQueries.delete()
                pendingSessionQueries.insertOrUpdate(pendingSessionEntity)
            }
        }
    }

    override fun getPendingSessionData(): PendingSessionData? {
        val pendingSessionEntity = pendingSessionQueries.get().executeAsOneOrNull()
        return mapper.map(pendingSessionEntity)
    }

    override suspend fun delete() = withContext(coroutineDispatchers.dbTransaction) {
        pendingSessionQueries.delete()
    }

}
