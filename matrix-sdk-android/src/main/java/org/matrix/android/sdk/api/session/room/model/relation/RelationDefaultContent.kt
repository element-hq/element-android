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
package org.matrix.android.sdk.api.session.room.model.relation

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RelationDefaultContent(
        @Json(name = "rel_type") override val type: String?,
        @Json(name = "event_id") override val eventId: String?,
        @Json(name = "m.in_reply_to") override val inReplyTo: ReplyToContent? = null,
        @Json(name = "option") override val option: Int? = null,
        @Json(name = "is_falling_back") override val isFallingBack: Boolean? = null
) : RelationContent

fun RelationDefaultContent.shouldRenderInThread(): Boolean = isFallingBack == false
