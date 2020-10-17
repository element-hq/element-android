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

package im.vector.app.features.home.room.detail.composer.rainbow

import im.vector.app.test.trimIndentOneLine
import org.junit.Assert.assertEquals
import org.junit.Test

@Suppress("SpellCheckingInspection")
class RainbowGeneratorTest {

    private val rainbowGenerator = RainbowGenerator()

    @Test
    fun testEmpty() {
        assertEquals("", rainbowGenerator.generate(""))
    }

    @Test
    fun testAscii1() {
        assertEquals("""<font color="#ff00be">a</font>""", rainbowGenerator.generate("a"))
    }

    @Test
    fun testAscii2() {
        val expected = """
            <font color="#ff00be">a</font>
            <font color="#00e6b6">b</font>
        """.trimIndentOneLine()

        assertEquals(expected, rainbowGenerator.generate("ab"))
    }

    @Test
    fun testAscii3() {
        val expected = """
            <font color="#ff00be">T</font>
            <font color="#ff0072">h</font>
            <font color="#ff3b1d">i</font>
            <font color="#ff7e00">s</font>
             
            <font color="#bdc100">i</font>
            <font color="#64d200">s</font>
             
            <font color="#00e261">a</font>
             
            <font color="#00e7ff">r</font>
            <font color="#00e6ff">a</font>
            <font color="#00e1ff">i</font>
            <font color="#00d4ff">n</font>
            <font color="#00bdff">b</font>
            <font color="#9598ff">o</font>
            <font color="#ff60ff">w</font>
            <font color="#ff00ff">!</font>
        """.trimIndentOneLine()

        assertEquals(expected, rainbowGenerator.generate("This is a rainbow!"))
    }

    @Test
    fun testEmoji1() {
        assertEquals("""<font color="#ff00be">🤞</font>""", rainbowGenerator.generate("\uD83E\uDD1E")) // 🤞
    }

    @Test
    fun testEmoji2() {
        assertEquals("""<font color="#ff00be">🤞</font>""", rainbowGenerator.generate("🤞"))
    }

    @Test
    fun testEmoji3() {
        val expected = """
            <font color="#ff00be">🤞</font>
            <font color="#00e6b6">🙂</font>
        """.trimIndentOneLine()

        assertEquals(expected, rainbowGenerator.generate("🤞🙂"))
    }

    @Test
    fun testEmojiMix1() {
        val expected = """
            <font color="#ff00be">H</font>
            <font color="#ff005d">e</font>
            <font color="#ff6700">l</font>
            <font color="#ffa100">l</font>
            <font color="#b2c400">o</font>
             
            <font color="#00e147">🤞</font>
             
            <font color="#00e7ff">w</font>
            <font color="#00e4ff">o</font>
            <font color="#00d6ff">r</font>
            <font color="#00b9ff">l</font>
            <font color="#da83ff">d</font>
            <font color="#ff03ff">!</font>
        """.trimIndentOneLine()

        assertEquals(expected, rainbowGenerator.generate("Hello 🤞 world!"))
    }

    @Test
    fun testEmojiMix2() {
        val expected = """
            <font color="#ff00be">a</font>
            <font color="#00e6b6">🤞</font>
        """.trimIndentOneLine()

        assertEquals(expected, rainbowGenerator.generate("a🤞"))
    }

    @Test
    fun testEmojiMix3() {
        val expected = """
            <font color="#ff00be">🤞</font>
            <font color="#00e6b6">a</font>
        """.trimIndentOneLine()

        assertEquals(expected, rainbowGenerator.generate("🤞a"))
    }

    @Test
    fun testError1() {
        assertEquals("<font color=\"#ff00be\">\uD83E</font>", rainbowGenerator.generate("\uD83E"))
    }
}
