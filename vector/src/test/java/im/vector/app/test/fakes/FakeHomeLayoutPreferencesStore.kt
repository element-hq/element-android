/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.test.fakes

import im.vector.app.features.home.room.list.home.HomeLayoutPreferencesStore
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow

class FakeHomeLayoutPreferencesStore {

    private val _areRecentsEnabledFlow = MutableSharedFlow<Boolean>()
    private val _areFiltersEnabledFlow = MutableSharedFlow<Boolean>()
    private val _isAZOrderingEnabledFlow = MutableSharedFlow<Boolean>()

    val instance = mockk<HomeLayoutPreferencesStore>(relaxed = true) {
        every { areRecentsEnabledFlow } returns _areRecentsEnabledFlow
        every { areFiltersEnabledFlow } returns _areFiltersEnabledFlow
        every { isAZOrderingEnabledFlow } returns _isAZOrderingEnabledFlow
    }

    suspend fun givenRecentsEnabled(enabled: Boolean) {
        _areRecentsEnabledFlow.emit(enabled)
    }

    suspend fun givenFiltersEnabled(enabled: Boolean) {
        _areFiltersEnabledFlow.emit(enabled)
    }
}
