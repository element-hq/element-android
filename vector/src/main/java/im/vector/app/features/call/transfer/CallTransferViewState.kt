/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.call.transfer

import com.airbnb.mvrx.MavericksState

data class CallTransferViewState(
        val callId: String
) : MavericksState {

    constructor(args: CallTransferArgs) : this(callId = args.callId)
}
