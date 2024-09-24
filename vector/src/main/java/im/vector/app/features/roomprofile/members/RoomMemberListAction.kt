/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package im.vector.app.features.roomprofile.members

import im.vector.app.core.platform.VectorViewModelAction

sealed class RoomMemberListAction : VectorViewModelAction {
    data class RevokeThreePidInvite(val stateKey: String) : RoomMemberListAction()
    data class FilterMemberList(val searchTerm: String) : RoomMemberListAction()
}
