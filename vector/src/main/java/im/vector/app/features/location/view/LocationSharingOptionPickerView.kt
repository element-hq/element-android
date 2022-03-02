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
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import im.vector.app.databinding.ViewLocationSharingOptionPickerBinding
import im.vector.app.features.location.LocationSharingOption

/**
 * Custom view to display the location sharing option picker.
 */
class LocationSharingOptionPickerView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding = ViewLocationSharingOptionPickerBinding.inflate(
            LayoutInflater.from(context),
            this
    )

    fun setOptions(vararg options: LocationSharingOption) {
        val optionsNumber = options.toSet().size
        val isPinnedVisible = options.contains(LocationSharingOption.PINNED)
        val isUserCurrentVisible = options.contains(LocationSharingOption.USER_CURRENT)
        val isUserLiveVisible = options.contains(LocationSharingOption.USER_LIVE)

        binding.locationSharingOptionPinned.isVisible = isPinnedVisible
        binding.locationSharingOptionsDivider1.isVisible = isPinnedVisible && optionsNumber > 1
        binding.locationSharingOptionUserCurrentLocation.isVisible = isUserCurrentVisible
        binding.locationSharingOptionsDivider2.isVisible = isUserCurrentVisible && isUserLiveVisible
        binding.locationSharingOptionUserLiveLocation.isVisible = isUserLiveVisible
    }
}
