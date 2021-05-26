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

package im.vector.app.features.roomdirectory.picker

import com.airbnb.epoxy.TypedEpoxyController
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Incomplete
import com.airbnb.mvrx.Success
import im.vector.app.R
import im.vector.app.core.epoxy.dividerItem
import im.vector.app.core.epoxy.errorWithRetryItem
import im.vector.app.core.epoxy.loadingItem
import im.vector.app.core.error.ErrorFormatter
import im.vector.app.core.extensions.join
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.roomdirectory.RoomDirectoryData
import im.vector.app.features.roomdirectory.RoomDirectoryServer
import javax.inject.Inject

class RoomDirectoryPickerController @Inject constructor(
        private val stringProvider: StringProvider,
        colorProvider: ColorProvider,
        private val errorFormatter: ErrorFormatter,
        private val roomDirectoryListCreator: RoomDirectoryListCreator
) : TypedEpoxyController<RoomDirectoryPickerViewState>() {

    var callback: Callback? = null

    private val dividerColor = colorProvider.getColorFromAttribute(R.attr.vctr_list_divider_color)

    override fun buildModels(viewState: RoomDirectoryPickerViewState) {
        val host = this

        when (val asyncThirdPartyProtocol = viewState.asyncThirdPartyRequest) {
            is Success    -> {
                val directories = roomDirectoryListCreator.computeDirectories(asyncThirdPartyProtocol())
                directories.join(
                        each = { _, roomDirectoryServer -> buildDirectory(roomDirectoryServer) },
                        between = { idx, _ -> buildDivider(idx) }
                )
            }
            is Incomplete -> {
                loadingItem {
                    id("loading")
                }
            }
            is Fail       -> {
                errorWithRetryItem {
                    id("error")
                    text(host.errorFormatter.toHumanReadable(asyncThirdPartyProtocol.error))
                    listener { host.callback?.retry() }
                }
            }
        }
    }

    private fun buildDivider(idx: Int) {
        val host = this
        dividerItem {
            id("divider_${idx}")
            color(host.dividerColor)
        }
    }

    private fun buildDirectory(roomDirectoryServer: RoomDirectoryServer) {
        val host = this
        roomDirectoryServerItem {
            id("server_" + roomDirectoryServer.serverName)
            serverName(roomDirectoryServer.serverName)

            if (roomDirectoryServer.isUserServer) {
                serverDescription(host.stringProvider.getString(R.string.directory_your_server))
            }
        }

        roomDirectoryServer.protocols.forEach { roomDirectoryData ->
            roomDirectoryItem {
                id("server_" + roomDirectoryServer.serverName + "_proto_" + roomDirectoryData.displayName)
                directoryName(
                        if (roomDirectoryData.includeAllNetworks) {
                            host.stringProvider.getString(R.string.directory_server_all_rooms_on_server, roomDirectoryServer.serverName)
                        } else {
                            roomDirectoryData.displayName
                        }
                )
                if (roomDirectoryData.displayName == RoomDirectoryData.MATRIX_PROTOCOL_NAME && !roomDirectoryData.includeAllNetworks) {
                    directoryDescription(
                            host.stringProvider.getString(R.string.directory_server_native_rooms, roomDirectoryServer.serverName)
                    )
                }
                directoryAvatarUrl(roomDirectoryData.avatarUrl)
                includeAllNetworks(roomDirectoryData.includeAllNetworks)

                globalListener {
                    host.callback?.onRoomDirectoryClicked(roomDirectoryData)
                }
            }
        }
    }

    interface Callback {
        fun onRoomDirectoryClicked(roomDirectoryData: RoomDirectoryData)
        fun retry()
    }
}
