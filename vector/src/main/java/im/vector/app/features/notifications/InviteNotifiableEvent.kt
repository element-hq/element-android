/*
 * Copyright 2019 New Vector Ltd
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

import androidx.core.app.NotificationCompat

data class InviteNotifiableEvent(
        override var matrixID: String?,
        override val eventId: String,
        override val editedEventId: String?,
        var roomId: String,
        override var noisy: Boolean,
        override val title: String,
        override val description: String,
        override val type: String?,
        override val timestamp: Long,
        override var soundName: String?,
        override var isPushGatewayEvent: Boolean = false) : NotifiableEvent {

    override var hasBeenDisplayed: Boolean = false
    override var isRedacted: Boolean = false
    override var lockScreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
}
