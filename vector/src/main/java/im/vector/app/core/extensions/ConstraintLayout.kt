/*
 * Copyright (c) 2020 New Vector Ltd
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
