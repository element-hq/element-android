/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.reactions.widget

import android.animation.ArgbEvaluator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.util.AttributeSet
import android.util.Property
import android.view.View
import kotlin.math.max
import kotlin.math.min

/**
 * This view is responsible for drawing big circle that will pulse when clicked
 * As describe in http://frogermcs.github.io/twitters-like-animation-in-android-alternative/
 */
class CircleView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var startColor = -0xa8de
    var endColor = -0x3ef9

    private val argbEvaluator = ArgbEvaluator()

    private val circlePaint = Paint()
    private val maskPaint = Paint()

    private lateinit var tempBitmap: Bitmap
    private lateinit var tempCanvas: Canvas

    var outerCircleRadiusProgress = 0f
        set(value) {
            field = value
            updateCircleColor()
            postInvalidate()
        }
    var innerCircleRadiusProgress = 0f
        set(value) {
            field = value
            postInvalidate()
        }

    private var maxCircleSize: Int = 0

    init {
        circlePaint.style = Paint.Style.FILL
        maskPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        maxCircleSize = w / 2
        tempBitmap = Bitmap.createBitmap(width, width, Bitmap.Config.ARGB_8888)
        tempCanvas = Canvas(tempBitmap)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        tempCanvas.drawColor(0xffffff, PorterDuff.Mode.CLEAR)
        tempCanvas.drawCircle(width / 2f, height / 2f, outerCircleRadiusProgress * maxCircleSize, circlePaint)
        tempCanvas.drawCircle(width / 2f, height / 2f, innerCircleRadiusProgress * maxCircleSize, maskPaint)
        canvas.drawBitmap(tempBitmap, 0f, 0f, null)
    }

//    fun setInnerCircleRadiusProgress(innerCircleRadiusProgress: Float) {
//        this.innerCircleRadiusProgress = innerCircleRadiusProgress
//        postInvalidate()
//    }

//    fun getInnerCircleRadiusProgress(): Float {
//        return innerCircleRadiusProgress
//    }

//    fun setOuterCircleRadiusProgress(outerCircleRadiusProgress: Float) {
//        this.outerCircleRadiusProgress = outerCircleRadiusProgress
//        updateCircleColor()
//        postInvalidate()
//    }

    private fun updateCircleColor() {
        var colorProgress = clamp(outerCircleRadiusProgress, 0.5f, 1f)
        colorProgress = mapValueFromRangeToRange(colorProgress, 0.5f, 1f, 0f, 1f)
        this.circlePaint.color = argbEvaluator.evaluate(colorProgress, startColor, endColor) as Int
    }

//    fun getOuterCircleRadiusProgress(): Float {
//        return outerCircleRadiusProgress
//    }

    companion object {

        val INNER_CIRCLE_RADIUS_PROGRESS: Property<CircleView, Float> = object : Property<CircleView, Float>(Float::class.java, "innerCircleRadiusProgress") {
            override operator fun get(o: CircleView): Float? {
                return o.innerCircleRadiusProgress
            }

            override operator fun set(o: CircleView, value: Float?) {
                value?.let {
                    o.innerCircleRadiusProgress = it
                }
            }
        }

        val OUTER_CIRCLE_RADIUS_PROGRESS: Property<CircleView, Float> = object : Property<CircleView, Float>(Float::class.java, "outerCircleRadiusProgress") {
            override operator fun get(o: CircleView): Float? {
                return o.outerCircleRadiusProgress
            }

            override operator fun set(o: CircleView, value: Float?) {
                value?.let {
                    o.outerCircleRadiusProgress = it
                }
            }
        }

        fun mapValueFromRangeToRange(value: Float, fromLow: Float, fromHigh: Float, toLow: Float, toHigh: Float): Float {
            return toLow + (value - fromLow) / (fromHigh - fromLow) * (toHigh - toLow)
        }

        fun clamp(value: Float, low: Float, high: Float): Float {
            return min(max(value, low), high)
        }
    }
}
