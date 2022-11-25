/*
 * Copyright (c) 2022 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
