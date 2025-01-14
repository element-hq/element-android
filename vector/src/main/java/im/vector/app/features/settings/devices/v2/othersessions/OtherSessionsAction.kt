/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.othersessions

import im.vector.app.core.platform.VectorViewModelAction
import im.vector.app.features.settings.devices.v2.filter.DeviceManagerFilterType

sealed class OtherSessionsAction : VectorViewModelAction {
    // ReAuth
    object SsoAuthDone : OtherSessionsAction()
    data class PasswordAuthDone(val password: String) : OtherSessionsAction()
    object ReAuthCancelled : OtherSessionsAction()

    // Others
    data class FilterDevices(val filterType: DeviceManagerFilterType) : OtherSessionsAction()
    data class EnableSelectMode(val deviceId: String?) : OtherSessionsAction()
    object DisableSelectMode : OtherSessionsAction()
    data class ToggleSelectionForDevice(val deviceId: String) : OtherSessionsAction()
    object SelectAll : OtherSessionsAction()
    object DeselectAll : OtherSessionsAction()
    object MultiSignout : OtherSessionsAction()
    object ToggleIpAddressVisibility : OtherSessionsAction()
}
