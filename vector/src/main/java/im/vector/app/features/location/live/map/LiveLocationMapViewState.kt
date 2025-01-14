/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.location.live.map

import android.graphics.drawable.Drawable
import com.airbnb.mvrx.MavericksState
import im.vector.app.features.location.LocationData
import org.matrix.android.sdk.api.util.MatrixItem

data class LiveLocationMapViewState(
        val roomId: String,
        val userLocations: List<UserLiveLocationViewState> = emptyList(),
        /**
         * Map to keep track of symbol ids associated to each user Id.
         */
        val mapSymbolIds: Map<String, Long> = emptyMap(),
        val loadingMapHasFailed: Boolean = false,
        val showLocateUserButton: Boolean = false,
        val isLoadingUserLocation: Boolean = false,
        val lastKnownUserLocation: LocationData? = null,
) : MavericksState {
    constructor(liveLocationMapViewArgs: LiveLocationMapViewArgs) : this(
            roomId = liveLocationMapViewArgs.roomId
    )
}

data class UserLiveLocationViewState(
        val matrixItem: MatrixItem,
        val pinDrawable: Drawable,
        val locationData: LocationData,
        val endOfLiveTimestampMillis: Long?,
        val locationTimestampMillis: Long?,
        val showStopSharingButton: Boolean
)
