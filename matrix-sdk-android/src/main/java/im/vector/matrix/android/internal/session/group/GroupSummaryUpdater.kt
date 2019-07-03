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

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.internal.database.RealmLiveEntityObserver
import im.vector.matrix.android.internal.database.model.GroupEntity
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.di.SessionDatabase
import im.vector.matrix.android.internal.worker.WorkerParamsFactory
import io.realm.RealmConfiguration
import javax.inject.Inject

private const val GET_GROUP_DATA_WORKER = "GET_GROUP_DATA_WORKER"

internal class GroupSummaryUpdater @Inject constructor(private val context: Context,
                                                       private val credentials: Credentials,
                                                       @SessionDatabase realmConfiguration: RealmConfiguration) : RealmLiveEntityObserver<GroupEntity>(realmConfiguration) {

    override val query = Monarchy.Query<GroupEntity> { GroupEntity.where(it) }

    private val workConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

    override fun processChanges(inserted: List<GroupEntity>, updated: List<GroupEntity>, deleted: List<GroupEntity>) {
        val newGroupIds = inserted.map { it.groupId }
        val getGroupDataWorkerParams = GetGroupDataWorker.Params(credentials.userId, newGroupIds)
        val workData = WorkerParamsFactory.toData(getGroupDataWorkerParams)

        val sendWork = OneTimeWorkRequestBuilder<GetGroupDataWorker>()
                .setInputData(workData)
                .setConstraints(workConstraints)
                .build()

        WorkManager.getInstance(context)
                .beginUniqueWork(GET_GROUP_DATA_WORKER, ExistingWorkPolicy.APPEND, sendWork)
                .enqueue()
    }


}