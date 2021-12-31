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

package org.matrix.android.sdk.api.session.permalinks

import android.net.Uri
import android.net.UrlQuerySanitizer
import org.matrix.android.sdk.api.MatrixPatterns
import timber.log.Timber
import java.net.URLDecoder

/**
 * This class turns a uri to a [PermalinkData]
 * element-based domains (e.g. https://app.element.io/#/user/@chagai95:matrix.org) permalinks
 * or matrix.to permalinks (e.g. https://matrix.to/#/@chagai95:matrix.org)
 * or client permalinks (e.g. <clientPermalinkBaseUrl>user/@chagai95:matrix.org)
 */
object PermalinkParser {

    /**
     * Turns a uri string to a [PermalinkData]
     */
    fun parse(uriString: String): PermalinkData {
        val uri = Uri.parse(uriString)
        return parse(uri)
    }

    /**
     * Turns a uri to a [PermalinkData]
     * https://github.com/matrix-org/matrix-doc/blob/master/proposals/1704-matrix.to-permalinks.md
     */
    fun parse(uri: Uri): PermalinkData {
        // the client or element-based domain permalinks (e.g. https://app.element.io/#/user/@chagai95:matrix.org) don't have the
        // mxid in the first param (like matrix.to does - https://matrix.to/#/@chagai95:matrix.org) but rather in the second after /user/ so /user/mxid
        // so convert URI to matrix.to to simplify parsing process
        val matrixToUri = MatrixToConverter.convert(uri) ?: return PermalinkData.FallbackLink(uri)

        // We can't use uri.fragment as it is decoding to early and it will break the parsing
        // of parameters that represents url (like signurl)
        val fragment = matrixToUri.toString().substringAfter("#") // uri.fragment
        if (fragment.isEmpty()) {
            return PermalinkData.FallbackLink(uri)
        }
        val safeFragment = fragment.substringBefore('?')
        val viaQueryParameters = fragment.getViaParameters()

        // we are limiting to 2 params
        val params = safeFragment
                .split(MatrixPatterns.SEP_REGEX)
                .filter { it.isNotEmpty() }
                .map { URLDecoder.decode(it, "UTF-8") }
                .take(2)

        val identifier = params.getOrNull(0)
        val extraParameter = params.getOrNull(1)
        return when {
            identifier.isNullOrEmpty()             -> PermalinkData.FallbackLink(uri)
            MatrixPatterns.isUserId(identifier)    -> PermalinkData.UserLink(userId = identifier)
            MatrixPatterns.isGroupId(identifier)   -> PermalinkData.GroupLink(groupId = identifier)
            MatrixPatterns.isRoomId(identifier)    -> {
                handleRoomIdCase(fragment, identifier, matrixToUri, extraParameter, viaQueryParameters)
            }
            MatrixPatterns.isRoomAlias(identifier) -> {
                PermalinkData.RoomLink(
                        roomIdOrAlias = identifier,
                        isRoomAlias = true,
                        eventId = extraParameter.takeIf { !it.isNullOrEmpty() && MatrixPatterns.isEventId(it) },
                        viaParameters = viaQueryParameters
                )
            }
            else                                   -> PermalinkData.FallbackLink(uri)
        }
    }

    private fun handleRoomIdCase(fragment: String, identifier: String, uri: Uri, extraParameter: String?, viaQueryParameters: List<String>): PermalinkData {
        // Can't rely on built in parsing because it's messing around the signurl
        val paramList = safeExtractParams(fragment)
        val signUrl = paramList.firstOrNull { it.first == "signurl" }?.second
        val email = paramList.firstOrNull { it.first == "email" }?.second
        return if (signUrl.isNullOrEmpty().not() && email.isNullOrEmpty().not()) {
            try {
                val signValidUri = Uri.parse(signUrl)
                val identityServerHost = signValidUri.authority ?: throw IllegalArgumentException()
                val token = signValidUri.getQueryParameter("token") ?: throw IllegalArgumentException()
                val privateKey = signValidUri.getQueryParameter("private_key") ?: throw IllegalArgumentException()
                PermalinkData.RoomEmailInviteLink(
                        roomId = identifier,
                        email = email!!,
                        signUrl = signUrl!!,
                        roomName = paramList.firstOrNull { it.first == "room_name" }?.second,
                        inviterName = paramList.firstOrNull { it.first == "inviter_name" }?.second,
                        roomAvatarUrl = paramList.firstOrNull { it.first == "room_avatar_url" }?.second,
                        roomType = paramList.firstOrNull { it.first == "room_type" }?.second,
                        identityServer = identityServerHost,
                        token = token,
                        privateKey = privateKey
                )
            } catch (failure: Throwable) {
                Timber.i("## Permalink: Failed to parse permalink $signUrl")
                PermalinkData.FallbackLink(uri)
            }
        } else {
            PermalinkData.RoomLink(
                    roomIdOrAlias = identifier,
                    isRoomAlias = false,
                    eventId = extraParameter.takeIf { !it.isNullOrEmpty() && MatrixPatterns.isEventId(it) },
                    viaParameters = viaQueryParameters
            )
        }
    }

    private fun safeExtractParams(fragment: String) =
            fragment.substringAfter("?").split('&').mapNotNull {
                val splitNameValue = it.split("=")
                if (splitNameValue.size == 2) {
                    Pair(splitNameValue[0], URLDecoder.decode(splitNameValue[1], "UTF-8"))
                } else null
            }

    private fun String.getViaParameters(): List<String> {
        return UrlQuerySanitizer(this)
                .parameterList
                .filter {
                    it.mParameter == "via"
                }.map {
                    URLDecoder.decode(it.mValue, "UTF-8")
                }
    }
}
