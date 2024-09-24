/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package im.vector.app.features.room

import im.vector.app.core.platform.VectorViewModelAction

sealed class RequireActiveMembershipAction : VectorViewModelAction {
    data class ChangeRoom(val roomId: String) : RequireActiveMembershipAction()
}
