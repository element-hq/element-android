/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile

import im.vector.app.core.platform.VectorViewModelAction
import org.matrix.android.sdk.api.session.room.notification.RoomNotificationState

sealed class RoomProfileAction : VectorViewModelAction {
    object EnableEncryption : RoomProfileAction()
    object LeaveRoom : RoomProfileAction()
    data class ChangeRoomNotificationState(val notificationState: RoomNotificationState) : RoomProfileAction()
    object ShareRoomProfile : RoomProfileAction()
    object CreateShortcut : RoomProfileAction()
    object RestoreEncryptionState : RoomProfileAction()
    data class SetEncryptToVerifiedDeviceOnly(val enabled: Boolean) : RoomProfileAction()
    data class ReportRoom(val reason: String) : RoomProfileAction()
}
