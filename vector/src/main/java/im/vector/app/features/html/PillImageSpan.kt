/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

@file:Suppress("DEPRECATION")

package im.vector.app.features.html

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.text.style.ReplacementSpan
import android.widget.TextView
import androidx.annotation.UiThread
import androidx.core.content.ContextCompat
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.chip.ChipDrawable
import im.vector.app.R
import im.vector.app.core.extensions.isMatrixId
import im.vector.app.core.glide.GlideRequests
import im.vector.app.features.displayname.getBestName
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.themes.ThemeUtils
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.extensions.orTrue
import org.matrix.android.sdk.api.session.room.send.MatrixItemSpan
import org.matrix.android.sdk.api.util.MatrixItem
import java.lang.ref.WeakReference

/**
 * This span is able to replace a text by a [ChipDrawable]
 * It's needed to call [bind] method to start requesting avatar, otherwise only the placeholder icon will be displayed if not already cached.
 * Implements MatrixItemSpan so that it could be automatically transformed in matrix links and displayed as pills.
 */
class PillImageSpan(
        private val glideRequests: GlideRequests,
        private val avatarRenderer: AvatarRenderer,
        private val context: Context,
        override val matrixItem: MatrixItem
) : ReplacementSpan(), MatrixItemSpan {

    private val pillDrawable = createChipDrawable()
    private val target = PillImageSpanTarget(this)
    private var tv: WeakReference<TextView>? = null

    @UiThread
    fun bind(textView: TextView) {
        tv = WeakReference(textView)
        avatarRenderer.render(glideRequests, matrixItem, target)
    }

    // ReplacementSpan *****************************************************************************

    override fun getSize(
            paint: Paint, text: CharSequence,
            start: Int,
            end: Int,
            fm: Paint.FontMetricsInt?
    ): Int {
        val rect = pillDrawable.bounds
        if (fm != null) {
            val fmPaint = paint.fontMetricsInt
            val fontHeight = fmPaint.bottom - fmPaint.top
            val drHeight = rect.bottom - rect.top
            val top = drHeight / 2 - fontHeight / 4
            val bottom = drHeight / 2 + fontHeight / 4
            fm.ascent = -bottom
            fm.top = -bottom
            fm.bottom = top
            fm.descent = top
        }
        return rect.right
    }

    override fun draw(
            canvas: Canvas, text: CharSequence,
            start: Int,
            end: Int,
            x: Float,
            top: Int,
            y: Int,
            bottom: Int,
            paint: Paint
    ) {
        canvas.save()
        val fm = paint.fontMetricsInt
        val transY: Int = y + (fm.descent + fm.ascent - pillDrawable.bounds.bottom) / 2
        canvas.save()
        canvas.translate(x, transY.toFloat())

        val rect = Rect()
        canvas.getClipBounds(rect)
        val maxWidth = rect.right
        if (pillDrawable.intrinsicWidth > maxWidth) {
            pillDrawable.setBounds(0, 0, maxWidth, pillDrawable.intrinsicHeight)
            pillDrawable.ellipsize = TextUtils.TruncateAt.END
        }

        pillDrawable.draw(canvas)
        canvas.restore()
    }

    internal fun updateAvatarDrawable(drawable: Drawable?) {
        pillDrawable.chipIcon = drawable
        tv?.get()?.invalidate()
    }

    // Private methods *****************************************************************************

    private fun createChipDrawable(): ChipDrawable {
        val textPadding = context.resources.getDimension(im.vector.lib.ui.styles.R.dimen.pill_text_padding)
        val icon = when {
            matrixItem is MatrixItem.RoomAliasItem && matrixItem.avatarUrl.isNullOrEmpty() &&
                    matrixItem.displayName == context.getString(CommonStrings.pill_message_in_room, matrixItem.id) -> {
                ContextCompat.getDrawable(context, R.drawable.ic_permalink_round)
            }
            matrixItem is MatrixItem.RoomItem && matrixItem.avatarUrl.isNullOrEmpty() && (
                    matrixItem.displayName == context.getString(CommonStrings.pill_message_in_unknown_room) ||
                            matrixItem.displayName == context.getString(CommonStrings.pill_message_unknown_room_or_space) ||
                            matrixItem.displayName == context.getString(CommonStrings.pill_message_from_unknown_user)
                    ) -> {
                ContextCompat.getDrawable(context, R.drawable.ic_permalink_round)
            }
            matrixItem is MatrixItem.UserItem && matrixItem.avatarUrl.isNullOrEmpty() && matrixItem.displayName?.isMatrixId().orTrue() -> {
                ContextCompat.getDrawable(context, R.drawable.ic_user_round)
            }
            else -> {
                try {
                    avatarRenderer.getCachedDrawable(glideRequests, matrixItem)
                } catch (exception: Exception) {
                    avatarRenderer.getPlaceholderDrawable(matrixItem)
                }
            }
        }

        return ChipDrawable.createFromResource(context, R.xml.pill_view).apply {
            text = matrixItem.getBestName()
            textEndPadding = textPadding
            textStartPadding = textPadding
            setChipMinHeightResource(im.vector.lib.ui.styles.R.dimen.pill_min_height)
            setChipIconSizeResource(im.vector.lib.ui.styles.R.dimen.pill_avatar_size)
            chipIcon = icon
            if (matrixItem is MatrixItem.EveryoneInRoomItem) {
                chipBackgroundColor = ColorStateList.valueOf(ThemeUtils.getColor(context, com.google.android.material.R.attr.colorError))
                // setTextColor API does not exist right now for ChipDrawable, use textAppearance
                setTextAppearanceResource(im.vector.lib.ui.styles.R.style.TextAppearance_Vector_Body_OnError)
            }
            setBounds(0, 0, intrinsicWidth, intrinsicHeight)
        }
    }
}

/**
 * Glide target to handle avatar retrieval into [PillImageSpan].
 */
private class PillImageSpanTarget(pillImageSpan: PillImageSpan) : SimpleTarget<Drawable>() {

    private val pillImageSpan = WeakReference(pillImageSpan)

    override fun onResourceReady(drawable: Drawable, transition: Transition<in Drawable>?) {
        updateWith(drawable)
    }

    override fun onLoadCleared(placeholder: Drawable?) {
        updateWith(placeholder)
    }

    private fun updateWith(drawable: Drawable?) {
        pillImageSpan.get()?.apply {
            updateAvatarDrawable(drawable)
        }
    }
}
