/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.call

import im.vector.app.core.platform.VectorViewEvents
import im.vector.app.features.call.audio.CallAudioManager
import org.matrix.android.sdk.api.session.call.TurnServerResponse

sealed class VectorCallViewEvents : VectorViewEvents {

    data class ConnectionTimeout(val turn: TurnServerResponse?) : VectorCallViewEvents()
    data class ShowSoundDeviceChooser(
            val available: Set<CallAudioManager.Device>,
            val current: CallAudioManager.Device
    ) : VectorCallViewEvents()

    object ShowDialPad : VectorCallViewEvents()
    object ShowCallTransferScreen : VectorCallViewEvents()
    object FailToTransfer : VectorCallViewEvents()
    object ShowScreenSharingPermissionDialog : VectorCallViewEvents()
    object StopScreenSharingService : VectorCallViewEvents()
}
