/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.location

import im.vector.app.core.platform.VectorViewEvents

sealed class LocationSharingViewEvents : VectorViewEvents {
    object Close : LocationSharingViewEvents()
    object LocationNotAvailableError : LocationSharingViewEvents()
    data class ZoomToUserLocation(val userLocation: LocationData) : LocationSharingViewEvents()
    data class StartLiveLocationService(val sessionId: String, val roomId: String, val durationMillis: Long) : LocationSharingViewEvents()
    object ChooseLiveLocationDuration : LocationSharingViewEvents()
    object ShowLabsFlagPromotion : LocationSharingViewEvents()
    object LiveLocationSharingNotEnoughPermission : LocationSharingViewEvents()
}
