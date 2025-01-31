/*
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.list.home.release

import im.vector.app.core.platform.VectorViewEvents

sealed class ReleaseNotesViewEvents : VectorViewEvents {
    object Close : ReleaseNotesViewEvents()
    data class SelectPage(val index: Int) : ReleaseNotesViewEvents()
}
