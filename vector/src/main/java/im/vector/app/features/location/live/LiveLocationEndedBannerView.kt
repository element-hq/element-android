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
