/*
 * Copyright 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.elementx

import im.vector.app.core.platform.VectorViewModelAction

sealed interface MigrationBannerAction : VectorViewModelAction {
    data object Close : MigrationBannerAction
    data object OnResume : MigrationBannerAction
}
