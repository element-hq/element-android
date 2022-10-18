/*
 * Copyright (c) 2021 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.session.pushers

import com.zhuinden.monarchy.Monarchy
import org.matrix.android.sdk.internal.database.model.PusherEntity
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.RequestExecutor
import org.matrix.android.sdk.internal.task.Task
import org.matrix.android.sdk.internal.util.awaitTransaction
import javax.inject.Inject

internal interface TogglePusherTask : Task<TogglePusherTask.Params, Unit> {
    data class Params(val pusher: JsonPusher, val enable: Boolean)
}

internal class DefaultTogglePusherTask @Inject constructor(
        private val pushersAPI: PushersAPI,
        @SessionDatabase private val monarchy: Monarchy,
        private val requestExecutor: RequestExecutor,
        private val globalErrorReceiver: GlobalErrorReceiver
) : TogglePusherTask {

    override suspend fun execute(params: TogglePusherTask.Params) {
        val pusher = params.pusher.copy(enabled = params.enable)

        requestExecutor.executeRequest(globalErrorReceiver) {
            pushersAPI.setPusher(pusher)
        }

        monarchy.awaitTransaction { realm ->
            val entity = PusherEntity.where(realm, params.pusher.pushKey).findFirst()
            entity?.apply { enabled = params.enable }?.let { realm.insertOrUpdate(it) }
        }
    }
}
