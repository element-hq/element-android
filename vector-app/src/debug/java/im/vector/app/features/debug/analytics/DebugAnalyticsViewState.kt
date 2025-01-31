/*
 * Copyright 2021-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.debug.analytics

import com.airbnb.mvrx.MavericksState

data class DebugAnalyticsViewState(
        val analyticsId: String? = null,
        val userConsent: Boolean = false,
        val didAskUserConsent: Boolean = false
) : MavericksState
