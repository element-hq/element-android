/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.list.actions

import im.vector.app.features.roomprofile.notifications.RoomNotificationSettingsViewState

data class RoomListQuickActionViewState(
        val roomListActionsArgs: RoomListActionsArgs,
        val notificationSettingsViewState: RoomNotificationSettingsViewState
)
