/*
 * Copyright 2015 OpenMarket Ltd
 * Copyright 2017 Vector Creations Ltd
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

package im.vector.matrix.android.internal.crypto.algorithms

import im.vector.matrix.android.api.session.events.model.Content

/**
 * An interface for encrypting data
 */
internal interface IMXEncrypting {

    /**
     * Encrypt an event content according to the configuration of the room.
     *
     * @param eventContent the content of the event.
     * @param eventType    the type of the event.
     * @param userIds      the room members the event will be sent to.
     * @return the encrypted content
     */
    suspend fun encryptEventContent(eventContent: Content, eventType: String, userIds: List<String>): Content

    /**
     * Re-shares a session key with devices if the key has already been
     * sent to them.
     *
     * @param sessionId The id of the outbound session to share.
     * @param userId The id of the user who owns the target device.
     * @param deviceId The id of the target device.
     * @param senderKey The key of the originating device for the session.
     *
     * @return true in case of success
     */
    suspend fun reshareKey(sessionId: String,
                           userId: String,
                           deviceId: String,
                           senderKey: String): Boolean
}
