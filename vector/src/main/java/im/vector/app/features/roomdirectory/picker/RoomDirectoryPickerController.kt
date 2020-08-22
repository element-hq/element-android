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
import im.vector.app.core.epoxy.errorWithRetryItem
import im.vector.app.core.epoxy.loadingItem
import im.vector.app.core.error.ErrorFormatter
import im.vector.app.core.resources.StringProvider
import org.matrix.android.sdk.api.session.room.model.thirdparty.RoomDirectoryData
import javax.inject.Inject

class RoomDirectoryPickerController @Inject constructor(private val stringProvider: StringProvider,
                                                        private val errorFormatter: ErrorFormatter,
                                                        private val roomDirectoryListCreator: RoomDirectoryListCreator
) : TypedEpoxyController<RoomDirectoryPickerViewState>() {

    var callback: Callback? = null

    var index = 0

    override fun buildModels(viewState: RoomDirectoryPickerViewState) {
        val asyncThirdPartyProtocol = viewState.asyncThirdPartyRequest

        when (asyncThirdPartyProtocol) {
            is Success    -> {
                val directories = roomDirectoryListCreator.computeDirectories(asyncThirdPartyProtocol())

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
                    text(errorFormatter.toHumanReadable(asyncThirdPartyProtocol.error))
                    listener { callback?.retry() }
                }
            }
        }
    }

    private fun buildDirectory(roomDirectoryData: RoomDirectoryData) {
        roomDirectoryItem {
            id(index++)

            directoryName(roomDirectoryData.displayName)

            val description = when {
                roomDirectoryData.includeAllNetworks      ->
                    stringProvider.getString(R.string.directory_server_all_rooms_on_server, roomDirectoryData.displayName)
                "Matrix" == roomDirectoryData.displayName ->
                    stringProvider.getString(R.string.directory_server_native_rooms, roomDirectoryData.displayName)
                else                                      ->
                    null
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
        fun onRoomDirectoryClicked(roomDirectoryData: RoomDirectoryData)
        fun retry()
    }
}
