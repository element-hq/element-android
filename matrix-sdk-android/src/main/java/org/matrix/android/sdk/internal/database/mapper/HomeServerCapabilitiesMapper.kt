/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.database.mapper

import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.homeserver.HomeServerCapabilities
import org.matrix.android.sdk.api.session.homeserver.RoomCapabilitySupport
import org.matrix.android.sdk.api.session.homeserver.RoomVersionCapabilities
import org.matrix.android.sdk.api.session.homeserver.RoomVersionInfo
import org.matrix.android.sdk.api.session.homeserver.RoomVersionStatus
import org.matrix.android.sdk.internal.database.model.HomeServerCapabilitiesEntity
import org.matrix.android.sdk.internal.di.MoshiProvider
import org.matrix.android.sdk.internal.session.homeserver.RoomVersions
import org.matrix.android.sdk.internal.session.room.version.DefaultRoomVersionService

/**
 * HomeServerCapabilitiesEntity -> HomeSeverCapabilities.
 */
internal object HomeServerCapabilitiesMapper {

    fun map(entity: HomeServerCapabilitiesEntity): HomeServerCapabilities {
        return HomeServerCapabilities(
                canChangePassword = entity.canChangePassword,
                canChangeDisplayName = entity.canChangeDisplayName,
                canChangeAvatar = entity.canChangeAvatar,
                canChange3pid = entity.canChange3pid,
                maxUploadFileSize = entity.maxUploadFileSize,
                lastVersionIdentityServerSupported = entity.lastVersionIdentityServerSupported,
                defaultIdentityServerUrl = entity.defaultIdentityServerUrl,
                roomVersions = mapRoomVersion(entity.roomVersionsJson),
                canUseThreading = entity.canUseThreading,
                canControlLogoutDevices = entity.canControlLogoutDevices
        )
    }

    private fun mapRoomVersion(roomVersionsJson: String?): RoomVersionCapabilities? {
        roomVersionsJson ?: return null

        return tryOrNull {
            MoshiProvider.providesMoshi().adapter(RoomVersions::class.java).fromJson(roomVersionsJson)?.let { roomVersions ->
                RoomVersionCapabilities(
                        defaultRoomVersion = roomVersions.default ?: DefaultRoomVersionService.DEFAULT_ROOM_VERSION,
                        supportedVersion = roomVersions.available?.entries?.map { entry ->
                            RoomVersionInfo(entry.key, RoomVersionStatus.STABLE
                                    .takeIf { entry.value == "stable" }
                                    ?: RoomVersionStatus.UNSTABLE)
                        }.orEmpty(),
                        capabilities = roomVersions.roomCapabilities?.entries?.mapNotNull { entry ->
                            (entry.value as? Map<*, *>)?.let {
                                val preferred = it["preferred"] as? String ?: return@mapNotNull null
                                val support = (it["support"] as? List<*>)?.filterIsInstance<String>()
                                entry.key to RoomCapabilitySupport(preferred, support.orEmpty())
                            }
                        }?.toMap()
                        // Just for debug purpose
//                                ?: mapOf(
//                                HomeServerCapabilities.ROOM_CAP_RESTRICTED to RoomCapabilitySupport(
//                                        preferred = null,
//                                        support = listOf("org.matrix.msc3083")
//                                )
//                                )
                )
            }
        }
    }
}
