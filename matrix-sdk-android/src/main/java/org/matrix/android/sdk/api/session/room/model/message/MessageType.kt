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

package org.matrix.android.sdk.api.session.room.model.message

object MessageType {
    const val MSGTYPE_TEXT = "m.text"
    const val MSGTYPE_EMOTE = "m.emote"
    const val MSGTYPE_NOTICE = "m.notice"
    const val MSGTYPE_IMAGE = "m.image"
    const val MSGTYPE_AUDIO = "m.audio"
    const val MSGTYPE_VIDEO = "m.video"
    const val MSGTYPE_LOCATION = "m.location"
    const val MSGTYPE_FILE = "m.file"

    const val MSGTYPE_VERIFICATION_REQUEST = "m.key.verification.request"

    // Add, in local, a fake message type in order to StickerMessage can inherit Message class
    // Because sticker isn't a message type but a event type without msgtype field
    const val MSGTYPE_STICKER_LOCAL = "org.matrix.android.sdk.sticker"

    // Fake message types for poll events to be able to inherit them from MessageContent
    // Because poll events are not message events and they don't have msgtype field
    const val MSGTYPE_POLL_START = "org.matrix.android.sdk.poll.start"
    const val MSGTYPE_POLL_RESPONSE = "org.matrix.android.sdk.poll.response"

    const val MSGTYPE_CONFETTI = "nic.custom.confetti"
    const val MSGTYPE_SNOWFALL = "io.element.effect.snowfall"

    // Fake message types for live location events to be able to inherit them from MessageContent
    const val MSGTYPE_LIVE_LOCATION = "org.matrix.android.sdk.livelocation"
}
