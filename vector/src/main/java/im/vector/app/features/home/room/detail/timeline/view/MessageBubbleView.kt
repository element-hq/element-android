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
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.RelativeLayout
import androidx.constraintlayout.widget.ConstraintLayout
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
import im.vector.app.features.themes.ThemeUtils

class MessageBubbleView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null,
                                                  defStyleAttr: Int = 0) :
    RelativeLayout(context, attrs, defStyleAttr), MessageViewConfiguration {

    override var isIncoming: Boolean = false
        set(value) {
            field = value
            render()
        }

    override var isFirstFromSender: Boolean = false
        set(value) {
            field = value
            render()
        }
    override var isLastFromSender: Boolean = false
        set(value) {
            field = value
            render()
        }

    override var showTimeAsOverlay: Boolean = true
        set(value) {
            field = value
            render()
        }

    private val cornerRadius = DimensionConverter(resources).dpToPx(12).toFloat()

    init {
        inflate(context, R.layout.view_message_bubble, this)
        context.withStyledAttributes(attrs, R.styleable.MessageBubble) {
            isIncoming = getBoolean(R.styleable.MessageBubble_incoming_style, false)
            showTimeAsOverlay = getBoolean(R.styleable.MessageBubble_show_time_overlay, true)
            isFirstFromSender = getBoolean(R.styleable.MessageBubble_is_first, false)
            isLastFromSender = getBoolean(R.styleable.MessageBubble_is_last, false)
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        render()
    }

    private fun render() {
        val currentLayoutDirection = layoutDirection
        val bubbleView: ConstraintLayout = findViewById(R.id.bubbleView)
        bubbleView.apply {
            background = createBackgroundDrawable()
            outlineProvider = ViewOutlineProvider.BACKGROUND
            clipToOutline = true
        }
        if (isIncoming) {
            findViewById<View>(R.id.informationBottom).layoutDirection = currentLayoutDirection
            findViewById<View>(R.id.bubbleWrapper).layoutDirection = currentLayoutDirection
            bubbleView.layoutDirection = currentLayoutDirection
            findViewById<View>(R.id.messageEndGuideline).updateLayoutParams<LayoutParams> {
                marginEnd = resources.getDimensionPixelSize(R.dimen.chat_bubble_margin_end)
            }
        } else {
            val oppositeLayoutDirection = if (currentLayoutDirection == View.LAYOUT_DIRECTION_LTR) {
                View.LAYOUT_DIRECTION_RTL
            } else {
                View.LAYOUT_DIRECTION_LTR
            }

            findViewById<View>(R.id.informationBottom).layoutDirection = oppositeLayoutDirection
            findViewById<View>(R.id.bubbleWrapper).layoutDirection = oppositeLayoutDirection
            bubbleView.layoutDirection = currentLayoutDirection
            findViewById<View>(R.id.messageEndGuideline).updateLayoutParams<LayoutParams> {
                marginEnd = resources.getDimensionPixelSize(R.dimen.chat_bubble_margin_start)
            }
        }
        ConstraintSet().apply {
            clone(bubbleView)
            clear(R.id.viewStubContainer, ConstraintSet.END)
            if (showTimeAsOverlay) {
                connect(R.id.viewStubContainer, ConstraintSet.END, R.id.messageTimeView, ConstraintSet.START, 0)
            } else {
                connect(R.id.viewStubContainer, ConstraintSet.END, R.id.parent, ConstraintSet.END, 0)
            }
            applyTo(bubbleView)
        }
    }

    private fun createBackgroundDrawable(): Drawable {
        val (topCornerFamily, topRadius) = if (isFirstFromSender) {
            Pair(CornerFamily.ROUNDED, cornerRadius)
        } else {
            Pair(CornerFamily.CUT, 0f)
        }
        val (bottomCornerFamily, bottomRadius) = if (isLastFromSender) {
            Pair(CornerFamily.ROUNDED, cornerRadius)
        } else {
            Pair(CornerFamily.CUT, 0f)
        }
        val shapeAppearanceModelBuilder = ShapeAppearanceModel().toBuilder()
        val backgroundColor: Int
        if (isIncoming) {
            backgroundColor = ThemeUtils.getColor(context, R.attr.vctr_system)
            shapeAppearanceModelBuilder
                    .setTopRightCorner(CornerFamily.ROUNDED, cornerRadius)
                    .setBottomRightCorner(CornerFamily.ROUNDED, cornerRadius)
                    .setTopLeftCorner(topCornerFamily, topRadius)
                    .setBottomLeftCorner(bottomCornerFamily, bottomRadius)
        } else {
            val resolvedColor = ContextCompat.getColor(context, R.color.palette_element_green)
            val alpha = if (ThemeUtils.isLightTheme(context)) 0x0E else 0x26
            backgroundColor = ColorUtils.setAlphaComponent(resolvedColor, alpha)
            shapeAppearanceModelBuilder
                    .setTopLeftCorner(CornerFamily.ROUNDED, cornerRadius)
                    .setBottomLeftCorner(CornerFamily.ROUNDED, cornerRadius)
                    .setTopRightCorner(topCornerFamily, topRadius)
                    .setBottomRightCorner(bottomCornerFamily, bottomRadius)
        }
        val shapeAppearanceModel = shapeAppearanceModelBuilder.build()
        return MaterialShapeDrawable(shapeAppearanceModel).apply {
            fillColor = ColorStateList.valueOf(backgroundColor)
        }
    }
}
