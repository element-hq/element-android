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
import androidx.core.view.updateLayoutParams
import im.vector.app.R
import im.vector.app.databinding.ViewLocationLiveEndedBannerBinding

class LocationLiveEndedBannerView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding = ViewLocationLiveEndedBannerBinding.inflate(
            LayoutInflater.from(context),
            this
    )

    init {
        context.theme.obtainStyledAttributes(
                attrs,
                R.styleable.LocationLiveEndedBannerView,
                0,
                0
        ).run {
            try {
                setBackgroundAlpha(this)
                setIconMarginStart(this)
            } finally {
                recycle()
            }
        }
    }

    private fun setBackgroundAlpha(typedArray: TypedArray) {
        val withAlpha = typedArray.getBoolean(R.styleable.LocationLiveEndedBannerView_locLiveEndedBkgWithAlpha, false)
        binding.locationLiveEndedBannerBackground.alpha = if (withAlpha) 0.75f else 1f
    }

    private fun setIconMarginStart(typedArray: TypedArray) {
        val margin = typedArray.getDimensionPixelOffset(R.styleable.LocationLiveEndedBannerView_locLiveEndedIconMarginStart, 0)
        binding.locationLiveEndedBannerIcon.updateLayoutParams<MarginLayoutParams> {
            marginStart = margin
        }
    }
}
