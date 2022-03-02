/*
 * Copyright (c) 2022 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
