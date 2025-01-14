/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.location

import android.content.Context
import android.content.res.TypedArray
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.use
import im.vector.app.core.glide.GlideApp
import im.vector.app.databinding.ViewMapLoadingErrorBinding
import im.vector.app.features.themes.ThemeUtils
import im.vector.lib.strings.CommonStrings

/**
 * Custom view to display an error when map fails to load.
 */
class MapLoadingErrorView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding = ViewMapLoadingErrorBinding.inflate(
            LayoutInflater.from(context),
            this
    )

    init {
        context.obtainStyledAttributes(
                attrs,
                im.vector.lib.ui.styles.R.styleable.MapLoadingErrorView,
                0,
                0
        ).use {
            setErrorDescription(it)
        }
    }

    private fun setErrorDescription(typedArray: TypedArray) {
        val description = typedArray.getString(im.vector.lib.ui.styles.R.styleable.MapLoadingErrorView_mapErrorDescription)
        if (description.isNullOrEmpty()) {
            binding.mapLoadingErrorDescription.setText(CommonStrings.location_share_loading_map_error)
        } else {
            binding.mapLoadingErrorDescription.text = description
        }
    }

    fun render(mapLoadingErrorViewState: MapLoadingErrorViewState) {
        GlideApp.with(binding.mapLoadingErrorBackground)
                .load(ColorDrawable(ThemeUtils.getColor(context, im.vector.lib.ui.styles.R.attr.vctr_system)))
                .transform(mapLoadingErrorViewState.backgroundTransformation)
                .into(binding.mapLoadingErrorBackground)
    }
}
