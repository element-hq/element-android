/*
 * Copyright 2020 New Vector Ltd
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
package im.vector.matrix.android.api.session.room.model.message

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import im.vector.matrix.android.api.session.events.model.Content
import im.vector.matrix.android.api.session.room.model.relation.RelationDefaultContent

enum class OptionsType(val value: String) {
    POLL("m.pool"),
    BUTTONS("m.buttons"),
}

/**
 * Polls and bot buttons are m.room.message events with a msgtype of m.options,
 */
@JsonClass(generateAdapter = true)
data class MessageOptionsContent(
        @Json(name = "msgtype") override val type: String = MessageType.MSGTYPE_OPTIONS,
        @Json(name = "type") val optionType: String? = null,
        @Json(name = "body") override val body: String,
        @Json(name = "label") val label: String?,
        @Json(name = "m.relates_to") override val relatesTo: RelationDefaultContent? = null,
        @Json(name = "options") val options: List<OptionItems>? = null,
        @Json(name = "m.new_content") override val newContent: Content? = null
) : MessageContent

@JsonClass(generateAdapter = true)
data class OptionItems(
        @Json(name = "label") val label: String?,
        @Json(name = "value") val value: String?
)

@JsonClass(generateAdapter = true)
data class MessagePollResponseContent(
        @Json(name = "msgtype") override val type: String = MessageType.MSGTYPE_RESPONSE,
        @Json(name = "body") override val body: String,
        @Json(name = "m.relates_to") override val relatesTo: RelationDefaultContent? = null,
        @Json(name = "m.new_content") override val newContent: Content? = null
) : MessageContent
