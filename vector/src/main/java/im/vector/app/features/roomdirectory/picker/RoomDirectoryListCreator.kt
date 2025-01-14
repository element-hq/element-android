/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomdirectory.picker

import im.vector.app.R
import im.vector.app.core.resources.StringArrayProvider
import im.vector.app.features.roomdirectory.RoomDirectoryData
import im.vector.app.features.roomdirectory.RoomDirectoryServer
import org.matrix.android.sdk.api.MatrixPatterns.getServerName
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.room.model.thirdparty.ThirdPartyProtocol
import javax.inject.Inject

class RoomDirectoryListCreator @Inject constructor(
        private val stringArrayProvider: StringArrayProvider,
        private val session: Session
) {

    fun computeDirectories(
            thirdPartyProtocolData: Map<String, ThirdPartyProtocol>,
            customHomeservers: Set<String>
    ): List<RoomDirectoryServer> {
        val result = ArrayList<RoomDirectoryServer>()

        val protocols = ArrayList<RoomDirectoryData>()

        // Add user homeserver name
        val userHsName = session.myUserId.getServerName()

        // Add default protocol
        protocols.add(
                RoomDirectoryData(
                        homeServer = null,
                        displayName = RoomDirectoryData.MATRIX_PROTOCOL_NAME,
                        includeAllNetworks = false
                )
        )

        // Add result of the request
        thirdPartyProtocolData.forEach {
            it.value.instances?.forEach { thirdPartyProtocolInstance ->
                protocols.add(
                        RoomDirectoryData(
                                homeServer = null,
                                displayName = thirdPartyProtocolInstance.desc ?: "",
                                thirdPartyInstanceId = thirdPartyProtocolInstance.instanceId,
                                includeAllNetworks = false,
                                // Default to protocol icon
                                avatarUrl = thirdPartyProtocolInstance.icon ?: it.value.icon
                        )
                )
            }
        }

        // Add all rooms
        protocols.add(
                RoomDirectoryData(
                        homeServer = null,
                        displayName = RoomDirectoryData.MATRIX_PROTOCOL_NAME,
                        includeAllNetworks = true
                )
        )

        result.add(
                RoomDirectoryServer(
                        serverName = userHsName,
                        isUserServer = true,
                        isManuallyAdded = false,
                        protocols = protocols
                )
        )

        // Add custom directory servers, form the config file, excluding the current user homeserver
        stringArrayProvider.getStringArray(im.vector.app.config.R.array.room_directory_servers)
                .filter { it != userHsName }
                .forEach {
                    // Use the server name as a default display name
                    result.add(
                            RoomDirectoryServer(
                                    serverName = it,
                                    isUserServer = false,
                                    isManuallyAdded = false,
                                    protocols = listOf(
                                            RoomDirectoryData(
                                                    homeServer = it,
                                                    displayName = RoomDirectoryData.MATRIX_PROTOCOL_NAME,
                                                    includeAllNetworks = false
                                            )
                                    )
                            )
                    )
                }

        // Add manually added server by the user
        customHomeservers
                .forEach {
                    // Use the server name as a default display name
                    result.add(
                            RoomDirectoryServer(
                                    serverName = it,
                                    isUserServer = false,
                                    isManuallyAdded = true,
                                    protocols = listOf(
                                            RoomDirectoryData(
                                                    homeServer = it,
                                                    displayName = RoomDirectoryData.MATRIX_PROTOCOL_NAME,
                                                    includeAllNetworks = false
                                            )
                                    )
                            )
                    )
                }

        return result
    }
}
