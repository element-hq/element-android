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
import im.vector.matrix.android.internal.di.MatrixKoinComponent
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.util.WorkerParamsFactory
import org.koin.standalone.inject

class AddHttpPusherWorker(context: Context, params: WorkerParameters)
    : CoroutineWorker(context, params), MatrixKoinComponent {


    @JsonClass(generateAdapter = true)
    internal data class Params(
            val pusher: JsonPusher,
            val userId: String
    )

    private val pushersAPI by inject<PushersAPI>()
    private val monarchy by inject<Monarchy>()

    override suspend fun doWork(): Result {

        val params = WorkerParamsFactory.fromData<Params>(inputData)
                ?: return Result.failure()

        val pusher = params.pusher

        if (pusher.pushKey.isBlank()) {
            return Result.failure()
        }

        val result = executeRequest<Unit> {
            apiCall = pushersAPI.setPusher(pusher)
        }
        return result.fold({
            when (it) {
                is Failure.NetworkConnection -> Result.retry()
                else                         -> {
                    monarchy.runTransactionSync { realm ->
                        PusherEntity.where(realm, params.userId, pusher.pushKey).findFirst()?.let {
                            //update it
                            it.state = PusherState.FAILED_TO_REGISTER
                        }
                    }
                    //always return success, or the chain will be stuck for ever!
                    Result.failure()
                }
            }
        }, {
            monarchy.runTransactionSync { realm ->
                val echo = PusherEntity.where(realm, params.userId, pusher.pushKey).findFirst()
                if (echo != null) {
                    //update it
                    echo.appDisplayName = pusher.appDisplayName
                    echo.appId = pusher.appId
                    echo.kind = pusher.kind
                    echo.lang = pusher.lang
                    echo.profileTag = pusher.profileTag
                    echo.data?.format = pusher.data?.format
                    echo.data?.url = pusher.data?.url
                    echo.state = PusherState.REGISTERED
                } else {
                    pusher.toEntity(params.userId).also {
                        it.state = PusherState.REGISTERED
                        realm.insertOrUpdate(it)
                    }
                }
            }
            Result.success()

        })
    }
}