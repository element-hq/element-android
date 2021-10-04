/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.api.session.room.typing

/**
 * This interface defines methods to handle typing data. It's implemented at the room level.
 */
interface TypingService {

    /**
     * To call when user is typing a message in the room
     * The SDK will handle the requests scheduling to the homeserver:
     * - No more than one typing request per 10s
     * - If not called after 10s, the SDK will notify the homeserver that the user is not typing anymore
     */
    fun userIsTyping()

    /**
     * To call when user stops typing in the room
     * Notify immediately the homeserver that the user is not typing anymore in the room, for
     * instance when user has emptied the composer, or when the user quits the timeline screen.
     */
    fun userStopsTyping()
}
