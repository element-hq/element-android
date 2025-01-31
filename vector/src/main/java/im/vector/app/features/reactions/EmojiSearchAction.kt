/*
 * Copyright 2019-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.reactions

import im.vector.app.core.platform.VectorViewModelAction

sealed class EmojiSearchAction : VectorViewModelAction {
    data class UpdateQuery(val queryString: String) : EmojiSearchAction()
}
