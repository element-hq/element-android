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

package im.vector.app.features.location.view

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.setPadding
import im.vector.app.R
import im.vector.app.databinding.ViewLocationSharingOptionBinding

/**
 * Custom view to display a location sharing option.
 */
class LocationSharingOptionView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding = ViewLocationSharingOptionBinding.inflate(
            LayoutInflater.from(context),
            this
    )

    init {
        applyRipple()
        context.theme.obtainStyledAttributes(
                attrs,
                R.styleable.LocationSharingOptionView,
                0,
                0
        ).run {
            try {
                setIcon(this)
                setTitle(this)
            } finally {
                recycle()
            }
        }
    }

    private fun applyRipple() {
        val outValue = TypedValue()
        context.theme.resolveAttribute(
                android.R.attr.selectableItemBackground,
                outValue,
                true
        )
        binding.root.background = ContextCompat.getDrawable(
                context,
                outValue.resourceId
        )
    }

    private fun setIcon(typedArray: TypedArray) {
        val icon = typedArray.getDrawable(R.styleable.LocationSharingOptionView_icon)
        val background = typedArray.getDrawable(R.styleable.LocationSharingOptionView_iconBackground)
        val backgroundTint = typedArray.getColor(
                R.styleable.LocationSharingOptionView_iconBackgroundTint,
                ContextCompat.getColor(context, android.R.color.transparent)
        )
        val padding = typedArray.getDimensionPixelOffset(
                R.styleable.LocationSharingOptionView_iconPadding,
                context.resources.getDimensionPixelOffset(R.dimen.location_sharing_option_default_padding)
        )
        val description = typedArray.getString(R.styleable.LocationSharingOptionView_iconDescription)

        binding.shareLocationOptionIcon.setImageDrawable(icon)
        val bkg = background?.let {
            val backgroundDrawable = DrawableCompat.wrap(it)
            DrawableCompat.setTint(backgroundDrawable, backgroundTint)
            backgroundDrawable
        } ?: background
        binding.shareLocationOptionIcon.background = bkg
        binding.shareLocationOptionIcon.setPadding(padding)
        binding.shareLocationOptionIcon.contentDescription = description
    }

    private fun setTitle(typedArray: TypedArray) {
        val title = typedArray.getString(R.styleable.LocationSharingOptionView_title)
        binding.shareLocationOptionTitle.text = title
    }
}
