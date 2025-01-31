/*
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.list

import im.vector.app.features.settings.devices.v2.DeviceFullInfo

data class SessionInfoViewState(
        val isCurrentSession: Boolean,
        val deviceFullInfo: DeviceFullInfo,
        val isDetailsButtonVisible: Boolean = true,
        val isLearnMoreLinkVisible: Boolean = false,
        val isLastSeenDetailsVisible: Boolean = false,
)
