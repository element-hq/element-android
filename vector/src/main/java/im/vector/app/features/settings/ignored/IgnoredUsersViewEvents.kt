/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.ignored

import im.vector.app.core.platform.VectorViewEvents

/**
 * Transient events for Ignored users screen.
 */
sealed class IgnoredUsersViewEvents : VectorViewEvents {
    data class Loading(val message: CharSequence? = null) : IgnoredUsersViewEvents()
    data class Failure(val throwable: Throwable) : IgnoredUsersViewEvents()
    object Success : IgnoredUsersViewEvents()
}
