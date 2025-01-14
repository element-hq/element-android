/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.animations

import androidx.transition.Transition

open class SimpleTransitionListener : Transition.TransitionListener {
    override fun onTransitionEnd(transition: Transition) {
        // No op
    }

    override fun onTransitionResume(transition: Transition) {
        // No op
    }

    override fun onTransitionPause(transition: Transition) {
        // No op
    }

    override fun onTransitionCancel(transition: Transition) {
        // No op
    }

    override fun onTransitionStart(transition: Transition) {
        // No op
    }
}
