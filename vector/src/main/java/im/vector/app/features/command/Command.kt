/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.command

import androidx.annotation.StringRes
import im.vector.lib.strings.CommonStrings

/**
 * Defines the command line operations.
 * The user can write theses messages to perform some actions.
 * The list will be displayed in this order.
 */
enum class Command(
        val command: String,
        val aliases: Array<CharSequence>?,
        val parameters: String,
        @StringRes val description: Int,
        val isDevCommand: Boolean,
        val isThreadCommand: Boolean
) {
    CRASH_APP("/crash", null, "", CommonStrings.command_description_crash_application, true, true),
    EMOTE("/me", null, "<message>", CommonStrings.command_description_emote, false, true),
    BAN_USER("/ban", null, "<user-id> [reason]", CommonStrings.command_description_ban_user, false, false),
    UNBAN_USER("/unban", null, "<user-id> [reason]", CommonStrings.command_description_unban_user, false, false),
    IGNORE_USER("/ignore", null, "<user-id> [reason]", CommonStrings.command_description_ignore_user, false, true),
    UNIGNORE_USER("/unignore", null, "<user-id>", CommonStrings.command_description_unignore_user, false, true),
    SET_USER_POWER_LEVEL("/op", null, "<user-id> [<power-level>]", CommonStrings.command_description_op_user, false, false),
    RESET_USER_POWER_LEVEL("/deop", null, "<user-id>", CommonStrings.command_description_deop_user, false, false),
    ROOM_NAME("/roomname", null, "<name>", CommonStrings.command_description_room_name, false, false),
    INVITE("/invite", null, "<user-id> [reason]", CommonStrings.command_description_invite_user, false, false),
    JOIN_ROOM("/join", arrayOf("/j", "/goto"), "<room-address> [reason]", CommonStrings.command_description_join_room, false, false),
    PART("/part", null, "[<room-address>]", CommonStrings.command_description_part_room, false, false),
    TOPIC("/topic", null, "<topic>", CommonStrings.command_description_topic, false, false),
    REMOVE_USER("/remove", arrayOf("/kick"), "<user-id> [reason]", CommonStrings.command_description_remove_user, false, false),
    CHANGE_DISPLAY_NAME("/nick", null, "<display-name>", CommonStrings.command_description_nick, false, false),
    CHANGE_DISPLAY_NAME_FOR_ROOM("/myroomnick", arrayOf("/roomnick"), "<display-name>", CommonStrings.command_description_nick_for_room, false, false),
    ROOM_AVATAR("/roomavatar", null, "<mxc_url>", CommonStrings.command_description_room_avatar, true /* User has to know the mxc url */, false),
    CHANGE_AVATAR_FOR_ROOM(
            "/myroomavatar", null, "<mxc_url>",
            CommonStrings.command_description_avatar_for_room, true /* User has to know the mxc url */, false
    ),
    MARKDOWN("/markdown", null, "<on|off>", CommonStrings.command_description_markdown, false, false),
    RAINBOW("/rainbow", null, "<message>", CommonStrings.command_description_rainbow, false, true),
    RAINBOW_EMOTE("/rainbowme", null, "<message>", CommonStrings.command_description_rainbow_emote, false, true),
    DEVTOOLS("/devtools", null, "", CommonStrings.command_description_devtools, true, false),
    CLEAR_SCALAR_TOKEN("/clear_scalar_token", null, "", CommonStrings.command_description_clear_scalar_token, false, false),
    SPOILER("/spoiler", null, "<message>", CommonStrings.command_description_spoiler, false, true),
    SHRUG("/shrug", null, "<message>", CommonStrings.command_description_shrug, false, true),
    LENNY("/lenny", null, "<message>", CommonStrings.command_description_lenny, false, true),
    PLAIN("/plain", null, "<message>", CommonStrings.command_description_plain, false, true),
    WHOIS("/whois", null, "<user-id>", CommonStrings.command_description_whois, false, true),
    DISCARD_SESSION("/discardsession", null, "", CommonStrings.command_description_discard_session, false, false),
    CONFETTI("/confetti", null, "<message>", CommonStrings.command_confetti, false, false),
    SNOWFALL("/snowfall", null, "<message>", CommonStrings.command_snow, false, false),
    CREATE_SPACE("/createspace", null, "<name> <invitee>*", CommonStrings.command_description_create_space, true, false),
    ADD_TO_SPACE("/addToSpace", null, "spaceId", CommonStrings.command_description_add_to_space, true, false),
    JOIN_SPACE("/joinSpace", null, "spaceId", CommonStrings.command_description_join_space, true, false),
    LEAVE_ROOM("/leave", null, "<roomId?>", CommonStrings.command_description_leave_room, true, false),
    UPGRADE_ROOM("/upgraderoom", null, "newVersion", CommonStrings.command_description_upgrade_room, true, false),
    TABLE_FLIP("/tableflip", null, "<message>", CommonStrings.command_description_table_flip, false, true);

    val allAliases = arrayOf(command, *aliases.orEmpty())

    fun matches(inputCommand: CharSequence) = allAliases.any { it.contentEquals(inputCommand, true) }

    fun startsWith(input: CharSequence) =
            allAliases.any { it.startsWith(input, 1, true) }
}
