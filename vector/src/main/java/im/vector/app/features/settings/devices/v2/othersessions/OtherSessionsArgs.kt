/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.othersessions

import android.os.Parcelable
import im.vector.app.features.settings.devices.v2.filter.DeviceManagerFilterType
import kotlinx.parcelize.Parcelize

@Parcelize
data class OtherSessionsArgs(
        val defaultFilter: DeviceManagerFilterType,
        val excludeCurrentDevice: Boolean,
) : Parcelable
