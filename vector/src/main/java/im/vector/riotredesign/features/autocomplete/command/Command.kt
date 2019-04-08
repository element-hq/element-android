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

package im.vector.riotredesign.features.autocomplete.command

import androidx.annotation.StringRes
import im.vector.riotredesign.R

enum class Command(val command: String, val parameters: String, @StringRes val description: Int) {
    EMOTE("/me", "<message>", R.string.command_description_emote),
    BAN_USER("/ban", "<user-id> [reason]", R.string.command_description_ban_user),
    UNBAN_USER("/unban", "<user-id>", R.string.command_description_unban_user),
    SET_USER_POWER_LEVEL("/op", "<user-id> [<power-level>]", R.string.command_description_op_user),
    RESET_USER_POWER_LEVEL("/deop", "<user-id>", R.string.command_description_deop_user),
    INVITE("/invite", "<user-id>", R.string.command_description_invite_user),
    JOIN_ROOM("/join", "<room-alias>", R.string.command_description_join_room),
    PART("/part", "<room-alias>", R.string.command_description_part_room),
    TOPIC("/topic", "<topic>", R.string.command_description_topic),
    KICK_USER("/kick", "<user-id> [reason]", R.string.command_description_kick_user),
    CHANGE_DISPLAY_NAME("/nick", "<display-name>", R.string.command_description_nick),
    MARKDOWN("/markdown", "<on|off>", R.string.command_description_markdown),
    CLEAR_SCALAR_TOKEN("/clear_scalar_token", "", R.string.command_description_clear_scalar_token);
}