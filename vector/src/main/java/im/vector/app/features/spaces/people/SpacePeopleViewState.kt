/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.spaces.people

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.Uninitialized
import im.vector.app.core.platform.GenericIdArgs

data class SpacePeopleViewState(
        val spaceId: String,
        val createAndInviteState: Async<String> = Uninitialized
) : MavericksState {
    constructor(args: GenericIdArgs) : this(
            spaceId = args.id
    )
}
