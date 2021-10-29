/*
 * Copyright (c) 2021 New Vector Ltd
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

package fr.gouv.tchap.core.utils

import im.vector.app.core.utils.safeCapitalize
import org.matrix.android.sdk.api.MatrixPatterns
import org.matrix.android.sdk.api.session.Session
import java.util.Locale

object TchapUtils {

    /**
     * Get the homeserver name of a matrix identifier.
     * The identifier type may be any matrix identifier type: user id, room id, ...
     * For example in case of "@jean-philippe.martin-modernisation.fr:matrix.test.org", this will return "matrix.test.org".
     * in case of "!AAAAAAA:matrix.test.org", this will return "matrix.test.org".
     *
     * @param mxId the matrix identifier.
     * @return the homeserver name, if any.
     */
    fun getHomeServerNameFromMXIdentifier(mxId: String): String = mxId.substringAfter(":", "")

    /**
     * Get the Tchap display name of the homeserver mentioned in a matrix identifier.
     * The identifier type may be any matrix identifier type: user id, room id, ...
     * The returned name is capitalize.
     * The Tchap HS display name is the component mentioned before the suffix "tchap.gouv.fr"
     * For example in case of "@jean-philippe.martin-modernisation.fr:name1.tchap.gouv.fr", this will return "Name1".
     * in case of "@jean-philippe.martin-modernisation.fr:agent.name2.tchap.gouv.fr", this will return "Name2".
     *
     * @param mxId the matrix identifier.
     * @return the Tchap display name of the homeserver.
     */
    fun getHomeServerDisplayNameFromMXIdentifier(mxId: String): String {
        var homeserverName = getHomeServerNameFromMXIdentifier(mxId)
        if (homeserverName.contains("tchap.gouv.fr")) {
            val components = homeserverName.split("\\.".toRegex())
            if (components.size >= 4) {
                homeserverName = components[components.size - 4]
            }
        }
        return homeserverName.safeCapitalize(Locale.getDefault())
    }

    /**
     * Tells whether a homeserver name corresponds to an external server or not.
     *
     * @param homeServerName the homeserver name to check.
     * @return true if external.
     */
    fun isExternalTchapServer(homeServerName: String) = homeServerName.startsWith("e.") || homeServerName.startsWith("agent.externe.")

    /**
     * Tells whether the provided tchap identifier corresponds to an extern user.
     * Note: invalid tchap identifier will be considered as external.
     *
     * @param tchapUserId user identifier (ie. the matrix identifier).
     * @return true if external.
     */
    fun isExternalTchapUser(tchapUserId: String): Boolean {
        val homeServerName = getHomeServerNameFromMXIdentifier(tchapUserId)
        return if (homeServerName.isNotEmpty()) isExternalTchapServer(homeServerName) else true
    }

    /**
     * Get name part of a display name by removing the domain part if any.
     * For example in case of "Jean Martin `[Modernisation]`", this will return "Jean Martin".
     *
     * @param displayName the display name to compute.
     * @return displayName without domain (or the display name itself if no domain has been found).
     */
    fun getNameFromDisplayName(displayName: String): String {
        return displayName.split(DISPLAY_NAME_FIRST_DELIMITER)
                .first()
                .trim()
    }

    /**
     * Get the potential domain name from a display name.
     * For example in case of "Jean Martin `[Modernisation]`", this will return "Modernisation".
     *
     * @param displayName the display name to compute.
     * @return displayName without name, empty string if no domain is available.
     */
    fun getDomainFromDisplayName(displayName: String): String {
        return displayName.split(DISPLAY_NAME_FIRST_DELIMITER)
                .elementAtOrNull(1)
                ?.split(DISPLAY_NAME_SECOND_DELIMITER)
                ?.first()
                ?.trim()
                ?: DEFAULT_EMPTY_STRING
    }

    /**
     * Build a display name from the tchap user identifier.
     * We don't extract the domain for the moment in order to not display unexpected information.
     * For example in case of "@jean-philippe.martin-modernisation.fr:matrix.org", this will return "Jean-Philippe Martin".
     * Note: in case of an external user identifier, we return the local part of the id which corresponds to their email.
     *
     * @param tchapUserId user identifier (ie. the matrix identifier).
     * @return displayName without domain, or null if the user identifier is not valid.
     */
    fun computeDisplayNameFromUserId(tchapUserId: String?): String? {
        var displayName: String? = null
        if (null != tchapUserId && MatrixPatterns.isUserId(tchapUserId)) {
            // Remove first the host from the id by ignoring the first character '@' too.
            val identifier = tchapUserId.substringAfter('@').substringBefore(':')
            if (isExternalTchapUser(tchapUserId)) {
                // Replace the hyphen character if there is only one
                displayName = if (identifier.filter { it == '-' }.count() == 1) {
                    identifier.replace('-', '@')
                } else {
                    identifier
                }
            } else {
                val hyphenPos = identifier.lastIndexOf('-')
                if (hyphenPos > 0) {
                    var capNextChar = true
                    val chars = identifier.substring(0, hyphenPos).mapNotNull { c ->
                        when {
                            // Check whether the identifier contains some hyphen or dot
                            (c == '-' || c == '.') -> {
                                if (!capNextChar) {
                                    capNextChar = true
                                    // Replace the dot character by space character
                                    if (c == '.') ' ' else c
                                } else {
                                    null
                                }
                            }
                            capNextChar            -> {
                                capNextChar = false
                                // Capitalize the character
                                Character.toTitleCase(c)
                            }
                            else                   -> {
                                c
                            }
                        }
                    }.toCharArray()

                    displayName = String(chars)
                }
            }
        }
        return displayName
    }

    /**
     * Create a room alias name with a prefix.
     *
     * @param prefix the alias name prefix.
     * @return the suggested alias name.
     */
    fun createRoomAliasName(prefix: String): String {
        return prefix.trim().replace("[^a-zA-Z0-9]".toRegex(), "") + getRandomString(10)
    }

    /**
     * Create a room alias with a prefix.
     *
     * @param session the user's session.
     * @param prefix the alias name prefix.
     * @return the suggested alias.
     */
    fun createRoomAlias(session: Session, prefix: String): String {
        return "#${createRoomAliasName(prefix)}:${getHomeServerNameFromMXIdentifier(session.myUserId)}"
    }

    /**
     * Extract the local part of the given room alias.
     *
     * @param roomAlias the room alias to parse.
     * @return the alias local part.
     */
    fun extractRoomAliasName(roomAlias: String): String {
        return roomAlias.substringAfter("#").substringBefore(":")
    }

    /**
     * Generate a random string of the given number of characters.
     *
     * @param length the random string length.
     * @return the resulting random string.
     */
    fun getRandomString(length: Int): String {
        val charPool = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        return (1..length).map { charPool.random() }.joinToString("")
    }

    private const val DISPLAY_NAME_FIRST_DELIMITER = "["
    private const val DISPLAY_NAME_SECOND_DELIMITER = "]"
    private const val DEFAULT_EMPTY_STRING = ""
}
