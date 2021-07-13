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
import androidx.core.content.withStyledAttributes
import im.vector.app.R
import kotlin.math.min
import kotlin.math.sqrt

class HexagonMaskView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null
) : AppCompatImageView(context, attrs) {

    private val hexagonPath = Path()
    private val borderPaint = Paint()
    private val fillPaint = Paint()

    private val width
        get() = measuredWidth.toFloat()
    private val height
        get() = measuredHeight.toFloat()

    /**
     * The ratio of the border width to the radius of the hexagon (value between 0 and 100, default value: 3).
     */
    var borderRatio = DEFAULT_RATIO
        set(value) {
            val finalRatio = value.coerceIn(0, MAX_RATIO)
            if (field != finalRatio) {
                field = finalRatio
                calculatePath()
            }
        }

    init {
        borderPaint.apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
        }

        fillPaint.apply {
            isAntiAlias = true
            style = Paint.Style.FILL

        }

        context.withStyledAttributes(attrs, R.styleable.HexagonMaskView) {
            borderPaint.color = getColor(R.styleable.HexagonMaskView_borderColor, Color.LTGRAY)
            fillPaint.color = getColor(R.styleable.HexagonMaskView_fillColor, Color.TRANSPARENT)
        }
    }

    private fun calculatePath() {
        // Compute the radius of the hexagon, and the border width
        val radius = min(width, height) / DEFAULT_DIVIDER
        val borderWidth = context.resources.getDimension(R.dimen.tchap_hexagon_stroke_width)
        borderPaint.strokeWidth = borderWidth

        // Define the hexagon path by placing it in the middle of the border.
        val pathRadius = radius - borderWidth / DEFAULT_DIVIDER
        val triangleHeight = (sqrt(borderRatio.toFloat()) * pathRadius / DEFAULT_DIVIDER)
        val centerX = width / DEFAULT_DIVIDER
        val centerY = height / DEFAULT_DIVIDER

        hexagonPath.apply {
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

            // Fill the hexagon
            drawPath(hexagonPath, fillPaint)

            save()

            clipPath(hexagonPath)

            super.onDraw(this)

            // Restore the canvas context
            restore()

            // Draw the border
            drawPath(hexagonPath, borderPaint)
        }
    }

    // getting the view size
    public override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        calculatePath()
    }

    companion object {
        private const val DEFAULT_DIVIDER = 2
        private const val PERCENT_VALUE = 100
        private const val MAX_RATIO = 100
        private const val DEFAULT_RATIO = 3
    }
}
