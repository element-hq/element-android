/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.command

import im.vector.app.core.extensions.isMsisdn
import im.vector.app.core.extensions.orEmpty
import im.vector.app.features.home.room.detail.ChatEffect
import im.vector.app.features.settings.VectorPreferences
import org.matrix.android.sdk.api.MatrixPatterns
import org.matrix.android.sdk.api.MatrixUrls.isMxcUrl
import org.matrix.android.sdk.api.extensions.isEmail
import org.matrix.android.sdk.api.session.identity.ThreePid
import timber.log.Timber
import javax.inject.Inject

class CommandParser @Inject constructor(
        private val vectorPreferences: VectorPreferences
) {

    /**
     * Convert the text message into a Slash command.
     *
     * @param textMessage the text message in plain text
     * @param formattedMessage the text messaged in HTML format
     * @param isInThreadTimeline true if the user is currently typing in a thread
     * @return a parsed slash command (ok or error)
     */
    @Suppress("NAME_SHADOWING")
    fun parseSlashCommand(textMessage: CharSequence, formattedMessage: String?, isInThreadTimeline: Boolean): ParsedCommand {
        // check if it has the Slash marker
        val message = formattedMessage ?: textMessage
        return if (!message.startsWith("/")) {
            ParsedCommand.ErrorNotACommand
        } else {
            // "/" only
            if (message.length == 1) {
                return ParsedCommand.ErrorEmptySlashCommand
            }

            // Exclude "//"
            if ("/" == message.substring(1, 2)) {
                return ParsedCommand.ErrorNotACommand
            }

            val (messageParts, message) = extractMessage(message.toString()) ?: return ParsedCommand.ErrorEmptySlashCommand
            val slashCommand = messageParts.first()

            getNotSupportedByThreads(isInThreadTimeline, slashCommand)?.let {
                return ParsedCommand.ErrorCommandNotSupportedInThreads(it)
            }

            when {
                Command.PLAIN.matches(slashCommand) -> {
                    if (message.isNotEmpty()) {
                        if (formattedMessage != null) {
                            val trimmedPlainTextMessage = extractMessage(textMessage.toString())?.second.orEmpty()
                            ParsedCommand.SendFormattedText(message = trimmedPlainTextMessage, formattedMessage = message)
                        } else {
                            ParsedCommand.SendPlainText(message = message)
                        }
                    } else {
                        ParsedCommand.ErrorSyntax(Command.PLAIN)
                    }
                }
                Command.CHANGE_DISPLAY_NAME.matches(slashCommand) -> {
                    if (message.isNotEmpty()) {
                        ParsedCommand.ChangeDisplayName(displayName = message)
                    } else {
                        ParsedCommand.ErrorSyntax(Command.CHANGE_DISPLAY_NAME)
                    }
                }
                Command.CHANGE_DISPLAY_NAME_FOR_ROOM.matches(slashCommand) -> {
                    if (message.isNotEmpty()) {
                        ParsedCommand.ChangeDisplayNameForRoom(displayName = message)
                    } else {
                        ParsedCommand.ErrorSyntax(Command.CHANGE_DISPLAY_NAME_FOR_ROOM)
                    }
                }
                Command.ROOM_AVATAR.matches(slashCommand) -> {
                    if (messageParts.size == 2) {
                        val url = messageParts[1]

                        if (url.isMxcUrl()) {
                            ParsedCommand.ChangeRoomAvatar(url)
                        } else {
                            ParsedCommand.ErrorSyntax(Command.ROOM_AVATAR)
                        }
                    } else {
                        ParsedCommand.ErrorSyntax(Command.ROOM_AVATAR)
                    }
                }
                Command.CHANGE_AVATAR_FOR_ROOM.matches(slashCommand) -> {
                    if (messageParts.size == 2) {
                        val url = messageParts[1]

                        if (url.isMxcUrl()) {
                            ParsedCommand.ChangeAvatarForRoom(url)
                        } else {
                            ParsedCommand.ErrorSyntax(Command.CHANGE_AVATAR_FOR_ROOM)
                        }
                    } else {
                        ParsedCommand.ErrorSyntax(Command.CHANGE_AVATAR_FOR_ROOM)
                    }
                }
                Command.TOPIC.matches(slashCommand) -> {
                    if (message.isNotEmpty()) {
                        ParsedCommand.ChangeTopic(topic = message)
                    } else {
                        ParsedCommand.ErrorSyntax(Command.TOPIC)
                    }
                }
                Command.EMOTE.matches(slashCommand) -> {
                    if (message.isNotEmpty()) {
                        ParsedCommand.SendEmote(message)
                    } else {
                        ParsedCommand.ErrorSyntax(Command.EMOTE)
                    }
                }
                Command.RAINBOW.matches(slashCommand) -> {
                    if (message.isNotEmpty()) {
                        ParsedCommand.SendRainbow(message)
                    } else {
                        ParsedCommand.ErrorSyntax(Command.RAINBOW)
                    }
                }
                Command.RAINBOW_EMOTE.matches(slashCommand) -> {
                    if (message.isNotEmpty()) {
                        ParsedCommand.SendRainbowEmote(message)
                    } else {
                        ParsedCommand.ErrorSyntax(Command.RAINBOW_EMOTE)
                    }
                }
                Command.JOIN_ROOM.matches(slashCommand) -> {
                    if (messageParts.size >= 2) {
                        val roomAlias = messageParts[1]

                        if (roomAlias.isNotEmpty()) {
                            ParsedCommand.JoinRoom(
                                    roomAlias,
                                    trimParts(textMessage, messageParts.take(2))
                            )
                        } else {
                            ParsedCommand.ErrorSyntax(Command.JOIN_ROOM)
                        }
                    } else {
                        ParsedCommand.ErrorSyntax(Command.JOIN_ROOM)
                    }
                }
                Command.PART.matches(slashCommand) -> {
                    when (messageParts.size) {
                        1 -> ParsedCommand.PartRoom(null)
                        2 -> ParsedCommand.PartRoom(messageParts[1])
                        else -> ParsedCommand.ErrorSyntax(Command.PART)
                    }
                }
                Command.ROOM_NAME.matches(slashCommand) -> {
                    if (message.isNotEmpty()) {
                        ParsedCommand.ChangeRoomName(name = message)
                    } else {
                        ParsedCommand.ErrorSyntax(Command.ROOM_NAME)
                    }
                }
                Command.INVITE.matches(slashCommand) -> {
                    if (messageParts.size >= 2) {
                        val userId = messageParts[1]

                        when {
                            MatrixPatterns.isUserId(userId) -> {
                                ParsedCommand.Invite(
                                        userId,
                                        trimParts(textMessage, messageParts.take(2))
                                )
                            }
                            userId.isEmail() -> {
                                ParsedCommand.Invite3Pid(ThreePid.Email(userId))
                            }
                            userId.isMsisdn() -> {
                                ParsedCommand.Invite3Pid(ThreePid.Msisdn(userId))
                            }
                            else -> {
                                ParsedCommand.ErrorSyntax(Command.INVITE)
                            }
                        }
                    } else {
                        ParsedCommand.ErrorSyntax(Command.INVITE)
                    }
                }
                Command.REMOVE_USER.matches(slashCommand) -> {
                    if (messageParts.size >= 2) {
                        val userId = messageParts[1]

                        if (MatrixPatterns.isUserId(userId)) {
                            ParsedCommand.RemoveUser(
                                    userId,
                                    trimParts(textMessage, messageParts.take(2))
                            )
                        } else {
                            ParsedCommand.ErrorSyntax(Command.REMOVE_USER)
                        }
                    } else {
                        ParsedCommand.ErrorSyntax(Command.REMOVE_USER)
                    }
                }
                Command.BAN_USER.matches(slashCommand) -> {
                    if (messageParts.size >= 2) {
                        val userId = messageParts[1]

                        if (MatrixPatterns.isUserId(userId)) {
                            ParsedCommand.BanUser(
                                    userId,
                                    trimParts(textMessage, messageParts.take(2))
                            )
                        } else {
                            ParsedCommand.ErrorSyntax(Command.BAN_USER)
                        }
                    } else {
                        ParsedCommand.ErrorSyntax(Command.BAN_USER)
                    }
                }
                Command.UNBAN_USER.matches(slashCommand) -> {
                    if (messageParts.size >= 2) {
                        val userId = messageParts[1]

                        if (MatrixPatterns.isUserId(userId)) {
                            ParsedCommand.UnbanUser(
                                    userId,
                                    trimParts(textMessage, messageParts.take(2))
                            )
                        } else {
                            ParsedCommand.ErrorSyntax(Command.UNBAN_USER)
                        }
                    } else {
                        ParsedCommand.ErrorSyntax(Command.UNBAN_USER)
                    }
                }
                Command.IGNORE_USER.matches(slashCommand) -> {
                    if (messageParts.size == 2) {
                        val userId = messageParts[1]

                        if (MatrixPatterns.isUserId(userId)) {
                            ParsedCommand.IgnoreUser(userId)
                        } else {
                            ParsedCommand.ErrorSyntax(Command.IGNORE_USER)
                        }
                    } else {
                        ParsedCommand.ErrorSyntax(Command.IGNORE_USER)
                    }
                }
                Command.UNIGNORE_USER.matches(slashCommand) -> {
                    if (messageParts.size == 2) {
                        val userId = messageParts[1]

                        if (MatrixPatterns.isUserId(userId)) {
                            ParsedCommand.UnignoreUser(userId)
                        } else {
                            ParsedCommand.ErrorSyntax(Command.UNIGNORE_USER)
                        }
                    } else {
                        ParsedCommand.ErrorSyntax(Command.UNIGNORE_USER)
                    }
                }
                Command.SET_USER_POWER_LEVEL.matches(slashCommand) -> {
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
                Command.RESET_USER_POWER_LEVEL.matches(slashCommand) -> {
                    if (messageParts.size == 2) {
                        val userId = messageParts[1]

                        if (MatrixPatterns.isUserId(userId)) {
                            ParsedCommand.SetUserPowerLevel(userId, null)
                        } else {
                            ParsedCommand.ErrorSyntax(Command.SET_USER_POWER_LEVEL)
                        }
                    } else {
                        ParsedCommand.ErrorSyntax(Command.SET_USER_POWER_LEVEL)
                    }
                }
                Command.MARKDOWN.matches(slashCommand) -> {
                    if (messageParts.size == 2) {
                        when {
                            "on".equals(messageParts[1], true) -> ParsedCommand.SetMarkdown(true)
                            "off".equals(messageParts[1], true) -> ParsedCommand.SetMarkdown(false)
                            else -> ParsedCommand.ErrorSyntax(Command.MARKDOWN)
                        }
                    } else {
                        ParsedCommand.ErrorSyntax(Command.MARKDOWN)
                    }
                }
                Command.DEVTOOLS.matches(slashCommand) -> {
                    if (messageParts.size == 1) {
                        ParsedCommand.DevTools
                    } else {
                        ParsedCommand.ErrorSyntax(Command.DEVTOOLS)
                    }
                }
                Command.CLEAR_SCALAR_TOKEN.matches(slashCommand) -> {
                    if (messageParts.size == 1) {
                        ParsedCommand.ClearScalarToken
                    } else {
                        ParsedCommand.ErrorSyntax(Command.CLEAR_SCALAR_TOKEN)
                    }
                }
                Command.SPOILER.matches(slashCommand) -> {
                    if (message.isNotEmpty()) {
                        ParsedCommand.SendSpoiler(message)
                    } else {
                        ParsedCommand.ErrorSyntax(Command.SPOILER)
                    }
                }
                Command.SHRUG.matches(slashCommand) -> {
                    ParsedCommand.SendShrug(message)
                }
                Command.LENNY.matches(slashCommand) -> {
                    ParsedCommand.SendLenny(message)
                }
                Command.TABLE_FLIP.matches(slashCommand) -> {
                    ParsedCommand.SendTableFlip(message)
                }
                Command.DISCARD_SESSION.matches(slashCommand) -> {
                    if (messageParts.size == 1) {
                        ParsedCommand.DiscardSession
                    } else {
                        ParsedCommand.ErrorSyntax(Command.DISCARD_SESSION)
                    }
                }
                Command.WHOIS.matches(slashCommand) -> {
                    if (messageParts.size == 2) {
                        val userId = messageParts[1]

                        if (MatrixPatterns.isUserId(userId)) {
                            ParsedCommand.ShowUser(userId)
                        } else {
                            ParsedCommand.ErrorSyntax(Command.WHOIS)
                        }
                    } else {
                        ParsedCommand.ErrorSyntax(Command.WHOIS)
                    }
                }
                Command.CONFETTI.matches(slashCommand) -> {
                    ParsedCommand.SendChatEffect(ChatEffect.CONFETTI, message)
                }
                Command.SNOWFALL.matches(slashCommand) -> {
                    ParsedCommand.SendChatEffect(ChatEffect.SNOWFALL, message)
                }
                Command.CREATE_SPACE.matches(slashCommand) -> {
                    if (messageParts.size >= 2) {
                        ParsedCommand.CreateSpace(
                                messageParts[1],
                                messageParts.drop(2)
                        )
                    } else {
                        ParsedCommand.ErrorSyntax(Command.CREATE_SPACE)
                    }
                }
                Command.ADD_TO_SPACE.matches(slashCommand) -> {
                    if (messageParts.size == 2) {
                        ParsedCommand.AddToSpace(spaceId = messageParts.last())
                    } else {
                        ParsedCommand.ErrorSyntax(Command.ADD_TO_SPACE)
                    }
                }
                Command.JOIN_SPACE.matches(slashCommand) -> {
                    if (messageParts.size == 2) {
                        ParsedCommand.JoinSpace(spaceIdOrAlias = messageParts.last())
                    } else {
                        ParsedCommand.ErrorSyntax(Command.JOIN_SPACE)
                    }
                }
                Command.LEAVE_ROOM.matches(slashCommand) -> {
                    ParsedCommand.LeaveRoom(roomId = message)
                }
                Command.UPGRADE_ROOM.matches(slashCommand) -> {
                    if (message.isNotEmpty()) {
                        ParsedCommand.UpgradeRoom(newVersion = message)
                    } else {
                        ParsedCommand.ErrorSyntax(Command.UPGRADE_ROOM)
                    }
                }
                Command.CRASH_APP.matches(slashCommand) && vectorPreferences.developerMode() -> {
                    throw RuntimeException("Application crashed from user demand")
                }
                else -> {
                    // Unknown command
                    ParsedCommand.ErrorUnknownSlashCommand(slashCommand)
                }
            }
        }
    }

    private fun extractMessage(message: String): Pair<List<String>, String>? {
        val messageParts = try {
            message.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }
        } catch (e: Exception) {
            Timber.e(e, "## parseSlashCommand() : split failed")
            null
        }

        // test if the string cut fails
        if (messageParts.isNullOrEmpty()) {
            return null
        }

        val slashCommand = messageParts.first()
        val trimmedMessage = message.substring(slashCommand.length).trim()

        return messageParts to trimmedMessage
    }

    private val notSupportedThreadsCommands: List<Command> by lazy {
        Command.values().filter {
            !it.isThreadCommand
        }
    }

    /**
     * Checks whether or not the current command is not supported by threads.
     * @param isInThreadTimeline if its true we are in a thread timeline
     * @param slashCommand the slash command that will be checked
     * @return The command that is not supported
     */
    private fun getNotSupportedByThreads(isInThreadTimeline: Boolean, slashCommand: String): Command? {
        return if (isInThreadTimeline) {
            notSupportedThreadsCommands.firstOrNull {
                it.command == slashCommand
            }
        } else {
            null
        }
    }

    private fun trimParts(message: CharSequence, messageParts: List<String>): String? {
        val partsSize = messageParts.sumOf { it.length }
        val gapsNumber = messageParts.size - 1
        return message.substring(partsSize + gapsNumber).trim().takeIf { it.isNotEmpty() }
    }
}
