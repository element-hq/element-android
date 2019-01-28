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

package im.vector.matrix.android.internal.session.room.read

import arrow.core.Try
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.room.RoomAPI
import im.vector.matrix.android.internal.task.Task

internal interface SetReadMarkersTask : Task<SetReadMarkersTask.Params, Void> {

    data class Params(
            val roomId: String,
            val fullyReadEventId: String?,
            val readReceiptEventId: String?
    )
}

private const val READ_MARKER = "m.fully_read"
private const val READ_RECEIPT = "m.read"

internal class DefaultSetReadMarkersTask(private val roomAPI: RoomAPI
) : SetReadMarkersTask {

    override fun execute(params: SetReadMarkersTask.Params): Try<Void> {
        val markers = HashMap<String, String>()
        if (params.fullyReadEventId?.isNotEmpty() == true) {
            markers[READ_MARKER] = params.fullyReadEventId
        }
        if (params.readReceiptEventId?.isNotEmpty() == true) {
            markers[READ_RECEIPT] = params.readReceiptEventId
        }
        return executeRequest {
            apiCall = roomAPI.sendReadMarker(params.roomId, markers)
        }
    }
}