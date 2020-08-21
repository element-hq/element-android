/*
 * Copyright 2020 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.core.animations.behavior

import android.animation.ArgbEvaluator
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.withStyledAttributes

import im.vector.app.R
import kotlin.math.abs

private const val UNSPECIFIED_INT = Integer.MAX_VALUE
private val UNSPECIFIED_FLOAT = Float.MAX_VALUE
private const val DEPEND_TYPE_HEIGHT = 0
private const val DEPEND_TYPE_WIDTH = 1
private const val DEPEND_TYPE_X = 2
private const val DEPEND_TYPE_Y = 3

class PercentViewBehavior<V : View>(context: Context, attrs: AttributeSet) : CoordinatorLayout.Behavior<V>(context, attrs) {

    private var dependType: Int = 0
    private var dependViewId: Int = 0
    private var dependTarget: Int = 0
    private var dependStartX: Int = 0
    private var dependStartY: Int = 0
    private var dependStartWidth: Int = 0
    private var dependStartHeight: Int = 0

    private var startX: Int = 0
    private var startY: Int = 0
    private var startWidth: Int = 0
    private var startHeight: Int = 0
    private var startBackgroundColor: Int = 0
    private var startAlpha: Float = 0f
    private var startRotateX: Float = 0f
    private var startRotateY: Float = 0f

    private var targetX: Int = 0
    private var targetY: Int = 0
    private var targetWidth: Int = 0
    private var targetHeight: Int = 0
    private var targetBackgroundColor: Int = 0
    private var targetAlpha: Float = 0f
    private var targetRotateX: Float = 0f
    private var targetRotateY: Float = 0f

    /**
     * Is the values prepared to be use
     */
    private var isPrepared: Boolean = false

    init {
        context.withStyledAttributes(attrs, R.styleable.PercentViewBehavior) {
            dependViewId = getResourceId(R.styleable.PercentViewBehavior_behavior_dependsOn, 0)
            dependType = getInt(R.styleable.PercentViewBehavior_behavior_dependType, DEPEND_TYPE_WIDTH)
            dependTarget = getDimensionPixelOffset(R.styleable.PercentViewBehavior_behavior_dependTarget, UNSPECIFIED_INT)
            targetX = getDimensionPixelOffset(R.styleable.PercentViewBehavior_behavior_targetX, UNSPECIFIED_INT)
            targetY = getDimensionPixelOffset(R.styleable.PercentViewBehavior_behavior_targetY, UNSPECIFIED_INT)
            targetWidth = getDimensionPixelOffset(R.styleable.PercentViewBehavior_behavior_targetWidth, UNSPECIFIED_INT)
            targetHeight = getDimensionPixelOffset(R.styleable.PercentViewBehavior_behavior_targetHeight, UNSPECIFIED_INT)
            targetBackgroundColor = getColor(R.styleable.PercentViewBehavior_behavior_targetBackgroundColor, UNSPECIFIED_INT)
            targetAlpha = getFloat(R.styleable.PercentViewBehavior_behavior_targetAlpha, UNSPECIFIED_FLOAT)
            targetRotateX = getFloat(R.styleable.PercentViewBehavior_behavior_targetRotateX, UNSPECIFIED_FLOAT)
            targetRotateY = getFloat(R.styleable.PercentViewBehavior_behavior_targetRotateY, UNSPECIFIED_FLOAT)
        }
    }

    private fun prepare(parent: CoordinatorLayout, child: View, dependency: View) {
        dependStartX = dependency.x.toInt()
        dependStartY = dependency.y.toInt()
        dependStartWidth = dependency.width
        dependStartHeight = dependency.height
        startX = child.x.toInt()
        startY = child.y.toInt()
        startWidth = child.width
        startHeight = child.height
        startAlpha = child.alpha
        startRotateX = child.rotationX
        startRotateY = child.rotationY

        // only set the start background color when the background is color drawable
        val background = child.background
        if (background is ColorDrawable) {
            startBackgroundColor = background.color
        }

        // if parent fitsSystemWindows is true, add status bar height to target y if specified
        if (parent.fitsSystemWindows && targetY != UNSPECIFIED_INT) {
            var result = 0
            val resources = parent.context.resources
            val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
            if (resourceId > 0) {
                result = resources.getDimensionPixelSize(resourceId)
            }
            targetY += result
        }
        isPrepared = true
    }

    override fun layoutDependsOn(parent: CoordinatorLayout, child: V, dependency: View): Boolean {
        return dependency.id == dependViewId
    }

    override fun onDependentViewChanged(parent: CoordinatorLayout, child: V, dependency: View): Boolean {
        // first time, prepare values before continue
        if (!isPrepared) {
            prepare(parent, child, dependency)
        }
        updateView(child, dependency)
        return false
    }

    override fun onLayoutChild(parent: CoordinatorLayout, child: V, layoutDirection: Int): Boolean {
        val bool = super.onLayoutChild(parent, child, layoutDirection)
        if (isPrepared) {
            updateView(child, parent.getDependencies(child)[0])
        }
        return bool
    }

    /**
     * Update the child view from the dependency states
     *
     * @param child      child view
     * @param dependency dependency view
     */
    private fun updateView(child: V, dependency: View) {
        var percent = 0f
        var start = 0f
        var current = 0f
        var end = UNSPECIFIED_INT.toFloat()
        when (dependType) {
            DEPEND_TYPE_WIDTH  -> {
                start = dependStartWidth.toFloat()
                current = dependency.width.toFloat()
                end = dependTarget.toFloat()
            }
            DEPEND_TYPE_HEIGHT -> {
                start = dependStartHeight.toFloat()
                current = dependency.height.toFloat()
                end = dependTarget.toFloat()
            }
            DEPEND_TYPE_X      -> {
                start = dependStartX.toFloat()
                current = dependency.x
                end = dependTarget.toFloat()
            }
            DEPEND_TYPE_Y      -> {
                start = dependStartY.toFloat()
                current = dependency.y
                end = dependTarget.toFloat()
            }
        }

        // need to define target value according to the depend type, if not then skip
        if (end != UNSPECIFIED_INT.toFloat()) {
            percent = abs(current - start) / abs(end - start)
        }
        updateViewWithPercent(child, if (percent > 1f) 1f else percent)
    }

    private fun updateViewWithPercent(child: View, percent: Float) {
        var newX = if (targetX == UNSPECIFIED_INT) 0f else (targetX - startX) * percent
        var newY = if (targetY == UNSPECIFIED_INT) 0f else (targetY - startY) * percent

        // set scale
        if (targetWidth != UNSPECIFIED_INT) {
            val newWidth = startWidth + (targetWidth - startWidth) * percent
            child.scaleX = newWidth / startWidth
            newX -= (startWidth - newWidth) / 2
        }
        if (targetHeight != UNSPECIFIED_INT) {
            val newHeight = startHeight + (targetHeight - startHeight) * percent
            child.scaleY = newHeight / startHeight
            newY -= (startHeight - newHeight) / 2
        }

        // set new position
        child.translationX = newX
        child.translationY = newY

        // set alpha
        if (targetAlpha != UNSPECIFIED_FLOAT) {
            child.alpha = startAlpha + (targetAlpha - startAlpha) * percent
        }

        // set background color
        if (targetBackgroundColor != UNSPECIFIED_INT && startBackgroundColor != 0) {
            val evaluator = ArgbEvaluator()
            val color = evaluator.evaluate(percent, startBackgroundColor, targetBackgroundColor) as Int
            child.setBackgroundColor(color)
        }

        // set rotation
        if (targetRotateX != UNSPECIFIED_FLOAT) {
            child.rotationX = startRotateX + (targetRotateX - startRotateX) * percent
        }
        if (targetRotateY != UNSPECIFIED_FLOAT) {
            child.rotationY = startRotateY + (targetRotateY - startRotateY) * percent
        }

        child.requestLayout()
    }
}
