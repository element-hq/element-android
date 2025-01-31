/*
 * Copyright 2019-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.platform

import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import im.vector.app.core.utils.DataSource
import im.vector.app.core.utils.PublishDataSource

abstract class VectorViewModel<S : MavericksState, VA : VectorViewModelAction, VE : VectorViewEvents>(initialState: S) :
        MavericksViewModel<S>(initialState) {

    // Used to post transient events to the View
    protected val _viewEvents = PublishDataSource<VE>()
    val viewEvents: DataSource<VE> = _viewEvents

    abstract fun handle(action: VA)
}
