/*
 * Copyright (c) 2023 New Vector Ltd
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

package org.matrix.android.sdk.internal.session.synapse

import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface IsSynapseFeatureEnabledTask : Task<IsSynapseFeatureEnabledTask.Params, Result> {
    data class Params(
            val featureId: String,
    )
}

internal class DefaultIsSynapseFeatureEnabledTask @Inject constructor(
        private val synapseCapabilityAPI: SynapseAPI,
) : IsSynapseFeatureEnabledTask {

    override suspend fun execute(params: IsSynapseFeatureEnabledTask.Params): Result {
        val isEnabled = runCatching {
            executeRequest(null) {
                synapseCapabilityAPI.synapseFeature(params.featureId)
            }.enabled.orFalse()
        }.getOrDefault(false)

        return Result(featureIsEnabled = isEnabled)
    }
}

internal data class Result(
        val featureIsEnabled: Boolean
)
