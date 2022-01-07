/*
 * Copyright (c) 2021 New Vector Ltd
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

import android.animation.Animator
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.viewpager2.widget.ViewPager2

fun ViewPager2.setCurrentItem(
        item: Int,
        duration: Long,
        interpolator: TimeInterpolator = AccelerateDecelerateInterpolator(),
        pagePxWidth: Int = width,
) {
    val pxToDrag: Int = pagePxWidth * (item - currentItem)
    val animator = ValueAnimator.ofInt(0, pxToDrag)
    var previousValue = 0
    val isRtl = this.layoutDirection == View.LAYOUT_DIRECTION_RTL

    animator.addUpdateListener { valueAnimator ->
        val currentValue = valueAnimator.animatedValue as Int
        val currentPxToDrag = (currentValue - previousValue).toFloat()
        kotlin.runCatching {
            when {
                isRtl -> fakeDragBy(currentPxToDrag)
                else  -> fakeDragBy(-currentPxToDrag)
            }
            previousValue = currentValue
        }.onFailure { animator.cancel() }
    }
    animator.addListener(object : Animator.AnimatorListener {
        override fun onAnimationStart(animation: Animator?) {
            isUserInputEnabled = false
            beginFakeDrag()
        }

        override fun onAnimationEnd(animation: Animator?) {
            isUserInputEnabled = true
            endFakeDrag()
        }

        override fun onAnimationCancel(animation: Animator?) = Unit
        override fun onAnimationRepeat(animation: Animator?) = Unit
    })
    animator.interpolator = interpolator
    animator.duration = duration
    animator.start()
}
