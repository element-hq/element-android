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

package org.matrix.android.sdk.api

import org.matrix.android.sdk.BuildConfig

/**
 * This class contains pattern to match the different Matrix ids
 */
object MatrixPatterns {

    // Note: TLD is not mandatory (localhost, IP address...)
    private const val DOMAIN_REGEX = ":[A-Z0-9.-]+(:[0-9]{2,5})?"

    // regex pattern to find matrix user ids in a string.
    // See https://matrix.org/speculator/spec/HEAD/appendices.html#historical-user-ids
    private const val MATRIX_USER_IDENTIFIER_REGEX = "@[A-Z0-9\\x21-\\x39\\x3B-\\x7F]+$DOMAIN_REGEX"
    val PATTERN_CONTAIN_MATRIX_USER_IDENTIFIER = MATRIX_USER_IDENTIFIER_REGEX.toRegex(RegexOption.IGNORE_CASE)

    // regex pattern to find room ids in a string.
    private const val MATRIX_ROOM_IDENTIFIER_REGEX = "![A-Z0-9]+$DOMAIN_REGEX"
    private val PATTERN_CONTAIN_MATRIX_ROOM_IDENTIFIER = MATRIX_ROOM_IDENTIFIER_REGEX.toRegex(RegexOption.IGNORE_CASE)

    // regex pattern to find room aliases in a string.
    private const val MATRIX_ROOM_ALIAS_REGEX = "#[A-Z0-9._%#@=+-]+$DOMAIN_REGEX"
    private val PATTERN_CONTAIN_MATRIX_ALIAS = MATRIX_ROOM_ALIAS_REGEX.toRegex(RegexOption.IGNORE_CASE)

    // regex pattern to find message ids in a string.
    private const val MATRIX_EVENT_IDENTIFIER_REGEX = "\\$[A-Z0-9]+$DOMAIN_REGEX"
    private val PATTERN_CONTAIN_MATRIX_EVENT_IDENTIFIER = MATRIX_EVENT_IDENTIFIER_REGEX.toRegex(RegexOption.IGNORE_CASE)

    // regex pattern to find message ids in a string.
    private const val MATRIX_EVENT_IDENTIFIER_V3_REGEX = "\\$[A-Z0-9/+]+"
    private val PATTERN_CONTAIN_MATRIX_EVENT_IDENTIFIER_V3 = MATRIX_EVENT_IDENTIFIER_V3_REGEX.toRegex(RegexOption.IGNORE_CASE)

    // Ref: https://matrix.org/docs/spec/rooms/v4#event-ids
    private const val MATRIX_EVENT_IDENTIFIER_V4_REGEX = "\\$[A-Z0-9\\-_]+"
    private val PATTERN_CONTAIN_MATRIX_EVENT_IDENTIFIER_V4 = MATRIX_EVENT_IDENTIFIER_V4_REGEX.toRegex(RegexOption.IGNORE_CASE)

    // regex pattern to find group ids in a string.
    private const val MATRIX_GROUP_IDENTIFIER_REGEX = "\\+[A-Z0-9=_\\-./]+$DOMAIN_REGEX"
    private val PATTERN_CONTAIN_MATRIX_GROUP_IDENTIFIER = MATRIX_GROUP_IDENTIFIER_REGEX.toRegex(RegexOption.IGNORE_CASE)

    // regex pattern to find permalink with message id.
    // Android does not support in URL so extract it.
    private const val PERMALINK_BASE_REGEX = "https://matrix\\.to/#/"
    private const val APP_BASE_REGEX = "https://[A-Z0-9.-]+\\.[A-Z]{2,}/[A-Z]{3,}/#/room/"
    const val SEP_REGEX = "/"

    private const val LINK_TO_ROOM_ID_REGEXP = PERMALINK_BASE_REGEX + MATRIX_ROOM_IDENTIFIER_REGEX + SEP_REGEX + MATRIX_EVENT_IDENTIFIER_REGEX
    private val PATTERN_CONTAIN_MATRIX_TO_PERMALINK_ROOM_ID = LINK_TO_ROOM_ID_REGEXP.toRegex(RegexOption.IGNORE_CASE)

    private const val LINK_TO_ROOM_ALIAS_REGEXP = PERMALINK_BASE_REGEX + MATRIX_ROOM_ALIAS_REGEX + SEP_REGEX + MATRIX_EVENT_IDENTIFIER_REGEX
    private val PATTERN_CONTAIN_MATRIX_TO_PERMALINK_ROOM_ALIAS = LINK_TO_ROOM_ALIAS_REGEXP.toRegex(RegexOption.IGNORE_CASE)

    private const val LINK_TO_APP_ROOM_ID_REGEXP = APP_BASE_REGEX + MATRIX_ROOM_IDENTIFIER_REGEX + SEP_REGEX + MATRIX_EVENT_IDENTIFIER_REGEX
    private val PATTERN_CONTAIN_APP_LINK_PERMALINK_ROOM_ID = LINK_TO_APP_ROOM_ID_REGEXP.toRegex(RegexOption.IGNORE_CASE)

    private const val LINK_TO_APP_ROOM_ALIAS_REGEXP = APP_BASE_REGEX + MATRIX_ROOM_ALIAS_REGEX + SEP_REGEX + MATRIX_EVENT_IDENTIFIER_REGEX
    private val PATTERN_CONTAIN_APP_LINK_PERMALINK_ROOM_ALIAS = LINK_TO_APP_ROOM_ALIAS_REGEXP.toRegex(RegexOption.IGNORE_CASE)

    // ascii characters in the range \x20 (space) to \x7E (~)
    val ORDER_STRING_REGEX = "[ -~]+".toRegex()

    // list of patterns to find some matrix item.
    val MATRIX_PATTERNS = listOf(
            PATTERN_CONTAIN_MATRIX_TO_PERMALINK_ROOM_ID,
            PATTERN_CONTAIN_MATRIX_TO_PERMALINK_ROOM_ALIAS,
            PATTERN_CONTAIN_APP_LINK_PERMALINK_ROOM_ID,
            PATTERN_CONTAIN_APP_LINK_PERMALINK_ROOM_ALIAS,
            PATTERN_CONTAIN_MATRIX_USER_IDENTIFIER,
            PATTERN_CONTAIN_MATRIX_ALIAS,
            PATTERN_CONTAIN_MATRIX_ROOM_IDENTIFIER,
            PATTERN_CONTAIN_MATRIX_EVENT_IDENTIFIER,
            PATTERN_CONTAIN_MATRIX_GROUP_IDENTIFIER
    )

    /**
     * Tells if a string is a valid user Id.
     *
     * @param str the string to test
     * @return true if the string is a valid user id
     */
    fun isUserId(str: String?): Boolean {
        return str != null && str matches PATTERN_CONTAIN_MATRIX_USER_IDENTIFIER
    }

    /**
     * Tells if a string is a valid room id.
     *
     * @param str the string to test
     * @return true if the string is a valid room Id
     */
    fun isRoomId(str: String?): Boolean {
        return str != null && str matches PATTERN_CONTAIN_MATRIX_ROOM_IDENTIFIER
    }

    /**
     * Tells if a string is a valid room alias.
     *
     * @param str the string to test
     * @return true if the string is a valid room alias.
     */
    fun isRoomAlias(str: String?): Boolean {
        return str != null && str matches PATTERN_CONTAIN_MATRIX_ALIAS
    }

    /**
     * Tells if a string is a valid event id.
     *
     * @param str the string to test
     * @return true if the string is a valid event id.
     */
    fun isEventId(str: String?): Boolean {
        return str != null
                && (str matches PATTERN_CONTAIN_MATRIX_EVENT_IDENTIFIER
                || str matches PATTERN_CONTAIN_MATRIX_EVENT_IDENTIFIER_V3
                || str matches PATTERN_CONTAIN_MATRIX_EVENT_IDENTIFIER_V4)
    }

    /**
     * Tells if a string is a valid group id.
     *
     * @param str the string to test
     * @return true if the string is a valid group id.
     */
    fun isGroupId(str: String?): Boolean {
        return str != null && str matches PATTERN_CONTAIN_MATRIX_GROUP_IDENTIFIER
    }

    /**
     * Extract server name from a matrix id
     *
     * @param matrixId
     * @return null if not found or if matrixId is null
     */
    fun extractServerNameFromId(matrixId: String?): String? {
        return matrixId?.substringAfter(":", missingDelimiterValue = "")?.takeIf { it.isNotEmpty() }
    }

    /**
     * Orders which are not strings, or do not consist solely of ascii characters in the range \x20 (space) to \x7E (~),
     * or consist of more than 50 characters, are forbidden and the field should be ignored if received.
     */
    fun isValidOrderString(order: String?): Boolean {
        return order != null && order.length < 50 && order matches ORDER_STRING_REGEX
    }

    fun candidateAliasFromRoomName(name: String): String {
        return Regex("\\s").replace(name.lowercase(), "_").let {
            "[^a-z0-9._%#@=+-]".toRegex().replace(it, "")
        }
    }

    /**
     * Return the domain form a userId
     * Examples:
     * - "@alice:domain.org".getDomain() will return "domain.org"
     * - "@bob:domain.org:3455".getDomain() will return "domain.org:3455"
     */
    fun String.getDomain(): String {
        if (BuildConfig.DEBUG) {
            assert(isUserId(this))
        }
        return substringAfter(":")
    }
}
