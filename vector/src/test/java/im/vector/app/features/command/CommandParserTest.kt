/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.features.command

import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class CommandParserTest {
    @Test
    fun parseSlashCommandEmpty() {
        test("/", ParsedCommand.ErrorEmptySlashCommand)
    }

    @Test
    fun parseSlashCommandUnknown() {
        test("/unknown", ParsedCommand.ErrorUnknownSlashCommand("/unknown"))
        test("/unknown with param", ParsedCommand.ErrorUnknownSlashCommand("/unknown"))
    }

    @Test
    fun parseSlashCommandNotACommand() {
        test("", ParsedCommand.ErrorNotACommand)
        test("test", ParsedCommand.ErrorNotACommand)
        test("// test", ParsedCommand.ErrorNotACommand)
    }

    @Test
    fun parseSlashCommandEmote() {
        test("/me test", ParsedCommand.SendEmote("test"))
        test("/me", ParsedCommand.ErrorSyntax(Command.EMOTE))
    }

    @Test
    fun parseSlashCommandRemove() {
        // Nominal
        test("/remove @foo:bar", ParsedCommand.RemoveUser("@foo:bar", null))
        // With a reason
        test("/remove @foo:bar a reason", ParsedCommand.RemoveUser("@foo:bar", "a reason"))
        // Trim the reason
        test("/remove @foo:bar    a    reason    ", ParsedCommand.RemoveUser("@foo:bar", "a    reason"))
        // Alias
        test("/kick @foo:bar", ParsedCommand.RemoveUser("@foo:bar", null))
        // Error
        test("/remove", ParsedCommand.ErrorSyntax(Command.REMOVE_USER))
    }

    private fun test(message: String, expectedResult: ParsedCommand) {
        val commandParser = CommandParser()
        val result = commandParser.parseSlashCommand(message, false)
        result shouldBeEqualTo expectedResult
    }
}
