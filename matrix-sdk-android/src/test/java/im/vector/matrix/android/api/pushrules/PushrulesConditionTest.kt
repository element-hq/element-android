package im.vector.matrix.android.api.pushrules

import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.toContent
import im.vector.matrix.android.api.session.room.model.Membership
import im.vector.matrix.android.api.session.room.model.RoomMember
import im.vector.matrix.android.api.session.room.model.message.MessageTextContent
import org.junit.Test

class PushrulesConditionTest {


    @Test
    fun test_eventmatch_type_condition() {
        val condition = EventMatchCondition("type", "m.room.message")

        val simpleTextEvent = Event(
                type = "m.room.message",
                eventId = "mx0",
                content = MessageTextContent("m.text", "Yo wtf?").toContent(),
                originServerTs = 0)

        val rm = RoomMember(
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
        val condition = EventMatchCondition("content.msgtype", "m.text")

        val simpleTextEvent = Event(
                type = "m.room.message",
                eventId = "mx0",
                content = MessageTextContent("m.text", "Yo wtf?").toContent(),
                originServerTs = 0)

        assert(condition.isSatisfied(simpleTextEvent))

        Event(
                type = "m.room.member",
                eventId = "mx0",
                stateKey = "@foo:matrix.org",
                content = RoomMember(
                        Membership.INVITE,
                        displayName = "Foo",
                        avatarUrl = "mxc://matrix.org/EqMZYbREvHXvYFyfxOlkf"
                ).toContent(),
                originServerTs = 0
        ).apply {
            assert(EventMatchCondition("content.membership", "invite").isSatisfied(this))
        }

    }

    @Test
    fun test_eventmatch_cake_condition() {
        val condition = EventMatchCondition("content.body", "cake")

        Event(
                type = "m.room.message",
                eventId = "mx0",
                content = MessageTextContent("m.text", "How was the cake?").toContent(),
                originServerTs = 0
        ).apply {
            assert(condition.isSatisfied(this))
        }

        Event(
                type = "m.room.message",
                eventId = "mx0",
                content = MessageTextContent("m.text", "Howwasthecake?").toContent(),
                originServerTs = 0
        ).apply {
            assert(condition.isSatisfied(this))
        }

    }

    @Test
    fun test_eventmatch_cakelie_condition() {
        val condition = EventMatchCondition("content.body", "cake*lie")

        val simpleTextEvent = Event(
                type = "m.room.message",
                eventId = "mx0",
                content = MessageTextContent("m.text", "How was the cakeisalie?").toContent(),
                originServerTs = 0)

        assert(condition.isSatisfied(simpleTextEvent))
    }
}
