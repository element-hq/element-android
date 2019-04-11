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

package im.vector.matrix.android.api.session.room.send

import im.vector.matrix.android.api.session.content.ContentAttachmentData
import im.vector.matrix.android.api.session.room.model.message.MessageType
import im.vector.matrix.android.api.util.Cancelable


/**
 * This interface defines methods to send events in a room. It's implemented at the room level.
 */
interface SendService {

    /**
     * Method to send a text message asynchronously.
     * @param text the text message to send
     * @param msgType the message type: MessageType.MSGTYPE_TEXT (default) or MessageType.MSGTYPE_EMOTE
     * @return a [Cancelable]
     */
    fun sendTextMessage(text: String, msgType: String = MessageType.MSGTYPE_TEXT): Cancelable

    /**
     * Method to send a media asynchronously.
     * @param attachment the media to send
     * @return a [Cancelable]
     */
    fun sendMedia(attachment: ContentAttachmentData): Cancelable

    /**
     * Method to send a list of media asynchronously.
     * @param attachments the list of media to send
     * @return a [Cancelable]
     */
    fun sendMedias(attachments: List<ContentAttachmentData>): Cancelable


}