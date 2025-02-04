/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.list.home.invites

import im.vector.app.core.platform.VectorViewEvents
import org.matrix.android.sdk.api.session.room.model.RoomSummary

sealed class InvitesViewEvents : VectorViewEvents {
    data class Failure(val throwable: Throwable) : InvitesViewEvents()
    data class OpenRoom(
            val roomSummary: RoomSummary,
            val shouldCloseInviteView: Boolean,
            val isInviteAlreadySelected: Boolean,
    ) : InvitesViewEvents()
}
