/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.platform

import android.content.Context
import android.util.AttributeSet
import androidx.core.content.withStyledAttributes
import androidx.core.widget.NestedScrollView

private const val DEFAULT_MAX_HEIGHT = 200

class MaxHeightScrollView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) :
        NestedScrollView(context, attrs, defStyle) {

    var maxHeight: Int = 0
        set(value) {
            field = value
            requestLayout()
        }

    init {
        if (attrs != null) {
            context.withStyledAttributes(attrs, im.vector.lib.ui.styles.R.styleable.MaxHeightScrollView) {
                maxHeight = getDimensionPixelSize(im.vector.lib.ui.styles.R.styleable.MaxHeightScrollView_maxHeight, DEFAULT_MAX_HEIGHT)
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val newHeightMeasureSpec = MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST)
        super.onMeasure(widthMeasureSpec, newHeightMeasureSpec)
    }
}
