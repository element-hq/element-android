/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.api.pushrules

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.content.ContentAttachmentData
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.toContent
import im.vector.matrix.android.api.session.room.Room
import im.vector.matrix.android.api.session.room.RoomService
import im.vector.matrix.android.api.session.room.model.*
import im.vector.matrix.android.api.session.room.model.create.CreateRoomParams
import im.vector.matrix.android.api.session.room.model.message.MessageTextContent
import im.vector.matrix.android.api.session.room.send.UserDraft
import im.vector.matrix.android.api.session.room.timeline.Timeline
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.matrix.android.api.session.room.timeline.TimelineSettings
import im.vector.matrix.android.api.util.Cancelable
import org.junit.Assert
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

    @Test
    fun test_roommember_condition() {
        val conditionEqual3 = RoomMemberCountCondition("3")
        val conditionEqual3Bis = RoomMemberCountCondition("==3")
        val conditionLessThan3 = RoomMemberCountCondition("<3")

        val session = MockRoomService()

        Event(
                type = "m.room.message",
                eventId = "mx0",
                content = MessageTextContent("m.text", "A").toContent(),
                originServerTs = 0,
                roomId = "2joined").also {
            Assert.assertFalse("This room does not have 3 members", conditionEqual3.isSatisfied(it, session))
            Assert.assertFalse("This room does not have 3 members", conditionEqual3Bis.isSatisfied(it, session))
            Assert.assertTrue("This room has less than 3 members", conditionLessThan3.isSatisfied(it, session))
        }

        Event(
                type = "m.room.message",
                eventId = "mx0",
                content = MessageTextContent("m.text", "A").toContent(),
                originServerTs = 0,
                roomId = "3joined").also {
            Assert.assertTrue("This room has 3 members", conditionEqual3.isSatisfied(it, session))
            Assert.assertTrue("This room has 3 members", conditionEqual3Bis.isSatisfied(it, session))
            Assert.assertFalse("This room has more than 3 members", conditionLessThan3.isSatisfied(it, session))
        }
    }

    @Test
    fun test_notice_condition() {
        val conditionEqual = EventMatchCondition("content.msgtype", "m.notice")

        Event(
                type = "m.room.message",
                eventId = "mx0",
                content = MessageTextContent("m.notice", "A").toContent(),
                originServerTs = 0,
                roomId = "2joined").also {
            Assert.assertTrue("Notice", conditionEqual.isSatisfied(it))
        }
    }

    class MockRoomService() : RoomService {

        override fun createRoom(createRoomParams: CreateRoomParams, callback: MatrixCallback<String>): Cancelable {
            TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
        }

        override fun joinRoom(roomId: String, viaServers: List<String>, callback: MatrixCallback<Unit>): Cancelable {
            TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
        }

        override fun getRoom(roomId: String): Room? {
            return when (roomId) {
                "2joined" -> MockRoom(roomId, 2)
                "3joined" -> MockRoom(roomId, 3)
                else      -> null
            }
        }

        override fun liveRoomSummaries(): LiveData<List<RoomSummary>> {
            return MutableLiveData()
        }
    }

    class MockRoom(override val roomId: String, val _numberOfJoinedMembers: Int) : Room {
        override fun resendTextMessage(localEcho: TimelineEvent): Cancelable? {
            TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
        }

        override fun resendMediaMessage(localEcho: TimelineEvent): Cancelable? {
            TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
        }

        override fun deleteFailedEcho(localEcho: TimelineEvent) {
            TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
        }

        override fun clearSendingQueue() {
            TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
        }

        override fun resendAllFailedMessages() {
            TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
        }

        override fun saveDraft(draft: UserDraft) {
            TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
        }

        override fun deleteDraft() {
            TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
        }

        override fun getDraftsLive(): LiveData<List<UserDraft>> {
            TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
        }

        override fun getEventReadReceiptsLive(eventId: String): LiveData<List<ReadReceipt>> {
            TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
        }

        override fun getStateEvent(eventType: String): Event? {
            TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
        }

        override fun editReply(replyToEdit: TimelineEvent, originalTimelineEvent: TimelineEvent, newBodyText: String, compatibilityBodyText: String): Cancelable {
            TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
        }

        override fun fetchEditHistory(eventId: String, callback: MatrixCallback<List<Event>>) {
        }

        override fun liveTimeLineEvent(eventId: String): LiveData<TimelineEvent> {
            TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
        }

        override fun getNumberOfJoinedMembers(): Int {
            return _numberOfJoinedMembers
        }

        override fun liveRoomSummary(): LiveData<RoomSummary> {
            TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
        }

        override fun roomSummary(): RoomSummary? {
            TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
        }

        override fun createTimeline(eventId: String?, settings: TimelineSettings): Timeline {
            TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
        }

        override fun getTimeLineEvent(eventId: String): TimelineEvent? {
            TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
        }

        override fun sendTextMessage(text: String, msgType: String, autoMarkdown: Boolean): Cancelable {
            TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
        }

        override fun sendFormattedTextMessage(text: String, formattedText: String): Cancelable {
            TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
        }

        override fun sendMedia(attachment: ContentAttachmentData): Cancelable {
            TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
        }

        override fun sendMedias(attachments: List<ContentAttachmentData>): Cancelable {
            TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
        }

        override fun redactEvent(event: Event, reason: String?): Cancelable {
            TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
        }

        override fun markAllAsRead(callback: MatrixCallback<Unit>) {
            TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
        }

        override fun setReadReceipt(eventId: String, callback: MatrixCallback<Unit>) {
            TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
        }

        override fun setReadMarker(fullyReadEventId: String, callback: MatrixCallback<Unit>) {
            TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
        }

        override fun isEventRead(eventId: String): Boolean {
            TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
        }

        override fun loadRoomMembersIfNeeded(matrixCallback: MatrixCallback<Unit>): Cancelable {
            TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
        }

        override fun getRoomMember(userId: String): RoomMember? {
            TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
        }

        override fun getRoomMemberIdsLive(): LiveData<List<String>> {
            TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
        }

        override fun invite(userId: String, callback: MatrixCallback<Unit>): Cancelable {
            TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
        }

        override fun join(viaServers: List<String>, callback: MatrixCallback<Unit>): Cancelable {
            TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
        }

        override fun leave(callback: MatrixCallback<Unit>): Cancelable {
            TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
        }

        override fun updateTopic(topic: String, callback: MatrixCallback<Unit>) {
            TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
        }

        override fun sendReaction(targetEventId: String, reaction: String): Cancelable {
            TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
        }

        override fun undoReaction(reaction: String, targetEventId: String, myUserId: String) {
            TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
        }

        override fun editTextMessage(targetEventId: String, msgType: String, newBodyText: String,
                                     newBodyAutoMarkdown: Boolean, compatibilityBodyText: String): Cancelable {
            TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
        }

        override fun replyToMessage(eventReplied: TimelineEvent, replyText: String, autoMarkdown: Boolean): Cancelable? {
            TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
        }

        override fun getEventSummaryLive(eventId: String): LiveData<EventAnnotationsSummary> {
            TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
        }

        override fun isEncrypted(): Boolean {
            TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
        }

        override fun encryptionAlgorithm(): String? {
            TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
        }

        override fun shouldEncryptForInvitedMembers(): Boolean {
            TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
        }
    }
}
