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

package im.vector.matrix.android.api.session.room.read

/**
 * This interface defines methods to handle read receipts and read marker in a room. It's implemented at the room level.
 */
interface ReadService {

    /**
     * Force the read marker to be set on the latest event.
     */
    suspend fun markAllAsRead()

    /**
     * Set the read receipt on the event with provided eventId.
     */
    suspend fun setReadReceipt(eventId: String)

    /**
     * Set the read marker on the event with provided eventId.
     */
    suspend fun setReadMarker(fullyReadEventId: String)

    fun isEventRead(eventId: String): Boolean
}