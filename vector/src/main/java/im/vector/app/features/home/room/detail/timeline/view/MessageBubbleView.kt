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
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.RelativeLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.ColorUtils
import androidx.core.view.updateLayoutParams
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import im.vector.app.R
import im.vector.app.core.utils.DimensionConverter
import im.vector.app.databinding.ViewMessageBubbleBinding
import im.vector.app.features.home.room.detail.timeline.style.TimelineMessageLayout
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
        if (messageLayout.isPseudoBubble) {
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

    private fun createBackgroundDrawable(messageLayout: TimelineMessageLayout.Bubble): Drawable {
        val (topCornerFamily, topRadius) = if (messageLayout.isFirstFromThisSender) {
            Pair(CornerFamily.ROUNDED, cornerRadius)
        } else {
            Pair(CornerFamily.CUT, 0f)
        }
        val (bottomCornerFamily, bottomRadius) = if (messageLayout.isLastFromThisSender) {
            Pair(CornerFamily.ROUNDED, cornerRadius)
        } else {
            Pair(CornerFamily.CUT, 0f)
        }
        val shapeAppearanceModelBuilder = ShapeAppearanceModel().toBuilder()
        val backgroundColor: Int
        if (isIncoming) {
            backgroundColor = ThemeUtils.getColor(context, R.attr.vctr_message_bubble_inbound)
            shapeAppearanceModelBuilder
                    .setTopRightCorner(CornerFamily.ROUNDED, cornerRadius)
                    .setBottomRightCorner(CornerFamily.ROUNDED, cornerRadius)
                    .setTopLeftCorner(topCornerFamily, topRadius)
                    .setBottomLeftCorner(bottomCornerFamily, bottomRadius)
        } else {
            backgroundColor = ThemeUtils.getColor(context, R.attr.vctr_message_bubble_outbound)
            //val alpha = if (ThemeUtils.isLightTheme(context)) 0x0E else 0x26
            //backgroundColor = ColorUtils.setAlphaComponent(resolvedColor, alpha)
            shapeAppearanceModelBuilder
                    .setTopLeftCorner(CornerFamily.ROUNDED, cornerRadius)
                    .setBottomLeftCorner(CornerFamily.ROUNDED, cornerRadius)
                    .setTopRightCorner(topCornerFamily, topRadius)
                    .setBottomRightCorner(bottomCornerFamily, bottomRadius)
        }
        val shapeAppearanceModel = shapeAppearanceModelBuilder.build()
        return MaterialShapeDrawable(shapeAppearanceModel).apply {
            fillColor = if (messageLayout.isPseudoBubble) {
                ColorStateList.valueOf(Color.TRANSPARENT)
            } else {
                ColorStateList.valueOf(backgroundColor)
            }
        }
    }
}
