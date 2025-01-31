/*
 * Copyright 2021-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
                else -> fakeDragBy(-currentPxToDrag)
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
