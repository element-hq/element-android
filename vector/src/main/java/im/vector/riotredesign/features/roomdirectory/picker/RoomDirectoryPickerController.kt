/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.riotredesign.features.roomdirectory.picker

import com.airbnb.epoxy.TypedEpoxyController
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Incomplete
import com.airbnb.mvrx.Success
import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.api.session.room.model.thirdparty.RoomDirectoryData
import im.vector.matrix.android.api.session.room.model.thirdparty.ThirdPartyProtocol
import im.vector.riotredesign.R
import im.vector.riotredesign.core.epoxy.errorWithRetryItem
import im.vector.riotredesign.core.epoxy.loadingItem
import im.vector.riotredesign.core.resources.StringArrayProvider
import im.vector.riotredesign.core.resources.StringProvider

class RoomDirectoryPickerController(private val stringProvider: StringProvider,
                                    private val stringArrayProvider: StringArrayProvider,
                                    private val credentials: Credentials
) : TypedEpoxyController<RoomDirectoryPickerViewState>() {

    var callback: Callback? = null

    var index = 0

    override fun buildModels(viewState: RoomDirectoryPickerViewState) {
        val asyncThirdPartyProtocol = viewState.asyncThirdPartyRequest

        when (asyncThirdPartyProtocol) {
            is Success    -> {
                val directories = computeDirectories(asyncThirdPartyProtocol.invoke())

                directories.forEach {
                    buildDirectory(it)
                }
            }
            is Incomplete -> {
                loadingItem {
                    id("loading")
                }
            }
            is Fail       -> {
                errorWithRetryItem {
                    id("error")
                    text(asyncThirdPartyProtocol.error.localizedMessage)
                    listener { callback?.retry() }
                }
            }
        }
    }

    private fun computeDirectories(thirdPartyProtocolData: Map<String, ThirdPartyProtocol>): List<RoomDirectoryData> {
        val result = ArrayList<RoomDirectoryData>()

        // Add user homeserver name
        val userHsName = credentials.userId.substring(credentials.userId.indexOf(":") + 1)

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

    private fun buildDirectory(roomDirectoryData: RoomDirectoryData) {

        // TODO
        roomDirectoryItem {
            id(index++)

            directoryName(roomDirectoryData.displayName)

            val description = when {
                roomDirectoryData.includeAllNetworks      -> stringProvider.getString(R.string.directory_server_all_rooms_on_server, roomDirectoryData.displayName)
                "Matrix" == roomDirectoryData.displayName -> stringProvider.getString(R.string.directory_server_native_rooms, roomDirectoryData.displayName)
                else                                      -> null
            }

            directoryDescription(description)
            directoryAvatarUrl(roomDirectoryData.avatarUrl)
            includeAllNetworks(roomDirectoryData.includeAllNetworks)

            globalListener {
                callback?.onRoomDirectoryClicked(roomDirectoryData)
            }
        }
    }

    interface Callback {
        fun onRoomDirectoryClicked(roomDirectory: RoomDirectoryData)
        fun retry()
    }

}
