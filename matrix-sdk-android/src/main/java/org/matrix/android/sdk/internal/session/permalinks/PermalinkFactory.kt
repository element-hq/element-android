/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.session.permalinks

import org.matrix.android.sdk.api.MatrixConfiguration
import org.matrix.android.sdk.api.MatrixPatterns
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.permalinks.PermalinkData
import org.matrix.android.sdk.api.session.permalinks.PermalinkParser
import org.matrix.android.sdk.api.session.permalinks.PermalinkService
import org.matrix.android.sdk.api.session.permalinks.PermalinkService.Companion.MATRIX_TO_URL_BASE
import org.matrix.android.sdk.api.session.permalinks.PermalinkService.SpanTemplateType.HTML
import org.matrix.android.sdk.api.session.permalinks.PermalinkService.SpanTemplateType.MARKDOWN
import org.matrix.android.sdk.internal.di.UserId
import javax.inject.Inject

internal class PermalinkFactory @Inject constructor(
        @UserId
        private val userId: String,
        private val viaParameterFinder: ViaParameterFinder,
        private val matrixConfiguration: MatrixConfiguration
) {

    fun createPermalink(event: Event, forceMatrixTo: Boolean): String? {
        if (event.roomId.isNullOrEmpty() || event.eventId.isNullOrEmpty()) {
            return null
        }
        return createPermalink(event.roomId, event.eventId, forceMatrixTo)
    }

    fun createPermalink(id: String, forceMatrixTo: Boolean): String? {
        return when {
            id.isEmpty()                    -> null
            !useClientFormat(forceMatrixTo) -> MATRIX_TO_URL_BASE + escape(id)
            else                            -> {
                buildString {
                    append(matrixConfiguration.clientPermalinkBaseUrl)
                    when {
                        MatrixPatterns.isRoomId(id) || MatrixPatterns.isRoomAlias(id) -> append(ROOM_PATH)
                        MatrixPatterns.isUserId(id)                                   -> append(USER_PATH)
                        MatrixPatterns.isGroupId(id)                                  -> append(GROUP_PATH)
                    }
                    append(escape(id))
                }
            }
        }
    }

    fun createRoomPermalink(roomId: String, via: List<String>? = null, forceMatrixTo: Boolean): String? {
        return if (roomId.isEmpty()) {
            null
        } else {
            buildString {
                append(baseUrl(forceMatrixTo))
                if (useClientFormat(forceMatrixTo)) {
                    append(ROOM_PATH)
                }
                append(escape(roomId))
                append(
                        via?.takeIf { it.isNotEmpty() }?.let { viaParameterFinder.asUrlViaParameters(it) }
                                ?: viaParameterFinder.computeViaParams(userId, roomId)
                )
            }
        }
    }

    fun createPermalink(roomId: String, eventId: String, forceMatrixTo: Boolean): String {
        return buildString {
            append(baseUrl(forceMatrixTo))
            if (useClientFormat(forceMatrixTo)) {
                append(ROOM_PATH)
            }
            append(escape(roomId))
            append("/")
            append(escape(eventId))
            append(viaParameterFinder.computeViaParams(userId, roomId))
        }
    }

    fun getLinkedId(url: String): String? {
        val clientBaseUrl = matrixConfiguration.clientPermalinkBaseUrl
        return when {
            url.startsWith(MATRIX_TO_URL_BASE)                     -> url.substring(MATRIX_TO_URL_BASE.length)
            clientBaseUrl != null && url.startsWith(clientBaseUrl) -> {
                when (PermalinkParser.parse(url)) {
                    is PermalinkData.GroupLink -> url.substring(clientBaseUrl.length + GROUP_PATH.length)
                    is PermalinkData.RoomLink  -> url.substring(clientBaseUrl.length + ROOM_PATH.length)
                    is PermalinkData.UserLink  -> url.substring(clientBaseUrl.length + USER_PATH.length)
                    else                       -> null
                }
            }
            else                                                   -> null
        }
                ?.substringBeforeLast("?")
    }

    fun createMentionSpanTemplate(type: PermalinkService.SpanTemplateType, forceMatrixTo: Boolean): String {
        return buildString {
            when (type) {
                HTML     -> append(MENTION_SPAN_TO_HTML_TEMPLATE_BEGIN)
                MARKDOWN -> append(MENTION_SPAN_TO_MD_TEMPLATE_BEGIN)
            }
            append(baseUrl(forceMatrixTo))
            if (useClientFormat(forceMatrixTo)) {
                append(USER_PATH)
            }
            when (type) {
                HTML     -> append(MENTION_SPAN_TO_HTML_TEMPLATE_END)
                MARKDOWN -> append(MENTION_SPAN_TO_MD_TEMPLATE_END)
            }
        }
    }

    /**
     * Escape '/' in id, because it is used as a separator
     *
     * @param id the id to escape
     * @return the escaped id
     */
    private fun escape(id: String): String {
        return id.replace("/", "%2F")
    }

    /**
     * Unescape '/' in id
     *
     * @param id the id to escape
     * @return the escaped id
     */
    private fun unescape(id: String): String {
        return id.replace("%2F", "/")
    }

    /**
     * Get the permalink base URL according to the potential one in [MatrixConfiguration.clientPermalinkBaseUrl]
     * and the [forceMatrixTo] parameter.
     *
     * @param forceMatrixTo whether we should force using matrix.to base URL.
     *
     * @return the permalink base URL.
     */
    private fun baseUrl(forceMatrixTo: Boolean): String {
        return matrixConfiguration.clientPermalinkBaseUrl
                ?.takeUnless { forceMatrixTo }
                ?: MATRIX_TO_URL_BASE
    }

    private fun useClientFormat(forceMatrixTo: Boolean): Boolean {
        return !forceMatrixTo && matrixConfiguration.clientPermalinkBaseUrl != null
    }

    companion object {
        private const val ROOM_PATH = "room/"
        private const val USER_PATH = "user/"
        private const val GROUP_PATH = "group/"
        private const val MENTION_SPAN_TO_HTML_TEMPLATE_BEGIN = "<a href=\""
        private const val MENTION_SPAN_TO_HTML_TEMPLATE_END = "%1\$s\">%2\$s</a>"
        private const val MENTION_SPAN_TO_MD_TEMPLATE_BEGIN = "[%2\$s]("
        private const val MENTION_SPAN_TO_MD_TEMPLATE_END = "%1\$s)"
    }
}
