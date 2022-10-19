/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
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

import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel

/**
 * [Event] wrapper for [EventType.MESSAGE] event type.
 * Provides additional fields and functions related to this event type.
 */
@JvmInline
value class MessageAudioEvent(val root: Event) {

    /**
     * The mapped [MessageAudioContent] model of the event content.
     */
    val content: MessageAudioContent
        get() = root.getClearContent().toModel<MessageContent>() as MessageAudioContent

    init {
        require(tryOrNull { content } != null)
    }
}

/**
 * Map a [EventType.MESSAGE] event to a [MessageAudioEvent].
 */
fun Event.asMessageAudioEvent() = if (getClearType() == EventType.MESSAGE) {
    tryOrNull { MessageAudioEvent(this) }
} else null
