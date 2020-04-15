/*
 * Copyright (C) 2011 Micah Hainline
 * Copyright (C) 2012 Triposo
 * Copyright (C) 2013 Paul Imhoff
 * Copyright (C) 2014 Shahin Yousefi
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
 *
 */
package im.vector.riotx.core.platform

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.text.Layout
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextUtils.TruncateAt
import android.text.TextUtils.concat
import android.text.TextUtils.copySpansFrom
import android.text.TextUtils.indexOf
import android.text.TextUtils.lastIndexOf
import android.text.TextUtils.substring
import android.text.style.ForegroundColorSpan
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import timber.log.Timber
import java.util.ArrayList
import java.util.regex.Pattern

/*
 * Imported from https://gist.github.com/hateum/d2095575b441007d62b8
 *
 * Use it in your layout to avoid this issue: https://issuetracker.google.com/issues/121092510
 */

/**
 * A [android.widget.TextView] that ellipsizes more intelligently.
 * This class supports ellipsizing multiline text through setting `android:ellipsize`
 * and `android:maxLines`.
 *
 *
 * Note: [TruncateAt.MARQUEE] ellipsizing type is not supported.
 * This as to be used to get rid of the StaticLayout issue with maxLines and ellipsize causing some performance issues.
 */
class EllipsizingTextView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = android.R.attr.textViewStyle)
    : AppCompatTextView(context, attrs, defStyle) {

    private val ELLIPSIS = SpannableString("\u2026")
    private val ellipsizeListeners: MutableList<EllipsizeListener> = ArrayList()
    private var ellipsizeStrategy: EllipsizeStrategy? = null
    var isEllipsized = false
        private set
    private var isStale = false
    private var programmaticChange = false
    private var fullText: CharSequence? = null
    private var maxLines = 0
    private var lineSpacingMult = 1.0f
    private var lineAddVertPad = 0.0f
    /**
     * The end punctuation which will be removed when appending [.ELLIPSIS].
     */
    private var mEndPunctPattern: Pattern? = null

    fun setEndPunctuationPattern(pattern: Pattern?) {
        mEndPunctPattern = pattern
    }

    fun addEllipsizeListener(listener: EllipsizeListener) {
        ellipsizeListeners.add(listener)
    }

    fun removeEllipsizeListener(listener: EllipsizeListener) {
        ellipsizeListeners.remove(listener)
    }

    /**
     * @return The maximum number of lines displayed in this [android.widget.TextView].
     */
    override fun getMaxLines(): Int {
        return maxLines
    }

    override fun setMaxLines(maxLines: Int) {
        super.setMaxLines(maxLines)
        this.maxLines = maxLines
        isStale = true
    }

    /**
     * Determines if the last fully visible line is being ellipsized.
     *
     * @return `true` if the last fully visible line is being ellipsized;
     * otherwise, returns `false`.
     */
    fun ellipsizingLastFullyVisibleLine(): Boolean {
        return maxLines == Int.MAX_VALUE
    }

    override fun setLineSpacing(add: Float, mult: Float) {
        lineAddVertPad = add
        lineSpacingMult = mult
        super.setLineSpacing(add, mult)
    }

    override fun setText(text: CharSequence?, type: BufferType) {
        if (!programmaticChange) {
            fullText = if (text is Spanned) text else text
            isStale = true
        }
        super.setText(text, type)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (ellipsizingLastFullyVisibleLine()) {
            isStale = true
        }
    }

    override fun setPadding(left: Int, top: Int, right: Int, bottom: Int) {
        super.setPadding(left, top, right, bottom)
        if (ellipsizingLastFullyVisibleLine()) {
            isStale = true
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (isStale) {
            resetText()
        }
        super.onDraw(canvas)
    }

    /**
     * Sets the ellipsized text if appropriate.
     */
    private fun resetText() {
        val maxLines = maxLines
        var workingText = fullText
        var ellipsized = false
        if (maxLines != -1) {
            if (ellipsizeStrategy == null) setEllipsize(null)
            workingText = ellipsizeStrategy!!.processText(fullText)
            ellipsized = !ellipsizeStrategy!!.isInLayout(fullText)
        }
        if (workingText != text) {
            programmaticChange = true
            text = try {
                workingText
            } finally {
                programmaticChange = false
            }
        }
        isStale = false
        if (ellipsized != isEllipsized) {
            isEllipsized = ellipsized
            for (listener in ellipsizeListeners) {
                listener.ellipsizeStateChanged(ellipsized)
            }
        }
    }

    /**
     * Causes words in the text that are longer than the view is wide to be ellipsized
     * instead of broken in the middle. Use `null` to turn off ellipsizing.
     *
     *
     * Note: Method does nothing for [TruncateAt.MARQUEE]
     * ellipsizing type.
     *
     * @param where part of text to ellipsize
     */
    override fun setEllipsize(where: TruncateAt?) {
        if (where == null) {
            ellipsizeStrategy = EllipsizeNoneStrategy()
            return
        }
        ellipsizeStrategy = when (where) {
            TruncateAt.END     -> EllipsizeEndStrategy()
            TruncateAt.START   -> EllipsizeStartStrategy()
            TruncateAt.MIDDLE  -> EllipsizeMiddleStrategy()
            TruncateAt.MARQUEE -> EllipsizeNoneStrategy()
            else               -> EllipsizeNoneStrategy()
        }
    }

    /**
     * A listener that notifies when the ellipsize state has changed.
     */
    interface EllipsizeListener {
        fun ellipsizeStateChanged(ellipsized: Boolean)
    }

    /**
     * A base class for an ellipsize strategy.
     */
    private abstract inner class EllipsizeStrategy {
        /**
         * Returns ellipsized text if the text does not fit inside of the layout;
         * otherwise, returns the full text.
         *
         * @param text text to process
         * @return Ellipsized text if the text does not fit inside of the layout;
         * otherwise, returns the full text.
         */
        fun processText(text: CharSequence?): CharSequence? {
            return if (!isInLayout(text)) createEllipsizedText(text) else text
        }

        /**
         * Determines if the text fits inside of the layout.
         *
         * @param text text to fit
         * @return `true` if the text fits inside of the layout;
         * otherwise, returns `false`.
         */
        fun isInLayout(text: CharSequence?): Boolean {
            val layout = createWorkingLayout(text)
            return layout.lineCount <= linesCount
        }

        /**
         * Creates a working layout with the given text.
         *
         * @param workingText text to create layout with
         * @return [android.text.Layout] with the given text.
         */
        @Suppress("DEPRECATION")
        protected fun createWorkingLayout(workingText: CharSequence?): Layout {
            return StaticLayout(
                    workingText ?: "",
                    paint,
                    width - compoundPaddingLeft - compoundPaddingRight,
                    Layout.Alignment.ALIGN_NORMAL,
                    lineSpacingMult,
                    lineAddVertPad,
                    false
            )
        }

        /**
         * Get how many lines of text we are allowed to display.
         */
        protected val linesCount: Int
            get() = if (ellipsizingLastFullyVisibleLine()) {
                val fullyVisibleLinesCount = fullyVisibleLinesCount
                if (fullyVisibleLinesCount == -1) 1 else fullyVisibleLinesCount
            } else {
                maxLines
            }

        /**
         * Get how many lines of text we can display so their full height is visible.
         */
        protected val fullyVisibleLinesCount: Int
            get() {
                val layout = createWorkingLayout("")
                val height = height - compoundPaddingTop - compoundPaddingBottom
                val lineHeight = layout.getLineBottom(0)
                return height / lineHeight
            }

        /**
         * Creates ellipsized text from the given text.
         *
         * @param fullText text to ellipsize
         * @return Ellipsized text
         */
        protected abstract fun createEllipsizedText(fullText: CharSequence?): CharSequence?
    }

    /**
     * An [EllipsizingTextView.EllipsizeStrategy] that
     * does not ellipsize text.
     */
    private inner class EllipsizeNoneStrategy : EllipsizeStrategy() {
        override fun createEllipsizedText(fullText: CharSequence?): CharSequence? {
            return fullText
        }
    }

    /**
     * An [EllipsizingTextView.EllipsizeStrategy] that
     * ellipsizes text at the end.
     */
    private inner class EllipsizeEndStrategy : EllipsizeStrategy() {
        override fun createEllipsizedText(fullText: CharSequence?): CharSequence? {
            val layout = createWorkingLayout(fullText)
            val cutOffIndex = try {
                layout.getLineEnd(maxLines - 1)
            } catch (exception: IndexOutOfBoundsException) {
                // Not sure to understand why this is happening
                Timber.e(exception, "IndexOutOfBoundsException, maxLine: $maxLines")
                0
            }
            val textLength = fullText!!.length
            var cutOffLength = textLength - cutOffIndex
            if (cutOffLength < ELLIPSIS.length) cutOffLength = ELLIPSIS.length
            var workingText: CharSequence = substring(fullText, 0, textLength - cutOffLength).trim()
            while (!isInLayout(concat(stripEndPunctuation(workingText), ELLIPSIS))) {
                val lastSpace = lastIndexOf(workingText, ' ')
                if (lastSpace == -1) {
                    break
                }
                workingText = substring(workingText, 0, lastSpace).trim()
            }
            workingText = concat(stripEndPunctuation(workingText), ELLIPSIS)
            val dest = SpannableStringBuilder(workingText)
            if (fullText is Spanned) {
                copySpansFrom(fullText as Spanned?, 0, workingText.length, null, dest, 0)
            }
            return dest
        }

        /**
         * Strips the end punctuation from a given text according to [.mEndPunctPattern].
         *
         * @param workingText text to strip end punctuation from
         * @return Text without end punctuation.
         */
        fun stripEndPunctuation(workingText: CharSequence): String {
            return mEndPunctPattern!!.matcher(workingText).replaceFirst("")
        }
    }

    /**
     * An [EllipsizingTextView.EllipsizeStrategy] that
     * ellipsizes text at the start.
     */
    private inner class EllipsizeStartStrategy : EllipsizeStrategy() {
        override fun createEllipsizedText(fullText: CharSequence?): CharSequence? {
            val layout = createWorkingLayout(fullText)
            val cutOffIndex = layout.getLineEnd(maxLines - 1)
            val textLength = fullText!!.length
            var cutOffLength = textLength - cutOffIndex
            if (cutOffLength < ELLIPSIS.length) cutOffLength = ELLIPSIS.length
            var workingText: CharSequence = substring(fullText, cutOffLength, textLength).trim()
            while (!isInLayout(concat(ELLIPSIS, workingText))) {
                val firstSpace = indexOf(workingText, ' ')
                if (firstSpace == -1) {
                    break
                }
                workingText = substring(workingText, firstSpace, workingText.length).trim()
            }
            workingText = concat(ELLIPSIS, workingText)
            val dest = SpannableStringBuilder(workingText)
            if (fullText is Spanned) {
                copySpansFrom(fullText as Spanned?, textLength - workingText.length,
                        textLength, null, dest, 0)
            }
            return dest
        }
    }

    /**
     * An [EllipsizingTextView.EllipsizeStrategy] that
     * ellipsizes text in the middle.
     */
    private inner class EllipsizeMiddleStrategy : EllipsizeStrategy() {
        override fun createEllipsizedText(fullText: CharSequence?): CharSequence? {
            val layout = createWorkingLayout(fullText)
            val cutOffIndex = layout.getLineEnd(maxLines - 1)
            val textLength = fullText!!.length
            var cutOffLength = textLength - cutOffIndex
            if (cutOffLength < ELLIPSIS.length) cutOffLength = ELLIPSIS.length
            cutOffLength += cutOffIndex % 2 // Make it even.
            var firstPart = substring(
                    fullText, 0, textLength / 2 - cutOffLength / 2).trim()
            var secondPart = substring(
                    fullText, textLength / 2 + cutOffLength / 2, textLength).trim()
            while (!isInLayout(concat(firstPart, ELLIPSIS, secondPart))) {
                val lastSpaceFirstPart = firstPart.lastIndexOf(' ')
                val firstSpaceSecondPart = secondPart.indexOf(' ')
                if (lastSpaceFirstPart == -1 || firstSpaceSecondPart == -1) break
                firstPart = firstPart.substring(0, lastSpaceFirstPart).trim()
                secondPart = secondPart.substring(firstSpaceSecondPart, secondPart.length).trim()
            }
            val firstDest = SpannableStringBuilder(firstPart)
            val secondDest = SpannableStringBuilder(secondPart)
            if (fullText is Spanned) {
                copySpansFrom(fullText as Spanned?, 0, firstPart.length,
                        null, firstDest, 0)
                copySpansFrom(fullText as Spanned?, textLength - secondPart.length,
                        textLength, null, secondDest, 0)
            }
            return concat(firstDest, ELLIPSIS, secondDest)
        }
    }

    companion object {
        const val ELLIPSIZE_ALPHA = 0x88
        private val DEFAULT_END_PUNCTUATION = Pattern.compile("[.!?,;:\u2026]*$", Pattern.DOTALL)
    }

    init {
        val a = context.obtainStyledAttributes(attrs, intArrayOf(android.R.attr.maxLines, android.R.attr.ellipsize), defStyle, 0)
        maxLines = a.getInt(0, Int.MAX_VALUE)
        a.recycle()
        setEndPunctuationPattern(DEFAULT_END_PUNCTUATION)
        val currentTextColor = currentTextColor
        val ellipsizeColor = Color.argb(ELLIPSIZE_ALPHA, Color.red(currentTextColor), Color.green(currentTextColor), Color.blue(currentTextColor))
        ELLIPSIS.setSpan(ForegroundColorSpan(ellipsizeColor), 0, ELLIPSIS.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
}
