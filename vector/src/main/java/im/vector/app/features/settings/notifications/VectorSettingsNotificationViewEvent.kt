/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.notifications

import im.vector.app.core.platform.VectorViewEvents

sealed interface VectorSettingsNotificationViewEvent : VectorViewEvents {
    object NotificationsForDeviceEnabled : VectorSettingsNotificationViewEvent
    object NotificationsForDeviceDisabled : VectorSettingsNotificationViewEvent
    object AskUserForPushDistributor : VectorSettingsNotificationViewEvent
    object NotificationMethodChanged : VectorSettingsNotificationViewEvent
}
