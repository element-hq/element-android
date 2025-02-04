/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.account.deactivation

import im.vector.app.core.platform.VectorViewModelAction

sealed class DeactivateAccountAction : VectorViewModelAction {
    data class DeactivateAccount(val eraseAllData: Boolean) : DeactivateAccountAction()

    object SsoAuthDone : DeactivateAccountAction()
    data class PasswordAuthDone(val password: String) : DeactivateAccountAction()
    object ReAuthCancelled : DeactivateAccountAction()
}
