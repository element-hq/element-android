/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.auth

import im.vector.app.core.platform.VectorViewEvents

sealed class ReAuthEvents : VectorViewEvents {
    data class OpenSsoURl(val url: String) : ReAuthEvents()
    object Dismiss : ReAuthEvents()
    data class PasswordFinishSuccess(val passwordSafeForIntent: String) : ReAuthEvents()
}
