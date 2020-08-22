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

package org.matrix.android.sdk.internal.session.group

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.internal.worker.SessionWorkerParams
import org.matrix.android.sdk.internal.worker.WorkerParamsFactory
import org.matrix.android.sdk.internal.worker.getSessionComponent
import timber.log.Timber
import javax.inject.Inject

/**
 * Possible previous worker: None
 * Possible next worker    : None
 */
internal class GetGroupDataWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    @JsonClass(generateAdapter = true)
    internal data class Params(
            override val sessionId: String,
            override val lastFailureMessage: String? = null
    ) : SessionWorkerParams

    @Inject lateinit var getGroupDataTask: GetGroupDataTask

    override suspend fun doWork(): Result {
        val params = WorkerParamsFactory.fromData<Params>(inputData)
                ?: return Result.failure()
                        .also { Timber.e("Unable to parse work parameters") }

        val sessionComponent = getSessionComponent(params.sessionId) ?: return Result.success()
        sessionComponent.inject(this)
        return runCatching {
            getGroupDataTask.execute(GetGroupDataTask.Params.FetchAllActive)
        }.fold(
                { Result.success() },
                { Result.retry() }
        )
    }
}
