/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.composer.link

import im.vector.app.core.platform.VectorViewEvents

sealed class SetLinkViewEvents : VectorViewEvents {

    data class SavedLink(
            val link: String,
    ) : SetLinkViewEvents()

    data class SavedLinkAndText(
            val link: String,
            val text: String,
    ) : SetLinkViewEvents()
}
