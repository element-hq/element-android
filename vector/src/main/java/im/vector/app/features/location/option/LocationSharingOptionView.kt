/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.location.option

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import androidx.core.view.setPadding
import im.vector.app.core.extensions.tintBackground
import im.vector.app.databinding.ViewLocationSharingOptionBinding

/**
 * Custom view to display a location sharing option.
 */
class LocationSharingOptionView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    val iconView: ImageView
        get() = binding.shareLocationOptionIcon

    private val binding = ViewLocationSharingOptionBinding.inflate(
            LayoutInflater.from(context),
            this
    )

    init {
        context.obtainStyledAttributes(
                attrs,
                im.vector.lib.ui.styles.R.styleable.LocationSharingOptionView,
                0,
                0
        ).use {
            setIcon(it)
            setTitle(it)
        }
    }

    fun setIconBackgroundTint(@ColorInt color: Int) {
        binding.shareLocationOptionIcon.tintBackground(color)
    }

    private fun setIcon(typedArray: TypedArray) {
        val icon = typedArray.getDrawable(im.vector.lib.ui.styles.R.styleable.LocationSharingOptionView_locShareIcon)
        val background = typedArray.getDrawable(im.vector.lib.ui.styles.R.styleable.LocationSharingOptionView_locShareIconBackground)
        val backgroundTint = typedArray.getColor(
                im.vector.lib.ui.styles.R.styleable.LocationSharingOptionView_locShareIconBackgroundTint,
                ContextCompat.getColor(context, android.R.color.transparent)
        )
        val padding = typedArray.getDimensionPixelOffset(
                im.vector.lib.ui.styles.R.styleable.LocationSharingOptionView_locShareIconPadding,
                context.resources.getDimensionPixelOffset(im.vector.lib.ui.styles.R.dimen.location_sharing_option_default_padding)
        )
        val description = typedArray.getString(im.vector.lib.ui.styles.R.styleable.LocationSharingOptionView_locShareIconDescription)

        iconView.setImageDrawable(icon)
        iconView.background = background
        iconView.tintBackground(backgroundTint)
        iconView.setPadding(padding)
        iconView.contentDescription = description
    }

    private fun setTitle(typedArray: TypedArray) {
        val title = typedArray.getString(im.vector.lib.ui.styles.R.styleable.LocationSharingOptionView_locShareTitle)
        binding.shareLocationOptionTitle.text = title
    }
}
