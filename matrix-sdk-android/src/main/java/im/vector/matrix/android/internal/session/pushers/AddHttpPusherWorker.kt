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
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.squareup.moshi.JsonClass
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.failure.Failure
import im.vector.matrix.android.api.session.pushers.PusherState
import im.vector.matrix.android.internal.database.mapper.toEntity
import im.vector.matrix.android.internal.database.model.PusherEntity
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.util.awaitTransaction
import im.vector.matrix.android.internal.worker.SessionWorkerParams
import im.vector.matrix.android.internal.worker.WorkerParamsFactory
import im.vector.matrix.android.internal.worker.getSessionComponent
import org.greenrobot.eventbus.EventBus
import javax.inject.Inject

internal class AddHttpPusherWorker(context: Context, params: WorkerParameters)
    : CoroutineWorker(context, params) {

    @JsonClass(generateAdapter = true)
    internal data class Params(
            override val sessionId: String,
            val pusher: JsonPusher,
            override val lastFailureMessage: String? = null
    ) : SessionWorkerParams

    @Inject lateinit var pushersAPI: PushersAPI
    @Inject lateinit var monarchy: Monarchy
    @Inject lateinit var eventBus: EventBus

    override suspend fun doWork(): Result {
        val params = WorkerParamsFactory.fromData<Params>(inputData)
                ?: return Result.failure()

        val sessionComponent = getSessionComponent(params.sessionId) ?: return Result.success()
        sessionComponent.inject(this)

        val pusher = params.pusher

        if (pusher.pushKey.isBlank()) {
            return Result.failure()
        }
        return try {
            setPusher(pusher)
            Result.success()
        } catch (exception: Throwable) {
            when (exception) {
                is Failure.NetworkConnection -> Result.retry()
                else                         -> {
                    monarchy.awaitTransaction { realm ->
                        PusherEntity.where(realm, pusher.pushKey).findFirst()?.let {
                            // update it
                            it.state = PusherState.FAILED_TO_REGISTER
                        }
                    }
                    // always return success, or the chain will be stuck for ever!
                    Result.failure()
                }
            }
        }
    }

    private suspend fun setPusher(pusher: JsonPusher) {
        executeRequest<Unit>(eventBus) {
            apiCall = pushersAPI.setPusher(pusher)
        }
        monarchy.awaitTransaction { realm ->
            val echo = PusherEntity.where(realm, pusher.pushKey).findFirst()
            if (echo != null) {
                // update it
                echo.appDisplayName = pusher.appDisplayName
                echo.appId = pusher.appId
                echo.kind = pusher.kind
                echo.lang = pusher.lang
                echo.profileTag = pusher.profileTag
                echo.data?.format = pusher.data?.format
                echo.data?.url = pusher.data?.url
                echo.state = PusherState.REGISTERED
            } else {
                pusher.toEntity().also {
                    it.state = PusherState.REGISTERED
                    realm.insertOrUpdate(it)
                }
            }
        }
    }
}
