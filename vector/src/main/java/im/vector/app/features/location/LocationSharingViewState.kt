/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.location

import android.graphics.drawable.Drawable
import androidx.annotation.StringRes
import com.airbnb.mvrx.MavericksState
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.extensions.orTrue
import org.matrix.android.sdk.api.util.MatrixItem

enum class LocationSharingMode(@StringRes val titleRes: Int) {
    STATIC_SHARING(CommonStrings.location_activity_title_static_sharing),
    PREVIEW(CommonStrings.location_activity_title_preview)
}

data class LocationSharingViewState(
        val roomId: String,
        val mode: LocationSharingMode,
        val userItem: MatrixItem.UserItem? = null,
        val areTargetAndUserLocationEqual: Boolean? = null,
        val lastKnownUserLocation: LocationData? = null,
        val locationTargetDrawable: Drawable? = null,
        val canShareLiveLocation: Boolean = false,
        val loadingMapHasFailed: Boolean = false
) : MavericksState {

    constructor(locationSharingArgs: LocationSharingArgs) : this(
            roomId = locationSharingArgs.roomId,
            mode = locationSharingArgs.mode,
    )
}

fun LocationSharingViewState.toMapState() = MapState(
        zoomOnlyOnce = true,
        pinLocationData = lastKnownUserLocation,
        pinId = DEFAULT_PIN_ID,
        pinDrawable = null,
        // show the map pin only when target location and user location are not equal
        showPin = areTargetAndUserLocationEqual.orTrue().not()
)
