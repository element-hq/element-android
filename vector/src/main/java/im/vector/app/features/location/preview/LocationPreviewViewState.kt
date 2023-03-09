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

package im.vector.app.features.location.preview

import android.graphics.drawable.Drawable
import com.airbnb.mvrx.MavericksState
import im.vector.app.features.location.LocationData
import im.vector.app.features.location.LocationSharingArgs

data class LocationPreviewViewState(
        val pinLocationData: LocationData? = null,
        val pinUserId: String? = null,
        val pinDrawable: Drawable? = null,
        val loadingMapHasFailed: Boolean = false,
        val isLoadingUserLocation: Boolean = false,
        val lastKnownUserLocation: LocationData? = null,
) : MavericksState {

    constructor(args: LocationSharingArgs) : this(
            pinLocationData = args.initialLocationData,
            pinUserId = args.locationOwnerId,
    )
}
