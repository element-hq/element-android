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

package im.vector.app.features.location.live.map

import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.home.room.detail.timeline.helper.LocationPinProvider
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.getRoom

class LocationLiveMapViewModel @AssistedInject constructor(
        @Assisted private val initialState: LocationLiveMapViewState,
) : VectorViewModel<LocationLiveMapViewState, LocationLiveMapAction, LocationLiveMapViewEvents>(initialState) {

    // TODO create useCase to get Flow of user live location in room => Mock data for now

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<LocationLiveMapViewModel, LocationLiveMapViewState> {
        override fun create(initialState: LocationLiveMapViewState): LocationLiveMapViewModel
    }

    companion object : MavericksViewModelFactory<LocationLiveMapViewModel, LocationLiveMapViewState> by hiltMavericksViewModelFactory()

    init {
        // TODO call use case to collect flow of user live location
    }

    override fun handle(action: LocationLiveMapAction) {
        // do nothing, no action for now
    }
}
