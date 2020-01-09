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

package im.vector.matrix.android.internal.session.group

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.squareup.moshi.JsonClass
import im.vector.matrix.android.internal.worker.SessionWorkerParams
import im.vector.matrix.android.internal.worker.WorkerParamsFactory
import im.vector.matrix.android.internal.worker.getSessionComponent
import javax.inject.Inject

internal class GetGroupDataWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    @JsonClass(generateAdapter = true)
    internal data class Params(
            override val sessionId: String,
            val groupIds: List<String>,
            override val lastFailureMessage: String? = null
    ) : SessionWorkerParams

    @Inject lateinit var getGroupDataTask: GetGroupDataTask

    override suspend fun doWork(): Result {
        val params = WorkerParamsFactory.fromData<Params>(inputData)
                ?: return Result.failure()

        val sessionComponent = getSessionComponent(params.sessionId) ?: return Result.success()
        sessionComponent.inject(this)
        val results = params.groupIds.map { groupId ->
            runCatching { fetchGroupData(groupId) }
        }
        val isSuccessful = results.none { it.isFailure }
        return if (isSuccessful) Result.success() else Result.retry()
    }

    private suspend fun fetchGroupData(groupId: String) {
        getGroupDataTask.execute(GetGroupDataTask.Params(groupId))
    }
}
