/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fixtures

import org.matrix.android.sdk.api.session.homeserver.HomeServerCapabilities
import org.matrix.android.sdk.api.session.homeserver.RoomVersionCapabilities

fun aHomeServerCapabilities(
        canChangePassword: Boolean = true,
        canChangeDisplayName: Boolean = true,
        canChangeAvatar: Boolean = true,
        canChange3pid: Boolean = true,
        maxUploadFileSize: Long = 100L,
        lastVersionIdentityServerSupported: Boolean = false,
        defaultIdentityServerUrl: String? = null,
        roomVersions: RoomVersionCapabilities? = null,
        canRemotelyTogglePushNotificationsOfDevices: Boolean = true,
        externalAccountManagementUrl: String? = null,
) = HomeServerCapabilities(
        canChangePassword = canChangePassword,
        canChangeDisplayName = canChangeDisplayName,
        canChangeAvatar = canChangeAvatar,
        canChange3pid = canChange3pid,
        maxUploadFileSize = maxUploadFileSize,
        lastVersionIdentityServerSupported = lastVersionIdentityServerSupported,
        defaultIdentityServerUrl = defaultIdentityServerUrl,
        roomVersions = roomVersions,
        canRemotelyTogglePushNotificationsOfDevices = canRemotelyTogglePushNotificationsOfDevices,
        externalAccountManagementUrl = externalAccountManagementUrl,
)
