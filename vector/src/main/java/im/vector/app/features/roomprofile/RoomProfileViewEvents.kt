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

package im.vector.app.features.roomprofile

import androidx.core.content.pm.ShortcutInfoCompat
import im.vector.app.core.platform.VectorViewEvents

/**
 * Transient events for RoomProfile
 */
sealed class RoomProfileViewEvents : VectorViewEvents {
    data class Loading(val message: CharSequence? = null) : RoomProfileViewEvents()
    object DismissLoading : RoomProfileViewEvents()
    data class Failure(val throwable: Throwable) : RoomProfileViewEvents()

    data class ShareRoomProfile(val permalink: String) : RoomProfileViewEvents()
    data class OnShortcutReady(val shortcutInfo: ShortcutInfoCompat) : RoomProfileViewEvents()
}
