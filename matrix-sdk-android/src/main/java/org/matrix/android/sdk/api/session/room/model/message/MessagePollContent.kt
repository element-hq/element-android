/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
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
data class MessagePollContent(
        /**
         * Local message type, not from server.
         */
        @Transient
        override val msgType: String = MessageType.MSGTYPE_POLL_START,
        @Json(name = "body") override val body: String = "",
        @Json(name = "m.relates_to") override val relatesTo: RelationDefaultContent? = null,
        @Json(name = "m.new_content") override val newContent: Content? = null,
        @Json(name = "org.matrix.msc3381.poll.start") val unstablePollCreationInfo: PollCreationInfo? = null,
        @Json(name = "m.poll.start") val pollCreationInfo: PollCreationInfo? = null
) : MessageContent {

    fun getBestPollCreationInfo() = pollCreationInfo ?: unstablePollCreationInfo
}
