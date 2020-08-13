/*
 * Copyright (c) 2020 New Vector Ltd
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

package org.matrix.android.sdk.internal.session.room.send

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.matrix.android.sdk.InstrumentedTest
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.commonmark.renderer.text.TextContentRenderer
import org.junit.Assert.assertEquals
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

/**
 * It will not be possible to test all combinations. For the moment I add a few tests, then, depending on the problem discovered in the wild,
 * we can add more tests to cover the edge cases.
 * Some tests are suffixed with `_not_passing`, maybe one day we will fix them...
 * Riot-Web should be used as a reference for expected results, but not always. Especially Riot-Web add lots of `\n` in the
 * formatted body, which is quite useless.
 * Also Riot-Web does not provide plain text body when formatted text is provided. The body contains what the user has entered.
 * See https://matrix.org/docs/spec/client_server/latest#m-room-message-msgtypes
 */
@Suppress("SpellCheckingInspection")
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.JVM)
class MarkdownParserTest : InstrumentedTest {

    /**
     * Create the same parser than in the RoomModule
     */
    private val markdownParser = MarkdownParser(
            Parser.builder().build(),
            HtmlRenderer.builder().build(),
            TextContentRenderer.builder().build()
    )

    @Test
    fun parseNoMarkdown() {
        testIdentity("")
        testIdentity("a")
        testIdentity("1")
        testIdentity("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et " +
                "dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea com" +
                "modo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pari" +
                "atur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.")
    }

    @Test
    fun parseSpaces() {
        testIdentity(" ")
        testIdentity("  ")
        testIdentity("\n")
    }

    @Test
    fun parseNewLines() {
        testIdentity("line1\nline2")
        testIdentity("line1\nline2\nline3")
    }

    @Test
    fun parseBold() {
        testType(
                name = "bold",
                markdownPattern = "**",
                htmlExpectedTag = "strong"
        )
    }

    @Test
    fun parseItalic() {
        testType(
                name = "italic",
                markdownPattern = "*",
                htmlExpectedTag = "em"
        )
    }

    @Test
    fun parseItalic2() {
        // Riot-Web format
        "_italic_".let { markdownParser.parse(it) }.expect("italic", "<em>italic</em>")
    }

    /**
     * Note: the test is not passing, it does not work on Riot-Web neither
     */
    @Test
    fun parseStrike_not_passing() {
        testType(
                name = "strike",
                markdownPattern = "~~",
                htmlExpectedTag = "del"
        )
    }

    @Test
    fun parseCode() {
        testType(
                name = "code",
                markdownPattern = "`",
                htmlExpectedTag = "code",
                plainTextPrefix = "\"",
                plainTextSuffix = "\""
        )
    }

    @Test
    fun parseCode2() {
        testType(
                name = "code",
                markdownPattern = "``",
                htmlExpectedTag = "code",
                plainTextPrefix = "\"",
                plainTextSuffix = "\""
        )
    }

    @Test
    fun parseCode3() {
        testType(
                name = "code",
                markdownPattern = "```",
                htmlExpectedTag = "code",
                plainTextPrefix = "\"",
                plainTextSuffix = "\""
        )
    }

    @Test
    fun parseUnorderedList() {
        "- item1".let { markdownParser.parse(it).expect(it, "<ul><li>item1</li></ul>") }
        "- item1\n- item2".let { markdownParser.parse(it).expect(it, "<ul><li>item1</li><li>item2</li></ul>") }
    }

    @Test
    fun parseOrderedList() {
        "1. item1".let { markdownParser.parse(it).expect(it, "<ol><li>item1</li></ol>") }
        "1. item1\n2. item2".let { markdownParser.parse(it).expect(it, "<ol><li>item1</li><li>item2</li></ol>") }
    }

    @Test
    fun parseHorizontalLine() {
        "---".let { markdownParser.parse(it) }.expect("***", "<hr />")
    }

    @Test
    fun parseH2AndContent() {
        "a\n---\nb".let { markdownParser.parse(it) }.expect("a\nb", "<h2>a</h2><p>b</p>")
    }

    @Test
    fun parseQuote() {
        "> quoted".let { markdownParser.parse(it) }.expect("«quoted»", "<blockquote><p>quoted</p></blockquote>")
    }

    @Test
    fun parseQuote_not_passing() {
        "> quoted\nline2".let { markdownParser.parse(it) }.expect("«quoted\nline2»", "<blockquote><p>quoted<br/>line2</p></blockquote>")
    }

    @Test
    fun parseBoldItalic() {
        "*italic* **bold**".let { markdownParser.parse(it) }.expect("italic bold", "<em>italic</em> <strong>bold</strong>")
        "**bold** *italic*".let { markdownParser.parse(it) }.expect("bold italic", "<strong>bold</strong> <em>italic</em>")
    }

    @Test
    fun parseHead() {
        "# head1".let { markdownParser.parse(it) }.expect("head1", "<h1>head1</h1>")
        "## head2".let { markdownParser.parse(it) }.expect("head2", "<h2>head2</h2>")
        "### head3".let { markdownParser.parse(it) }.expect("head3", "<h3>head3</h3>")
        "#### head4".let { markdownParser.parse(it) }.expect("head4", "<h4>head4</h4>")
        "##### head5".let { markdownParser.parse(it) }.expect("head5", "<h5>head5</h5>")
        "###### head6".let { markdownParser.parse(it) }.expect("head6", "<h6>head6</h6>")
    }

    @Test
    fun parseHeads() {
        "# head1\n# head2".let { markdownParser.parse(it) }.expect("head1\nhead2", "<h1>head1</h1><h1>head2</h1>")
    }

    @Test
    fun parseBoldNewLines_not_passing() {
        "**bold**\nline2".let { markdownParser.parse(it) }.expect("bold\nline2", "<strong>bold</strong><br />line2")
    }

    @Test
    fun parseLinks() {
        "[link](target)".let { markdownParser.parse(it) }.expect(""""link" (target)""", """<a href="target">link</a>""")
    }

    @Test
    fun parseParagraph() {
        "# head\ncontent".let { markdownParser.parse(it) }.expect("head\ncontent", "<h1>head</h1><p>content</p>")
    }

    private fun testIdentity(text: String) {
        markdownParser.parse(text).expect(text, null)
    }

    private fun testType(name: String,
                         markdownPattern: String,
                         htmlExpectedTag: String,
                         plainTextPrefix: String = "",
                         plainTextSuffix: String = "") {
        // Test simple case
        "$markdownPattern$name$markdownPattern"
                .let { markdownParser.parse(it) }
                .expect(expectedText = "$plainTextPrefix$name$plainTextSuffix",
                        expectedFormattedText = "<$htmlExpectedTag>$name</$htmlExpectedTag>")

        // Test twice the same tag
        "$markdownPattern$name$markdownPattern and $markdownPattern$name bis$markdownPattern"
                .let { markdownParser.parse(it) }
                .expect(expectedText = "$plainTextPrefix$name$plainTextSuffix and $plainTextPrefix$name bis$plainTextSuffix",
                        expectedFormattedText = "<$htmlExpectedTag>$name</$htmlExpectedTag> and <$htmlExpectedTag>$name bis</$htmlExpectedTag>")

        val textBefore = "a"
        val textAfter = "b"

        // With sticked text before
        "$textBefore$markdownPattern$name$markdownPattern"
                .let { markdownParser.parse(it) }
                .expect(expectedText = "$textBefore$plainTextPrefix$name$plainTextSuffix",
                        expectedFormattedText = "$textBefore<$htmlExpectedTag>$name</$htmlExpectedTag>")

        // With text before and space
        "$textBefore $markdownPattern$name$markdownPattern"
                .let { markdownParser.parse(it) }
                .expect(expectedText = "$textBefore $plainTextPrefix$name$plainTextSuffix",
                        expectedFormattedText = "$textBefore <$htmlExpectedTag>$name</$htmlExpectedTag>")

        // With sticked text after
        "$markdownPattern$name$markdownPattern$textAfter"
                .let { markdownParser.parse(it) }
                .expect(expectedText = "$plainTextPrefix$name$plainTextSuffix$textAfter",
                        expectedFormattedText = "<$htmlExpectedTag>$name</$htmlExpectedTag>$textAfter")

        // With space and text after
        "$markdownPattern$name$markdownPattern $textAfter"
                .let { markdownParser.parse(it) }
                .expect(expectedText = "$plainTextPrefix$name$plainTextSuffix $textAfter",
                        expectedFormattedText = "<$htmlExpectedTag>$name</$htmlExpectedTag> $textAfter")

        // With sticked text before and text after
        "$textBefore$markdownPattern$name$markdownPattern$textAfter"
                .let { markdownParser.parse(it) }
                .expect(expectedText = "$textBefore$plainTextPrefix$name$plainTextSuffix$textAfter",
                        expectedFormattedText = "a<$htmlExpectedTag>$name</$htmlExpectedTag>$textAfter")

        // With text before and after, with spaces
        "$textBefore $markdownPattern$name$markdownPattern $textAfter"
                .let { markdownParser.parse(it) }
                .expect(expectedText = "$textBefore $plainTextPrefix$name$plainTextSuffix $textAfter",
                        expectedFormattedText = "$textBefore <$htmlExpectedTag>$name</$htmlExpectedTag> $textAfter")
    }

    private fun TextContent.expect(expectedText: String, expectedFormattedText: String?) {
        assertEquals("TextContent are not identical", TextContent(expectedText, expectedFormattedText), this)
    }
}
