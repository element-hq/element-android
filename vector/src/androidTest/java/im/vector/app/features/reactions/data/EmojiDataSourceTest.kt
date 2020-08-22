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

package im.vector.app.features.reactions.data

import im.vector.app.InstrumentedTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.MethodSorters
import kotlin.system.measureTimeMillis

@RunWith(JUnit4::class)
@FixMethodOrder(MethodSorters.JVM)
class EmojiDataSourceTest : InstrumentedTest {

    @Test
    fun checkParsingTime() {
        val time = measureTimeMillis {
            EmojiDataSource(context().resources)
        }

        assertTrue("Too long to parse", time < 100)
    }

    @Test
    fun checkNumberOfResult() {
        val emojiDataSource = EmojiDataSource(context().resources)

        assertEquals("Wrong number of emojis", 1545, emojiDataSource.rawData.emojis.size)
        assertEquals("Wrong number of categories", 8, emojiDataSource.rawData.categories.size)
        assertEquals("Wrong number of aliases", 57, emojiDataSource.rawData.aliases.size)
    }

    @Test
    fun searchTestEmptySearch() {
        val emojiDataSource = EmojiDataSource(context().resources)

        assertEquals("Empty search should return 1545 results", 1545, emojiDataSource.filterWith("").size)
    }

    @Test
    fun searchTestNoResult() {
        val emojiDataSource = EmojiDataSource(context().resources)

        assertTrue("Should not have result", emojiDataSource.filterWith("noresult").isEmpty())
    }

    @Test
    fun searchTestOneResult() {
        val emojiDataSource = EmojiDataSource(context().resources)

        assertEquals("Should have 1 result", 1, emojiDataSource.filterWith("france").size)
    }

    @Test
    fun searchTestManyResult() {
        val emojiDataSource = EmojiDataSource(context().resources)

        assertTrue("Should have many result", emojiDataSource.filterWith("fra").size > 1)
    }

    @Test
    fun testTada() {
        val emojiDataSource = EmojiDataSource(context().resources)

        val result = emojiDataSource.filterWith("tada")

        assertEquals("Should find tada emoji", 1, result.size)
        assertEquals("Should find tada emoji", "🎉", result[0].emoji)
    }

    @Test
    fun testQuickReactions() {
        val emojiDataSource = EmojiDataSource(context().resources)

        assertEquals("Should have 8 quick reactions", 8, emojiDataSource.getQuickReactions().size)
    }
}
