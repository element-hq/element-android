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
import androidx.lifecycle.Observer
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.toContent
import im.vector.matrix.android.api.session.room.Room
import im.vector.matrix.android.api.session.room.model.Membership
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.matrix.android.api.session.room.model.create.CreateRoomParams
import im.vector.matrix.android.api.session.room.roomSummaryQueryParams
import im.vector.matrix.android.api.session.room.timeline.Timeline
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.matrix.android.api.session.room.timeline.TimelineSettings
import im.vector.matrix.android.internal.crypto.MXCRYPTO_ALGORITHM_MEGOLM
import im.vector.matrix.android.internal.crypto.MXCRYPTO_ALGORITHM_MEGOLM_BACKUP
import im.vector.matrix.android.internal.crypto.keysbackup.model.MegolmBackupAuthData
import im.vector.matrix.android.internal.crypto.keysbackup.model.MegolmBackupCreationInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import java.util.concurrent.CountDownLatch

class CryptoTestHelper(private val mTestHelper: CommonTestHelper) {

    private val messagesFromAlice: List<String> = listOf("0 - Hello I'm Alice!", "4 - Go!")
    private val messagesFromBob: List<String> = listOf("1 - Hello I'm Bob!", "2 - Isn't life grand?", "3 - Let's go to the opera.")

    private val defaultSessionParams = SessionTestParams(true)

    /**
     * @return alice session
     */
    fun doE2ETestWithAliceInARoom(encryptedRoom: Boolean = true): CryptoTestData {
        val aliceSession = mTestHelper.createAccount(TestConstants.USER_ALICE, defaultSessionParams)

        val roomId = mTestHelper.doSync<String> {
            aliceSession.createRoom(CreateRoomParams(name = "MyRoom"), it)
        }

        if (encryptedRoom) {
            val room = aliceSession.getRoom(roomId)!!

            mTestHelper.doSync<Unit> {
                room.enableEncryption(callback = it)
            }
        }

        return CryptoTestData(aliceSession, roomId)
    }

    /**
     * @return alice and bob sessions
     */
    fun doE2ETestWithAliceAndBobInARoom(encryptedRoom: Boolean = true): CryptoTestData {
        val cryptoTestData = doE2ETestWithAliceInARoom(encryptedRoom)
        val aliceSession = cryptoTestData.firstSession
        val aliceRoomId = cryptoTestData.roomId

        val aliceRoom = aliceSession.getRoom(aliceRoomId)!!

        val bobSession = mTestHelper.createAccount(TestConstants.USER_BOB, defaultSessionParams)

        val lock1 = CountDownLatch(1)

        val bobRoomSummariesLive = runBlocking(Dispatchers.Main) {
            bobSession.getRoomSummariesLive(roomSummaryQueryParams { })
        }

        val newRoomObserver = object : Observer<List<RoomSummary>> {
            override fun onChanged(t: List<RoomSummary>?) {
                if (t?.isNotEmpty() == true) {
                    lock1.countDown()
                    bobRoomSummariesLive.removeObserver(this)
                }
            }
        }

        GlobalScope.launch(Dispatchers.Main) {
            bobRoomSummariesLive.observeForever(newRoomObserver)
        }

        mTestHelper.doSync<Unit> {
            aliceRoom.invite(bobSession.myUserId, callback = it)
        }

        mTestHelper.await(lock1)

        val lock = CountDownLatch(1)

        val roomJoinedObserver = object : Observer<List<RoomSummary>> {
            override fun onChanged(t: List<RoomSummary>?) {
                if (bobSession.getRoom(aliceRoomId)
                                ?.getRoomMember(aliceSession.myUserId)
                                ?.membership == Membership.JOIN) {
                    lock.countDown()
                    bobRoomSummariesLive.removeObserver(this)
                }
            }
        }

        GlobalScope.launch(Dispatchers.Main) {
            bobRoomSummariesLive.observeForever(roomJoinedObserver)
        }

        mTestHelper.doSync<Unit> { bobSession.joinRoom(aliceRoomId, callback = it) }

        mTestHelper.await(lock)

        // Ensure bob can send messages to the room
//        val roomFromBobPOV = bobSession.getRoom(aliceRoomId)!!
//        assertNotNull(roomFromBobPOV.powerLevels)
//        assertTrue(roomFromBobPOV.powerLevels.maySendMessage(bobSession.myUserId))

        return CryptoTestData(aliceSession, aliceRoomId, bobSession)
    }

    /**
     * @return Alice, Bob and Sam session
     */
    fun doE2ETestWithAliceAndBobAndSamInARoom(): CryptoTestData {
        val cryptoTestData = doE2ETestWithAliceAndBobInARoom()
        val aliceSession = cryptoTestData.firstSession
        val aliceRoomId = cryptoTestData.roomId

        val room = aliceSession.getRoom(aliceRoomId)!!

        val samSession = createSamAccountAndInviteToTheRoom(room)

        // wait the initial sync
        SystemClock.sleep(1000)

        return CryptoTestData(aliceSession, aliceRoomId, cryptoTestData.secondSession, samSession)
    }

    /**
     * Create Sam account and invite him in the room. He will accept the invitation
     * @Return Sam session
     */
    fun createSamAccountAndInviteToTheRoom(room: Room): Session {
        val samSession = mTestHelper.createAccount(TestConstants.USER_SAM, defaultSessionParams)

        mTestHelper.doSync<Unit> {
            room.invite(samSession.myUserId, null, it)
        }

        mTestHelper.doSync<Unit> {
            samSession.joinRoom(room.roomId, null, it)
        }

        return samSession
    }

    /**
     * @return Alice and Bob sessions
     */
    fun doE2ETestWithAliceAndBobInARoomWithEncryptedMessages(): CryptoTestData {
        val cryptoTestData = doE2ETestWithAliceAndBobInARoom()
        val aliceSession = cryptoTestData.firstSession
        val aliceRoomId = cryptoTestData.roomId
        val bobSession = cryptoTestData.secondSession!!

        bobSession.cryptoService().setWarnOnUnknownDevices(false)

        aliceSession.cryptoService().setWarnOnUnknownDevices(false)

        val roomFromBobPOV = bobSession.getRoom(aliceRoomId)!!
        val roomFromAlicePOV = aliceSession.getRoom(aliceRoomId)!!

        val lock = CountDownLatch(1)

        val bobEventsListener = object : Timeline.Listener {
            override fun onTimelineFailure(throwable: Throwable) {
                // noop
            }

            override fun onNewTimelineEvents(eventIds: List<String>) {
                // noop
            }

            override fun onTimelineUpdated(snapshot: List<TimelineEvent>) {
                val messages = snapshot.filter { it.root.getClearType() == EventType.MESSAGE }
                        .groupBy { it.root.senderId!! }

                // Alice has sent 2 messages and Bob has sent 3 messages
                if (messages[aliceSession.myUserId]?.size == 2 && messages[bobSession.myUserId]?.size == 3) {
                    lock.countDown()
                }
            }
        }

        val bobTimeline = roomFromBobPOV.createTimeline(null, TimelineSettings(20))
        bobTimeline.start()
        bobTimeline.addListener(bobEventsListener)

        // Alice sends a message
        roomFromAlicePOV.sendTextMessage(messagesFromAlice[0])

        // Bob send 3 messages
        roomFromBobPOV.sendTextMessage(messagesFromBob[0])
        roomFromBobPOV.sendTextMessage(messagesFromBob[1])
        roomFromBobPOV.sendTextMessage(messagesFromBob[2])

        // Alice sends a message
        roomFromAlicePOV.sendTextMessage(messagesFromAlice[1])

        mTestHelper.await(lock)

        bobTimeline.removeListener(bobEventsListener)
        bobTimeline.dispose()

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
                signatures = mapOf("something" to mapOf("ed25519:something" to "hijklmnop"))
        )
    }

    fun createFakeMegolmBackupCreationInfo(): MegolmBackupCreationInfo {
        return MegolmBackupCreationInfo(
                algorithm = MXCRYPTO_ALGORITHM_MEGOLM_BACKUP,
                authData = createFakeMegolmBackupAuthData()
        )
    }
}
