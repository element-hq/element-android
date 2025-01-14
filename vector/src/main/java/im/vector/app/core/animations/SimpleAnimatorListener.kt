/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.animations

import android.animation.Animator

open class SimpleAnimatorListener : Animator.AnimatorListener {
    override fun onAnimationRepeat(animation: Animator) {
        // No op
    }

    override fun onAnimationEnd(animation: Animator) {
        // No op
    }

    override fun onAnimationCancel(animation: Animator) {
        // No op
    }

    override fun onAnimationStart(animation: Animator) {
        // No op
    }
}
