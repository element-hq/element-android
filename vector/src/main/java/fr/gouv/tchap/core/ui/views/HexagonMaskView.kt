/*
 * Copyright 2021 New Vector Ltd
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
package fr.gouv.tchap.core.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.sqrt

class HexagonMaskView : AppCompatImageView {
    private var hexagonPath: Path? = null
    private var width = 0f
    private var height = 0f
    private var borderPaint: Paint? = null
    private var borderRatio = 0

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(
            context: Context,
            attrs: AttributeSet?,
            defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
        init()
    }

    /**
     * Define the border settings
     *
     * @param color the border color (Color.LTGRAY by default).
     * @param ratio the ratio of the border width to the radius
     * of the hexagon (value between 0 and 100, default value: 1)
     */
    fun setBorderSettings(color: Int, ratio: Int) {
        borderPaint?.color = color

        val finalRatio: Int = if (ratio >= 0) {
            if (ratio > MAX_RATIO) MAX_RATIO else ratio
        } else 0

        if (borderRatio != ratio) {
            borderRatio = finalRatio
            // The hexagon path must be updated
            calculatePath()
        } else {
            invalidate()
        }
    }

    private fun init() {
        hexagonPath = Path()
        borderPaint = Paint()

        borderPaint?.apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            color = Color.LTGRAY
        }

        borderRatio = DEFAULT_RATIO
    }

    private fun calculatePath() {
        // Compute the radius of the hexagon, and the border width
        val radius = height / DEFAULT_DIVIDER
        val borderWidth = radius * borderRatio / PCT_VALUE
        borderPaint?.strokeWidth = borderWidth

        // Define the hexagon path by placing it in the middle of the border.
        val pathRadius = radius - borderWidth / DEFAULT_DIVIDER
        val triangleHeight = (sqrt(DEFAULT_DOUBLE_RADIUS) * pathRadius / DEFAULT_DIVIDER).toFloat()
        val centerX = width / DEFAULT_DIVIDER
        val centerY = height / DEFAULT_DIVIDER

        hexagonPath?.apply {
            reset()
            moveTo(centerX, centerY + pathRadius)
            lineTo(centerX - triangleHeight, centerY + pathRadius / DEFAULT_DIVIDER)
            lineTo(centerX - triangleHeight, centerY - pathRadius / DEFAULT_DIVIDER)
            lineTo(centerX, centerY - pathRadius)
            lineTo(centerX + triangleHeight, centerY - pathRadius / DEFAULT_DIVIDER)
            lineTo(centerX + triangleHeight, centerY + pathRadius / DEFAULT_DIVIDER)
            lineTo(centerX, centerY + pathRadius)
            // Add again the first segment to get the right display of the border.
            lineTo(centerX - triangleHeight, centerY + pathRadius / DEFAULT_DIVIDER)
        }

        invalidate()
    }

    public override fun onDraw(canvas: Canvas) {
        // Apply a clip to draw the bitmap inside an hexagon shape
        canvas.apply {
            save()

            hexagonPath?.let { clipPath(it) }

            super.onDraw(this)

            // Restore the canvas context
            restore()

            // Draw the border
            hexagonPath?.let { hexPath ->
                borderPaint?.let { border ->
                    drawPath(hexPath, border)
                }
            }
        }
    }

    // getting the view size
    public override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        width = this.measuredWidth.toFloat()
        height = this.measuredHeight.toFloat()
        calculatePath()
    }

    companion object {
        private const val DEFAULT_DIVIDER = 2
        private const val PCT_VALUE = 100
        private const val MAX_RATIO = 100
        private const val DEFAULT_RATIO = 3
        private const val DEFAULT_DOUBLE_RADIUS = DEFAULT_RATIO.toDouble()
    }
}
