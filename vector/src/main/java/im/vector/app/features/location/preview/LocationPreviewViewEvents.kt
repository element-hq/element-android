/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.location.preview

import im.vector.app.core.platform.VectorViewEvents
import im.vector.app.features.location.LocationData

sealed class LocationPreviewViewEvents : VectorViewEvents {
    data class ZoomToUserLocation(val userLocation: LocationData) : LocationPreviewViewEvents()
    object UserLocationNotAvailableError : LocationPreviewViewEvents()
}
