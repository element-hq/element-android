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

// import android.os.SystemClock
// import android.text.TextUtils
// import im.vector.matrix.android.api.session.Session
// import im.vector.matrix.android.api.session.events.model.Event
// import im.vector.matrix.android.api.session.events.model.EventType
// import im.vector.matrix.android.api.session.events.model.toContent
// import im.vector.matrix.android.api.session.room.model.create.CreateRoomParams
// import im.vector.matrix.android.api.session.room.model.message.MessageTextContent
// import im.vector.matrix.android.api.session.room.model.message.MessageType
// import im.vector.matrix.android.internal.crypto.MXCRYPTO_ALGORITHM_MEGOLM
// import im.vector.matrix.android.internal.crypto.MXCRYPTO_ALGORITHM_MEGOLM_BACKUP
// import im.vector.matrix.android.internal.crypto.keysbackup.model.MegolmBackupAuthData
// import im.vector.matrix.android.internal.crypto.keysbackup.model.MegolmBackupCreationInfo
// import org.junit.Assert.*
// import java.util.*
// import java.util.concurrent.CountDownLatch
//
// class CryptoTestHelper(val mTestHelper: CommonTestHelper) {
//
//     val messagesFromAlice: List<String> = Arrays.asList("0 - Hello I'm Alice!", "4 - Go!")
//     val messagesFromBob: List<String> = Arrays.asList("1 - Hello I'm Bob!", "2 - Isn't life grand?", "3 - Let's go to the opera.")
//
//     // Set this value to false to test the new Realm store and to true to test legacy Filestore
//     val USE_LEGACY_CRYPTO_STORE = false
//
//     // Lazy loading is on by default now
//     private val LAZY_LOADING_ENABLED = true
//
//     val defaultSessionParams = SessionTestParams(true, false, LAZY_LOADING_ENABLED, USE_LEGACY_CRYPTO_STORE)
//     val encryptedSessionParams = SessionTestParams(true, true, LAZY_LOADING_ENABLED, USE_LEGACY_CRYPTO_STORE)
//
//     fun buildTextEvent(text: String, session: Session, roomId: String): Event {
//         val message = MessageTextContent(
//                 MessageType.MSGTYPE_TEXT,
//                 text
//         )
//
//         return Event(
//                 type = EventType.MESSAGE,
//                 content = message.toContent(),
//                 senderId = session.myUserId,
//                 roomId = roomId)
//     }
//
//     /**
//      * @return alice session
//      */
//     fun doE2ETestWithAliceInARoom(): CryptoTestData {
//         val results = HashMap<String, Any>()
//         val aliceSession = mTestHelper.createAccount(TestConstants.USER_ALICE, defaultSessionParams)
//
//         var roomId: String? = null
//         val lock1 = CountDownLatch(1)
//
//         aliceSession.createRoom(CreateRoomParams().apply { name = "MyRoom" }, object : TestMatrixCallback<String>(lock1) {
//             override fun onSuccess(data: String) {
//                 roomId = data
//                 super.onSuccess(data)
//             }
//         })
//
//         mTestHelper.await(lock1)
//         assertNotNull(roomId)
//
//         val room = aliceSession.getRoom(roomId!!)
//
//         val lock2 = CountDownLatch(1)
//         room.enableEncryptionWithAlgorithm(MXCRYPTO_ALGORITHM_MEGOLM, object : TestMatrixCallback<Void?>(lock2) {
//             override fun onSuccess(data: Void?) {
//                 results["enableEncryptionWithAlgorithm"] = "enableEncryptionWithAlgorithm"
//                 super.onSuccess(data)
//             }
//         })
//         mTestHelper.await(lock2)
//         assertTrue(results.containsKey("enableEncryptionWithAlgorithm"))
//
//         return CryptoTestData(aliceSession, roomId!!)
//     }
//
//     /**
//      * @param cryptedBob
//      * @return alice and bob sessions
//      */
//     fun doE2ETestWithAliceAndBobInARoom(cryptedBob: Boolean = true): CryptoTestData {
//         val statuses = HashMap<String, String>()
//
//         val cryptoTestData = doE2ETestWithAliceInARoom()
//         val aliceSession = cryptoTestData.firstSession
//         val aliceRoomId = cryptoTestData.roomId
//
//         val room = aliceSession.getRoom(aliceRoomId)!!
//
//         val bobSession = mTestHelper.createAccount(TestConstants.USER_BOB, defaultSessionParams)
//
//         val lock1 = CountDownLatch(2)
//
//         val bobEventListener = object : MXEventListener() {
//             override fun onNewRoom(roomId: String) {
//                 if (TextUtils.equals(roomId, aliceRoomId)) {
//                     if (!statuses.containsKey("onNewRoom")) {
//                         statuses["onNewRoom"] = "onNewRoom"
//                         lock1.countDown()
//                     }
//                 }
//             }
//         }
//
//         bobSession.dataHandler.addListener(bobEventListener)
//
//         room.invite(bobSession.myUserId, callback = object : TestMatrixCallback<Unit>(lock1) {
//             override fun onSuccess(data: Unit) {
//                 statuses["invite"] = "invite"
//                 super.onSuccess(data)
//             }
//         })
//
//         mTestHelper.await(lock1)
//
//         assertTrue(statuses.containsKey("invite") && statuses.containsKey("onNewRoom"))
//
//         bobSession.dataHandler.removeListener(bobEventListener)
//
//         val lock2 = CountDownLatch(2)
//
//         bobSession.joinRoom(aliceRoomId, callback = TestMatrixCallback(lock2))
//
//         room.addEventListener(object : MXEventListener() {
//             override fun onLiveEvent(event: Event, roomState: RoomState) {
//                 if (TextUtils.equals(event.getType(), Event.EVENT_TYPE_STATE_ROOM_MEMBER)) {
//                     val contentToConsider = event.contentAsJsonObject
//                     val member = JsonUtils.toRoomMember(contentToConsider)
//
//                     if (TextUtils.equals(member.membership, RoomMember.MEMBERSHIP_JOIN)) {
//                         statuses["AliceJoin"] = "AliceJoin"
//                         lock2.countDown()
//                     }
//                 }
//             }
//         })
//
//         mTestHelper.await(lock2)
//
//         // Ensure bob can send messages to the room
//         val roomFromBobPOV = bobSession.getRoom(aliceRoomId)!!
//         assertNotNull(roomFromBobPOV.state.powerLevels)
//         assertTrue(roomFromBobPOV.state.powerLevels.maySendMessage(bobSession.myUserId))
//
//         assertTrue(statuses.toString() + "", statuses.containsKey("AliceJoin"))
//
//         bobSession.dataHandler.removeListener(bobEventListener)
//
//         return CryptoTestData(aliceSession, aliceRoomId, bobSession)
//     }
//
//     /**
//      * @return Alice, Bob and Sam session
//      */
//     fun doE2ETestWithAliceAndBobAndSamInARoom(): CryptoTestData {
//         val statuses = HashMap<String, String>()
//
//         val cryptoTestData = doE2ETestWithAliceAndBobInARoom(true)
//         val aliceSession = cryptoTestData.firstSession
//         val aliceRoomId = cryptoTestData.roomId
//
//         val room = aliceSession.getRoom(aliceRoomId)!!
//
//         val samSession = mTestHelper.createAccount(TestConstants.USER_SAM, defaultSessionParams)
//
//         val lock1 = CountDownLatch(2)
//
//         val samEventListener = object : MXEventListener() {
//             override fun onNewRoom(roomId: String) {
//                 if (TextUtils.equals(roomId, aliceRoomId)) {
//                     if (!statuses.containsKey("onNewRoom")) {
//                         statuses["onNewRoom"] = "onNewRoom"
//                         lock1.countDown()
//                     }
//                 }
//             }
//         }
//
//         samSession.dataHandler.addListener(samEventListener)
//
//         room.invite(aliceSession, samSession.myUserId, object : TestMatrixCallback<Void?>(lock1) {
//             override fun onSuccess(info: Void?) {
//                 statuses["invite"] = "invite"
//                 super.onSuccess(info)
//             }
//         })
//
//         mTestHelper.await(lock1)
//
//         assertTrue(statuses.containsKey("invite") && statuses.containsKey("onNewRoom"))
//
//         samSession.dataHandler.removeListener(samEventListener)
//
//         val lock2 = CountDownLatch(1)
//
//         samSession.joinRoom(aliceRoomId, object : TestMatrixCallback<String>(lock2) {
//             override fun onSuccess(info: String) {
//                 statuses["joinRoom"] = "joinRoom"
//                 super.onSuccess(info)
//             }
//         })
//
//         mTestHelper.await(lock2)
//         assertTrue(statuses.containsKey("joinRoom"))
//
//         // wait the initial sync
//         SystemClock.sleep(1000)
//
//         samSession.dataHandler.removeListener(samEventListener)
//
//         return CryptoTestData(aliceSession, aliceRoomId, cryptoTestData.secondSession, samSession)
//     }
//
//     /**
//      * @param cryptedBob
//      * @return Alice and Bob sessions
//      */
//     fun doE2ETestWithAliceAndBobInARoomWithEncryptedMessages(cryptedBob: Boolean): CryptoTestData {
//         val cryptoTestData = doE2ETestWithAliceAndBobInARoom(cryptedBob)
//         val aliceSession = cryptoTestData.firstSession
//         val aliceRoomId = cryptoTestData.roomId
//         val bobSession = cryptoTestData.secondSession!!
//
//         bobSession.setWarnOnUnknownDevices(false)
//
//         aliceSession.setWarnOnUnknownDevices(false)
//
//         val roomFromBobPOV = bobSession.getRoom(aliceRoomId)!!
//         val roomFromAlicePOV = aliceSession.getRoom(aliceRoomId)!!
//
//         var messagesReceivedByBobCount = 0
//         var lock = CountDownLatch(3)
//
//         val bobEventsListener = object : MXEventListener() {
//             override fun onLiveEvent(event: Event, roomState: RoomState) {
//                 if (TextUtils.equals(event.getType(), Event.EVENT_TYPE_MESSAGE) && !TextUtils.equals(event.getSender(), bobSession.myUserId)) {
//                     messagesReceivedByBobCount++
//                     lock.countDown()
//                 }
//             }
//         }
//
//         roomFromBobPOV.addEventListener(bobEventsListener)
//
//         val results = HashMap<String, Any>()
//
//         bobSession.dataHandler.addListener(object : MXEventListener() {
//             override fun onToDeviceEvent(event: Event) {
//                 results["onToDeviceEvent"] = event
//                 lock.countDown()
//             }
//         })
//
//         // Alice sends a message
//         roomFromAlicePOV.sendEvent(buildTextEvent(messagesFromAlice[0], aliceSession, aliceRoomId), TestMatrixCallback<Void>(lock, true))
//         mTestHelper.await(lock)
//         assertTrue(results.containsKey("onToDeviceEvent"))
//         assertEquals(1, messagesReceivedByBobCount)
//
//         // Bob send a message
//         lock = CountDownLatch(1)
//         roomFromBobPOV.sendEvent(buildTextEvent(messagesFromBob[0], bobSession, aliceRoomId), TestMatrixCallback<Void>(lock, true))
//         // android does not echo the messages sent from itself
//         messagesReceivedByBobCount++
//         mTestHelper.await(lock)
//         assertEquals(2, messagesReceivedByBobCount)
//
//         // Bob send a message
//         lock = CountDownLatch(1)
//         roomFromBobPOV.sendEvent(buildTextEvent(messagesFromBob[1], bobSession, aliceRoomId), TestMatrixCallback<Void>(lock, true))
//         // android does not echo the messages sent from itself
//         messagesReceivedByBobCount++
//         mTestHelper.await(lock)
//         assertEquals(3, messagesReceivedByBobCount)
//
//         // Bob send a message
//         lock = CountDownLatch(1)
//         roomFromBobPOV.sendEvent(buildTextEvent(messagesFromBob[2], bobSession, aliceRoomId), TestMatrixCallback<Void>(lock, true))
//         // android does not echo the messages sent from itself
//         messagesReceivedByBobCount++
//         mTestHelper.await(lock)
//         assertEquals(4, messagesReceivedByBobCount)
//
//         // Alice sends a message
//         lock = CountDownLatch(2)
//         roomFromAlicePOV.sendEvent(buildTextEvent(messagesFromAlice[1], aliceSession, aliceRoomId), TestMatrixCallback<Void>(lock, true))
//         mTestHelper.await(lock)
//         assertEquals(5, messagesReceivedByBobCount)
//
//         return cryptoTestData
//     }
//
//     fun checkEncryptedEvent(event: CryptoEvent, roomId: String, clearMessage: String, senderSession: Session) {
//         assertEquals(EventType.ENCRYPTED, event.wireType)
//         assertNotNull(event.wireContent)
//
//         val eventWireContent = event.wireContent.asJsonObject
//         assertNotNull(eventWireContent)
//
//         assertNull(eventWireContent.get("body"))
//         assertEquals(MXCRYPTO_ALGORITHM_MEGOLM, eventWireContent.get("algorithm").asString)
//
//         assertNotNull(eventWireContent.get("ciphertext"))
//         assertNotNull(eventWireContent.get("session_id"))
//         assertNotNull(eventWireContent.get("sender_key"))
//
//         assertEquals(senderSession.sessionParams.credentials.deviceId, eventWireContent.get("device_id").asString)
//
//         assertNotNull(event.getEventId())
//         assertEquals(roomId, event.getRoomId())
//         assertEquals(EventType.MESSAGE, event.getType())
//         assertTrue(event.getAge() < 10000)
//
//         val eventContent = event.contentAsJsonObject
//         assertNotNull(eventContent)
//         assertEquals(clearMessage, eventContent.get("body").asString)
//         assertEquals(senderSession.myUserId, event.getSender())
//     }
//
//     fun createFakeMegolmBackupAuthData(): MegolmBackupAuthData {
//         return MegolmBackupAuthData(
//                 publicKey = "abcdefg",
//                 signatures = HashMap<String, Map<String, String>>().apply {
//                     this["something"] = HashMap<String, String>().apply {
//                         this["ed25519:something"] = "hijklmnop"
//                     }
//                 }
//         )
//     }
//
//     fun createFakeMegolmBackupCreationInfo(): MegolmBackupCreationInfo {
//         return MegolmBackupCreationInfo().apply {
//             algorithm = MXCRYPTO_ALGORITHM_MEGOLM_BACKUP
//             authData = createFakeMegolmBackupAuthData()
//         }
//     }
// }
