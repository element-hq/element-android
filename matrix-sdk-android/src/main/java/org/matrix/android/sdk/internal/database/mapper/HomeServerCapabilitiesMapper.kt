/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.database.mapper

import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.homeserver.HomeServerCapabilities
import org.matrix.android.sdk.api.session.homeserver.RoomVersionCapabilities
import org.matrix.android.sdk.api.session.homeserver.RoomVersionInfo
import org.matrix.android.sdk.api.session.homeserver.RoomVersionStatus
import org.matrix.android.sdk.internal.database.model.HomeServerCapabilitiesEntity
import org.matrix.android.sdk.internal.di.MoshiProvider
import org.matrix.android.sdk.internal.session.homeserver.RoomVersions
import org.matrix.android.sdk.internal.session.room.version.DefaultRoomVersionService

/**
 * HomeServerCapabilitiesEntity -> HomeSeverCapabilities
 */
internal object HomeServerCapabilitiesMapper {

    fun map(entity: HomeServerCapabilitiesEntity): HomeServerCapabilities {
        return HomeServerCapabilities(
                canChangePassword = entity.canChangePassword,
                maxUploadFileSize = entity.maxUploadFileSize,
                lastVersionIdentityServerSupported = entity.lastVersionIdentityServerSupported,
                defaultIdentityServerUrl = entity.defaultIdentityServerUrl,
                roomVersions = mapRoomVersion(entity.roomVersionsJson)
        )
    }

    private fun mapRoomVersion(roomVersionsJson: String?): RoomVersionCapabilities? {
        roomVersionsJson ?: return null

        return tryOrNull {
            MoshiProvider.providesMoshi().adapter(RoomVersions::class.java).fromJson(roomVersionsJson)?.let {
                RoomVersionCapabilities(
                        defaultRoomVersion = it.default ?: DefaultRoomVersionService.DEFAULT_ROOM_VERSION,
                        supportedVersion = it.available.entries.map { entry ->
                            RoomVersionInfo(
                                    version = entry.key,
                                    status = if (entry.value == "stable") {
                                        RoomVersionStatus.STABLE
                                    } else {
                                        RoomVersionStatus.UNSTABLE
                                    }
                            )
                        }
                )
            }
        }
    }
}
