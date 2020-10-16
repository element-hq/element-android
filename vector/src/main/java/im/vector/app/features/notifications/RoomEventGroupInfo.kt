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

package im.vector.app.features.notifications

/**
 * Data class to hold information about a group of notifications for a room
 */
data class RoomEventGroupInfo(
        val roomId: String,
        val roomDisplayName: String = "",
        val isDirect: Boolean = false
) {
    // An event in the list has not yet been display
    var hasNewEvent: Boolean = false

    // true if at least one on the not yet displayed event is noisy
    var shouldBing: Boolean = false
    var customSound: String? = null
    var hasSmartReplyError: Boolean = false
}
