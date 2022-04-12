/*
 * Copyright (c) 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.api.session.crypto

/**
 * This listener notifies on new Megolm sessions being created
 */
interface NewSessionListener {

    /**
     * @param roomId the room id where the new Megolm session has been created for, may be null when importing from external sessions
     * @param senderKey the sender key of the device which the Megolm session is shared with
     * @param sessionId the session id of the Megolm session
     */
    fun onNewSession(roomId: String?, senderKey: String, sessionId: String)
}
