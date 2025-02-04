/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.debug.leak

import im.vector.app.core.platform.VectorViewModelAction

sealed interface DebugMemoryLeaksViewActions : VectorViewModelAction {
    data class EnableMemoryLeaksAnalysis(val isEnabled: Boolean) : DebugMemoryLeaksViewActions
}
