/*
 * Copyright (c) 2021 New Vector Ltd
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

package fr.gouv.tchap.android.sdk.internal.services.threepidplatformdiscover

import fr.gouv.tchap.android.sdk.internal.services.threepidplatformdiscover.model.Platform
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface ThreePidPlatformDiscoverTask : Task<ThreePidPlatformDiscoverTask.Params, Platform> {
    data class Params(
            val threePidPlatformDiscoverAPI: ThreePidPlatformDiscoverAPI,
            val address: String,
            val medium: String
    )
}

internal class TchapThreePidPlatformDiscoverTask @Inject constructor() : ThreePidPlatformDiscoverTask {

    override suspend fun execute(params: ThreePidPlatformDiscoverTask.Params): Platform {
        return executeRequest(null) {
            params.threePidPlatformDiscoverAPI.info(params.address, params.medium)
        }
    }
}
