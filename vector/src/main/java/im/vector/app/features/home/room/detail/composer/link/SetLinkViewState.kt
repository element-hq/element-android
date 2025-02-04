/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.composer.link

import com.airbnb.mvrx.MavericksState

data class SetLinkViewState(
        val isTextSupported: Boolean,
        val initialLink: String?,
        val saveEnabled: Boolean,
) : MavericksState {

    constructor(args: SetLinkFragment.Args) : this(
            isTextSupported = args.isTextSupported,
            initialLink = args.initialLink,
            saveEnabled = false,
    )

    val removeVisible = initialLink != null
}
