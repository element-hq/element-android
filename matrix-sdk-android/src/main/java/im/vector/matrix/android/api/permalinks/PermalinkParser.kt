/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.api.permalinks

import android.net.Uri
import im.vector.matrix.android.api.MatrixPatterns

/**
 * This class turns an uri to a [PermalinkData]
 */
object PermalinkParser {

    /**
     * Turns an uri string to a [PermalinkData]
     */
    fun parse(uriString: String): PermalinkData {
        val uri = Uri.parse(uriString)
        return parse(uri)
    }

    /**
     * Turns an uri to a [PermalinkData]
     */
    fun parse(uri: Uri): PermalinkData {
        if (!uri.toString().startsWith(PermalinkFactory.MATRIX_TO_URL_BASE)) {
            return PermalinkData.FallbackLink(uri)
        }

        val fragment = uri.fragment
        if (fragment.isNullOrEmpty()) {
            return PermalinkData.FallbackLink(uri)
        }

        val indexOfQuery = fragment.indexOf("?")
        val safeFragment = if (indexOfQuery != -1) fragment.substring(0, indexOfQuery) else fragment

        // we are limiting to 2 params
        val params = safeFragment
                .split(MatrixPatterns.SEP_REGEX.toRegex())
                .filter { it.isNotEmpty() }
                .take(2)

        val identifier = params.getOrNull(0)
        val extraParameter = params.getOrNull(1)
        if (identifier.isNullOrEmpty()) {
            return PermalinkData.FallbackLink(uri)
        }
        return when {
            MatrixPatterns.isUserId(identifier)  -> PermalinkData.UserLink(userId = identifier)
            MatrixPatterns.isGroupId(identifier) -> PermalinkData.GroupLink(groupId = identifier)
            MatrixPatterns.isRoomId(identifier)  -> {
                if (!extraParameter.isNullOrEmpty() && MatrixPatterns.isEventId(extraParameter)) {
                    PermalinkData.EventLink(roomIdOrAlias = identifier, eventId = extraParameter)
                } else {
                    PermalinkData.RoomLink(roomIdOrAlias = identifier)
                }
            }
            else                                 -> PermalinkData.FallbackLink(uri)
        }
    }

}