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
