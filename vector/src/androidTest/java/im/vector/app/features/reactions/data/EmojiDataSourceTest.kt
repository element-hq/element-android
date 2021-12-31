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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
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

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @Test
    fun checkParsingTime() {
        val time = measureTimeMillis {
            createEmojiDataSource()
        }
        assertTrue("Too long to parse", time < 100)
    }

    @Test
    fun checkNumberOfResult() {
        val emojiDataSource = createEmojiDataSource()
        val rawData = runBlocking {
            emojiDataSource.rawData.await()
        }
        assertTrue("Wrong number of emojis", rawData.emojis.size >= 500)
        assertTrue("Wrong number of categories", rawData.categories.size >= 8)
    }

    @Test
    fun searchTestEmptySearch() {
        val emojiDataSource = createEmojiDataSource()
        val result = runBlocking {
            emojiDataSource.filterWith("")
        }
        assertTrue("Empty search should return at least 500 results", result.size >= 500)
    }

    @Test
    fun searchTestNoResult() {
        val emojiDataSource = createEmojiDataSource()
        val result = runBlocking {
            emojiDataSource.filterWith("noresult")
        }
        assertTrue("Should not have result", result.isEmpty())
    }

    @Test
    fun searchTestOneResult() {
        val emojiDataSource = createEmojiDataSource()
        val result = runBlocking {
            emojiDataSource.filterWith("france")
        }
        assertEquals("Should have 1 result", 1, result.size)
    }

    @Test
    fun searchTestManyResult() {
        val emojiDataSource = createEmojiDataSource()
        val result = runBlocking {
            emojiDataSource.filterWith("fra")
        }
        assertTrue("Should have many result", result.size > 1)
    }

    @Test
    fun testTada() {
        val emojiDataSource = createEmojiDataSource()
        val result = runBlocking {
            emojiDataSource.filterWith("tada")
        }
        assertEquals("Should find tada emoji", 1, result.size)
        assertEquals("Should find tada emoji", "🎉", result[0].emoji)
    }

    @Test
    fun testQuickReactions() {
        val emojiDataSource = createEmojiDataSource()
        val result = runBlocking {
            emojiDataSource.getQuickReactions()
        }
        assertEquals("Should have 8 quick reactions", 8, result.size)
    }

    private fun createEmojiDataSource() = EmojiDataSource(coroutineScope, context().resources)
}
