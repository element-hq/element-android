/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.platform

import com.airbnb.mvrx.MavericksState

data class VectorDummyViewState(
        val isDummy: Unit = Unit
) : MavericksState
