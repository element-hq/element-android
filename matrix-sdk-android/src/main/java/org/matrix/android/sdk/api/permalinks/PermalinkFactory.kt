/*
 * Copyright 2019 New Vector Ltd
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

package org.matrix.android.sdk.api.permalinks

import org.matrix.android.sdk.api.session.events.model.Event

/**
 * Useful methods to create Matrix permalink (matrix.to links).
 */
object PermalinkFactory {

    const val MATRIX_TO_URL_BASE = "https://matrix.to/#/"

    /**
     * Creates a permalink for an event.
     * Ex: "https://matrix.to/#/!nbzmcXAqpxBXjAdgoX:matrix.org/$1531497316352799BevdV:matrix.org"
     *
     * @param event the event
     * @return the permalink, or null in case of error
     */
    fun createPermalink(event: Event): String? {
        if (event.roomId.isNullOrEmpty() || event.eventId.isNullOrEmpty()) {
            return null
        }
        return createPermalink(event.roomId, event.eventId)
    }

    /**
     * Creates a permalink for an id (can be a user Id, Room Id, etc.).
     * Ex: "https://matrix.to/#/@benoit:matrix.org"
     *
     * @param id the id
     * @return the permalink, or null in case of error
     */
    fun createPermalink(id: String): String? {
        return if (id.isEmpty()) {
            null
        } else MATRIX_TO_URL_BASE + escape(id)
    }

    /**
     * Creates a permalink for an event. If you have an event you can use [.createPermalink]
     * Ex: "https://matrix.to/#/!nbzmcXAqpxBXjAdgoX:matrix.org/$1531497316352799BevdV:matrix.org"
     *
     * @param roomId  the id of the room
     * @param eventId the id of the event
     * @return the permalink
     */
    fun createPermalink(roomId: String, eventId: String): String {
        return MATRIX_TO_URL_BASE + escape(roomId) + "/" + escape(eventId)
    }

    /**
     * Extract the linked id from the universal link
     *
     * @param url the universal link, Ex: "https://matrix.to/#/@benoit:matrix.org"
     * @return the id from the url, ex: "@benoit:matrix.org", or null if the url is not a permalink
     */
    fun getLinkedId(url: String): String? {
        val isSupported = url.startsWith(MATRIX_TO_URL_BASE)

        return if (isSupported) {
            url.substring(MATRIX_TO_URL_BASE.length)
        } else null
    }

    /**
     * Escape '/' in id, because it is used as a separator
     *
     * @param id the id to escape
     * @return the escaped id
     */
    internal fun escape(id: String): String {
        return id.replace("/", "%2F")
    }

    /**
     * Unescape '/' in id
     *
     * @param id the id to escape
     * @return the escaped id
     */
    internal fun unescape(id: String): String {
        return id.replace("%2F", "/")
    }
}
