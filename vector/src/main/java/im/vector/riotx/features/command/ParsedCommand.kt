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

/**
 * Represent a parsed command
 */
sealed class ParsedCommand {
    // This is not a Slash command
    object ErrorNotACommand : ParsedCommand()

    object ErrorEmptySlashCommand : ParsedCommand()

    // Unknown/Unsupported slash command
    class ErrorUnknownSlashCommand(val slashCommand: String) : ParsedCommand()

    // A slash command is detected, but there is an error
    class ErrorSyntax(val command: Command) : ParsedCommand()

    // Valid commands:

    class SendEmote(val message: CharSequence) : ParsedCommand()
    class BanUser(val userId: String, val reason: String) : ParsedCommand()
    class UnbanUser(val userId: String) : ParsedCommand()
    class SetUserPowerLevel(val userId: String, val powerLevel: Int) : ParsedCommand()
    class Invite(val userId: String) : ParsedCommand()
    class JoinRoom(val roomAlias: String) : ParsedCommand()
    class PartRoom(val roomAlias: String) : ParsedCommand()
    class ChangeTopic(val topic: String) : ParsedCommand()
    class KickUser(val userId: String, val reason: String) : ParsedCommand()
    class ChangeDisplayName(val displayName: String) : ParsedCommand()
    class SetMarkdown(val enable: Boolean) : ParsedCommand()
    object ClearScalarToken : ParsedCommand()
    class SendSpoiler(val message: String) : ParsedCommand()
}
