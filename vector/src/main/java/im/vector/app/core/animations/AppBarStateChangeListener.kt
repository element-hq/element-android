/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.animations

import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener
import kotlin.math.abs

abstract class AppBarStateChangeListener : OnOffsetChangedListener {

    enum class State {
        EXPANDED, COLLAPSED, IDLE
    }

    var currentState = State.IDLE
        private set

    override fun onOffsetChanged(appBarLayout: AppBarLayout, i: Int) {
        currentState = if (i == 0) {
            if (currentState != State.EXPANDED) {
                onStateChanged(appBarLayout, State.EXPANDED)
            }
            State.EXPANDED
        } else if (abs(i) >= appBarLayout.totalScrollRange) {
            if (currentState != State.COLLAPSED) {
                onStateChanged(appBarLayout, State.COLLAPSED)
            }
            State.COLLAPSED
        } else {
            if (currentState != State.IDLE) {
                onStateChanged(appBarLayout, State.IDLE)
            }
            State.IDLE
        }
    }

    abstract fun onStateChanged(appBarLayout: AppBarLayout, state: State)
}
