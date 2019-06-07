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

package im.vector.riotredesign.features.roomdirectory

import im.vector.matrix.android.api.session.Session
import im.vector.riotredesign.features.roomdirectory.createroom.CreateRoomController
import im.vector.riotredesign.features.roomdirectory.picker.RoomDirectoryListCreator
import im.vector.riotredesign.features.roomdirectory.picker.RoomDirectoryPickerController
import org.koin.dsl.module.module

class RoomDirectoryModule {

    companion object {
        const val ROOM_DIRECTORY_SCOPE = "ROOM_DIRECTORY_SCOPE"
    }

    val definition = module(override = true) {

        scope(ROOM_DIRECTORY_SCOPE) {
            RoomDirectoryPickerController(get(), get(), get())
        }

        scope(ROOM_DIRECTORY_SCOPE) {
            RoomDirectoryListCreator(get(), get<Session>().sessionParams.credentials)
        }

        scope(ROOM_DIRECTORY_SCOPE) {
            PublicRoomsController(get(), get())
        }

        /* ==========================================================================================
         * Create room
         * ========================================================================================== */

        scope(ROOM_DIRECTORY_SCOPE) {
            CreateRoomController(get(), get())
        }

    }
}