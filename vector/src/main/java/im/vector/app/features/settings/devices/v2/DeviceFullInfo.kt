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

package im.vector.app.features.settings.devices.v2

import im.vector.app.core.session.clientinfo.MatrixClientInfoContent
import im.vector.app.features.settings.devices.v2.details.extended.DeviceExtendedInfo
import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.api.session.crypto.model.DeviceInfo
import org.matrix.android.sdk.api.session.crypto.model.RoomEncryptionTrustLevel

data class DeviceFullInfo(
        val deviceInfo: DeviceInfo,
        val cryptoDeviceInfo: CryptoDeviceInfo?,
        val roomEncryptionTrustLevel: RoomEncryptionTrustLevel?,
        val isInactive: Boolean,
        val isCurrentDevice: Boolean,
        val deviceExtendedInfo: DeviceExtendedInfo,
        val matrixClientInfo: MatrixClientInfoContent?,
        val isSelected: Boolean = false,
)
