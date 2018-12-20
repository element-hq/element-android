package im.vector.matrix.android.api

import java.util.*
import java.util.regex.Pattern

/**
 * This class contains pattern to match the different Matrix ids
 */
object MatrixPatterns {

    // Note: TLD is not mandatory (localhost, IP address...)
    private val DOMAIN_REGEX = ":[A-Z0-9.-]+(:[0-9]{2,5})?"

    // regex pattern to find matrix user ids in a string.
    // See https://matrix.org/speculator/spec/HEAD/appendices.html#historical-user-ids
    private val MATRIX_USER_IDENTIFIER_REGEX = "@[A-Z0-9\\x21-\\x39\\x3B-\\x7F]+$DOMAIN_REGEX"
    val PATTERN_CONTAIN_MATRIX_USER_IDENTIFIER = Pattern.compile(MATRIX_USER_IDENTIFIER_REGEX, Pattern.CASE_INSENSITIVE)

    // regex pattern to find room ids in a string.
    private val MATRIX_ROOM_IDENTIFIER_REGEX = "![A-Z0-9]+$DOMAIN_REGEX"
    val PATTERN_CONTAIN_MATRIX_ROOM_IDENTIFIER = Pattern.compile(MATRIX_ROOM_IDENTIFIER_REGEX, Pattern.CASE_INSENSITIVE)

    // regex pattern to find room aliases in a string.
    private val MATRIX_ROOM_ALIAS_REGEX = "#[A-Z0-9._%#@=+-]+$DOMAIN_REGEX"
    val PATTERN_CONTAIN_MATRIX_ALIAS = Pattern.compile(MATRIX_ROOM_ALIAS_REGEX, Pattern.CASE_INSENSITIVE)

    // regex pattern to find message ids in a string.
    private val MATRIX_EVENT_IDENTIFIER_REGEX = "\\$[A-Z0-9]+$DOMAIN_REGEX"
    val PATTERN_CONTAIN_MATRIX_EVENT_IDENTIFIER = Pattern.compile(MATRIX_EVENT_IDENTIFIER_REGEX, Pattern.CASE_INSENSITIVE)

    // regex pattern to find group ids in a string.
    private val MATRIX_GROUP_IDENTIFIER_REGEX = "\\+[A-Z0-9=_\\-./]+$DOMAIN_REGEX"
    val PATTERN_CONTAIN_MATRIX_GROUP_IDENTIFIER = Pattern.compile(MATRIX_GROUP_IDENTIFIER_REGEX, Pattern.CASE_INSENSITIVE)

    // regex pattern to find permalink with message id.
    // Android does not support in URL so extract it.
    private val PERMALINK_BASE_REGEX = "https://matrix\\.to/#/"
    private val APP_BASE_REGEX = "https://[A-Z0-9.-]+\\.[A-Z]{2,}/[A-Z]{3,}/#/room/"
    val SEP_REGEX = "/"

    private val LINK_TO_ROOM_ID_REGEXP = PERMALINK_BASE_REGEX + MATRIX_ROOM_IDENTIFIER_REGEX + SEP_REGEX + MATRIX_EVENT_IDENTIFIER_REGEX
    val PATTERN_CONTAIN_MATRIX_TO_PERMALINK_ROOM_ID = Pattern.compile(LINK_TO_ROOM_ID_REGEXP, Pattern.CASE_INSENSITIVE)

    private val LINK_TO_ROOM_ALIAS_REGEXP = PERMALINK_BASE_REGEX + MATRIX_ROOM_ALIAS_REGEX + SEP_REGEX + MATRIX_EVENT_IDENTIFIER_REGEX
    val PATTERN_CONTAIN_MATRIX_TO_PERMALINK_ROOM_ALIAS = Pattern.compile(LINK_TO_ROOM_ALIAS_REGEXP, Pattern.CASE_INSENSITIVE)

    private val LINK_TO_APP_ROOM_ID_REGEXP = APP_BASE_REGEX + MATRIX_ROOM_IDENTIFIER_REGEX + SEP_REGEX + MATRIX_EVENT_IDENTIFIER_REGEX
    val PATTERN_CONTAIN_APP_LINK_PERMALINK_ROOM_ID = Pattern.compile(LINK_TO_APP_ROOM_ID_REGEXP, Pattern.CASE_INSENSITIVE)

    private val LINK_TO_APP_ROOM_ALIAS_REGEXP = APP_BASE_REGEX + MATRIX_ROOM_ALIAS_REGEX + SEP_REGEX + MATRIX_EVENT_IDENTIFIER_REGEX
    val PATTERN_CONTAIN_APP_LINK_PERMALINK_ROOM_ALIAS = Pattern.compile(LINK_TO_APP_ROOM_ALIAS_REGEXP, Pattern.CASE_INSENSITIVE)

    // list of patterns to find some matrix item.
    val MATRIX_PATTERNS = Arrays.asList(
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
        return str != null && PATTERN_CONTAIN_MATRIX_USER_IDENTIFIER.matcher(str).matches()
    }

    /**
     * Tells if a string is a valid room id.
     *
     * @param str the string to test
     * @return true if the string is a valid room Id
     */
    fun isRoomId(str: String?): Boolean {
        return str != null && PATTERN_CONTAIN_MATRIX_ROOM_IDENTIFIER.matcher(str).matches()
    }

    /**
     * Tells if a string is a valid room alias.
     *
     * @param str the string to test
     * @return true if the string is a valid room alias.
     */
    fun isRoomAlias(str: String?): Boolean {
        return str != null && PATTERN_CONTAIN_MATRIX_ALIAS.matcher(str).matches()
    }

    /**
     * Tells if a string is a valid event id.
     *
     * @param str the string to test
     * @return true if the string is a valid event id.
     */
    fun isEventId(str: String?): Boolean {
        return str != null && PATTERN_CONTAIN_MATRIX_EVENT_IDENTIFIER.matcher(str).matches()
    }

    /**
     * Tells if a string is a valid group id.
     *
     * @param str the string to test
     * @return true if the string is a valid group id.
     */
    fun isGroupId(str: String?): Boolean {
        return str != null && PATTERN_CONTAIN_MATRIX_GROUP_IDENTIFIER.matcher(str).matches()
    }
}// Cannot be instantiated
