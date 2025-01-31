/*
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
