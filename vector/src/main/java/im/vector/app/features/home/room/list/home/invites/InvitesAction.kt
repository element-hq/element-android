/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.list.home.invites

import im.vector.app.core.platform.VectorViewModelAction
import org.matrix.android.sdk.api.session.room.model.RoomSummary

sealed class InvitesAction : VectorViewModelAction {
    data class SelectRoom(val roomSummary: RoomSummary) : InvitesAction()
    data class AcceptInvitation(val roomSummary: RoomSummary) : InvitesAction()
    data class RejectInvitation(val roomSummary: RoomSummary) : InvitesAction()
}
