/*
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.othersessions

import im.vector.app.core.platform.VectorViewModelAction
import im.vector.app.features.settings.devices.v2.filter.DeviceManagerFilterType

sealed class OtherSessionsAction : VectorViewModelAction {
    data class FilterDevices(val filterType: DeviceManagerFilterType) : OtherSessionsAction()
}
