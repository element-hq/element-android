/*
 * Copyright 2019 New Vector Ltd
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
package im.vector.app.features.reactions.widget

import android.animation.ArgbEvaluator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Property
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

/**
 * This view will draw dots floating around the center of it's view
 * As describe in http://frogermcs.github.io/twitters-like-animation-in-android-alternative/
 */
class DotsView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null,
                                         defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr) {

    private var COLOR_1 = -0x3ef9
    private var COLOR_2 = -0x6800
    private var COLOR_3 = -0xa8de
    private var COLOR_4 = -0xbbcca

    private val circlePaints = listOf(
            Paint().apply { style = Paint.Style.FILL },
            Paint().apply { style = Paint.Style.FILL },
            Paint().apply { style = Paint.Style.FILL },
            Paint().apply { style = Paint.Style.FILL }
    )

    private var centerX: Int = 0
    private var centerY: Int = 0

    private var maxOuterDotsRadius: Float = 0.toFloat()
    private var maxInnerDotsRadius: Float = 0.toFloat()
    private var maxDotSize: Float = 0.toFloat()

    var currentProgress = 0f
        set(value) {
            field = value
            updateInnerDotsPosition()
            updateOuterDotsPosition()
            updateDotsPaints()
            updateDotsAlpha()

            postInvalidate()
        }

    private var currentRadius1 = 0f
    private var currentDotSize1 = 0f

    private var currentDotSize2 = 0f
    private var currentRadius2 = 0f

    private val argbEvaluator = ArgbEvaluator()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = w / 2
        centerY = h / 2
        maxDotSize = 3f
        maxOuterDotsRadius = w / 2 - maxDotSize * 2
        maxInnerDotsRadius = 0.8f * maxOuterDotsRadius
    }

    override fun onDraw(canvas: Canvas) {
        drawOuterDotsFrame(canvas)
        drawInnerDotsFrame(canvas)
    }

    private fun drawOuterDotsFrame(canvas: Canvas) {
        for (i in 0 until DOTS_COUNT) {
            val cX = (centerX + currentRadius1 * cos(i.toDouble() * OUTER_DOTS_POSITION_ANGLE.toDouble() * Math.PI / 180)).toFloat()
            val cY = (centerY + currentRadius1 * sin(i.toDouble() * OUTER_DOTS_POSITION_ANGLE.toDouble() * Math.PI / 180)).toFloat()
            canvas.drawCircle(cX, cY, currentDotSize1, circlePaints[i % circlePaints.size])
        }
    }

    private fun drawInnerDotsFrame(canvas: Canvas) {
        for (i in 0 until DOTS_COUNT) {
            val cX = (centerX + currentRadius2 * cos((i * OUTER_DOTS_POSITION_ANGLE - 10) * Math.PI / 180)).toFloat()
            val cY = (centerY + currentRadius2 * sin((i * OUTER_DOTS_POSITION_ANGLE - 10) * Math.PI / 180)).toFloat()
            canvas.drawCircle(cX, cY, currentDotSize2, circlePaints[(i + 1) % circlePaints.size])
        }
    }

//    fun setCurrentProgress(currentProgress: Float) {
//        this.currentProgress = currentProgress
//
//        updateInnerDotsPosition()
//        updateOuterDotsPosition()
//        updateDotsPaints()
//        updateDotsAlpha()
//
//        postInvalidate()
//    }
//
//    fun getCurrentProgress(): Float {
//        return currentProgress
//    }

    private fun updateInnerDotsPosition() {
        if (currentProgress < 0.3f) {
            this.currentRadius2 = CircleView.mapValueFromRangeToRange(currentProgress, 0f, 0.3f, 0f, maxInnerDotsRadius)
        } else {
            this.currentRadius2 = maxInnerDotsRadius
        }

        if (currentProgress < 0.2) {
            this.currentDotSize2 = maxDotSize
        } else if (currentProgress < 0.5) {
            this.currentDotSize2 = CircleView.mapValueFromRangeToRange(
                    currentProgress, 0.2f, 0.5f, maxDotSize, 0.3f * maxDotSize)
        } else {
            this.currentDotSize2 = CircleView.mapValueFromRangeToRange(
                    currentProgress, 0.5f, 1f, maxDotSize * 0.3f, 0f)
        }
    }

    fun setColors(primary: Int, secondary: Int) {
        COLOR_1 = primary
        COLOR_2 = secondary
        COLOR_3 = primary
        COLOR_4 = secondary
    }

    private fun updateOuterDotsPosition() {
        if (currentProgress < 0.3f) {
            this.currentRadius1 = CircleView.mapValueFromRangeToRange(
                    currentProgress, 0.0f, 0.3f, 0f, maxOuterDotsRadius * 0.8f)
        } else {
            this.currentRadius1 = CircleView.mapValueFromRangeToRange(
                    currentProgress, 0.3f, 1f, 0.8f * maxOuterDotsRadius, maxOuterDotsRadius)
        }

        if (currentProgress < 0.7) {
            this.currentDotSize1 = maxDotSize
        } else {
            this.currentDotSize1 = CircleView.mapValueFromRangeToRange(
                    currentProgress, 0.7f, 1f, maxDotSize, 0f)
        }
    }

    private fun updateDotsPaints() {
        if (currentProgress < 0.5f) {
            val progress = CircleView.mapValueFromRangeToRange(currentProgress, 0f, 0.5f, 0f, 1f)
            circlePaints[0].color = argbEvaluator.evaluate(progress, COLOR_1, COLOR_2) as Int
            circlePaints[1].color = argbEvaluator.evaluate(progress, COLOR_2, COLOR_3) as Int
            circlePaints[2].color = argbEvaluator.evaluate(progress, COLOR_3, COLOR_4) as Int
            circlePaints[3].color = argbEvaluator.evaluate(progress, COLOR_4, COLOR_1) as Int
        } else {
            val progress = CircleView.mapValueFromRangeToRange(currentProgress, 0.5f, 1f, 0f, 1f)
            circlePaints[0].color = argbEvaluator.evaluate(progress, COLOR_2, COLOR_3) as Int
            circlePaints[1].color = argbEvaluator.evaluate(progress, COLOR_3, COLOR_4) as Int
            circlePaints[2].color = argbEvaluator.evaluate(progress, COLOR_4, COLOR_1) as Int
            circlePaints[3].color = argbEvaluator.evaluate(progress, COLOR_1, COLOR_2) as Int
        }
    }

    private fun updateDotsAlpha() {
        val progress = CircleView.clamp(currentProgress, 0.6f, 1f)
        val alpha = (CircleView.mapValueFromRangeToRange(progress, 0.6f, 1f, 255f, 0f) as? Float)?.toInt()
                ?: 0
        circlePaints.forEach { it.alpha = alpha }
    }

    companion object {

        private const val DOTS_COUNT = 7
        private const val OUTER_DOTS_POSITION_ANGLE = 360 / DOTS_COUNT

        val DOTS_PROGRESS: Property<DotsView, Float> = object : Property<DotsView, Float>(Float::class.java, "dotsProgress") {
            override operator fun get(o: DotsView): Float? {
                return o.currentProgress
            }

            override operator fun set(o: DotsView, value: Float?) {
                o.currentProgress = value!!
            }
        }
    }
}
