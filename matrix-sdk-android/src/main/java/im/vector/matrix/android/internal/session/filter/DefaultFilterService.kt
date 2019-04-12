/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.internal.session.filter

import im.vector.matrix.android.api.session.sync.FilterService
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.configureWith

internal class DefaultFilterService(private val filterRepository: FilterRepository,
                                    private val saveFilterTask: SaveFilterTask,
                                    private val taskExecutor: TaskExecutor) : FilterService {

    // TODO Pass a list of support events instead
    override fun setFilter(filterPreset: FilterService.FilterPreset) {
        val filterBody = when (filterPreset) {
            FilterService.FilterPreset.RiotFilter -> {
                FilterFactory.createRiotFilterBody()
            }
            FilterService.FilterPreset.NoFilter -> {
                FilterFactory.createDefaultFilterBody()
            }
        }

        val roomFilter = when (filterPreset) {
            FilterService.FilterPreset.RiotFilter -> {
                FilterFactory.createRiotRoomFilter()
            }
            FilterService.FilterPreset.NoFilter -> {
                FilterFactory.createDefaultRoomFilter()
            }
        }

        val updated = filterRepository.storeFilter(filterBody, roomFilter)

        if (updated) {
            saveFilterTask
                    .configureWith(SaveFilterTask.Params(filterBody))
                    .executeBy(taskExecutor)
        }
    }
}