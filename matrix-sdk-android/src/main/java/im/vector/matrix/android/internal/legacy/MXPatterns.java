/*
 * Copyright 2018 New Vector Ltd
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

package im.vector.matrix.android.internal.legacy;

import android.support.annotation.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * This class contains pattern to match the different Matrix ids
 */
public class MXPatterns {

    private MXPatterns() {
        // Cannot be instantiated
    }

    // Note: TLD is not mandatory (localhost, IP address...)
    private static final String DOMAIN_REGEX = ":[A-Z0-9.-]+(:[0-9]{2,5})?";

    // regex pattern to find matrix user ids in a string.
    // See https://matrix.org/speculator/spec/HEAD/appendices.html#historical-user-ids
    private static final String MATRIX_USER_IDENTIFIER_REGEX = "@[A-Z0-9\\x21-\\x39\\x3B-\\x7F]+" + DOMAIN_REGEX;
    public static final Pattern PATTERN_CONTAIN_MATRIX_USER_IDENTIFIER = Pattern.compile(MATRIX_USER_IDENTIFIER_REGEX, Pattern.CASE_INSENSITIVE);

    // regex pattern to find room ids in a string.
    private static final String MATRIX_ROOM_IDENTIFIER_REGEX = "![A-Z0-9]+" + DOMAIN_REGEX;
    public static final Pattern PATTERN_CONTAIN_MATRIX_ROOM_IDENTIFIER = Pattern.compile(MATRIX_ROOM_IDENTIFIER_REGEX, Pattern.CASE_INSENSITIVE);

    // regex pattern to find room aliases in a string.
    private static final String MATRIX_ROOM_ALIAS_REGEX = "#[A-Z0-9._%#@=+-]+" + DOMAIN_REGEX;
    public static final Pattern PATTERN_CONTAIN_MATRIX_ALIAS = Pattern.compile(MATRIX_ROOM_ALIAS_REGEX, Pattern.CASE_INSENSITIVE);

    // regex pattern to find message ids in a string.
    private static final String MATRIX_EVENT_IDENTIFIER_REGEX = "\\$[A-Z0-9]+" + DOMAIN_REGEX;
    public static final Pattern PATTERN_CONTAIN_MATRIX_EVENT_IDENTIFIER = Pattern.compile(MATRIX_EVENT_IDENTIFIER_REGEX, Pattern.CASE_INSENSITIVE);

    // regex pattern to find group ids in a string.
    private static final String MATRIX_GROUP_IDENTIFIER_REGEX = "\\+[A-Z0-9=_\\-./]+" + DOMAIN_REGEX;
    public static final Pattern PATTERN_CONTAIN_MATRIX_GROUP_IDENTIFIER = Pattern.compile(MATRIX_GROUP_IDENTIFIER_REGEX, Pattern.CASE_INSENSITIVE);

    // regex pattern to find permalink with message id.
    // Android does not support in URL so extract it.
    private static final String PERMALINK_BASE_REGEX = "https://matrix\\.to/#/";
    private static final String APP_BASE_REGEX = "https://[A-Z0-9.-]+\\.[A-Z]{2,}/[A-Z]{3,}/#/room/";
    private static final String SEP_REGEX = "/";

    private static final String LINK_TO_ROOM_ID_REGEXP = PERMALINK_BASE_REGEX + MATRIX_ROOM_IDENTIFIER_REGEX + SEP_REGEX + MATRIX_EVENT_IDENTIFIER_REGEX;
    public static final Pattern PATTERN_CONTAIN_MATRIX_TO_PERMALINK_ROOM_ID = Pattern.compile(LINK_TO_ROOM_ID_REGEXP, Pattern.CASE_INSENSITIVE);

    private static final String LINK_TO_ROOM_ALIAS_REGEXP = PERMALINK_BASE_REGEX + MATRIX_ROOM_ALIAS_REGEX + SEP_REGEX + MATRIX_EVENT_IDENTIFIER_REGEX;
    public static final Pattern PATTERN_CONTAIN_MATRIX_TO_PERMALINK_ROOM_ALIAS = Pattern.compile(LINK_TO_ROOM_ALIAS_REGEXP, Pattern.CASE_INSENSITIVE);

    private static final String LINK_TO_APP_ROOM_ID_REGEXP = APP_BASE_REGEX + MATRIX_ROOM_IDENTIFIER_REGEX + SEP_REGEX + MATRIX_EVENT_IDENTIFIER_REGEX;
    public static final Pattern PATTERN_CONTAIN_APP_LINK_PERMALINK_ROOM_ID = Pattern.compile(LINK_TO_APP_ROOM_ID_REGEXP, Pattern.CASE_INSENSITIVE);

    private static final String LINK_TO_APP_ROOM_ALIAS_REGEXP = APP_BASE_REGEX + MATRIX_ROOM_ALIAS_REGEX + SEP_REGEX + MATRIX_EVENT_IDENTIFIER_REGEX;
    public static final Pattern PATTERN_CONTAIN_APP_LINK_PERMALINK_ROOM_ALIAS = Pattern.compile(LINK_TO_APP_ROOM_ALIAS_REGEXP, Pattern.CASE_INSENSITIVE);

    // list of patterns to find some matrix item.
    public static final List<Pattern> MATRIX_PATTERNS = Arrays.asList(
            MXPatterns.PATTERN_CONTAIN_MATRIX_TO_PERMALINK_ROOM_ID,
            MXPatterns.PATTERN_CONTAIN_MATRIX_TO_PERMALINK_ROOM_ALIAS,
            MXPatterns.PATTERN_CONTAIN_APP_LINK_PERMALINK_ROOM_ID,
            MXPatterns.PATTERN_CONTAIN_APP_LINK_PERMALINK_ROOM_ALIAS,
            MXPatterns.PATTERN_CONTAIN_MATRIX_USER_IDENTIFIER,
            MXPatterns.PATTERN_CONTAIN_MATRIX_ALIAS,
            MXPatterns.PATTERN_CONTAIN_MATRIX_ROOM_IDENTIFIER,
            MXPatterns.PATTERN_CONTAIN_MATRIX_EVENT_IDENTIFIER,
            MXPatterns.PATTERN_CONTAIN_MATRIX_GROUP_IDENTIFIER
    );

    /**
     * Tells if a string is a valid user Id.
     *
     * @param str the string to test
     * @return true if the string is a valid user id
     */
    public static boolean isUserId(@Nullable final String str) {
        return str != null && PATTERN_CONTAIN_MATRIX_USER_IDENTIFIER.matcher(str).matches();
    }

    /**
     * Tells if a string is a valid room id.
     *
     * @param str the string to test
     * @return true if the string is a valid room Id
     */
    public static boolean isRoomId(@Nullable final String str) {
        return str != null && PATTERN_CONTAIN_MATRIX_ROOM_IDENTIFIER.matcher(str).matches();
    }

    /**
     * Tells if a string is a valid room alias.
     *
     * @param str the string to test
     * @return true if the string is a valid room alias.
     */
    public static boolean isRoomAlias(@Nullable final String str) {
        return str != null && PATTERN_CONTAIN_MATRIX_ALIAS.matcher(str).matches();
    }

    /**
     * Tells if a string is a valid event id.
     *
     * @param str the string to test
     * @return true if the string is a valid event id.
     */
    public static boolean isEventId(@Nullable final String str) {
        return str != null && PATTERN_CONTAIN_MATRIX_EVENT_IDENTIFIER.matcher(str).matches();
    }

    /**
     * Tells if a string is a valid group id.
     *
     * @param str the string to test
     * @return true if the string is a valid group id.
     */
    public static boolean isGroupId(@Nullable final String str) {
        return str != null && PATTERN_CONTAIN_MATRIX_GROUP_IDENTIFIER.matcher(str).matches();
    }
}
