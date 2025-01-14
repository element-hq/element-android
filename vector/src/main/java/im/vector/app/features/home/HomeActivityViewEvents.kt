/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home

import im.vector.app.core.platform.VectorViewEvents
import org.matrix.android.sdk.api.util.MatrixItem

sealed interface HomeActivityViewEvents : VectorViewEvents {
    data class AskPasswordToInitCrossSigning(val userItem: MatrixItem.UserItem) : HomeActivityViewEvents
    data class CurrentSessionNotVerified(
            val userItem: MatrixItem.UserItem,
            val afterMigration: Boolean
    ) : HomeActivityViewEvents

    data class CurrentSessionCannotBeVerified(
            val userItem: MatrixItem.UserItem,
    ) : HomeActivityViewEvents

    data class OnCrossSignedInvalidated(val userItem: MatrixItem.UserItem) : HomeActivityViewEvents
    object PromptToEnableSessionPush : HomeActivityViewEvents
    object ShowAnalyticsOptIn : HomeActivityViewEvents
    object ShowNotificationDialog : HomeActivityViewEvents
    object ShowReleaseNotes : HomeActivityViewEvents
    object NotifyUserForThreadsMigration : HomeActivityViewEvents
    data class MigrateThreads(val checkSession: Boolean) : HomeActivityViewEvents
    object StartRecoverySetupFlow : HomeActivityViewEvents
    data class ForceVerification(val sendRequest: Boolean) : HomeActivityViewEvents
    object AskUserForPushDistributor : HomeActivityViewEvents
}
