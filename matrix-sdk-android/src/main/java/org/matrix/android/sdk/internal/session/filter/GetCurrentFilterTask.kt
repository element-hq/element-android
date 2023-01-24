/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.filter

import org.matrix.android.sdk.api.MatrixConfiguration
import org.matrix.android.sdk.api.session.homeserver.HomeServerCapabilities
import org.matrix.android.sdk.internal.session.homeserver.HomeServerCapabilitiesDataSource
import org.matrix.android.sdk.internal.sync.filter.SyncFilterBuilder
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface GetCurrentFilterTask : Task<Unit, String>

internal class DefaultGetCurrentFilterTask @Inject constructor(
        private val filterRepository: FilterRepository,
        private val homeServerCapabilitiesDataSource: HomeServerCapabilitiesDataSource,
        private val saveFilterTask: SaveFilterTask,
        private val matrixConfiguration: MatrixConfiguration
) : GetCurrentFilterTask {

    override suspend fun execute(params: Unit): String {
        val storedFilterId = filterRepository.getStoredSyncFilterId()
        val storedFilterBody = filterRepository.getStoredSyncFilterBody()
        val homeServerCapabilities = homeServerCapabilitiesDataSource.getHomeServerCapabilities() ?: HomeServerCapabilities()
        val currentFilter = SyncFilterBuilder()
                .with(matrixConfiguration.syncConfig.syncFilterParams)
                .build(homeServerCapabilities)

        val currentFilterBody = currentFilter.toJSONString()

        return when (storedFilterBody) {
            currentFilterBody -> storedFilterId ?: storedFilterBody
            else -> saveFilter(currentFilter) ?: currentFilterBody
        }
    }

    private suspend fun saveFilter(filter: Filter) = saveFilterTask
            .execute(
                    SaveFilterTask.Params(
                            filter = filter
                    )
            )
}
