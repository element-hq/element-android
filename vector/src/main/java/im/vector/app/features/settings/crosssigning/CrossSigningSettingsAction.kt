/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.crosssigning

import im.vector.app.core.platform.VectorViewModelAction

sealed class CrossSigningSettingsAction : VectorViewModelAction {
    object InitializeCrossSigning : CrossSigningSettingsAction()
    object SsoAuthDone : CrossSigningSettingsAction()
    data class PasswordAuthDone(val password: String) : CrossSigningSettingsAction()
    object ReAuthCancelled : CrossSigningSettingsAction()
}
