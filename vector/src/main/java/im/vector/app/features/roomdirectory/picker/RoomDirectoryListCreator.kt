/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.app.features.roomdirectory.picker

import im.vector.app.R
import im.vector.app.core.resources.StringArrayProvider
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.room.model.thirdparty.RoomDirectoryData
import org.matrix.android.sdk.api.session.room.model.thirdparty.ThirdPartyProtocol
import javax.inject.Inject

class RoomDirectoryListCreator @Inject constructor(private val stringArrayProvider: StringArrayProvider,
                                                   private val session: Session) {

    fun computeDirectories(thirdPartyProtocolData: Map<String, ThirdPartyProtocol>): List<RoomDirectoryData> {
        val result = ArrayList<RoomDirectoryData>()

        // Add user homeserver name
        val userHsName = session.myUserId.substringAfter(":")

        result.add(RoomDirectoryData(
                displayName = userHsName,
                includeAllNetworks = true
        ))

        // Add user's HS but for Matrix public rooms only
        result.add(RoomDirectoryData())

        // Add custom directory servers
        val hsNamesList = stringArrayProvider.getStringArray(R.array.room_directory_servers)
        hsNamesList.forEach {
            if (it != userHsName) {
                // Use the server name as a default display name
                result.add(RoomDirectoryData(
                        homeServer = it,
                        displayName = it,
                        includeAllNetworks = true
                ))
            }
        }

        // Add result of the request
        thirdPartyProtocolData.forEach {
            it.value.instances?.forEach { thirdPartyProtocolInstance ->
                result.add(RoomDirectoryData(
                        homeServer = null,
                        displayName = thirdPartyProtocolInstance.desc ?: "",
                        thirdPartyInstanceId = thirdPartyProtocolInstance.instanceId,
                        includeAllNetworks = false,
                        // Default to protocol icon
                        avatarUrl = thirdPartyProtocolInstance.icon ?: it.value.icon
                ))
            }
        }

        return result
    }
}
