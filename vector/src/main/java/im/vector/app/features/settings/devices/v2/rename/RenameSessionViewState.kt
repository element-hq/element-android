/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.rename

import com.airbnb.mvrx.MavericksState

data class RenameSessionViewState(
        val deviceId: String,
        val editedDeviceName: String = "",
) : MavericksState {
    constructor(args: RenameSessionArgs) : this(
            deviceId = args.deviceId
    )
}
