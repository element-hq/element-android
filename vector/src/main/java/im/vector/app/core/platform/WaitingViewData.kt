/*
 * Copyright 2019-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.platform

/**
 * Model to display a Waiting View.
 */
data class WaitingViewData(
        val message: String,
        val progress: Int? = null,
        val progressTotal: Int? = null,
        val isIndeterminate: Boolean = false
)
