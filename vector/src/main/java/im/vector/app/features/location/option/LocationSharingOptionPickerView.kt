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

package im.vector.app.features.location.option

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import im.vector.app.R
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
                R.attr.colorSurface,
                outValue,
                true
        )
        binding.root.background = ContextCompat.getDrawable(
                context,
                outValue.resourceId
        )
    }
}
