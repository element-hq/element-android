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
import org.matrix.android.sdk.api.session.pushers.Pusher
import org.matrix.android.sdk.api.session.pushers.PushersService
import org.matrix.android.sdk.internal.database.mapper.asDomain
import org.matrix.android.sdk.internal.database.model.PusherEntity
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.di.SessionId
import org.matrix.android.sdk.internal.di.WorkManagerProvider
import org.matrix.android.sdk.internal.session.pushers.gateway.PushGatewayNotifyTask
import org.matrix.android.sdk.internal.task.TaskExecutor
import org.matrix.android.sdk.internal.task.configureWith
import org.matrix.android.sdk.internal.worker.WorkerParamsFactory
import java.security.InvalidParameterException
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject

internal class DefaultPushersService @Inject constructor(
        private val workManagerProvider: WorkManagerProvider,
        @SessionDatabase private val monarchy: Monarchy,
        @SessionId private val sessionId: String,
        private val getPusherTask: GetPushersTask,
        private val pushGatewayNotifyTask: PushGatewayNotifyTask,
        private val removePusherTask: RemovePusherTask,
        private val taskExecutor: TaskExecutor
) : PushersService {

    override suspend fun testPush(url: String,
                                  appId: String,
                                  pushkey: String,
                                  eventId: String) {
        pushGatewayNotifyTask.execute(PushGatewayNotifyTask.Params(url, appId, pushkey, eventId))
    }

    override fun refreshPushers() {
        getPusherTask
                .configureWith()
                .executeBy(taskExecutor)
    }

    override fun addHttpPusher(pushkey: String,
                               appId: String,
                               profileTag: String,
                               lang: String,
                               appDisplayName: String,
                               deviceDisplayName: String,
                               url: String,
                               append: Boolean,
                               withEventIdOnly: Boolean)
            : UUID {
        // Do some parameter checks. It's ok to throw Exception, to inform developer of the problem
        if (pushkey.length > 512) throw InvalidParameterException("pushkey should not exceed 512 chars")
        if (appId.length > 64) throw InvalidParameterException("appId should not exceed 64 chars")
        if ("/_matrix/push/v1/notify" !in url) throw InvalidParameterException("url should contain '/_matrix/push/v1/notify'")

        val pusher = JsonPusher(
                pushKey = pushkey,
                kind = "http",
                appId = appId,
                appDisplayName = appDisplayName,
                deviceDisplayName = deviceDisplayName,
                profileTag = profileTag,
                lang = lang,
                data = JsonPusherData(url, EVENT_ID_ONLY.takeIf { withEventIdOnly }),
                append = append)

        val params = AddHttpPusherWorker.Params(sessionId, pusher)

        val request = workManagerProvider.matrixOneTimeWorkRequestBuilder<AddHttpPusherWorker>()
                .setConstraints(WorkManagerProvider.workConstraints)
                .setInputData(WorkerParamsFactory.toData(params))
                .setBackoffCriteria(BackoffPolicy.LINEAR, WorkManagerProvider.BACKOFF_DELAY_MILLIS, TimeUnit.MILLISECONDS)
                .build()
        workManagerProvider.workManager.enqueue(request)
        return request.id
    }

    override suspend fun removeHttpPusher(pushkey: String, appId: String) {
        val params = RemovePusherTask.Params(pushkey, appId)
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
