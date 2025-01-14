/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.verification

/**
 * Used to hold some info about the cross signing of the current Session.
 */
data class CurrentSessionCrossSigningInfo(
        val deviceId: String = "",
        val isCrossSigningInitialized: Boolean = false,
        val isCrossSigningVerified: Boolean = false,
)
