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
enum class Command(val command: String, val parameters: String, @StringRes val description: Int, val isDevCommand: Boolean) {
    EMOTE("/me", "<message>", R.string.command_description_emote, false),
    BAN_USER("/ban", "<user-id> [reason]", R.string.command_description_ban_user, false),
    UNBAN_USER("/unban", "<user-id> [reason]", R.string.command_description_unban_user, false),
    SET_USER_POWER_LEVEL("/op", "<user-id> [<power-level>]", R.string.command_description_op_user, false),
    RESET_USER_POWER_LEVEL("/deop", "<user-id>", R.string.command_description_deop_user, false),
    INVITE("/invite", "<user-id> [reason]", R.string.command_description_invite_user, false),
    JOIN_ROOM("/join", "<room-alias> [reason]", R.string.command_description_join_room, false),
    PART("/part", "<room-alias> [reason]", R.string.command_description_part_room, false),
    TOPIC("/topic", "<topic>", R.string.command_description_topic, false),
    KICK_USER("/kick", "<user-id> [reason]", R.string.command_description_kick_user, false),
    CHANGE_DISPLAY_NAME("/nick", "<display-name>", R.string.command_description_nick, false),
    MARKDOWN("/markdown", "<on|off>", R.string.command_description_markdown, false),
    RAINBOW("/rainbow", "<message>", R.string.command_description_rainbow, false),
    RAINBOW_EMOTE("/rainbowme", "<message>", R.string.command_description_rainbow_emote, false),
    CLEAR_SCALAR_TOKEN("/clear_scalar_token", "", R.string.command_description_clear_scalar_token, false),
    SPOILER("/spoiler", "<message>", R.string.command_description_spoiler, false),
    POLL("/poll", "Question | Option 1 | Option 2 ...", R.string.command_description_poll, false),
    SHRUG("/shrug", "<message>", R.string.command_description_shrug, false),
    PLAIN("/plain", "<message>", R.string.command_description_plain, false),
    DISCARD_SESSION("/discardsession", "", R.string.command_description_discard_session, false),
    CONFETTI("/confetti", "<message>", R.string.command_confetti, false),
    SNOWFALL("/snowfall", "<message>", R.string.command_snow, false),
    CREATE_SPACE("/createspace", "<name> <invitee>*", R.string.command_description_create_space,  true),
    ADD_TO_SPACE("/addToSpace", "spaceId", R.string.command_description_create_space, true),
    JOIN_SPACE("/joinSpace", "spaceId", R.string.command_description_join_space, true),
    LEAVE_ROOM("/leave", "<roomId?>", R.string.command_description_leave_room, true),
    UPGRADE_ROOM("/upgraderoom", "newVersion", R.string.command_description_upgrade_room, true);

    val length
        get() = command.length + 1
}
