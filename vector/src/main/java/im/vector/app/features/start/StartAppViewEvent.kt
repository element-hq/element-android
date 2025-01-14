/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.start

import im.vector.app.core.platform.VectorViewEvents

sealed interface StartAppViewEvent : VectorViewEvents {
    /**
     * Will be sent if the process is taking more than 1 second.
     */
    object StartForegroundService : StartAppViewEvent

    /**
     * Will be sent when the current Session has been set.
     */
    object AppStarted : StartAppViewEvent
}
