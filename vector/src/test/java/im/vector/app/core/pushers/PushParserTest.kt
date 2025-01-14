/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
    fun `test edge cases Firebase`() {
        val pushParser = PushParser()
        // Empty Json
        pushParser.parsePushDataFcm(emptyMap()) shouldBeEqualTo emptyData
        // Bad Json
        pushParser.parsePushDataFcm(FIREBASE_PUSH_DATA.mutate("unread", "str")) shouldBeEqualTo validData.copy(unread = null)
        // Extra data
        pushParser.parsePushDataFcm(FIREBASE_PUSH_DATA.mutate("extra", "5")) shouldBeEqualTo validData
    }

    @Test
    fun `test edge cases UnifiedPush`() {
        val pushParser = PushParser()
        // Empty string
        pushParser.parsePushDataUnifiedPush("".toByteArray()) shouldBe null
        // Empty Json
        pushParser.parsePushDataUnifiedPush("{}".toByteArray()) shouldBeEqualTo emptyData
        // Bad Json
        pushParser.parsePushDataUnifiedPush("ABC".toByteArray()) shouldBe null
    }

    @Test
    fun `test UnifiedPush format`() {
        val pushParser = PushParser()
        pushParser.parsePushDataUnifiedPush(UNIFIED_PUSH_DATA.toByteArray()) shouldBeEqualTo validData
    }

    @Test
    fun `test Firebase format`() {
        val pushParser = PushParser()
        pushParser.parsePushDataFcm(FIREBASE_PUSH_DATA) shouldBeEqualTo validData
    }

    @Test
    fun `test empty roomId`() {
        val pushParser = PushParser()
        val expected = validData.copy(roomId = null)
        pushParser.parsePushDataFcm(FIREBASE_PUSH_DATA.mutate("room_id", null)) shouldBeEqualTo expected
        pushParser.parsePushDataFcm(FIREBASE_PUSH_DATA.mutate("room_id", "")) shouldBeEqualTo expected
        pushParser.parsePushDataUnifiedPush(UNIFIED_PUSH_DATA.replace("!aRoomId:domain", "").toByteArray()) shouldBeEqualTo expected
    }

    @Test
    fun `test invalid roomId`() {
        val pushParser = PushParser()
        val expected = validData.copy(roomId = null)
        pushParser.parsePushDataFcm(FIREBASE_PUSH_DATA.mutate("room_id", "aRoomId:domain")) shouldBeEqualTo expected
        pushParser.parsePushDataUnifiedPush(UNIFIED_PUSH_DATA.mutate("!aRoomId:domain", "aRoomId:domain")) shouldBeEqualTo expected
    }

    @Test
    fun `test empty eventId`() {
        val pushParser = PushParser()
        val expected = validData.copy(eventId = null)
        pushParser.parsePushDataFcm(FIREBASE_PUSH_DATA.mutate("event_id", null)) shouldBeEqualTo expected
        pushParser.parsePushDataFcm(FIREBASE_PUSH_DATA.mutate("event_id", "")) shouldBeEqualTo expected
        pushParser.parsePushDataUnifiedPush(UNIFIED_PUSH_DATA.mutate("\$anEventId", "")) shouldBeEqualTo expected
    }

    @Test
    fun `test invalid eventId`() {
        val pushParser = PushParser()
        val expected = validData.copy(eventId = null)
        pushParser.parsePushDataFcm(FIREBASE_PUSH_DATA.mutate("event_id", "anEventId")) shouldBeEqualTo expected
        pushParser.parsePushDataUnifiedPush(UNIFIED_PUSH_DATA.mutate("\$anEventId", "anEventId")) shouldBeEqualTo expected
    }

    companion object {
        private const val UNIFIED_PUSH_DATA =
                "{\"notification\":{\"event_id\":\"\$anEventId\",\"room_id\":\"!aRoomId:domain\",\"counts\":{\"unread\":1},\"prio\":\"high\"}}"
        private val FIREBASE_PUSH_DATA = mapOf(
                "event_id" to "\$anEventId",
                "room_id" to "!aRoomId:domain",
                "unread" to "1",
                "prio" to "high",
        )
    }
}

private fun Map<String, String?>.mutate(key: String, value: String?): Map<String, String?> {
    return toMutableMap().apply { put(key, value) }
}

private fun String.mutate(oldValue: String, newValue: String): ByteArray {
    return replace(oldValue, newValue).toByteArray()
}
