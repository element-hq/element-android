/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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
package org.matrix.android.sdk.internal.session.pushers

import androidx.lifecycle.LiveData
import androidx.work.BackoffPolicy
import com.zhuinden.monarchy.Monarchy
import org.matrix.android.sdk.api.session.pushers.HttpPusher
import org.matrix.android.sdk.api.session.pushers.Pusher
import org.matrix.android.sdk.api.session.pushers.PushersService
import org.matrix.android.sdk.internal.database.mapper.asDomain
import org.matrix.android.sdk.internal.database.model.PusherEntity
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.di.SessionId
import org.matrix.android.sdk.internal.di.WorkManagerProvider
import org.matrix.android.sdk.internal.session.pushers.gateway.PushGatewayNotifyTask
import org.matrix.android.sdk.internal.session.workmanager.WorkManagerConfig
import org.matrix.android.sdk.internal.task.TaskExecutor
import org.matrix.android.sdk.internal.task.configureWith
import org.matrix.android.sdk.internal.worker.WorkerParamsFactory
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject

internal class DefaultPushersService @Inject constructor(
        private val workManagerProvider: WorkManagerProvider,
        @SessionDatabase private val monarchy: Monarchy,
        @SessionId private val sessionId: String,
        private val getPusherTask: GetPushersTask,
        private val pushGatewayNotifyTask: PushGatewayNotifyTask,
        private val addPusherTask: AddPusherTask,
        private val togglePusherTask: TogglePusherTask,
        private val removePusherTask: RemovePusherTask,
        private val taskExecutor: TaskExecutor,
        private val workManagerConfig: WorkManagerConfig,
) : PushersService {

    override suspend fun testPush(
            url: String,
            appId: String,
            pushkey: String,
            eventId: String
    ) {
        pushGatewayNotifyTask.execute(PushGatewayNotifyTask.Params(url, appId, pushkey, eventId))
    }

    override fun refreshPushers() {
        getPusherTask
                .configureWith()
                .executeBy(taskExecutor)
    }

    override fun enqueueAddHttpPusher(httpPusher: HttpPusher): UUID {
        return enqueueAddPusher(httpPusher.toJsonPusher())
    }

    override suspend fun addHttpPusher(httpPusher: HttpPusher) {
        addPusherTask.execute(AddPusherTask.Params(httpPusher.toJsonPusher()))
    }

    private fun HttpPusher.toJsonPusher() = JsonPusher(
            pushKey = pushkey,
            kind = "http",
            appId = appId,
            profileTag = profileTag,
            lang = lang,
            appDisplayName = appDisplayName,
            deviceDisplayName = deviceDisplayName,
            data = JsonPusherData(url, EVENT_ID_ONLY.takeIf { withEventIdOnly }),
            append = append,
            enabled = enabled,
            deviceId = deviceId,
    )

    override suspend fun addEmailPusher(
            email: String,
            lang: String,
            emailBranding: String,
            appDisplayName: String,
            deviceDisplayName: String,
            append: Boolean
    ) {
        addPusherTask.execute(
                AddPusherTask.Params(
                        JsonPusher(
                                pushKey = email,
                                kind = Pusher.KIND_EMAIL,
                                appId = Pusher.APP_ID_EMAIL,
                                profileTag = "",
                                lang = lang,
                                appDisplayName = appDisplayName,
                                deviceDisplayName = deviceDisplayName,
                                data = JsonPusherData(brand = emailBranding),
                                append = append
                        )
                )
        )
    }

    override suspend fun togglePusher(pusher: Pusher, enable: Boolean) {
        togglePusherTask.execute(TogglePusherTask.Params(pusher.toJsonPusher(), enable))
    }

    private fun Pusher.toJsonPusher() = JsonPusher(
            pushKey = pushKey,
            kind = kind,
            appId = appId,
            appDisplayName = appDisplayName,
            deviceDisplayName = deviceDisplayName,
            profileTag = profileTag,
            lang = lang,
            data = JsonPusherData(data.url, data.format),
            append = false,
            enabled = enabled,
            deviceId = deviceId,
    )

    private fun enqueueAddPusher(pusher: JsonPusher): UUID {
        val params = AddPusherWorker.Params(sessionId, pusher)
        val request = workManagerProvider.matrixOneTimeWorkRequestBuilder<AddPusherWorker>()
                .setConstraints(WorkManagerProvider.getWorkConstraints(workManagerConfig))
                .setInputData(WorkerParamsFactory.toData(params))
                .setBackoffCriteria(BackoffPolicy.LINEAR, WorkManagerProvider.BACKOFF_DELAY_MILLIS, TimeUnit.MILLISECONDS)
                .build()
        workManagerProvider.workManager.enqueue(request)
        return request.id
    }

    override suspend fun removePusher(pusher: Pusher) {
        removePusher(pusher.pushKey, pusher.appId)
    }

    override suspend fun removeHttpPusher(pushkey: String, appId: String) {
        removePusher(pushkey, appId)
    }

    override suspend fun removeEmailPusher(email: String) {
        removePusher(pushKey = email, Pusher.APP_ID_EMAIL)
    }

    private suspend fun removePusher(pushKey: String, pushAppId: String) {
        val params = RemovePusherTask.Params(pushKey, pushAppId)
        removePusherTask.execute(params)
    }

    override fun getPushersLive(): LiveData<List<Pusher>> {
        return monarchy.findAllMappedWithChanges(
                { realm -> PusherEntity.where(realm) },
                { it.asDomain() }
        )
    }

    override fun getPushers(): List<Pusher> {
        return monarchy.fetchAllCopiedSync { PusherEntity.where(it) }.map { it.asDomain() }
    }

    companion object {
        const val EVENT_ID_ONLY = "event_id_only"
    }
}
