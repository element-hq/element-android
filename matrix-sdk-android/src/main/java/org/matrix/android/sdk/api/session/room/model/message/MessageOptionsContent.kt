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

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.session.events.model.Content
import org.matrix.android.sdk.api.session.room.model.relation.RelationDefaultContent

// Possible values for optionType
const val OPTION_TYPE_POLL = "org.matrix.poll"
const val OPTION_TYPE_BUTTONS = "org.matrix.buttons"

/**
 * Polls and bot buttons are m.room.message events with a msgtype of m.options,
 * Ref: https://github.com/matrix-org/matrix-doc/pull/2192
 */
@JsonClass(generateAdapter = true)
data class MessageOptionsContent(
        @Json(name = MessageContent.MSG_TYPE_JSON_KEY) override val msgType: String = MessageType.MSGTYPE_OPTIONS,
        @Json(name = "type") val optionType: String? = null,
        @Json(name = "body") override val body: String,
        @Json(name = "label") val label: String?,
        @Json(name = "m.relates_to") override val relatesTo: RelationDefaultContent? = null,
        @Json(name = "options") val options: List<OptionItem>? = null,
        @Json(name = "m.new_content") override val newContent: Content? = null
) : MessageContent
