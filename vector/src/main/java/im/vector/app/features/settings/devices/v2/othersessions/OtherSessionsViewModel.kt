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

package im.vector.app.features.settings.devices.v2.othersessions

import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.features.settings.devices.v2.GetDeviceFullInfoListUseCase
import im.vector.app.features.settings.devices.v2.RefreshDevicesUseCase
import im.vector.app.features.settings.devices.v2.VectorSessionsListViewModel
import im.vector.app.features.settings.devices.v2.filter.DeviceManagerFilterType
import kotlinx.coroutines.Job

class OtherSessionsViewModel @AssistedInject constructor(
        @Assisted private val initialState: OtherSessionsViewState,
        activeSessionHolder: ActiveSessionHolder,
        private val getDeviceFullInfoListUseCase: GetDeviceFullInfoListUseCase,
        refreshDevicesUseCase: RefreshDevicesUseCase
) : VectorSessionsListViewModel<OtherSessionsViewState, OtherSessionsAction, OtherSessionsViewEvents>(
        initialState, activeSessionHolder, refreshDevicesUseCase
) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<OtherSessionsViewModel, OtherSessionsViewState> {
        override fun create(initialState: OtherSessionsViewState): OtherSessionsViewModel
    }

    companion object : MavericksViewModelFactory<OtherSessionsViewModel, OtherSessionsViewState> by hiltMavericksViewModelFactory()

    private var observeDevicesJob: Job? = null

    init {
        observeDevices(initialState.currentFilter)
    }

    private fun observeDevices(currentFilter: DeviceManagerFilterType) {
        observeDevicesJob?.cancel()
        observeDevicesJob = getDeviceFullInfoListUseCase.execute(
                filterType = currentFilter,
                excludeCurrentDevice = initialState.excludeCurrentDevice
        )
                .execute { async ->
                    copy(
                            devices = async,
                    )
                }
    }

    override fun handle(action: OtherSessionsAction) {
        when (action) {
            is OtherSessionsAction.FilterDevices -> handleFilterDevices(action)
        }
    }

    private fun handleFilterDevices(action: OtherSessionsAction.FilterDevices) {
        setState {
            copy(
                    currentFilter = action.filterType
            )
        }
        observeDevices(action.filterType)
    }
}
