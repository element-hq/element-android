/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.api.pushrules

import io.mockk.every
import io.mockk.mockk
import org.amshove.kluent.shouldBe
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.matrix.android.sdk.MatrixTest
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomMemberContent
import org.matrix.android.sdk.api.session.room.model.message.MessageTextContent
import org.matrix.android.sdk.internal.session.room.RoomGetter

class PushRulesConditionTest : MatrixTest {

    /* ==========================================================================================
     * Test EventMatchCondition
     * ========================================================================================== */

    private fun createSimpleTextEvent(text: String): Event {
        return Event(
                type = "m.room.message",
                eventId = "mx0",
                content = MessageTextContent("m.text", text).toContent(),
                originServerTs = 0)
    }

    @Test
    fun test_eventmatch_type_condition() {
        val condition = EventMatchCondition("type", "m.room.message", false)

        val simpleTextEvent = createSimpleTextEvent("Yo wtf?")

        val rm = RoomMemberContent(
                Membership.INVITE,
                displayName = "Foo",
                avatarUrl = "mxc://matrix.org/EqMZYbREvHXvYFyfxOlkf"
        )
        val simpleRoomMemberEvent = Event(
                type = "m.room.member",
                eventId = "mx0",
                stateKey = "@foo:matrix.org",
                content = rm.toContent(),
                originServerTs = 0)

        assert(condition.isSatisfied(simpleTextEvent))
        assert(!condition.isSatisfied(simpleRoomMemberEvent))
    }

    @Test
    fun test_eventmatch_path_condition() {
        val condition = EventMatchCondition("content.msgtype", "m.text", false)

        val simpleTextEvent = createSimpleTextEvent("Yo wtf?")

        assert(condition.isSatisfied(simpleTextEvent))

        Event(
                type = "m.room.member",
                eventId = "mx0",
                stateKey = "@foo:matrix.org",
                content = RoomMemberContent(
                        Membership.INVITE,
                        displayName = "Foo",
                        avatarUrl = "mxc://matrix.org/EqMZYbREvHXvYFyfxOlkf"
                ).toContent(),
                originServerTs = 0
        ).apply {
            assert(EventMatchCondition("content.membership", "invite", false).isSatisfied(this))
        }
    }

    @Test
    fun test_eventmatch_cake_condition() {
        val condition = EventMatchCondition("content.body", "cake", false)

        assert(condition.isSatisfied(createSimpleTextEvent("How was the cake?")))
        assert(condition.isSatisfied(createSimpleTextEvent("Howwasthecake?")))
    }

    @Test
    fun test_eventmatch_cakelie_condition() {
        val condition = EventMatchCondition("content.body", "cake*lie", false)

        assert(condition.isSatisfied(createSimpleTextEvent("How was the cakeisalie?")))
    }

    @Test
    fun test_eventmatch_words_only_condition() {
        val condition = EventMatchCondition("content.body", "ben", true)

        assertFalse(condition.isSatisfied(createSimpleTextEvent("benoit")))
        assertFalse(condition.isSatisfied(createSimpleTextEvent("Hello benoit")))
        assertFalse(condition.isSatisfied(createSimpleTextEvent("superben")))

        assert(condition.isSatisfied(createSimpleTextEvent("ben")))
        assert(condition.isSatisfied(createSimpleTextEvent("hello ben")))
        assert(condition.isSatisfied(createSimpleTextEvent("ben is there")))
        assert(condition.isSatisfied(createSimpleTextEvent("hello ben!")))
        assert(condition.isSatisfied(createSimpleTextEvent("hello Ben!")))
        assert(condition.isSatisfied(createSimpleTextEvent("BEN")))
    }

    @Test
    fun test_notice_condition() {
        val conditionEqual = EventMatchCondition("content.msgtype", "m.notice", false)

        Event(
                type = "m.room.message",
                eventId = "mx0",
                content = MessageTextContent("m.notice", "A").toContent(),
                originServerTs = 0,
                roomId = "2joined").also {
            assertTrue("Notice", conditionEqual.isSatisfied(it))
        }
    }

    /* ==========================================================================================
     * Test RoomMemberCountCondition
     * ========================================================================================== */

    @Test
    fun test_roommember_condition() {
        val conditionEqual3 = RoomMemberCountCondition("3")
        val conditionEqual3Bis = RoomMemberCountCondition("==3")
        val conditionLessThan3 = RoomMemberCountCondition("<3")

        val room2JoinedId = "2joined"
        val room3JoinedId = "3joined"

        val roomStub2Joined = mockk<Room> {
            every { getNumberOfJoinedMembers() } returns 2
        }

        val roomStub3Joined = mockk<Room> {
            every { getNumberOfJoinedMembers() } returns 3
        }

        val roomGetterStub = mockk<RoomGetter> {
            every { getRoom(room2JoinedId) } returns roomStub2Joined
            every { getRoom(room3JoinedId) } returns roomStub3Joined
        }

        Event(
                type = "m.room.message",
                eventId = "mx0",
                content = MessageTextContent("m.text", "A").toContent(),
                originServerTs = 0,
                roomId = room2JoinedId).also {
            assertFalse("This room does not have 3 members", conditionEqual3.isSatisfied(it, roomGetterStub))
            assertFalse("This room does not have 3 members", conditionEqual3Bis.isSatisfied(it, roomGetterStub))
            assertTrue("This room has less than 3 members", conditionLessThan3.isSatisfied(it, roomGetterStub))
        }

        Event(
                type = "m.room.message",
                eventId = "mx0",
                content = MessageTextContent("m.text", "A").toContent(),
                originServerTs = 0,
                roomId = room3JoinedId).also {
            assertTrue("This room has 3 members", conditionEqual3.isSatisfied(it, roomGetterStub))
            assertTrue("This room has 3 members", conditionEqual3Bis.isSatisfied(it, roomGetterStub))
            assertFalse("This room has more than 3 members", conditionLessThan3.isSatisfied(it, roomGetterStub))
        }
    }

    /* ==========================================================================================
     * Test ContainsDisplayNameCondition
     * ========================================================================================== */

    @Test
    fun test_displayName_condition() {
        val condition = ContainsDisplayNameCondition()

        val event = Event(
                type = "m.room.message",
                eventId = "mx0",
                content = MessageTextContent("m.text", "How was the cake benoit?").toContent(),
                originServerTs = 0,
                roomId = "2joined")

        condition.isSatisfied(event, "how") shouldBe true
        condition.isSatisfied(event, "How") shouldBe true
        condition.isSatisfied(event, "benoit") shouldBe true
        condition.isSatisfied(event, "Benoit") shouldBe true
        condition.isSatisfied(event, "cake") shouldBe true

        condition.isSatisfied(event, "ben") shouldBe false
        condition.isSatisfied(event, "oit") shouldBe false
        condition.isSatisfied(event, "enoi") shouldBe false
        condition.isSatisfied(event, "H") shouldBe false
    }
}
