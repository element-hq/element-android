/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.devtools

import im.vector.app.core.platform.VectorViewEvents

sealed class DevToolsViewEvents : VectorViewEvents {
    object Dismiss : DevToolsViewEvents()

    //    object ShowStateList : DevToolsViewEvents()
    data class ShowAlertMessage(val message: String) : DevToolsViewEvents()
    data class ShowSnackMessage(val message: String) : DevToolsViewEvents()
}
