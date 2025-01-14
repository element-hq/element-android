/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roommemberprofile.devices

import im.vector.app.core.platform.VectorViewModelAction
import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo

sealed class DeviceListAction : VectorViewModelAction {
    data class SelectDevice(val device: CryptoDeviceInfo) : DeviceListAction()
    object DeselectDevice : DeviceListAction()
}
