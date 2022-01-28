/*
 * Copyright (c) 2021 New Vector Ltd
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
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.RelativeLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.google.android.material.shape.MaterialShapeDrawable
import im.vector.app.R
import im.vector.app.core.utils.DimensionConverter
import im.vector.app.databinding.ViewMessageBubbleBinding
import im.vector.app.features.home.room.detail.timeline.style.TimelineMessageLayout
import im.vector.app.features.home.room.detail.timeline.style.shapeAppearanceModel
import im.vector.app.features.themes.ThemeUtils
import timber.log.Timber

class MessageBubbleView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null,
                                                  defStyleAttr: Int = 0) :
        RelativeLayout(context, attrs, defStyleAttr), TimelineMessageLayoutRenderer {

    private var isIncoming: Boolean = false

    private val cornerRadius = resources.getDimensionPixelSize(R.dimen.chat_bubble_corner_radius).toFloat()
    private val horizontalStubPadding = DimensionConverter(resources).dpToPx(12)
    private val verticalStubPadding = DimensionConverter(resources).dpToPx(4)

    private lateinit var views: ViewMessageBubbleBinding

    init {
        inflate(context, R.layout.view_message_bubble, this)
        context.withStyledAttributes(attrs, R.styleable.MessageBubble) {
            isIncoming = getBoolean(R.styleable.MessageBubble_incoming_style, false)
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        views = ViewMessageBubbleBinding.bind(this)
        val currentLayoutDirection = layoutDirection
        if (isIncoming) {
            views.informationBottom.layoutDirection = currentLayoutDirection
            views.bubbleWrapper.layoutDirection = currentLayoutDirection
            views.bubbleView.layoutDirection = currentLayoutDirection
        } else {
            val oppositeLayoutDirection = if (currentLayoutDirection == View.LAYOUT_DIRECTION_LTR) {
                View.LAYOUT_DIRECTION_RTL
            } else {
                View.LAYOUT_DIRECTION_LTR
            }

            views.informationBottom.layoutDirection = oppositeLayoutDirection
            views.bubbleWrapper.layoutDirection = oppositeLayoutDirection
            views.bubbleView.layoutDirection = currentLayoutDirection
        }
    }

    override fun render(messageLayout: TimelineMessageLayout) {
        if (messageLayout !is TimelineMessageLayout.Bubble) {
            Timber.v("Can't render messageLayout $messageLayout")
            return
        }
        views.bubbleView.apply {
            background = createBackgroundDrawable(messageLayout)
            outlineProvider = ViewOutlineProvider.BACKGROUND
            clipToOutline = true
        }
        ConstraintSet().apply {
            clone(views.bubbleView)
            clear(R.id.viewStubContainer, ConstraintSet.END)
            if (messageLayout.timestampAsOverlay) {
                val timeColor = ContextCompat.getColor(context, R.color.palette_white)
                views.messageTimeView.setTextColor(timeColor)
                connect(R.id.viewStubContainer, ConstraintSet.END, R.id.parent, ConstraintSet.END, 0)
            } else {
                val timeColor = ThemeUtils.getColor(context, R.attr.vctr_content_tertiary)
                views.messageTimeView.setTextColor(timeColor)
                connect(R.id.viewStubContainer, ConstraintSet.END, R.id.messageTimeView, ConstraintSet.START, 0)
            }
            applyTo(views.bubbleView)
        }
        if (messageLayout.timestampAsOverlay) {
            views.messageOverlayView.isVisible = true
            (views.messageOverlayView.background as? GradientDrawable)?.cornerRadii = messageLayout.cornerRadii(cornerRadius)
        } else {
            views.messageOverlayView.isVisible = false
        }
        if (messageLayout.isPseudoBubble && messageLayout.timestampAsOverlay) {
            views.viewStubContainer.root.setPadding(0, 0, 0, 0)
        } else {
            views.viewStubContainer.root.setPadding(horizontalStubPadding, verticalStubPadding, horizontalStubPadding, verticalStubPadding)
        }
        if (messageLayout.isIncoming) {
            views.messageEndGuideline.updateLayoutParams<LayoutParams> {
                marginEnd = resources.getDimensionPixelSize(R.dimen.chat_bubble_margin_end)
            }
            views.messageStartGuideline.updateLayoutParams<LayoutParams> {
                marginStart = resources.getDimensionPixelSize(R.dimen.chat_bubble_margin_start)
            }
        } else {
            views.messageEndGuideline.updateLayoutParams<LayoutParams> {
                marginEnd = resources.getDimensionPixelSize(R.dimen.chat_bubble_margin_start)
            }
            views.messageStartGuideline.updateLayoutParams<LayoutParams> {
                marginStart = resources.getDimensionPixelSize(R.dimen.chat_bubble_margin_end)
            }
        }
    }

    private fun TimelineMessageLayout.Bubble.cornerRadii(cornerRadius: Float): FloatArray {
        val topRadius = if (isFirstFromThisSender) cornerRadius else 0f
        val bottomRadius = if (isLastFromThisSender) cornerRadius else 0f
        return if (isIncoming) {
            floatArrayOf(topRadius, topRadius, cornerRadius, cornerRadius, cornerRadius, cornerRadius, bottomRadius, bottomRadius)
        } else {
            floatArrayOf(cornerRadius, cornerRadius, topRadius, topRadius, bottomRadius, bottomRadius, cornerRadius, cornerRadius)
        }
    }

    private fun createBackgroundDrawable(messageLayout: TimelineMessageLayout.Bubble): Drawable {
        val shapeAppearanceModel = messageLayout.shapeAppearanceModel(cornerRadius)
        return MaterialShapeDrawable(shapeAppearanceModel).apply {
            fillColor = if (messageLayout.isPseudoBubble) {
                ColorStateList.valueOf(Color.TRANSPARENT)
            } else {
                val backgroundColorAttr = if (isIncoming) R.attr.vctr_message_bubble_inbound else R.attr.vctr_message_bubble_outbound
                val backgroundColor = ThemeUtils.getColor(context, backgroundColorAttr)
                ColorStateList.valueOf(backgroundColor)
            }
        }
    }
}
