/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.model.message

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.session.events.model.Content
import org.matrix.android.sdk.api.session.room.model.relation.RelationDefaultContent

@JsonClass(generateAdapter = true)
data class MessageNoticeContent(
        /**
         * Required. Must be 'm.notice'.
         */
        @Json(name = MessageContent.MSG_TYPE_JSON_KEY) override val msgType: String,

        /**
         * Required. The notice text to send.
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
