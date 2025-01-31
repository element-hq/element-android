/*
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.debug.leak

import com.airbnb.mvrx.MavericksState

data class DebugMemoryLeaksViewState(
        val isMemoryLeaksAnalysisEnabled: Boolean = false
) : MavericksState
