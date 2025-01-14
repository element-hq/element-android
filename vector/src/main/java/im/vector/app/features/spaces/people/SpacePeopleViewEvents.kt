/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.spaces.people

import im.vector.app.core.platform.VectorViewEvents

sealed class SpacePeopleViewEvents : VectorViewEvents {
    data class OpenRoom(val roomId: String) : SpacePeopleViewEvents()
    data class InviteToSpace(val spaceId: String) : SpacePeopleViewEvents()
}
