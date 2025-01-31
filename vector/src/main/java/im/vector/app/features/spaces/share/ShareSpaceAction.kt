/*
 * Copyright 2021-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.spaces.share

import im.vector.app.core.platform.VectorViewModelAction

sealed class ShareSpaceAction : VectorViewModelAction {
    object InviteByMxId : ShareSpaceAction()
    object InviteByLink : ShareSpaceAction()
}
