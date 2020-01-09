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
package im.vector.matrix.android.internal.session.pushers

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.work.BackoffPolicy
import androidx.work.WorkManager
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.pushers.Pusher
import im.vector.matrix.android.api.session.pushers.PushersService
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.database.model.PusherEntity
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.di.UserId
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.configureWith
import im.vector.matrix.android.internal.worker.WorkManagerUtil
import im.vector.matrix.android.internal.worker.WorkManagerUtil.matrixOneTimeWorkRequestBuilder
import im.vector.matrix.android.internal.worker.WorkerParamsFactory
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject

internal class DefaultPusherService @Inject constructor(private val context: Context,
                                                        private val monarchy: Monarchy,
                                                        @UserId private val userId: String,
                                                        private val getPusherTask: GetPushersTask,
                                                        private val removePusherTask: RemovePusherTask,
                                                        private val taskExecutor: TaskExecutor
) : PushersService {

    override fun refreshPushers() {
        getPusherTask
                .configureWith()
                .executeBy(taskExecutor)
    }

    override fun addHttpPusher(pushkey: String, appId: String, profileTag: String,
                               lang: String, appDisplayName: String, deviceDisplayName: String,
                               url: String, append: Boolean, withEventIdOnly: Boolean)
            : UUID {
        val pusher = JsonPusher(
                pushKey = pushkey,
                kind = "http",
                appId = appId,
                appDisplayName = appDisplayName,
                deviceDisplayName = deviceDisplayName,
                profileTag = profileTag,
                lang = lang,
                data = JsonPusherData(url, if (withEventIdOnly) PushersService.EVENT_ID_ONLY else null),
                append = append)

        val params = AddHttpPusherWorker.Params(pusher, userId)

        val request = matrixOneTimeWorkRequestBuilder<AddHttpPusherWorker>()
                .setConstraints(WorkManagerUtil.workConstraints)
                .setInputData(WorkerParamsFactory.toData(params))
                .setBackoffCriteria(BackoffPolicy.LINEAR, 10_000L, TimeUnit.MILLISECONDS)
                .build()
        WorkManager.getInstance(context).enqueue(request)
        return request.id
    }

    override fun removeHttpPusher(pushkey: String, appId: String, callback: MatrixCallback<Unit>) {
        val params = RemovePusherTask.Params(pushkey, appId)
        removePusherTask
                .configureWith(params) {
                    this.callback = callback
                }
                // .enableRetry() ??
                .executeBy(taskExecutor)
    }

    override fun getPushersLive(): LiveData<List<Pusher>> {
        return monarchy.findAllMappedWithChanges(
                { realm -> PusherEntity.where(realm) },
                { it.asDomain() }
        )
    }

    override fun pushers(): List<Pusher> {
        return monarchy.fetchAllCopiedSync { PusherEntity.where(it) }.map { it.asDomain() }
    }
}
