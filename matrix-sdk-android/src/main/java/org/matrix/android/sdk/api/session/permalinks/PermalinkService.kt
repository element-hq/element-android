/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.permalinks

import org.matrix.android.sdk.api.session.events.model.Event

/**
 * Useful methods to create permalink (like matrix.to links or client permalinks).
 * See [org.matrix.android.sdk.api.MatrixConfiguration.clientPermalinkBaseUrl] to setup a custom permalink base url.
 */
interface PermalinkService {

    companion object {
        const val MATRIX_TO_URL_BASE = "https://matrix.to/#/"
    }

    enum class SpanTemplateType {
        HTML,
        MARKDOWN
    }

    /**
     * Creates a permalink for an event.
     * Ex: "https://matrix.to/#/!nbzmcXAqpxBXjAdgoX:matrix.org/$1531497316352799BevdV:matrix.org"
     *
     * @param event the event
     * @param forceMatrixTo whether we should force using matrix.to base URL
     *
     * @return the permalink, or null in case of error
     */
    fun createPermalink(event: Event, forceMatrixTo: Boolean = false): String?

    /**
     * Creates a permalink for an id (can be a user Id, etc.).
     * For a roomId, consider using [createRoomPermalink]
     * Ex: "https://matrix.to/#/@benoit:matrix.org"
     *
     * @param id the id
     * @param forceMatrixTo whether we should force using matrix.to base URL
     *
     * @return the permalink, or null in case of error
     */
    fun createPermalink(id: String, forceMatrixTo: Boolean = false): String?

    /**
     * Creates a permalink for a roomId, including the via parameters.
     *
     * @param roomId the room id
     * @param viaServers the via parameter
     * @param forceMatrixTo whether we should force using matrix.to base URL
     *
     * @return the permalink, or null in case of error
     */
    fun createRoomPermalink(roomId: String, viaServers: List<String>? = null, forceMatrixTo: Boolean = false): String?

    /**
     * Creates a permalink for an event. If you have an event you can use [createPermalink]
     * Ex: "https://matrix.to/#/!nbzmcXAqpxBXjAdgoX:matrix.org/$1531497316352799BevdV:matrix.org?via=matrix.org"
     *
     * @param roomId the id of the room
     * @param eventId the id of the event
     * @param forceMatrixTo whether we should force using matrix.to base URL
     *
     * @return the permalink
     */
    fun createPermalink(roomId: String, eventId: String, forceMatrixTo: Boolean = false): String

    /**
     * Extract the linked id from the universal link.
     *
     * @param url the universal link, Ex: "https://matrix.to/#/@benoit:matrix.org"
     * @return the id from the url, ex: "@benoit:matrix.org", or null if the url is not a permalink
     */
    fun getLinkedId(url: String): String?

    /**
     * Creates a HTML or Markdown mention span template. Can be used to replace a mention with a permalink to mentioned user.
     * Ex: "<a href=\"https://matrix.to/#/%1\$s\">%2\$s</a>" or "[%2\$s](https://matrix.to/#/%1\$s)"
     *
     * @param type type of template to create
     * @param forceMatrixTo whether we should force using matrix.to base URL
     *
     * @return the created template
     */
    fun createMentionSpanTemplate(type: SpanTemplateType, forceMatrixTo: Boolean = false): String
}
