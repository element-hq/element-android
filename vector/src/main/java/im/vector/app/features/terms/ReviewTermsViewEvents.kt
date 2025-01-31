/*
 * Copyright 2020-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.terms

import im.vector.app.core.platform.VectorViewEvents

sealed class ReviewTermsViewEvents : VectorViewEvents {
    data class Loading(val message: CharSequence? = null) : ReviewTermsViewEvents()
    data class Failure(val throwable: Throwable, val finish: Boolean) : ReviewTermsViewEvents()
    object Success : ReviewTermsViewEvents()
}
