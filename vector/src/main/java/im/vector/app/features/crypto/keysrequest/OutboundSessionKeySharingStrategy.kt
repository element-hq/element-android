/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.crypto.keysrequest

enum class OutboundSessionKeySharingStrategy {
    /**
     * Keys will be sent for the first time when the first message is sent
     */
    WhenSendingEvent,

    /**
     * Keys will be sent for the first time when the timeline displayed
     */
    WhenEnteringRoom,

    /**
     * Keys will be sent for the first time when a typing started
     */
    WhenTyping
}
