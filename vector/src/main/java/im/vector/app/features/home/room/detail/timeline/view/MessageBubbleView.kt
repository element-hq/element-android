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
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import androidx.core.view.updateLayoutParams
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import im.vector.app.R
import im.vector.app.core.utils.DimensionConverter

class MessageBubbleView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null,
                                                  defStyleAttr: Int = 0) : RelativeLayout(context, attrs, defStyleAttr) {

    var incoming: Boolean = false
    var isFirst: Boolean = false
    var isLast: Boolean = false
    var cornerRadius = DimensionConverter(resources).dpToPx(12).toFloat()

    init {
        inflate(context, R.layout.view_message_bubble, this)
        context.withStyledAttributes(attrs, R.styleable.MessageBubble) {
            incoming = getBoolean(R.styleable.MessageBubble_incoming_style, false)
            isFirst = getBoolean(R.styleable.MessageBubble_is_first, false)
            isLast = getBoolean(R.styleable.MessageBubble_is_last, false)
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        val currentLayoutDirection = layoutDirection
        findViewById<ViewGroup>(R.id.bubbleView).apply {
            background = createBackgroundDrawable()
            outlineProvider = ViewOutlineProvider.BACKGROUND
            clipToOutline = true
        }
        if (incoming) {
            findViewById<View>(R.id.informationBottom).layoutDirection = currentLayoutDirection
            findViewById<View>(R.id.bubbleWrapper).layoutDirection = currentLayoutDirection
            findViewById<View>(R.id.bubbleView).layoutDirection = currentLayoutDirection
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
            findViewById<View>(R.id.bubbleView).layoutDirection = currentLayoutDirection
            findViewById<View>(R.id.messageEndGuideline).updateLayoutParams<LayoutParams> {
                marginEnd = resources.getDimensionPixelSize(R.dimen.chat_bubble_margin_start)
            }
        }
    }

    private fun createBackgroundDrawable(): Drawable {
        val topCornerFamily = if (isFirst) CornerFamily.ROUNDED else CornerFamily.CUT
        val bottomCornerFamily = if (isLast) CornerFamily.ROUNDED else CornerFamily.CUT
        val topRadius = if (isFirst) cornerRadius else 0f
        val bottomRadius = if (isLast) cornerRadius else 0f
        val shapeAppearanceModelBuilder = ShapeAppearanceModel().toBuilder()
        val backgroundColor: Int
        if (incoming) {
            backgroundColor = R.color.bubble_background_incoming
            shapeAppearanceModelBuilder
                    .setTopRightCorner(CornerFamily.ROUNDED, cornerRadius)
                    .setBottomRightCorner(CornerFamily.ROUNDED, cornerRadius)
                    .setTopLeftCorner(topCornerFamily, topRadius)
                    .setBottomLeftCorner(bottomCornerFamily, bottomRadius)
        } else {
            backgroundColor = R.color.bubble_background_outgoing
            shapeAppearanceModelBuilder
                    .setTopLeftCorner(CornerFamily.ROUNDED, cornerRadius)
                    .setBottomLeftCorner(CornerFamily.ROUNDED, cornerRadius)
                    .setTopRightCorner(topCornerFamily, topRadius)
                    .setBottomRightCorner(bottomCornerFamily, bottomRadius)
        }
        val shapeAppearanceModel = shapeAppearanceModelBuilder.build()
        val shapeDrawable = MaterialShapeDrawable(shapeAppearanceModel)
        shapeDrawable.fillColor = ContextCompat.getColorStateList(context, backgroundColor)
        return shapeDrawable
    }
}
