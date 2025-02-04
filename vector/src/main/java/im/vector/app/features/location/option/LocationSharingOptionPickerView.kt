/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.location.option

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import im.vector.app.databinding.ViewLocationSharingOptionPickerBinding

/**
 * Custom view to display the location sharing option picker.
 */
class LocationSharingOptionPickerView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    val optionPinned: LocationSharingOptionView
        get() = binding.locationSharingOptionPinned

    val optionUserCurrent: LocationSharingOptionView
        get() = binding.locationSharingOptionUserCurrent

    val optionUserLive: LocationSharingOptionView
        get() = binding.locationSharingOptionUserLive

    private val divider1: View
        get() = binding.locationSharingOptionsDivider1

    private val divider2: View
        get() = binding.locationSharingOptionsDivider2

    private val binding = ViewLocationSharingOptionPickerBinding.inflate(
            LayoutInflater.from(context),
            this
    )

    init {
        applyBackground()
    }

    fun render(options: Set<LocationSharingOption> = emptySet()) {
        val optionsNumber = options.toSet().size
        val isPinnedVisible = options.contains(LocationSharingOption.PINNED)
        val isUserCurrentVisible = options.contains(LocationSharingOption.USER_CURRENT)
        val isUserLiveVisible = options.contains(LocationSharingOption.USER_LIVE)

        optionPinned.isVisible = isPinnedVisible
        divider1.isVisible = isPinnedVisible && optionsNumber > 1
        optionUserCurrent.isVisible = isUserCurrentVisible
        divider2.isVisible = isUserCurrentVisible && isUserLiveVisible
        optionUserLive.isVisible = isUserLiveVisible
    }

    private fun applyBackground() {
        val outValue = TypedValue()
        context.theme.resolveAttribute(
                com.google.android.material.R.attr.colorSurface,
                outValue,
                true
        )
        binding.root.background = ContextCompat.getDrawable(
                context,
                outValue.resourceId
        )
    }
}
