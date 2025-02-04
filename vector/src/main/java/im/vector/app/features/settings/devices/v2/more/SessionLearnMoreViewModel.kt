/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.more

import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.platform.EmptyAction
import im.vector.app.core.platform.EmptyViewEvents
import im.vector.app.core.platform.VectorViewModel

class SessionLearnMoreViewModel @AssistedInject constructor(
        @Assisted initialState: SessionLearnMoreViewState,
) : VectorViewModel<SessionLearnMoreViewState, EmptyAction, EmptyViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<SessionLearnMoreViewModel, SessionLearnMoreViewState> {
        override fun create(initialState: SessionLearnMoreViewState): SessionLearnMoreViewModel
    }

    companion object : MavericksViewModelFactory<SessionLearnMoreViewModel, SessionLearnMoreViewState> by hiltMavericksViewModelFactory()

    override fun handle(action: EmptyAction) {
        // do nothing
    }
}
