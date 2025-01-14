/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.list

import im.vector.app.features.settings.devices.v2.DeviceFullInfo

data class SessionInfoViewState(
        val isCurrentSession: Boolean,
        val deviceFullInfo: DeviceFullInfo,
        val isVerifyButtonVisible: Boolean = true,
        val isDetailsButtonVisible: Boolean = true,
        val isLearnMoreLinkVisible: Boolean = false,
        val isLastActivityVisible: Boolean = false,
        val isShowingIpAddress: Boolean = false,
)
