/*
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.details

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.Uninitialized
import org.matrix.android.sdk.api.session.crypto.model.DeviceInfo

data class SessionDetailsViewState(
        val deviceId: String,
        val deviceInfo: Async<DeviceInfo> = Uninitialized,
) : MavericksState {
    constructor(args: SessionDetailsArgs) : this(
            deviceId = args.deviceId
    )
}
