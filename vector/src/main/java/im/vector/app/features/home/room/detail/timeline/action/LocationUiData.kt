/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.timeline.action

import im.vector.app.features.home.room.detail.timeline.helper.LocationPinProvider

/**
 * Data used to display Location data in the message bottom sheet.
 */
data class LocationUiData(
        val locationUrl: String,
        val locationOwnerId: String?,
        val locationPinProvider: LocationPinProvider,
)
