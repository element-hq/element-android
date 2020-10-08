/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.room

import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.session.room.RoomDirectoryService
import org.matrix.android.sdk.api.session.room.model.roomdirectory.PublicRoomsParams
import org.matrix.android.sdk.api.session.room.model.roomdirectory.PublicRoomsResponse
import org.matrix.android.sdk.api.session.room.model.thirdparty.ThirdPartyProtocol
import org.matrix.android.sdk.api.util.Cancelable
import org.matrix.android.sdk.internal.session.room.directory.GetPublicRoomTask
import org.matrix.android.sdk.internal.session.room.directory.GetThirdPartyProtocolsTask
import org.matrix.android.sdk.internal.task.TaskExecutor
import org.matrix.android.sdk.internal.task.configureWith
import javax.inject.Inject

internal class DefaultRoomDirectoryService @Inject constructor(private val getPublicRoomTask: GetPublicRoomTask,
                                                               private val getThirdPartyProtocolsTask: GetThirdPartyProtocolsTask,
                                                               private val taskExecutor: TaskExecutor) : RoomDirectoryService {

    override fun getPublicRooms(server: String?,
                                publicRoomsParams: PublicRoomsParams,
                                callback: MatrixCallback<PublicRoomsResponse>): Cancelable {
        return getPublicRoomTask
                .configureWith(GetPublicRoomTask.Params(server, publicRoomsParams)) {
                    this.callback = callback
                }
                .executeBy(taskExecutor)
    }

    override fun getThirdPartyProtocol(callback: MatrixCallback<Map<String, ThirdPartyProtocol>>): Cancelable {
        return getThirdPartyProtocolsTask
                .configureWith {
                    this.callback = callback
                }
                .executeBy(taskExecutor)
    }
}
