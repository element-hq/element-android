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

package im.vector.app.features.location.preview

import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.EmptyViewEvents
import im.vector.app.core.platform.VectorViewModel

class LocationPreviewViewModel @AssistedInject constructor(
        @Assisted private val initialState: LocationPreviewViewState,
) : VectorViewModel<LocationPreviewViewState, LocationPreviewAction, EmptyViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<LocationPreviewViewModel, LocationPreviewViewState> {
        override fun create(initialState: LocationPreviewViewState): LocationPreviewViewModel
    }

    companion object : MavericksViewModelFactory<LocationPreviewViewModel, LocationPreviewViewState> by hiltMavericksViewModelFactory()

    override fun handle(action: LocationPreviewAction) {
        when (action) {
            LocationPreviewAction.ShowMapLoadingError -> handleShowMapLoadingError()
        }
    }

    private fun handleShowMapLoadingError() {
        setState { copy(loadingMapHasFailed = true) }
    }
}
