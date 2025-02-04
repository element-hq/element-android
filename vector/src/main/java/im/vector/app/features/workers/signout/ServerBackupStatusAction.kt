/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.workers.signout

import im.vector.app.core.platform.VectorViewModelAction

sealed interface ServerBackupStatusAction : VectorViewModelAction {
    data class OnRecoverDoneForVersion(val version: String) : ServerBackupStatusAction
    object OnBannerDisplayed : ServerBackupStatusAction
    object OnBannerClosed : ServerBackupStatusAction
}
