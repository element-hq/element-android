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

import androidx.lifecycle.LiveData
import androidx.work.*
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.auth.data.SessionParams
import im.vector.matrix.android.api.session.pushers.Pusher
import im.vector.matrix.android.api.session.pushers.PusherState
import im.vector.matrix.android.api.session.pushers.PushersService
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.database.mapper.toEntity
import im.vector.matrix.android.internal.database.model.PusherEntity
import im.vector.matrix.android.internal.database.model.PusherEntityFields
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.configureWith
import im.vector.matrix.android.internal.util.WorkerParamsFactory
import java.util.*
import java.util.concurrent.TimeUnit


internal class DefaultPusherService(
        private val monarchy: Monarchy,
        private val sessionParam: SessionParams,
        private val getPusherTask: GetPushersTask,
        private val removePusherTask: RemovePusherTask,
        private val taskExecutor: TaskExecutor
) : PushersService {


    override fun refreshPushers() {
        getPusherTask
                .configureWith(Unit)
                .dispatchTo(object : MatrixCallback<GetPushersResponse> {
                    override fun onSuccess(data: GetPushersResponse) {
                        monarchy.runTransactionSync { realm ->
                            //clear existings?
                            realm.where(PusherEntity::class.java)
                                    .equalTo(PusherEntityFields.USER_ID, sessionParam.credentials.userId)
                                    .findAll().deleteAllFromRealm()
                            data.pushers?.forEach { jsonPusher ->
                                jsonPusher.toEntity(sessionParam.credentials.userId).also {
                                    it.state = PusherState.REGISTERED
                                    realm.insertOrUpdate(it)
                                }
                            }
                        }
                    }
                })
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


        val params = AddHttpPusherWorker.Params(pusher, sessionParam.credentials.userId)

        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        val request = OneTimeWorkRequestBuilder<AddHttpPusherWorker>()
                .setConstraints(constraints)
                .setInputData(WorkerParamsFactory.toData(params))
                .setBackoffCriteria(BackoffPolicy.LINEAR, 10_000L, TimeUnit.MILLISECONDS)
                .build()
        WorkManager.getInstance().enqueue(request)
        return request.id
    }

    override fun removeHttpPusher(pushkey: String, appId: String, callback: MatrixCallback<Unit>) {
        val params = RemovePusherTask.Params(sessionParam.credentials.userId,pushkey,appId)
        removePusherTask
                .configureWith(params)
                .dispatchTo(callback)
                //.enableRetry() ??
                .executeBy(taskExecutor)
    }

    override fun livePushers(): LiveData<List<Pusher>> {
        return monarchy.findAllMappedWithChanges(
                { realm -> PusherEntity.where(realm, sessionParam.credentials.userId) },
                { it.asDomain() }
        )
    }
}