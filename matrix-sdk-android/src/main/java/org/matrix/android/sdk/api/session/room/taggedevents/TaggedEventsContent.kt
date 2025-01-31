/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.taggedevents

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Keys are event IDs, values are event information.
 */
typealias TaggedEvent = Map<String, TaggedEventInfo>

/**
 * Keys are tagged event names (eg. m.favourite), values are the related events.
 */
typealias TaggedEvents = Map<String, TaggedEvent>

/**
 * Class used to parse the content of a m.tagged_events type event.
 * This kind of event defines the tagged events in a room.
 *
 * The content of this event is a tags key whose value is an object mapping the name of each tag
 * to another object. The JSON object associated with each tag is an object where the keys are the
 * event IDs and values give information about the events.
 *
 * Ref: https://github.com/matrix-org/matrix-doc/pull/2437
 */
@JsonClass(generateAdapter = true)
data class TaggedEventsContent(
        @Json(name = "tags")
        var tags: TaggedEvents = emptyMap()
) {
    val favouriteEvents
        get() = tags[TAG_FAVOURITE].orEmpty()

    val hiddenEvents
        get() = tags[TAG_HIDDEN].orEmpty()

    fun tagEvent(eventId: String, info: TaggedEventInfo, tag: String) {
        val taggedEvents = tags[tag].orEmpty().plus(eventId to info)
        tags = tags.plus(tag to taggedEvents)
    }

    fun untagEvent(eventId: String, tag: String) {
        val taggedEvents = tags[tag]?.minus(eventId).orEmpty()
        tags = tags.plus(tag to taggedEvents)
    }

    companion object {
        const val TAG_FAVOURITE = "m.favourite"
        const val TAG_HIDDEN = "m.hidden"
    }
}

@JsonClass(generateAdapter = true)
data class TaggedEventInfo(
        @Json(name = "keywords")
        val keywords: List<String>? = null,

        @Json(name = "origin_server_ts")
        val originServerTs: Long? = null,

        @Json(name = "tagged_at")
        val taggedAt: Long? = null
)
