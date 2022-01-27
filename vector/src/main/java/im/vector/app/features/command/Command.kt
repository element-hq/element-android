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

package im.vector.app.features.command

import androidx.annotation.StringRes
import im.vector.app.R

/**
 * Defines the command line operations
 * the user can write theses messages to perform some actions
 * the list will be displayed in this order
 */
enum class Command(val command: String,
                   val aliases: Array<CharSequence>?,
                   val parameters: String,
                   @StringRes val description: Int,
                   val isDevCommand: Boolean,
                   val isThreadCommand: Boolean) {
    EMOTE("/me", null, "<message>", R.string.command_description_emote, false, true),
    BAN_USER("/ban", null, "<user-id> [reason]", R.string.command_description_ban_user, false, false),
    UNBAN_USER("/unban", null, "<user-id> [reason]", R.string.command_description_unban_user, false, false),
    IGNORE_USER("/ignore", null, "<user-id> [reason]", R.string.command_description_ignore_user, false, true),
    UNIGNORE_USER("/unignore", null, "<user-id>", R.string.command_description_unignore_user, false, true),
    SET_USER_POWER_LEVEL("/op", null, "<user-id> [<power-level>]", R.string.command_description_op_user, false, false),
    RESET_USER_POWER_LEVEL("/deop", null, "<user-id>", R.string.command_description_deop_user, false, false),
    ROOM_NAME("/roomname", null, "<name>", R.string.command_description_room_name, false, false),
    INVITE("/invite", null, "<user-id> [reason]", R.string.command_description_invite_user, false, false),
    JOIN_ROOM("/join", arrayOf("/j", "/goto"), "<room-address> [reason]", R.string.command_description_join_room, false, false),
    PART("/part", null, "[<room-address>]", R.string.command_description_part_room, false, false),
    TOPIC("/topic", null, "<topic>", R.string.command_description_topic, false, false),
    REMOVE_USER("/remove", arrayOf("/kick"), "<user-id> [reason]", R.string.command_description_remove_user, false, false),
    CHANGE_DISPLAY_NAME("/nick", null, "<display-name>", R.string.command_description_nick, false, false),
    CHANGE_DISPLAY_NAME_FOR_ROOM("/myroomnick", arrayOf("/roomnick"), "<display-name>", R.string.command_description_nick_for_room, false, false),
    ROOM_AVATAR("/roomavatar", null, "<mxc_url>", R.string.command_description_room_avatar, true /* User has to know the mxc url */, false),
    CHANGE_AVATAR_FOR_ROOM("/myroomavatar", null, "<mxc_url>", R.string.command_description_avatar_for_room, true /* User has to know the mxc url */, false),
    MARKDOWN("/markdown", null, "<on|off>", R.string.command_description_markdown, false, false),
    RAINBOW("/rainbow", null, "<message>", R.string.command_description_rainbow, false, true),
    RAINBOW_EMOTE("/rainbowme", null, "<message>", R.string.command_description_rainbow_emote, false, true),
    CLEAR_SCALAR_TOKEN("/clear_scalar_token", null, "", R.string.command_description_clear_scalar_token, false, false),
    SPOILER("/spoiler", null, "<message>", R.string.command_description_spoiler, false, true),
    SHRUG("/shrug", null, "<message>", R.string.command_description_shrug, false, true),
    LENNY("/lenny", null, "<message>", R.string.command_description_lenny, false, true),
    PLAIN("/plain", null, "<message>", R.string.command_description_plain, false, true),
    WHOIS("/whois", null, "<user-id>", R.string.command_description_whois, false, true),
    DISCARD_SESSION("/discardsession", null, "", R.string.command_description_discard_session, false, false),
    CONFETTI("/confetti", null, "<message>", R.string.command_confetti, false, false),
    SNOWFALL("/snowfall", null, "<message>", R.string.command_snow, false, false),
    CREATE_SPACE("/createspace", null, "<name> <invitee>*", R.string.command_description_create_space, true, false),
    ADD_TO_SPACE("/addToSpace", null, "spaceId", R.string.command_description_add_to_space, true, false),
    JOIN_SPACE("/joinSpace", null, "spaceId", R.string.command_description_join_space, true, false),
    LEAVE_ROOM("/leave", null, "<roomId?>", R.string.command_description_leave_room, true, false),
    UPGRADE_ROOM("/upgraderoom", null, "newVersion", R.string.command_description_upgrade_room, true, false);

    val allAliases = arrayOf(command, *aliases.orEmpty())

    fun matches(inputCommand: CharSequence) = allAliases.any { it.contentEquals(inputCommand, true) }

    fun startsWith(input: CharSequence) =
            allAliases.any { it.startsWith(input, 1, true) }
}
