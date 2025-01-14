/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
