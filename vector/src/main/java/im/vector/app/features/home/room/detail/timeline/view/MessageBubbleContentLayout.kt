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

package im.vector.app.features.home.room.detail.timeline.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewStub
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.marginBottom
import androidx.core.view.marginEnd
import androidx.core.view.marginStart
import androidx.core.view.marginTop
import im.vector.app.R
import im.vector.app.core.resources.LocaleProvider
import im.vector.app.core.resources.getLayoutDirectionFromCurrentLocale

class MessageBubbleContentLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
        ConstraintLayout(context, attrs, defStyleAttr) {

    private var messageTextView: TextView? = null

    private lateinit var contentContainerView: View
    private lateinit var timeView: View
    private lateinit var contentOverlayView: View
    private var localeLayoutDirection: Int = View.LAYOUT_DIRECTION_LOCALE

    private val timeViewMeasuredWidthWithMargins: Int
        get() = timeView.measuredWidth + timeView.marginStart + timeView.marginEnd

    override fun onFinishInflate() {
        super.onFinishInflate()
        val textViewStub: ViewStub = findViewById(R.id.messageContentTextStub)
        contentContainerView = findViewById(R.id.viewStubContainer)
        contentOverlayView = findViewById(R.id.messageOverlayView)
        timeView = findViewById(R.id.messageTimeView)
        textViewStub.setOnInflateListener { _, inflated ->
            textViewStub.setOnInflateListener(null)
            messageTextView = inflated.findViewById(R.id.messageTextView)
        }
        localeLayoutDirection = LocaleProvider(resources).getLayoutDirectionFromCurrentLocale()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Let the ConstraintLayouts measure children
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val messageTextView = this.messageTextView
        // Then if we have a text message layout, we can resize ourself so we can position the timeView without losing space.
        if (messageTextView != null) {
            val width: Int
            val height: Int
            val textLineCount = messageTextView.lineCount
            val maxContentWidth = (contentContainerView.layoutParams as LayoutParams).matchConstraintMaxWidth
            val lastLineWidth = if (textLineCount != 0) messageTextView.layout.getLineWidth(textLineCount - 1) else 0f
            if (textLineCount == 1 && contentContainerView.measuredWidth + timeViewMeasuredWidthWithMargins < maxContentWidth) {
                width = contentContainerView.measuredWidth + timeViewMeasuredWidthWithMargins
                height = contentContainerView.measuredHeight
            } else if (textLineCount > 1 && lastLineWidth + timeViewMeasuredWidthWithMargins < contentContainerView.measuredWidth - contentContainerView.paddingEnd) {
                width = contentContainerView.measuredWidth
                height = contentContainerView.measuredHeight
            } else {
                width = contentContainerView.measuredWidth
                height = contentContainerView.measuredHeight + timeView.measuredHeight
            }
            setMeasuredDimension(width, height)
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        // If we have a text message layout, we want to render it manually, so we don't call super.onLayout
        if (messageTextView != null) {
            val parentLeft: Int = paddingLeft
            val parentRight: Int = right - left - paddingRight
            val parentTop: Int = paddingTop
            val parentBottom: Int = bottom - top - paddingBottom
            if (localeLayoutDirection == LAYOUT_DIRECTION_RTL) {
                val contentLeft  = parentRight - contentContainerView.measuredWidth - contentContainerView.marginEnd
                contentContainerView.layout(
                        contentLeft,
                        parentTop + contentContainerView.marginTop,
                        parentRight - contentContainerView.marginEnd,
                        parentTop + contentContainerView.marginTop + contentContainerView.measuredHeight
                )
                timeView.layout(
                        parentLeft + timeView.marginEnd,
                        parentBottom - timeView.measuredHeight - timeView.marginBottom,
                        parentLeft + timeView.measuredWidth + timeView.marginEnd,
                        parentBottom - timeView.marginBottom
                )
            } else {
                contentContainerView.layout(
                        parentLeft,
                        parentTop,
                        parentLeft + contentContainerView.measuredWidth,
                        parentTop + contentContainerView.measuredHeight
                )
                timeView.layout(
                        parentRight - timeView.measuredWidth - timeView.marginEnd,
                        parentBottom - timeView.measuredHeight - timeView.marginBottom,
                        parentRight - timeView.marginEnd,
                        parentBottom - timeView.marginBottom
                )
            }
        } else {
            super.onLayout(changed, left, top, right, bottom)
        }
    }

    /*
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthConstraints = paddingStart + paddingEnd
        val heightConstraints = paddingTop + paddingBottom

        val measuredWidth = MeasureSpec.getSize(widthMeasureSpec)
        val width: Int
        val height: Int

        val maxAvailableWidth = min(measuredWidth, maxWidth)
        measureChild(timeView, widthMeasureSpec, heightMeasureSpec)

        val messageTextView = this.messageTextView
        if (messageTextView != null) {
            val newWidthMeasureSpec = MeasureSpec.makeMeasureSpec(maxAvailableWidth, MeasureSpec.AT_MOST)
            measureChild(contentContainerView, newWidthMeasureSpec, heightMeasureSpec)
            val textLineCount = messageTextView.lineCount
            val lastLineWidth = if (textLineCount != 0) messageTextView.layout.getLineWidth(textLineCount - 1) else 0f
            if (textLineCount == 1 && contentContainerView.measuredWidth + widthConstraints + timeViewMeasuredWidthWithMargins < measuredWidth) {
                width = contentContainerView.measuredWidth + timeViewMeasuredWidthWithMargins + widthConstraints
                height = contentContainerView.measuredHeight + heightConstraints
            } else if (textLineCount > 1 && lastLineWidth + widthConstraints + timeViewMeasuredWidthWithMargins < contentContainerView.measuredWidth - contentContainerView.paddingEnd) {
                width = contentContainerView.measuredWidth + widthConstraints
                height = contentContainerView.measuredHeight + heightConstraints
            } else {
                width = contentContainerView.measuredWidth + widthConstraints
                height = contentContainerView.measuredHeight + heightConstraints + timeView.measuredHeight
            }
        } else {
            if (contentOverlayView.isVisible) {
                val newWidthMeasureSpec = MeasureSpec.makeMeasureSpec(maxAvailableWidth, MeasureSpec.AT_MOST)
                measureChild(contentContainerView, newWidthMeasureSpec, heightMeasureSpec)
                val overlayWidthSpec = MeasureSpec.makeMeasureSpec(contentContainerView.measuredWidth, MeasureSpec.EXACTLY)
                val overlayHeightSpec = MeasureSpec.makeMeasureSpec(contentContainerView.measuredHeight, MeasureSpec.EXACTLY)
                contentOverlayView.measure(overlayWidthSpec, overlayHeightSpec)
                width = contentContainerView.measuredWidth
                height = contentContainerView.measuredHeight
            } else {
                val newWidthMeasureSpec = MeasureSpec.makeMeasureSpec(maxAvailableWidth, MeasureSpec.AT_MOST)
                measureChild(contentContainerView, newWidthMeasureSpec, heightMeasureSpec)
                width = contentContainerView.measuredWidth
                height = contentContainerView.measuredHeight + timeView.measuredHeight
            }
        }
        setMeasuredDimension(width, height)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val parentLeft: Int = paddingLeft
        val parentRight: Int = right - left - paddingRight
        val parentTop: Int = paddingTop
        val parentBottom: Int = bottom - top - paddingBottom
        val contentRight = parentLeft + contentContainerView.measuredWidth
        val contentBottom = parentTop + contentContainerView.measuredHeight
        contentContainerView.layout(parentLeft, parentTop, contentRight, contentBottom)
        if (contentOverlayView.isVisible) {
            contentOverlayView.layout(parentLeft, parentTop, contentRight, contentBottom)
        }
        timeView.layout(parentRight - timeView.measuredWidth - timeViewMargins, parentBottom - timeView.measuredHeight - timeViewMargins, parentRight, parentBottom)
    }

     */
}
