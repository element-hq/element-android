/*
 * Copyright 2019 New Vector Ltd
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

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.squareup.moshi.JsonClass
import com.zhuinden.monarchy.Monarchy
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.session.pushers.PusherState
import org.matrix.android.sdk.internal.database.mapper.toEntity
import org.matrix.android.sdk.internal.database.model.PusherEntity
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.util.awaitTransaction
import org.matrix.android.sdk.internal.worker.SessionWorkerParams
import org.matrix.android.sdk.internal.worker.WorkerParamsFactory
import org.matrix.android.sdk.internal.worker.getSessionComponent
import org.greenrobot.eventbus.EventBus
import timber.log.Timber
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
    @Inject @SessionDatabase lateinit var monarchy: Monarchy
    @Inject lateinit var eventBus: EventBus

    override suspend fun doWork(): Result {
        val params = WorkerParamsFactory.fromData<Params>(inputData)
                ?: return Result.failure()
                        .also { Timber.e("Unable to parse work parameters") }

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
