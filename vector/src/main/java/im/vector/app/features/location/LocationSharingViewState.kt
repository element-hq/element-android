/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.location

import android.graphics.drawable.Drawable
import androidx.annotation.StringRes
import com.airbnb.mvrx.MavericksState
import im.vector.app.R
import org.matrix.android.sdk.api.extensions.orTrue
import org.matrix.android.sdk.api.util.MatrixItem

enum class LocationSharingMode(@StringRes val titleRes: Int) {
    STATIC_SHARING(R.string.location_activity_title_static_sharing),
    PREVIEW(R.string.location_activity_title_preview)
}

data class LocationSharingViewState(
        val roomId: String,
        val mode: LocationSharingMode,
        val userItem: MatrixItem.UserItem? = null,
        val areTargetAndUserLocationEqual: Boolean? = null,
        val lastKnownUserLocation: LocationData? = null,
        val locationTargetDrawable: Drawable? = null
) : MavericksState {

    constructor(locationSharingArgs: LocationSharingArgs) : this(
            roomId = locationSharingArgs.roomId,
            mode = locationSharingArgs.mode,
    )
}

fun LocationSharingViewState.toMapState() = MapState(
        zoomOnlyOnce = true,
        userLocationData = lastKnownUserLocation,
        pinId = DEFAULT_PIN_ID,
        pinDrawable = null,
        // show the map pin only when target location and user location are not equal
        showPin = areTargetAndUserLocationEqual.orTrue().not()
)
