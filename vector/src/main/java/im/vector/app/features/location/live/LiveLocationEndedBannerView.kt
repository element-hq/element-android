/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.location.live

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.use
import androidx.core.view.updateLayoutParams
import im.vector.app.databinding.ViewLiveLocationEndedBannerBinding

private const val BACKGROUND_ALPHA = 0.75f

class LiveLocationEndedBannerView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding = ViewLiveLocationEndedBannerBinding.inflate(
            LayoutInflater.from(context),
            this
    )

    init {
        context.obtainStyledAttributes(
                attrs,
                im.vector.lib.ui.styles.R.styleable.LiveLocationEndedBannerView,
                0,
                0
        ).use {
            setBackgroundAlpha(it)
            setIconMarginStart(it)
        }
    }

    private fun setBackgroundAlpha(typedArray: TypedArray) {
        val withAlpha = typedArray.getBoolean(im.vector.lib.ui.styles.R.styleable.LiveLocationEndedBannerView_locLiveEndedBkgWithAlpha, false)
        binding.liveLocationEndedBannerBackground.alpha = if (withAlpha) BACKGROUND_ALPHA else 1f
    }

    private fun setIconMarginStart(typedArray: TypedArray) {
        val margin = typedArray.getDimensionPixelOffset(im.vector.lib.ui.styles.R.styleable.LiveLocationEndedBannerView_locLiveEndedIconMarginStart, 0)
        binding.liveLocationEndedBannerIcon.updateLayoutParams<MarginLayoutParams> {
            marginStart = margin
        }
    }
}
