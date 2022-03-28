/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.features.voice

import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import im.vector.app.R
import kotlin.math.max
import kotlin.random.Random

class AudioWaveformView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private enum class Alignment(var value: Int) {
        CENTER(0),
        BOTTOM(1),
        TOP(2)
    }

    private enum class Flow(var value: Int) {
        LTR(0),
        RTL(1)
    }

    data class FFT(val value: Float, var color: Int)

    private fun Int.dp() = this * Resources.getSystem().displayMetrics.density

    // Configuration fields
    private var alignment = Alignment.CENTER
    private var flow = Flow.LTR
    private var verticalPadding = 4.dp()
    private var horizontalPadding = 4.dp()
    private var barWidth = 2.dp()
    private var barSpace = 1.dp()
    private var barMinHeight = 1.dp()
    private var isBarRounded = true

    private val rawFftList = mutableListOf<FFT>()
    private var visibleBarHeights = mutableListOf<FFT>()

    private val barPaint = Paint()

    init {
        attrs?.let {
            context
                    .theme
                    .obtainStyledAttributes(
                            attrs,
                            R.styleable.AudioWaveformView,
                            0,
                            0
                    )
                    .apply {
                        alignment = Alignment.values().find { it.value == getInt(R.styleable.AudioWaveformView_alignment, alignment.value) }!!
                        flow = Flow.values().find { it.value == getInt(R.styleable.AudioWaveformView_flow, alignment.value) }!!
                        verticalPadding = getDimension(R.styleable.AudioWaveformView_verticalPadding, verticalPadding)
                        horizontalPadding = getDimension(R.styleable.AudioWaveformView_horizontalPadding, horizontalPadding)
                        barWidth = getDimension(R.styleable.AudioWaveformView_barWidth, barWidth)
                        barSpace = getDimension(R.styleable.AudioWaveformView_barSpace, barSpace)
                        barMinHeight = getDimension(R.styleable.AudioWaveformView_barMinHeight, barMinHeight)
                        isBarRounded = getBoolean(R.styleable.AudioWaveformView_isBarRounded, isBarRounded)
                        setWillNotDraw(false)
                        barPaint.isAntiAlias = true
                    }
                    .apply { recycle() }
                    .also {
                        barPaint.strokeWidth = barWidth
                        barPaint.strokeCap = if (isBarRounded) Paint.Cap.ROUND else Paint.Cap.BUTT
                    }
        }
    }

    fun initialize(fftList: List<FFT>) {
        handleNewFftList(fftList)
        invalidate()
    }

    fun add(fft: FFT) {
        handleNewFftList(listOf(fft))
        invalidate()
    }

    fun summarize() {
        if (rawFftList.isEmpty()) return

        val maxVisibleBarCount = getMaxVisibleBarCount()
        val summarizedFftList = rawFftList.summarize(maxVisibleBarCount)
        clear()
        handleNewFftList(summarizedFftList)
        invalidate()
    }

    fun updateColors(limitPercentage: Float, colorBefore: Int, colorAfter: Int) {
        val size = visibleBarHeights.size
        val limitIndex = (size * limitPercentage).toInt()
        visibleBarHeights.forEachIndexed { index, fft ->
            fft.color = if (index < limitIndex) {
                colorBefore
            } else {
                colorAfter
            }
        }
        invalidate()
    }

    fun clear() {
        rawFftList.clear()
        visibleBarHeights.clear()
    }

    private fun List<FFT>.summarize(target: Int): List<FFT> {
        flow = Flow.LTR
        val result = mutableListOf<FFT>()
        if (size <= target) {
            result.addAll(this)
            val missingItemCount = target - size
            repeat(missingItemCount) {
                val index = Random.nextInt(result.size)
                result.add(index, result[index])
            }
        } else {
            val step = (size.toDouble() - 1) / (target - 1)
            var index = 0.0
            while (index < size) {
                result.add(get(index.toInt()))
                index += step
            }
        }
        return result
    }

    private fun handleNewFftList(fftList: List<FFT>) {
        val maxVisibleBarCount = getMaxVisibleBarCount()
        fftList.forEach { fft ->
            rawFftList.add(fft)
            val barHeight = max(fft.value / MAX_FFT * (height - verticalPadding * 2), barMinHeight)
            visibleBarHeights.add(FFT(barHeight, fft.color))
            if (visibleBarHeights.size > maxVisibleBarCount) {
                visibleBarHeights = visibleBarHeights.subList(visibleBarHeights.size - maxVisibleBarCount, visibleBarHeights.size)
            }
        }
    }

    private fun getMaxVisibleBarCount() = ((width - horizontalPadding * 2) / (barWidth + barSpace)).toInt()

    private fun drawBars(canvas: Canvas) {
        var currentX = horizontalPadding
        val flowableBarHeights = if (flow == Flow.LTR) visibleBarHeights else visibleBarHeights.reversed()

        flowableBarHeights.forEach {
            barPaint.color = it.color
            when (alignment) {
                Alignment.BOTTOM -> {
                    val startY = height - verticalPadding
                    val stopY = startY - it.value
                    canvas.drawLine(currentX, startY, currentX, stopY, barPaint)
                }
                Alignment.CENTER -> {
                    val startY = (height - it.value) / 2
                    val stopY = startY + it.value
                    canvas.drawLine(currentX, startY, currentX, stopY, barPaint)
                }
                Alignment.TOP    -> {
                    val startY = verticalPadding
                    val stopY = startY + it.value
                    canvas.drawLine(currentX, startY, currentX, stopY, barPaint)
                }
            }
            currentX += barWidth + barSpace
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawBars(canvas)
    }

    companion object {
        const val MAX_FFT = 32760
    }
}
