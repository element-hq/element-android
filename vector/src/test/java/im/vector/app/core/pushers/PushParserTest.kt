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
    companion object {
        private const val UNIFIED_PUSH_DATA =
                "{\"notification\":{\"event_id\":\"\$anEventId\",\"room_id\":\"!aRoomId\",\"counts\":{\"unread\":1},\"prio\":\"high\"}}"
        private const val FIREBASE_PUSH_DATA =
                "{\"event_id\":\"\$anEventId\",\"room_id\":\"!aRoomId\",\"unread\":\"1\",\"prio\":\"high\"}"
    }

    private val parsedData = PushData(
            eventId = "\$anEventId",
            roomId = "!aRoomId",
            unread = 1
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
        pushParser.parseData("{}", firebaseFormat) shouldBe null
        // Bad Json
        pushParser.parseData("ABC", firebaseFormat) shouldBe null
    }

    @Test
    fun `test unified push format`() {
        val pushParser = PushParser()

        pushParser.parseData(UNIFIED_PUSH_DATA, false) shouldBeEqualTo parsedData
        pushParser.parseData(UNIFIED_PUSH_DATA, true) shouldBe null
    }

    @Test
    fun `test firebase push format`() {
        val pushParser = PushParser()

        pushParser.parseData(FIREBASE_PUSH_DATA, true) shouldBeEqualTo parsedData
        pushParser.parseData(FIREBASE_PUSH_DATA, false) shouldBe null
    }

    @Test
    fun `test empty roomId`() {
        val pushParser = PushParser()

        pushParser.parseData(FIREBASE_PUSH_DATA.replace("!aRoomId", ""), true) shouldBe null
        pushParser.parseData(UNIFIED_PUSH_DATA.replace("!aRoomId", ""), false) shouldBe null
    }

    @Test
    fun `test empty eventId`() {
        val pushParser = PushParser()

        pushParser.parseData(FIREBASE_PUSH_DATA.replace("\$anEventId", ""), true) shouldBe null
        pushParser.parseData(UNIFIED_PUSH_DATA.replace("\$anEventId", ""), false) shouldBe null
    }
}
