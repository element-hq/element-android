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

package im.vector.matrix.android.internal.session.group

import androidx.work.ExistingWorkPolicy
import com.squareup.sqldelight.Query
import im.vector.matrix.android.internal.database.SqlLiveEntityObserver
import im.vector.matrix.android.internal.database.awaitTransaction
import im.vector.matrix.android.internal.di.SessionId
import im.vector.matrix.android.internal.di.WorkManagerProvider
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import im.vector.matrix.android.internal.worker.WorkerParamsFactory
import im.vector.matrix.sqldelight.session.Memberships
import im.vector.matrix.sqldelight.session.SessionDatabase
import javax.inject.Inject

private const val GET_GROUP_DATA_WORKER = "GET_GROUP_DATA_WORKER"

internal class GroupSummaryUpdater @Inject constructor(
        private val workManagerProvider: WorkManagerProvider,
        private val coroutineDispatchers: MatrixCoroutineDispatchers,
        @SessionId private val sessionId: String,
        sessionDatabase: SessionDatabase)
    : SqlLiveEntityObserver<String>(sessionDatabase) {

    override val query: Query<String>
        get() = sessionDatabase.observerTriggerQueries.getAllGroupNotifications()

    override suspend fun handleChanges(results: List<String>) {
        val groupIdsToFetchData = sessionDatabase.groupQueries.getAllGroupIdsWithinIdsAndMemberships(
                groupIds = results,
                memberships = listOf(Memberships.JOIN, Memberships.INVITE)
        ).executeAsList()
        fetchGroupsData(groupIdsToFetchData)

        val groupIdsToDelete = sessionDatabase.groupQueries.getAllGroupIdsWithinIdsAndMemberships(
                groupIds = results,
                memberships = listOf(Memberships.LEAVE)
        ).executeAsList()

        sessionDatabase.awaitTransaction(coroutineDispatchers) {
            sessionDatabase.groupSummaryQueries.deleteGroupSummaries(groupIdsToDelete)
            sessionDatabase.observerTriggerQueries.deleteGroupNotifications(results)
        }
    }

    private fun fetchGroupsData(groupIds: List<String>) {
        val getGroupDataWorkerParams = GetGroupDataWorker.Params(sessionId, groupIds)

        val workData = WorkerParamsFactory.toData(getGroupDataWorkerParams)

        val getGroupWork = workManagerProvider.matrixOneTimeWorkRequestBuilder<GetGroupDataWorker>()
                .setInputData(workData)
                .setConstraints(WorkManagerProvider.workConstraints)
                .build()

        workManagerProvider.workManager
                .beginUniqueWork(GET_GROUP_DATA_WORKER, ExistingWorkPolicy.APPEND, getGroupWork)
                .enqueue()
    }

}
