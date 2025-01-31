/*
 * Copyright 2022-2025 New Vector Ltd.
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
        roomVersions: RoomVersionCapabilities? = null
) = HomeServerCapabilities(
        canChangePassword,
        canChangeDisplayName,
        canChangeAvatar,
        canChange3pid,
        maxUploadFileSize,
        lastVersionIdentityServerSupported,
        defaultIdentityServerUrl,
        roomVersions
)
