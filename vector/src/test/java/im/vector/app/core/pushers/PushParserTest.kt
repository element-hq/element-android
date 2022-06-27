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

package im.vector.app.core.pushers

import im.vector.app.core.pushers.model.PushData
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class PushParserTest {
    private val validData = PushData(
            eventId = "\$anEventId",
            roomId = "!aRoomId:domain",
            unread = 1
    )

    private val emptyData = PushData(
            eventId = null,
            roomId = null,
            unread = null
    )

    @Test
    fun `test edge cases`() {
        doAllEdgeTests(true)
        doAllEdgeTests(false)
    }

    private fun doAllEdgeTests(firebaseFormat: Boolean) {
        val pushParser = PushParser()
        // Empty string
        pushParser.parseData("", firebaseFormat) shouldBe null
        // Empty Json
        pushParser.parseData("{}", firebaseFormat) shouldBeEqualTo emptyData
        // Bad Json
        pushParser.parseData("ABC", firebaseFormat) shouldBe null
    }

    @Test
    fun `test unified push format`() {
        val pushParser = PushParser()

        pushParser.parseData(UNIFIED_PUSH_DATA, false) shouldBeEqualTo validData
        pushParser.parseData(UNIFIED_PUSH_DATA, true) shouldBeEqualTo emptyData
    }

    @Test
    fun `test firebase push format`() {
        val pushParser = PushParser()

        pushParser.parseData(FIREBASE_PUSH_DATA, true) shouldBeEqualTo validData
        pushParser.parseData(FIREBASE_PUSH_DATA, false) shouldBeEqualTo emptyData
    }

    @Test
    fun `test empty roomId`() {
        val pushParser = PushParser()

        pushParser.parseData(FIREBASE_PUSH_DATA.replace("!aRoomId:domain", ""), true) shouldBeEqualTo validData.copy(roomId = null)
        pushParser.parseData(UNIFIED_PUSH_DATA.replace("!aRoomId:domain", ""), false) shouldBeEqualTo validData.copy(roomId = null)
    }

    @Test
    fun `test invalid roomId`() {
        val pushParser = PushParser()

        pushParser.parseData(FIREBASE_PUSH_DATA.replace("!aRoomId:domain", "aRoomId:domain"), true) shouldBeEqualTo validData.copy(roomId = null)
        pushParser.parseData(UNIFIED_PUSH_DATA.replace("!aRoomId:domain", "aRoomId:domain"), false) shouldBeEqualTo validData.copy(roomId = null)
    }

    @Test
    fun `test empty eventId`() {
        val pushParser = PushParser()

        pushParser.parseData(FIREBASE_PUSH_DATA.replace("\$anEventId", ""), true) shouldBeEqualTo validData.copy(eventId = null)
        pushParser.parseData(UNIFIED_PUSH_DATA.replace("\$anEventId", ""), false) shouldBeEqualTo validData.copy(eventId = null)
    }

    @Test
    fun `test invalid eventId`() {
        val pushParser = PushParser()

        pushParser.parseData(FIREBASE_PUSH_DATA.replace("\$anEventId", "anEventId"), true) shouldBeEqualTo validData.copy(eventId = null)
        pushParser.parseData(UNIFIED_PUSH_DATA.replace("\$anEventId", "anEventId"), false) shouldBeEqualTo validData.copy(eventId = null)
    }

    companion object {
        private const val UNIFIED_PUSH_DATA =
                "{\"notification\":{\"event_id\":\"\$anEventId\",\"room_id\":\"!aRoomId:domain\",\"counts\":{\"unread\":1},\"prio\":\"high\"}}"
        private const val FIREBASE_PUSH_DATA =
                "{\"event_id\":\"\$anEventId\",\"room_id\":\"!aRoomId:domain\",\"unread\":\"1\",\"prio\":\"high\"}"
    }
}
