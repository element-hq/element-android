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
data class MessagePollResponseContent(
        /**
         * Local message type, not from server.
         */
        @Transient
        override val msgType: String = MessageType.MSGTYPE_POLL_RESPONSE,
        @Json(name = "body") override val body: String = "",
        @Json(name = "m.relates_to") override val relatesTo: RelationDefaultContent? = null,
        @Json(name = "m.new_content") override val newContent: Content? = null,
        @Json(name = "org.matrix.msc3381.poll.response") val unstableResponse: PollResponse? = null,
        @Json(name = "m.response") val response: PollResponse? = null
) : MessageContent {

    fun getBestResponse() = response ?: unstableResponse
}
