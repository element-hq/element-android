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

@JsonClass(generateAdapter = true)
data class MessageEmoteContent(
        /**
         * Required. Must be 'm.emote'.
         */
        @Json(name = MessageContent.MSG_TYPE_JSON_KEY) override val msgType: String,

        /**
         * Required. The emote action to perform.
         */
        @Json(name = "body") override val body: String,

        /**
         * The format used in the formatted_body. Currently only "org.matrix.custom.html" is supported.
         */
        @Json(name = "format") override val format: String? = null,

        /**
         * The formatted version of the body. This is required if format is specified.
         */
        @Json(name = "formatted_body") override val formattedBody: String? = null,

        @Json(name = "m.relates_to") override val relatesTo: RelationDefaultContent? = null,
        @Json(name = "m.new_content") override val newContent: Content? = null
) : MessageContentWithFormattedBody
