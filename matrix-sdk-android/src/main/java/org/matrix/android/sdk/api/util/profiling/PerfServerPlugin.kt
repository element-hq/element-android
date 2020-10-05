/*
 * Copyright (c) 2020 New Vector Ltd
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

package org.matrix.android.sdk.api.util.profiling

import android.os.Build
import com.nikitakozlov.pury.Plugin
import com.nikitakozlov.pury.profile.ProfilerId
import com.nikitakozlov.pury.result.model.ProfileResult
import com.nikitakozlov.pury.result.model.SingleProfileResult
import org.matrix.android.sdk.BuildConfig
import org.matrix.android.sdk.internal.task.TaskExecutor
import org.matrix.android.sdk.internal.task.configureWith
import javax.inject.Inject

internal class PerfServerPlugin @Inject constructor(
        private val publishPerfTask: PublishPerfTask,
        private val taskExecutor: TaskExecutor
) : Plugin {
    override fun handleResult(result: ProfileResult?, profilerId: ProfilerId?) {
        val report = ProfileReport(
                user = BuildConfig.PERF_TRACING_SERVER_USER,
                device = Build.DEVICE,
                id = profilerId?.profilerName,
                rootProfileResult = (result as? SingleProfileResult)?.let { ResultMapper.map(it) },
                tag = "original"
        )

        publishPerfTask.configureWith(PublishPerfTask.Params(report))
                .executeBy(taskExecutor)
    }

    object ResultMapper {

        private const val MS_TO_NS = 1000000

        fun map(singleProfileResult: SingleProfileResult): SingleProfileResultRest {
            return SingleProfileResultRest(
                    name = singleProfileResult.stageName,
                    depth = singleProfileResult.depth,
                    execTime = singleProfileResult.execTime / MS_TO_NS,
                    startTime = singleProfileResult.startTime / MS_TO_NS,
                    nestedResults = singleProfileResult.nestedResults.filterIsInstance<SingleProfileResult>().map {
                        map(it)
                    }
            )
        }
    }
}
