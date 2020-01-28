/*
 * Copyright 2020 New Vector Ltd
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

package im.vector.riotx.features.home.room.detail.composer.rainbow

import org.junit.Assert.assertEquals
import org.junit.Test

class RainbowGeneratorTest {

    private val rainbowGenerator = RainbowGenerator()

    @Test
    fun testEmpty() {
        assertEquals("", rainbowGenerator.generate(""))
    }

    @Test
    fun testAscii1() {
        assertEquals("""<font color="#ff0000">a</font>""", rainbowGenerator.generate("a"))
    }

    @Test
    fun testAscii2() {
        val expected =
                """
            <font color="#ff0000">T</font>
            <font color="#ff5500">h</font>
            <font color="#ffaa00">i</font>
            <font color="#ffff00">s</font>
             
            <font color="#55ff00">i</font>
            <font color="#00ff00">s</font>
             
            <font color="#00ffaa">a</font>
             
            <font color="#00aaff">r</font>
            <font color="#0055ff">a</font>
            <font color="#0000ff">i</font>
            <font color="#5500ff">n</font>
            <font color="#aa00ff">b</font>
            <font color="#ff00ff">o</font>
            <font color="#ff00aa">w</font>
            <font color="#ff0055">!</font>
        """
                        .trimIndent()
                        .replace("\n", "")

        assertEquals(expected, rainbowGenerator.generate("This is a rainbow!"))
    }

    @Test
    fun testEmoji1() {
        assertEquals("""<font color="#ff0000">\uD83E\uDD1E</font>""", rainbowGenerator.generate("\uD83E\uDD1E")) // 🤞
    }

    @Test
    fun testEmoji2() {
        assertEquals("""<font color="#ff0000">🤞</font>""", rainbowGenerator.generate("🤞"))
    }

    @Test
    fun testEmojiMix() {
        val expected = """
            <font color="#ff0000">T</font>
            <font color="#ff5500">h</font>
            <font color="#ffaa00">i</font>
            <font color="#ffff00">s</font>
             
            <font color="#55ff00">i</font>
            <font color="#00ff00">s</font>
             
            <font color="#00ffaa">a</font>
             
            <font color="#00aaff">r</font>
            <font color="#0055ff">a</font>
            <font color="#0000ff">i</font>
            <font color="#5500ff">n</font>
            <font color="#aa00ff">b</font>
            <font color="#ff00ff">o</font>
            <font color="#ff00aa">w</font>
            <font color="#ff0055">!</font>
        """
                .trimIndent()
                .replace("\n", "")

        assertEquals(expected, rainbowGenerator.generate("Hello 🤞 world!"))
    }
}