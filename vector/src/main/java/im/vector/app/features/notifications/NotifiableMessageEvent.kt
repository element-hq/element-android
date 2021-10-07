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
import org.matrix.android.sdk.api.session.events.model.EventType

data class NotifiableMessageEvent(
        override val eventId: String,
        override val editedEventId: String?,
        override var noisy: Boolean,
        val timestamp: Long,
        var senderName: String?,
        var senderId: String?,
        var body: String?,
        var roomId: String,
        var roomName: String?,
        var roomIsDirect: Boolean = false
) : NotifiableEvent {

    var matrixID: String? = null
    var soundName: String? = null
    override var lockScreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
    override var hasBeenDisplayed: Boolean = false
    override var isRedacted: Boolean = false

    var roomAvatarPath: String? = null
    var senderAvatarPath: String? = null

    override var isPushGatewayEvent: Boolean = false

    val type: String = EventType.MESSAGE

    val description: String
        get() = body ?: ""

    val title: String = senderName ?: ""

    // This is used for >N notification, as the result of a smart reply
    var outGoingMessage = false
    var outGoingMessageFailed = false
}
