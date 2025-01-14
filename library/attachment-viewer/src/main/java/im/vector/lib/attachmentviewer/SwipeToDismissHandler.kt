/*
 * Copyright 2020-2024 New Vector Ltd.
 * Copyright 2018 stfalcon.com
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.lib.attachmentviewer

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import android.view.ViewPropertyAnimator
import android.view.animation.AccelerateInterpolator

class SwipeToDismissHandler(
        private val swipeView: View,
        private val onDismiss: () -> Unit,
        private val onSwipeViewMove: (translationY: Float, translationLimit: Int) -> Unit,
        private val shouldAnimateDismiss: () -> Boolean
) : View.OnTouchListener {

    companion object {
        private const val ANIMATION_DURATION = 200L
    }

    var translationLimit: Int = swipeView.height / 4
    private var isTracking = false
    private var startY: Float = 0f

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (swipeView.hitRect.contains(event.x.toInt(), event.y.toInt())) {
                    isTracking = true
                }
                startY = event.y
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isTracking) {
                    isTracking = false
                    onTrackingEnd(v.height)
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isTracking) {
                    val translationY = event.y - startY
                    swipeView.translationY = translationY
                    onSwipeViewMove(translationY, translationLimit)
                }
                return true
            }
            else -> {
                return false
            }
        }
    }

    internal fun initiateDismissToBottom() {
        animateTranslation(swipeView.height.toFloat())
    }

    private fun onTrackingEnd(parentHeight: Int) {
        val animateTo = when {
            swipeView.translationY < -translationLimit -> -parentHeight.toFloat()
            swipeView.translationY > translationLimit -> parentHeight.toFloat()
            else -> 0f
        }

        if (animateTo != 0f && !shouldAnimateDismiss()) {
            onDismiss()
        } else {
            animateTranslation(animateTo)
        }
    }

    private fun animateTranslation(translationTo: Float) {
        swipeView.animate()
                .translationY(translationTo)
                .setDuration(ANIMATION_DURATION)
                .setInterpolator(AccelerateInterpolator())
                .setUpdateListener { onSwipeViewMove(swipeView.translationY, translationLimit) }
                .setAnimatorEndListener {
                    if (translationTo != 0f) {
                        onDismiss()
                    }

                    // remove the update listener, otherwise it will be saved on the next animation execution:
                    swipeView.animate().setUpdateListener(null)
                }
                .start()
    }
}

private fun ViewPropertyAnimator.setAnimatorEndListener(
        onAnimationEnd: () -> Unit,
) = setListener(
        object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                onAnimationEnd()
            }
        }
)

private val View.hitRect: Rect
    get() = Rect().also { getHitRect(it) }
