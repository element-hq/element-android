/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.list.home.release

import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.VectorDummyViewState
import im.vector.app.core.platform.VectorViewModel

class ReleaseNotesViewModel @AssistedInject constructor(
        @Assisted initialState: VectorDummyViewState,
) : VectorViewModel<VectorDummyViewState, ReleaseNotesAction, ReleaseNotesViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<ReleaseNotesViewModel, VectorDummyViewState> {
        override fun create(initialState: VectorDummyViewState): ReleaseNotesViewModel
    }

    companion object : MavericksViewModelFactory<ReleaseNotesViewModel, VectorDummyViewState> by hiltMavericksViewModelFactory()

    private var selectedPageIndex = 0

    init {
        _viewEvents.post(ReleaseNotesViewEvents.SelectPage(0))
    }

    override fun handle(action: ReleaseNotesAction) {
        when (action) {
            is ReleaseNotesAction.NextPressed -> handleNextPressed(action)
            is ReleaseNotesAction.PageSelected -> handlePageSelected(action)
        }
    }

    private fun handlePageSelected(action: ReleaseNotesAction.PageSelected) {
        selectedPageIndex = action.selectedPageIndex
    }

    private fun handleNextPressed(action: ReleaseNotesAction.NextPressed) {
        if (action.isLastItemSelected) {
            _viewEvents.post(ReleaseNotesViewEvents.Close)
        } else {
            _viewEvents.post(ReleaseNotesViewEvents.SelectPage(++selectedPageIndex))
        }
    }
}
