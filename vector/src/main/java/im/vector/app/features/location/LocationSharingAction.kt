/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.location

import im.vector.app.core.platform.VectorViewModelAction

sealed class LocationSharingAction : VectorViewModelAction {
    object CurrentUserLocationSharing : LocationSharingAction()
    data class PinnedLocationSharing(val locationData: LocationData?) : LocationSharingAction()
    data class LocationTargetChange(val locationData: LocationData) : LocationSharingAction()
    object ZoomToUserLocation : LocationSharingAction()
    object LiveLocationSharingRequested : LocationSharingAction()
    data class StartLiveLocationSharing(val durationMillis: Long) : LocationSharingAction()
    object ShowMapLoadingError : LocationSharingAction()
}
