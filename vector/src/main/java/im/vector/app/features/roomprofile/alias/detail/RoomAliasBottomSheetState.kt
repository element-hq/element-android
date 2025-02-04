/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.alias.detail

import com.airbnb.mvrx.MavericksState

data class RoomAliasBottomSheetState(
        val alias: String,
        val matrixToLink: String? = null,
        val isPublished: Boolean,
        val isMainAlias: Boolean,
        val isLocal: Boolean,
        val canEditCanonicalAlias: Boolean
) : MavericksState {

    constructor(args: RoomAliasBottomSheetArgs) : this(
            alias = args.alias,
            isPublished = args.isPublished,
            isMainAlias = args.isMainAlias,
            isLocal = args.isLocal,
            canEditCanonicalAlias = args.canEditCanonicalAlias
    )
}
