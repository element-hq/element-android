/*
 * Copyright 2021-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.location.preview

import com.airbnb.mvrx.MavericksState

data class LocationPreviewViewState(
        val loadingMapHasFailed: Boolean = false
) : MavericksState
