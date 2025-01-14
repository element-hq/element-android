/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.ui.bottomsheet

import com.airbnb.mvrx.MavericksState
import im.vector.app.core.platform.EmptyAction
import im.vector.app.core.platform.EmptyViewEvents
import im.vector.app.core.platform.VectorViewModel

abstract class BottomSheetGenericViewModel<State : MavericksState>(initialState: State) :
        VectorViewModel<State, EmptyAction, EmptyViewEvents>(initialState) {

    override fun handle(action: EmptyAction) {
        // No op
    }
}
