/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.spaces.people

import im.vector.app.core.platform.VectorViewModelAction
import org.matrix.android.sdk.api.session.room.model.RoomMemberSummary

sealed class SpacePeopleViewAction : VectorViewModelAction {
    data class ChatWith(val member: RoomMemberSummary) : SpacePeopleViewAction()
    object InviteToSpace : SpacePeopleViewAction()
}
