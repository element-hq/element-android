/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.timeline.view

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.RelativeLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.google.android.material.shape.MaterialShapeDrawable
import im.vector.app.R
import im.vector.app.core.resources.DefaultLocaleProvider
import im.vector.app.core.resources.getLayoutDirectionFromCurrentLocale
import im.vector.app.core.utils.DimensionConverter
import im.vector.app.databinding.ViewMessageBubbleBinding
import im.vector.app.features.home.room.detail.timeline.style.TimelineMessageLayout
import im.vector.app.features.home.room.detail.timeline.style.shapeAppearanceModel
import im.vector.app.features.themes.ThemeUtils
import timber.log.Timber

class MessageBubbleView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
) : RelativeLayout(context, attrs, defStyleAttr), TimelineMessageLayoutRenderer {

    private var isIncoming: Boolean = false

    private val horizontalStubPadding = DimensionConverter(resources).dpToPx(12)
    private val verticalStubPadding = DimensionConverter(resources).dpToPx(8)

    private lateinit var views: ViewMessageBubbleBinding
    private lateinit var bubbleDrawable: MaterialShapeDrawable
    private lateinit var rippleMaskDrawable: MaterialShapeDrawable

    init {
        inflate(context, R.layout.view_message_bubble, this)
        context.withStyledAttributes(attrs, im.vector.lib.ui.styles.R.styleable.MessageBubble) {
            isIncoming = getBoolean(im.vector.lib.ui.styles.R.styleable.MessageBubble_incoming_style, false)
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        views = ViewMessageBubbleBinding.bind(this)
        val currentLayoutDirection = DefaultLocaleProvider(resources).getLayoutDirectionFromCurrentLocale()
        val layoutDirectionToSet = if (isIncoming) {
            currentLayoutDirection
        } else {
            if (currentLayoutDirection == View.LAYOUT_DIRECTION_LTR) {
                View.LAYOUT_DIRECTION_RTL
            } else {
                View.LAYOUT_DIRECTION_LTR
            }
        }
        views.informationBottom.layoutDirection = layoutDirectionToSet
        views.messageThreadSummaryContainer.layoutDirection = layoutDirectionToSet
        views.bubbleWrapper.layoutDirection = layoutDirectionToSet
        views.bubbleView.layoutDirection = currentLayoutDirection

        bubbleDrawable = MaterialShapeDrawable()
        rippleMaskDrawable = MaterialShapeDrawable()
        DrawableCompat.setTint(rippleMaskDrawable, Color.WHITE)
        views.bubbleView.apply {
            outlineProvider = ViewOutlineProvider.BACKGROUND
            clipToOutline = true
            background = RippleDrawable(
                    ContextCompat.getColorStateList(context, com.google.android.material.R.color.mtrl_btn_ripple_color)
                            ?: ColorStateList.valueOf(Color.TRANSPARENT),
                    bubbleDrawable,
                    rippleMaskDrawable
            )
        }
    }

    override fun renderMessageLayout(messageLayout: TimelineMessageLayout) {
        (messageLayout as? TimelineMessageLayout.Bubble)
                ?.updateDrawables()
                ?.setConstraints()
                ?.toggleMessageOverlay()
                ?.setPadding()
                ?.setMargins()
                ?.setAdditionalTopSpace()
                ?: Timber.v("Can't render messageLayout $messageLayout")
    }

    private fun TimelineMessageLayout.Bubble.updateDrawables() = apply {
        val shapeAppearanceModel = cornersRadius.shapeAppearanceModel()
        bubbleDrawable.apply {
            this.shapeAppearanceModel = shapeAppearanceModel
            this.fillColor = if (isPseudoBubble) {
                ColorStateList.valueOf(Color.TRANSPARENT)
            } else {
                val backgroundColorAttr =
                        if (isIncoming) {
                            im.vector.lib.ui.styles.R.attr.vctr_message_bubble_inbound
                        } else {
                            im.vector.lib.ui.styles.R.attr.vctr_message_bubble_outbound
                        }
                val backgroundColor = ThemeUtils.getColor(context, backgroundColorAttr)
                ColorStateList.valueOf(backgroundColor)
            }
        }
        rippleMaskDrawable.shapeAppearanceModel = shapeAppearanceModel
    }

    private fun TimelineMessageLayout.Bubble.setConstraints() = apply {
        ConstraintSet().apply {
            clone(views.bubbleView)
            clear(R.id.viewStubContainer, ConstraintSet.END)
            if (timestampInsideMessage) {
                connect(R.id.viewStubContainer, ConstraintSet.END, com.google.android.material.R.id.parent, ConstraintSet.END, 0)
            } else {
                connect(R.id.viewStubContainer, ConstraintSet.END, R.id.messageTimeView, ConstraintSet.START, 0)
            }
            applyTo(views.bubbleView)
        }
    }

    private fun TimelineMessageLayout.Bubble.toggleMessageOverlay() = apply {
        if (addMessageOverlay) {
            val timeColor = ContextCompat.getColor(context, im.vector.lib.ui.styles.R.color.palette_white)
            views.messageTimeView.setTextColor(timeColor)
            views.messageOverlayView.isVisible = true
            (views.messageOverlayView.background as? GradientDrawable)?.cornerRadii = cornersRadius.toFloatArray()
        } else {
            val timeColor = ThemeUtils.getColor(context, im.vector.lib.ui.styles.R.attr.vctr_content_tertiary)
            views.messageTimeView.setTextColor(timeColor)
            views.messageOverlayView.isVisible = false
        }
    }

    private fun TimelineMessageLayout.Bubble.setPadding() = apply {
        if (isPseudoBubble && timestampInsideMessage) {
            views.viewStubContainer.root.setPadding(0, 0, 0, 0)
        } else {
            views.viewStubContainer.root.setPadding(horizontalStubPadding, verticalStubPadding, horizontalStubPadding, verticalStubPadding)
        }
    }

    private fun TimelineMessageLayout.Bubble.setMargins() = apply {
        if (isIncoming) {
            views.messageEndGuideline.updateLayoutParams<LayoutParams> {
                marginEnd = resources.getDimensionPixelSize(im.vector.lib.ui.styles.R.dimen.chat_bubble_margin_end)
            }
            views.messageStartGuideline.updateLayoutParams<LayoutParams> {
                marginStart = resources.getDimensionPixelSize(im.vector.lib.ui.styles.R.dimen.chat_bubble_margin_start)
            }
        } else {
            views.messageEndGuideline.updateLayoutParams<LayoutParams> {
                marginEnd = resources.getDimensionPixelSize(im.vector.lib.ui.styles.R.dimen.chat_bubble_margin_start)
            }
            views.messageStartGuideline.updateLayoutParams<LayoutParams> {
                marginStart = resources.getDimensionPixelSize(im.vector.lib.ui.styles.R.dimen.chat_bubble_margin_end)
            }
        }
    }

    private fun TimelineMessageLayout.Bubble.setAdditionalTopSpace() = apply {
        views.additionalTopSpace.isVisible = addTopMargin
    }

    private fun TimelineMessageLayout.Bubble.CornersRadius.toFloatArray(): FloatArray {
        return floatArrayOf(topStartRadius, topStartRadius, topEndRadius, topEndRadius, bottomEndRadius, bottomEndRadius, bottomStartRadius, bottomStartRadius)
    }
}
