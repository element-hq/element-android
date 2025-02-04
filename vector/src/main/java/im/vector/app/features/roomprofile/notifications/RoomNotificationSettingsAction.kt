/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.notifications

import im.vector.app.core.platform.VectorViewModelAction
import org.matrix.android.sdk.api.session.room.notification.RoomNotificationState

sealed class RoomNotificationSettingsAction : VectorViewModelAction {
    data class SelectNotificationState(val notificationState: RoomNotificationState) : RoomNotificationSettingsAction()
}
