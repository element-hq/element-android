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

package org.matrix.android.sdk.internal.sync

import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import org.matrix.android.sdk.api.MatrixConfiguration
import org.matrix.android.sdk.api.SyncConfig
import org.matrix.android.sdk.api.session.homeserver.HomeServerCapabilities
import org.matrix.android.sdk.api.session.sync.filter.SyncFilterParams
import org.matrix.android.sdk.internal.session.filter.DefaultGetCurrentFilterTask
import org.matrix.android.sdk.internal.sync.filter.SyncFilterBuilder
import org.matrix.android.sdk.test.fakes.FakeFilterRepository
import org.matrix.android.sdk.test.fakes.FakeHomeServerCapabilitiesDataSource
import org.matrix.android.sdk.test.fakes.FakeSaveFilterTask

private const val A_FILTER_ID = "filter-id"
private val A_HOMESERVER_CAPABILITIES = HomeServerCapabilities()
private val A_SYNC_FILTER_PARAMS = SyncFilterParams(
        lazyLoadMembersForStateEvents = true,
        useThreadNotifications = true
)

@ExperimentalCoroutinesApi
class DefaultGetCurrentFilterTaskTest {

    private val filterRepository = FakeFilterRepository()
    private val homeServerCapabilitiesDataSource = FakeHomeServerCapabilitiesDataSource()
    private val saveFilterTask = FakeSaveFilterTask()

    private val getCurrentFilterTask = DefaultGetCurrentFilterTask(
            filterRepository = filterRepository,
            homeServerCapabilitiesDataSource = homeServerCapabilitiesDataSource.instance,
            saveFilterTask = saveFilterTask,
            matrixConfiguration = MatrixConfiguration(
                    applicationFlavor = "TestFlavor",
                    roomDisplayNameFallbackProvider = mockk(),
                    syncConfig = SyncConfig(syncFilterParams = SyncFilterParams(lazyLoadMembersForStateEvents = true, useThreadNotifications = true)),
            )
    )

    @Test
    fun `given no filter is stored, when execute, then executes task to save new filter`() = runTest {
        homeServerCapabilitiesDataSource.givenHomeServerCapabilities(A_HOMESERVER_CAPABILITIES)

        filterRepository.givenFilterStored(null, null)

        getCurrentFilterTask.execute(Unit)

        val filter = SyncFilterBuilder()
                .with(A_SYNC_FILTER_PARAMS)
                .build(A_HOMESERVER_CAPABILITIES)

        saveFilterTask.verifyExecution(filter)
    }

    @Test
    fun `given filter is stored and didn't change, when execute, then returns stored filter id`() = runTest {
        homeServerCapabilitiesDataSource.givenHomeServerCapabilities(A_HOMESERVER_CAPABILITIES)

        val filter = SyncFilterBuilder().with(A_SYNC_FILTER_PARAMS).build(A_HOMESERVER_CAPABILITIES)
        filterRepository.givenFilterStored(A_FILTER_ID, filter.toJSONString())

        val result = getCurrentFilterTask.execute(Unit)

        result shouldBeEqualTo A_FILTER_ID
    }

    @Test
    fun `given filter is set and home server capabilities has changed, when execute, then executes task to save new filter`() = runTest {
        homeServerCapabilitiesDataSource.givenHomeServerCapabilities(A_HOMESERVER_CAPABILITIES)

        val filter = SyncFilterBuilder().with(A_SYNC_FILTER_PARAMS).build(A_HOMESERVER_CAPABILITIES)
        filterRepository.givenFilterStored(A_FILTER_ID, filter.toJSONString())

        val newHomeServerCapabilities = HomeServerCapabilities(canUseThreadReadReceiptsAndNotifications = true)
        homeServerCapabilitiesDataSource.givenHomeServerCapabilities(newHomeServerCapabilities)
        val newFilter = SyncFilterBuilder().with(A_SYNC_FILTER_PARAMS).build(newHomeServerCapabilities)

        getCurrentFilterTask.execute(Unit)

        saveFilterTask.verifyExecution(newFilter)
    }
}
