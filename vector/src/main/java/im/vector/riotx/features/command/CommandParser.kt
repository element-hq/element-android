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

package im.vector.riotx.features.command

import im.vector.matrix.android.api.MatrixPatterns
import timber.log.Timber

object CommandParser {

    /**
     * Convert the text message into a Slash command.
     *
     * @param textMessage   the text message
     * @return a parsed slash command (ok or error)
     */
    fun parseSplashCommand(textMessage: CharSequence): ParsedCommand {
        // check if it has the Slash marker
        if (!textMessage.startsWith("/")) {
            return ParsedCommand.ErrorNotACommand
        } else {
            Timber.v("parseSplashCommand")

            // "/" only
            if (textMessage.length == 1) {
                return ParsedCommand.ErrorEmptySlashCommand
            }

            // Exclude "//"
            if ("/" == textMessage.substring(1, 2)) {
                return ParsedCommand.ErrorNotACommand
            }

            val messageParts = try {
                textMessage.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }
            } catch (e: Exception) {
                Timber.e(e, "## manageSplashCommand() : split failed")
                null
            }

            // test if the string cut fails
            if (messageParts.isNullOrEmpty()) {
                return ParsedCommand.ErrorEmptySlashCommand
            }

            return when (val slashCommand = messageParts.first()) {
                Command.CHANGE_DISPLAY_NAME.command    -> {
                    val newDisplayName = textMessage.substring(Command.CHANGE_DISPLAY_NAME.command.length).trim()

                    if (newDisplayName.isNotEmpty()) {
                        ParsedCommand.ChangeDisplayName(newDisplayName)
                    } else {
                        ParsedCommand.ErrorSyntax(Command.CHANGE_DISPLAY_NAME)
                    }
                }
                Command.TOPIC.command                  -> {
                    val newTopic = textMessage.substring(Command.TOPIC.command.length).trim()

                    if (newTopic.isNotEmpty()) {
                        ParsedCommand.ChangeTopic(newTopic)
                    } else {
                        ParsedCommand.ErrorSyntax(Command.TOPIC)
                    }
                }
                Command.EMOTE.command                  -> {
                    val message = textMessage.subSequence(Command.EMOTE.command.length, textMessage.length).trim()

                    ParsedCommand.SendEmote(message)
                }
                Command.JOIN_ROOM.command              -> {
                    val roomAlias = textMessage.substring(Command.JOIN_ROOM.command.length).trim()

                    if (roomAlias.isNotEmpty()) {
                        ParsedCommand.JoinRoom(roomAlias)
                    } else {
                        ParsedCommand.ErrorSyntax(Command.JOIN_ROOM)
                    }
                }
                Command.PART.command                   -> {
                    val roomAlias = textMessage.substring(Command.PART.command.length).trim()

                    if (roomAlias.isNotEmpty()) {
                        ParsedCommand.PartRoom(roomAlias)
                    } else {
                        ParsedCommand.ErrorSyntax(Command.PART)
                    }
                }
                Command.INVITE.command                 -> {
                    if (messageParts.size == 2) {
                        val userId = messageParts[1]

                        if (MatrixPatterns.isUserId(userId)) {
                            ParsedCommand.Invite(userId)
                        } else {
                            ParsedCommand.ErrorSyntax(Command.INVITE)
                        }
                    } else {
                        ParsedCommand.ErrorSyntax(Command.INVITE)
                    }
                }
                Command.KICK_USER.command              -> {
                    if (messageParts.size >= 2) {
                        val userId = messageParts[1]
                        if (MatrixPatterns.isUserId(userId)) {
                            val reason = textMessage.substring(Command.KICK_USER.command.length
                                    + 1
                                    + userId.length).trim()

                            ParsedCommand.KickUser(userId, reason)
                        } else {
                            ParsedCommand.ErrorSyntax(Command.KICK_USER)
                        }
                    } else {
                        ParsedCommand.ErrorSyntax(Command.KICK_USER)
                    }
                }
                Command.BAN_USER.command               -> {
                    if (messageParts.size >= 2) {
                        val userId = messageParts[1]
                        if (MatrixPatterns.isUserId(userId)) {
                            val reason = textMessage.substring(Command.BAN_USER.command.length
                                    + 1
                                    + userId.length).trim()

                            ParsedCommand.BanUser(userId, reason)
                        } else {
                            ParsedCommand.ErrorSyntax(Command.BAN_USER)
                        }
                    } else {
                        ParsedCommand.ErrorSyntax(Command.BAN_USER)
                    }
                }
                Command.UNBAN_USER.command             -> {
                    if (messageParts.size == 2) {
                        val userId = messageParts[1]

                        if (MatrixPatterns.isUserId(userId)) {
                            ParsedCommand.UnbanUser(userId)
                        } else {
                            ParsedCommand.ErrorSyntax(Command.UNBAN_USER)
                        }
                    } else {
                        ParsedCommand.ErrorSyntax(Command.UNBAN_USER)
                    }
                }
                Command.SET_USER_POWER_LEVEL.command   -> {
                    if (messageParts.size == 3) {
                        val userId = messageParts[1]
                        if (MatrixPatterns.isUserId(userId)) {
                            val powerLevelsAsString = messageParts[2]

                            try {
                                val powerLevelsAsInt = Integer.parseInt(powerLevelsAsString)

                                ParsedCommand.SetUserPowerLevel(userId, powerLevelsAsInt)
                            } catch (e: Exception) {
                                ParsedCommand.ErrorSyntax(Command.SET_USER_POWER_LEVEL)
                            }
                        } else {
                            ParsedCommand.ErrorSyntax(Command.SET_USER_POWER_LEVEL)
                        }
                    } else {
                        ParsedCommand.ErrorSyntax(Command.SET_USER_POWER_LEVEL)
                    }
                }
                Command.RESET_USER_POWER_LEVEL.command -> {
                    if (messageParts.size == 2) {
                        val userId = messageParts[1]

                        if (MatrixPatterns.isUserId(userId)) {
                            ParsedCommand.SetUserPowerLevel(userId, 0)
                        } else {
                            ParsedCommand.ErrorSyntax(Command.SET_USER_POWER_LEVEL)
                        }
                    } else {
                        ParsedCommand.ErrorSyntax(Command.SET_USER_POWER_LEVEL)
                    }
                }
                Command.MARKDOWN.command               -> {
                    if (messageParts.size == 2) {
                        when {
                            "on".equals(messageParts[1], true)  -> ParsedCommand.SetMarkdown(true)
                            "off".equals(messageParts[1], true) -> ParsedCommand.SetMarkdown(false)
                            else                                -> ParsedCommand.ErrorSyntax(Command.MARKDOWN)
                        }
                    } else {
                        ParsedCommand.ErrorSyntax(Command.MARKDOWN)
                    }
                }
                Command.CLEAR_SCALAR_TOKEN.command     -> {
                    if (messageParts.size == 1) {
                        ParsedCommand.ClearScalarToken
                    } else {
                        ParsedCommand.ErrorSyntax(Command.CLEAR_SCALAR_TOKEN)
                    }
                }
                Command.SPOILER.command                -> {
                    val message = textMessage.substring(Command.SPOILER.command.length).trim()

                    ParsedCommand.SendSpoiler(message)
                }
                else                                   -> {
                    // Unknown command
                    ParsedCommand.ErrorUnknownSlashCommand(slashCommand)
                }
            }
        }
    }
}
