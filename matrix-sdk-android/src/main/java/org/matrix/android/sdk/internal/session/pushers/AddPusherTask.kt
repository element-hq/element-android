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
import org.matrix.android.sdk.api.session.pushers.PusherState
import org.matrix.android.sdk.internal.database.mapper.toEntity
import org.matrix.android.sdk.internal.database.model.PusherEntity
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.RequestExecutor
import org.matrix.android.sdk.internal.task.Task
import org.matrix.android.sdk.internal.util.awaitTransaction
import javax.inject.Inject

internal interface AddPusherTask : Task<AddPusherTask.Params, Unit> {
    data class Params(val pusher: JsonPusher)
}

internal class DefaultAddPusherTask @Inject constructor(
        private val pushersAPI: PushersAPI,
        @SessionDatabase private val monarchy: Monarchy,
        private val requestExecutor: RequestExecutor,
        private val globalErrorReceiver: GlobalErrorReceiver
) : AddPusherTask {
    override suspend fun execute(params: AddPusherTask.Params) {
        val pusher = params.pusher
        try {
            setPusher(pusher)
        } catch (error: Throwable) {
            monarchy.awaitTransaction { realm ->
                PusherEntity.where(realm, pusher.pushKey).findFirst()?.let {
                    it.state = PusherState.FAILED_TO_REGISTER
                }
            }
            throw error
        }
    }

    private suspend fun setPusher(pusher: JsonPusher) {
        requestExecutor.executeRequest(globalErrorReceiver) {
            pushersAPI.setPusher(pusher)
        }
        monarchy.awaitTransaction { realm ->
            val echo = PusherEntity.where(realm, pusher.pushKey).findFirst()
            if (echo == null) {
                pusher.toEntity().also {
                    it.state = PusherState.REGISTERED
                    realm.insertOrUpdate(it)
                }
            } else {
                echo.appDisplayName = pusher.appDisplayName
                echo.appId = pusher.appId
                echo.kind = pusher.kind
                echo.lang = pusher.lang
                echo.profileTag = pusher.profileTag
                echo.data?.format = pusher.data?.format
                echo.data?.url = pusher.data?.url
                echo.state = PusherState.REGISTERED
            }
        }
    }
}
