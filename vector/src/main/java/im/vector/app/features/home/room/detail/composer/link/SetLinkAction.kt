/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.composer.link

import im.vector.app.core.platform.VectorViewModelAction

sealed class SetLinkAction : VectorViewModelAction {
    data class LinkChanged(
            val newLink: String
    ) : SetLinkAction()

    data class Save(
            val link: String,
            val text: String,
    ) : SetLinkAction()
}
