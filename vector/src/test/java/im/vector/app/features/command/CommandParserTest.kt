/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.command

import im.vector.app.test.fakes.FakeVectorPreferences
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

private const val A_SPACE_ID = "!my-space-id"

class CommandParserTest {
    private val fakeVectorPreferences = FakeVectorPreferences()

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
    fun parseSlashAddToSpaceCommand() {
        test("/addToSpace $A_SPACE_ID", ParsedCommand.AddToSpace(A_SPACE_ID))
    }
    @Test
    fun parseSlashJoinSpaceCommand() {
        test("/joinSpace $A_SPACE_ID", ParsedCommand.JoinSpace(A_SPACE_ID))
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
        val commandParser = CommandParser(fakeVectorPreferences.instance)
        val result = commandParser.parseSlashCommand(message, null, false)
        result shouldBeEqualTo expectedResult
    }
}
