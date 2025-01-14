/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.extensions

import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.children
import androidx.core.view.doOnLayout
import kotlin.math.roundToInt

fun ConstraintLayout.updateConstraintSet(block: (ConstraintSet) -> Unit) {
    ConstraintSet().let {
        it.clone(this)
        block.invoke(it)
        it.applyTo(this)
    }
}

/**
 * Helper to recalculate all ConstraintLayout child views with percentage based height against the parent's height.
 * This is helpful when using a ConstraintLayout within a ScrollView as any percentages will use the total scrolling size
 * instead of the viewport/ScrollView height
 */
fun ConstraintLayout.realignPercentagesToParent() {
    doOnLayout {
        val rootHeight = (parent as View).height
        children.forEach { child ->
            val params = child.layoutParams as ConstraintLayout.LayoutParams
            if (params.matchConstraintPercentHeight != 1.0f) {
                params.height = (rootHeight * params.matchConstraintPercentHeight).roundToInt()
                child.layoutParams = params
            }
        }
    }
}
