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

package im.vector.app.features.html

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.text.Layout
import android.text.TextPaint
import android.text.style.LeadingMarginSpan
import android.text.style.MetricAffectingSpan
import io.noties.markwon.core.MarkwonTheme

class HtmlCodeSpan(private val theme: MarkwonTheme, var isBlock: Boolean) : MetricAffectingSpan(), LeadingMarginSpan {

    private val rect = Rect()
    private val paint = Paint()

    override fun updateDrawState(p: TextPaint) {
        applyTextStyle(p)
        if (!isBlock) {
            p.bgColor = theme.getCodeBackgroundColor(p)
        }
    }

    override fun updateMeasureState(p: TextPaint) {
        applyTextStyle(p)
    }

    private fun applyTextStyle(p: TextPaint) {
        if (isBlock) {
            theme.applyCodeBlockTextStyle(p)
        } else {
            theme.applyCodeTextStyle(p)
        }
    }

    override fun getLeadingMargin(first: Boolean): Int {
        return theme.codeBlockMargin
    }

    override fun drawLeadingMargin(
            c: Canvas,
            p: Paint?,
            x: Int,
            dir: Int,
            top: Int,
            baseline: Int,
            bottom: Int,
            text: CharSequence?,
            start: Int,
            end: Int,
            first: Boolean,
            layout: Layout?
    ) {
        if (!isBlock) return

        paint.style = Paint.Style.FILL
        paint.color = theme.getCodeBlockBackgroundColor(p!!)
        val left: Int
        val right: Int
        if (dir > 0) {
            left = x
            right = c.width
        } else {
            left = x - c.width
            right = x
        }
        rect[left, top, right] = bottom
        c.drawRect(rect, paint)
    }
}
