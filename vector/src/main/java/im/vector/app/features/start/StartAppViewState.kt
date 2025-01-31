/*
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.start

import com.airbnb.mvrx.MavericksState

data class StartAppViewState(
        val mayBeLongToProcess: Boolean = false
) : MavericksState
