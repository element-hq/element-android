/*
 * Copyright 2018 New Vector Ltd
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

package im.vector.matrix.android.common

import android.os.SystemClock
import android.text.TextUtils
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.toContent
import im.vector.matrix.android.api.session.room.model.create.CreateRoomParams
import im.vector.matrix.android.api.session.room.timeline.Timeline
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.matrix.android.api.session.room.timeline.TimelineSettings
import im.vector.matrix.android.internal.crypto.MXCRYPTO_ALGORITHM_MEGOLM
import im.vector.matrix.android.internal.crypto.MXCRYPTO_ALGORITHM_MEGOLM_BACKUP
import im.vector.matrix.android.internal.crypto.keysbackup.model.MegolmBackupAuthData
import im.vector.matrix.android.internal.crypto.keysbackup.model.MegolmBackupCreationInfo
import org.junit.Assert.*
import java.util.*
import java.util.concurrent.CountDownLatch

class CryptoTestHelper(val mTestHelper: CommonTestHelper) {

    val messagesFromAlice: List<String> = Arrays.asList("0 - Hello I'm Alice!", "4 - Go!")
    val messagesFromBob: List<String> = Arrays.asList("1 - Hello I'm Bob!", "2 - Isn't life grand?", "3 - Let's go to the opera.")

    val defaultSessionParams = SessionTestParams(true)

    /**
     * @return alice session
     */
    fun doE2ETestWithAliceInARoom(): CryptoTestData {
        val aliceSession = mTestHelper.createAccount(TestConstants.USER_ALICE, defaultSessionParams)

        var roomId: String? = null
        val lock1 = CountDownLatch(1)

        aliceSession.createRoom(CreateRoomParams().apply { name = "MyRoom" }, object : TestMatrixCallback<String>(lock1) {
            override fun onSuccess(data: String) {
                roomId = data
                super.onSuccess(data)
            }
        })

        mTestHelper.await(lock1)
        assertNotNull(roomId)

        val room = aliceSession.getRoom(roomId!!)!!

        val lock2 = CountDownLatch(1)
        room.enableEncryptionWithAlgorithm(MXCRYPTO_ALGORITHM_MEGOLM, object : TestMatrixCallback<Unit>(lock2) {})
        mTestHelper.await(lock2)

        return CryptoTestData(aliceSession, roomId!!)
    }

    /**
     * @return alice and bob sessions
     */
    fun doE2ETestWithAliceAndBobInARoom(): CryptoTestData {
        val statuses = HashMap<String, String>()

        val cryptoTestData = doE2ETestWithAliceInARoom()
        val aliceSession = cryptoTestData.firstSession
        val aliceRoomId = cryptoTestData.roomId

        val room = aliceSession.getRoom(aliceRoomId)!!

        val bobSession = mTestHelper.createAccount(TestConstants.USER_BOB, defaultSessionParams)

        val lock1 = CountDownLatch(2)

//        val bobEventListener = object : MXEventListener() {
//            override fun onNewRoom(roomId: String) {
//                if (TextUtils.equals(roomId, aliceRoomId)) {
//                    if (!statuses.containsKey("onNewRoom")) {
//                        statuses["onNewRoom"] = "onNewRoom"
//                        lock1.countDown()
//                    }
//                }
//            }
//        }
//
//        bobSession.dataHandler.addListener(bobEventListener)

        room.invite(bobSession.myUserId, callback = object : TestMatrixCallback<Unit>(lock1) {
            override fun onSuccess(data: Unit) {
                statuses["invite"] = "invite"
                super.onSuccess(data)
            }
        })

        mTestHelper.await(lock1)

        assertTrue(statuses.containsKey("invite") && statuses.containsKey("onNewRoom"))

//        bobSession.dataHandler.removeListener(bobEventListener)

        val lock2 = CountDownLatch(2)

        bobSession.joinRoom(aliceRoomId, callback = TestMatrixCallback(lock2))

//        room.addEventListener(object : MXEventListener() {
//            override fun onLiveEvent(event: Event, roomState: RoomState) {
//                if (TextUtils.equals(event.getType(), Event.EVENT_TYPE_STATE_ROOM_MEMBER)) {
//                    val contentToConsider = event.contentAsJsonObject
//                    val member = JsonUtils.toRoomMember(contentToConsider)
//
//                    if (TextUtils.equals(member.membership, RoomMember.MEMBERSHIP_JOIN)) {
//                        statuses["AliceJoin"] = "AliceJoin"
//                        lock2.countDown()
//                    }
//                }
//            }
//        })

        mTestHelper.await(lock2)

        // Ensure bob can send messages to the room
//        val roomFromBobPOV = bobSession.getRoom(aliceRoomId)!!
//        assertNotNull(roomFromBobPOV.powerLevels)
//        assertTrue(roomFromBobPOV.powerLevels.maySendMessage(bobSession.myUserId))

        assertTrue(statuses.toString() + "", statuses.containsKey("AliceJoin"))

//        bobSession.dataHandler.removeListener(bobEventListener)

        return CryptoTestData(aliceSession, aliceRoomId, bobSession)
    }

    /**
     * @return Alice, Bob and Sam session
     */
    fun doE2ETestWithAliceAndBobAndSamInARoom(): CryptoTestData {
        val statuses = HashMap<String, String>()

        val cryptoTestData = doE2ETestWithAliceAndBobInARoom()
        val aliceSession = cryptoTestData.firstSession
        val aliceRoomId = cryptoTestData.roomId

        val room = aliceSession.getRoom(aliceRoomId)!!

        val samSession = mTestHelper.createAccount(TestConstants.USER_SAM, defaultSessionParams)

        val lock1 = CountDownLatch(2)

//        val samEventListener = object : MXEventListener() {
//            override fun onNewRoom(roomId: String) {
//                if (TextUtils.equals(roomId, aliceRoomId)) {
//                    if (!statuses.containsKey("onNewRoom")) {
//                        statuses["onNewRoom"] = "onNewRoom"
//                        lock1.countDown()
//                    }
//                }
//            }
//        }
//
//        samSession.dataHandler.addListener(samEventListener)

        room.invite(samSession.myUserId, null, object : TestMatrixCallback<Unit>(lock1) {
            override fun onSuccess(data: Unit) {
                statuses["invite"] = "invite"
                super.onSuccess(data)
            }
        })

        mTestHelper.await(lock1)

        assertTrue(statuses.containsKey("invite") && statuses.containsKey("onNewRoom"))

//        samSession.dataHandler.removeListener(samEventListener)

        val lock2 = CountDownLatch(1)

        samSession.joinRoom(aliceRoomId, null, object : TestMatrixCallback<Unit>(lock2) {
            override fun onSuccess(data: Unit) {
                statuses["joinRoom"] = "joinRoom"
                super.onSuccess(data)
            }
        })

        mTestHelper.await(lock2)
        assertTrue(statuses.containsKey("joinRoom"))

        // wait the initial sync
        SystemClock.sleep(1000)

//        samSession.dataHandler.removeListener(samEventListener)

        return CryptoTestData(aliceSession, aliceRoomId, cryptoTestData.secondSession, samSession)
    }

    /**
     * @return Alice and Bob sessions
     */
    fun doE2ETestWithAliceAndBobInARoomWithEncryptedMessages(): CryptoTestData {
        val cryptoTestData = doE2ETestWithAliceAndBobInARoom()
        val aliceSession = cryptoTestData.firstSession
        val aliceRoomId = cryptoTestData.roomId
        val bobSession = cryptoTestData.secondSession!!

        bobSession.setWarnOnUnknownDevices(false)

        aliceSession.setWarnOnUnknownDevices(false)

        val roomFromBobPOV = bobSession.getRoom(aliceRoomId)!!
        val roomFromAlicePOV = aliceSession.getRoom(aliceRoomId)!!

        var lock = CountDownLatch(1)

        val bobEventsListener = object : Timeline.Listener {
            override fun onTimelineFailure(throwable: Throwable) {
            }

            override fun onTimelineUpdated(snapshot: List<TimelineEvent>) {
                val size = snapshot.filter { it.root.senderId != bobSession.myUserId && it.root.getClearType() == EventType.MESSAGE }
                        .size

                if (size == 3) {
                    lock.countDown()
                }
            }
        }

        val bobTimeline = roomFromBobPOV.createTimeline(null, TimelineSettings(10))
        bobTimeline.addListener(bobEventsListener)

        val results = HashMap<String, Any>()

        // bobSession.dataHandler.addListener(object : MXEventListener() {
        //     override fun onToDeviceEvent(event: Event) {
        //         results["onToDeviceEvent"] = event
        //         lock.countDown()
        //     }
        // })

        // Alice sends a message
        roomFromAlicePOV.sendTextMessage(messagesFromAlice[0])
        assertTrue(results.containsKey("onToDeviceEvent"))
//        assertEquals(1, messagesReceivedByBobCount)

        // Bob send a message
        lock = CountDownLatch(1)
        roomFromBobPOV.sendTextMessage(messagesFromBob[0])
        // android does not echo the messages sent from itself
//        messagesReceivedByBobCount++
        mTestHelper.await(lock)
//        assertEquals(2, messagesReceivedByBobCount)

        // Bob send a message
        lock = CountDownLatch(1)
        roomFromBobPOV.sendTextMessage(messagesFromBob[1])
        // android does not echo the messages sent from itself
//        messagesReceivedByBobCount++
        mTestHelper.await(lock)
//        assertEquals(3, messagesReceivedByBobCount)

        // Bob send a message
        lock = CountDownLatch(1)
        roomFromBobPOV.sendTextMessage(messagesFromBob[2])
        // android does not echo the messages sent from itself
//        messagesReceivedByBobCount++
        mTestHelper.await(lock)
//        assertEquals(4, messagesReceivedByBobCount)

        // Alice sends a message
        lock = CountDownLatch(2)
        roomFromAlicePOV.sendTextMessage(messagesFromAlice[1])
        mTestHelper.await(lock)
//        assertEquals(5, messagesReceivedByBobCount)

        bobTimeline.removeListener(bobEventsListener)

        return cryptoTestData
    }

    fun checkEncryptedEvent(event: Event, roomId: String, clearMessage: String, senderSession: Session) {
        assertEquals(EventType.ENCRYPTED, event.type)
        assertNotNull(event.content)

        val eventWireContent = event.content.toContent()
        assertNotNull(eventWireContent)

        assertNull(eventWireContent.get("body"))
        assertEquals(MXCRYPTO_ALGORITHM_MEGOLM, eventWireContent.get("algorithm"))

        assertNotNull(eventWireContent.get("ciphertext"))
        assertNotNull(eventWireContent.get("session_id"))
        assertNotNull(eventWireContent.get("sender_key"))

        assertEquals(senderSession.sessionParams.credentials.deviceId, eventWireContent.get("device_id"))

        assertNotNull(event.eventId)
        assertEquals(roomId, event.roomId)
        assertEquals(EventType.MESSAGE, event.getClearType())
        // TODO assertTrue(event.getAge() < 10000)

        val eventContent = event.toContent()
        assertNotNull(eventContent)
        assertEquals(clearMessage, eventContent.get("body"))
        assertEquals(senderSession.myUserId, event.senderId)
    }

    fun createFakeMegolmBackupAuthData(): MegolmBackupAuthData {
        return MegolmBackupAuthData(
                publicKey = "abcdefg",
                signatures = HashMap<String, Map<String, String>>().apply {
                    this["something"] = HashMap<String, String>().apply {
                        this["ed25519:something"] = "hijklmnop"
                    }
                }
        )
    }

    fun createFakeMegolmBackupCreationInfo(): MegolmBackupCreationInfo {
        return MegolmBackupCreationInfo().apply {
            algorithm = MXCRYPTO_ALGORITHM_MEGOLM_BACKUP
            authData = createFakeMegolmBackupAuthData()
        }
    }
}
