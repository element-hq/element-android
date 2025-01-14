/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.platform

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.text.TextPaint
import android.util.AttributeSet
import androidx.core.content.res.use
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class BadgeFloatingActionButton @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FloatingActionButton(context, attrs, defStyleAttr) {

    private val textPaint = TextPaint(ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.LEFT
    }
    private val tintPaint = Paint(ANTI_ALIAS_FLAG)

    private var countStr: String
    private var countMaxStr: String
    private var counterBounds: RectF = RectF()
    private var counterTextBounds: Rect = Rect()
    private var counterMaxTextBounds: Rect = Rect()
    private var counterPossibleCenter: PointF = PointF()

    private var fabBounds: Rect = Rect()

    var counterTextColor: Int
        get() = textPaint.color
        set(value) {
            val was = textPaint.color
            if (was != value) {
                textPaint.color = value
                invalidate()
            }
        }

    var counterBackgroundColor: Int
        get() = tintPaint.color
        set(value) {
            val was = tintPaint.color
            if (was != value) {
                tintPaint.color = value
                invalidate()
            }
        }

    var counterTextSize: Float
        get() = textPaint.textSize
        set(value) {
            val was = textPaint.textSize
            if (was != value) {
                textPaint.textSize = value
                invalidate()
                requestLayout()
            }
        }

    var counterTextPadding: Float = 0f
        set(value) {
            if (field != value) {
                field = value
                invalidate()
                requestLayout()
            }
        }

    var maxCount: Int = 99
        set(value) {
            if (field != value) {
                field = value
                countMaxStr = "$value+"

                requestLayout()
            }
        }

    var count: Int = 0
        set(value) {
            if (field != value) {
                field = value
                countStr = countStr(value)
                textPaint.getTextBounds(countStr, 0, countStr.length, counterTextBounds)
                invalidate()
            }
        }

    var drawBadge: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    init {
        if (isInEditMode) {
            count = 3
        }

        countStr = countStr(count)
        textPaint.getTextBounds(countStr, 0, countStr.length, counterTextBounds)
        countMaxStr = "$maxCount+"

        attrs?.let { initAttrs(attrs) }
    }

    @SuppressWarnings("Recycle")
    private fun initAttrs(attrs: AttributeSet) {
        context.obtainStyledAttributes(attrs, im.vector.lib.ui.styles.R.styleable.BadgeFloatingActionButton).use {
            counterBackgroundColor = it.getColor(im.vector.lib.ui.styles.R.styleable.BadgeFloatingActionButton_badgeBackgroundColor, 0)
            counterTextPadding = it.getDimension(im.vector.lib.ui.styles.R.styleable.BadgeFloatingActionButton_badgeTextPadding, 0f)
            counterTextSize = it.getDimension(im.vector.lib.ui.styles.R.styleable.BadgeFloatingActionButton_badgeTextSize, 14f)
            counterTextColor = it.getColor(im.vector.lib.ui.styles.R.styleable.BadgeFloatingActionButton_badgeTextColor, Color.WHITE)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        calculateCounterBounds(counterBounds)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (count > 0 || drawBadge) {
            canvas.drawCircle(counterBounds.centerX(), counterBounds.centerY(), counterBounds.width() / 2f, tintPaint)
        }
        if (count > 0) {
            val textX = counterBounds.centerX() - counterTextBounds.width() / 2f - counterTextBounds.left
            val textY = counterBounds.centerY() + counterTextBounds.height() / 2f - counterTextBounds.bottom
            canvas.drawText(countStr, textX, textY, textPaint)
        }
    }

    private fun calculateCounterBounds(outRect: RectF) {
        getMeasuredContentRect(fabBounds)
        calculateCounterCenter(fabBounds, counterPossibleCenter)

        textPaint.getTextBounds(countMaxStr, 0, countMaxStr.length, counterMaxTextBounds)
        val counterDiameter = max(counterMaxTextBounds.width(), counterMaxTextBounds.height()) + 2 * counterTextPadding

        val counterRight = min(counterPossibleCenter.x + counterDiameter / 2, fabBounds.right.toFloat())
        val counterTop = max(counterPossibleCenter.y - counterDiameter / 2, fabBounds.top.toFloat())

        outRect.set(counterRight - counterDiameter, counterTop, counterRight, counterTop + counterDiameter)
    }

    private fun calculateCounterCenter(inBounds: Rect, outPoint: PointF) {
        val radius = min(inBounds.width(), inBounds.height()) / 2f
        calculateCounterCenter(radius, outPoint)
        outPoint.x = inBounds.centerX() + outPoint.x
        outPoint.y = inBounds.centerY() - outPoint.y
    }

    private fun calculateCounterCenter(radius: Float, outPoint: PointF) =
            calculateCounterCenter(radius, (PI / 4).toFloat(), outPoint)

    private fun calculateCounterCenter(radius: Float, angle: Float, outPoint: PointF) {
        outPoint.x = radius * cos(angle)
        outPoint.y = radius * sin(angle)
    }

    private fun countStr(count: Int) = if (count > maxCount) "$maxCount+" else count.toString()

    companion object {
        val TEXT_APPEARANCE_SUPPORTED_ATTRS = intArrayOf(android.R.attr.textSize, android.R.attr.textColor)
    }
}
