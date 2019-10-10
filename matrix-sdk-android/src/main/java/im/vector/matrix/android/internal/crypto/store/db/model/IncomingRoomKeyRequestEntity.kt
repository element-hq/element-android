/*
 * Copyright 2018 New Vector Ltd
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

package im.vector.matrix.android.internal.crypto.store.db.model

import im.vector.matrix.android.internal.crypto.IncomingRoomKeyRequest
import im.vector.matrix.android.internal.crypto.model.rest.RoomKeyRequestBody
import io.realm.RealmObject

internal open class IncomingRoomKeyRequestEntity(
        var requestId: String? = null,
        var userId: String? = null,
        var deviceId: String? = null,
        // RoomKeyRequestBody fields
        var requestBodyAlgorithm: String? = null,
        var requestBodyRoomId: String? = null,
        var requestBodySenderKey: String? = null,
        var requestBodySessionId: String? = null
) : RealmObject() {

    fun toIncomingRoomKeyRequest(): IncomingRoomKeyRequest {
        return IncomingRoomKeyRequest().also {
            it.requestId = requestId
            it.userId = userId
            it.deviceId = deviceId
            it.requestBody = RoomKeyRequestBody().apply {
                algorithm = requestBodyAlgorithm
                roomId = requestBodyRoomId
                senderKey = requestBodySenderKey
                sessionId = requestBodySessionId
            }
        }
    }

    fun putRequestBody(requestBody: RoomKeyRequestBody?) {
        requestBody?.let {
            requestBodyAlgorithm = it.algorithm
            requestBodyRoomId = it.roomId
            requestBodySenderKey = it.senderKey
            requestBodySessionId = it.sessionId
        }
    }
}
