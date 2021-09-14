/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.common

import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.Observer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.matrix.android.sdk.api.auth.UIABaseAuth
import org.matrix.android.sdk.api.auth.UserInteractiveAuthInterceptor
import org.matrix.android.sdk.api.auth.UserPasswordAuth
import org.matrix.android.sdk.api.auth.registration.RegistrationFlowResponse
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.verification.IncomingSasVerificationTransaction
import org.matrix.android.sdk.api.session.crypto.verification.OutgoingSasVerificationTransaction
import org.matrix.android.sdk.api.session.crypto.verification.VerificationMethod
import org.matrix.android.sdk.api.session.crypto.verification.VerificationTxState
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.model.create.CreateRoomParams
import org.matrix.android.sdk.api.session.room.roomSummaryQueryParams
import org.matrix.android.sdk.internal.crypto.MXCRYPTO_ALGORITHM_MEGOLM
import org.matrix.android.sdk.internal.crypto.MXCRYPTO_ALGORITHM_MEGOLM_BACKUP
import org.matrix.android.sdk.internal.crypto.keysbackup.model.MegolmBackupAuthData
import org.matrix.android.sdk.internal.crypto.keysbackup.model.MegolmBackupCreationInfo
import java.util.UUID
import java.util.concurrent.CountDownLatch
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class CryptoTestHelper(private val mTestHelper: CommonTestHelper) {

    private val messagesFromAlice: List<String> = listOf("0 - Hello I'm Alice!", "4 - Go!")
    private val messagesFromBob: List<String> = listOf("1 - Hello I'm Bob!", "2 - Isn't life grand?", "3 - Let's go to the opera.")

    private val defaultSessionParams = SessionTestParams(true)

    /**
     * @return alice session
     */
    fun doE2ETestWithAliceInARoom(encryptedRoom: Boolean = true): CryptoTestData {
        val aliceSession = mTestHelper.createAccount(TestConstants.USER_ALICE, defaultSessionParams)

        val roomId = mTestHelper.runBlockingTest {
            aliceSession.createRoom(CreateRoomParams().apply { name = "MyRoom" })
        }

        if (encryptedRoom) {
            val room = aliceSession.getRoom(roomId)!!

            mTestHelper.runBlockingTest {
                room.enableEncryption()
            }
        }

        return CryptoTestData(roomId, listOf(aliceSession))
    }

    /**
     * @return alice and bob sessions
     */
    @Suppress("EXPERIMENTAL_API_USAGE")
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

        mTestHelper.runBlockingTest {
            aliceRoom.invite(bobSession.myUserId)
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

        mTestHelper.runBlockingTest { bobSession.joinRoom(aliceRoomId) }

        mTestHelper.await(lock)

        // Ensure bob can send messages to the room
//        val roomFromBobPOV = bobSession.getRoom(aliceRoomId)!!
//        assertNotNull(roomFromBobPOV.powerLevels)
//        assertTrue(roomFromBobPOV.powerLevels.maySendMessage(bobSession.myUserId))

        return CryptoTestData(aliceRoomId, listOf(aliceSession, bobSession))
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

        return CryptoTestData(aliceRoomId, listOf(aliceSession, cryptoTestData.secondSession!!, samSession))
    }

    /**
     * Create Sam account and invite him in the room. He will accept the invitation
     * @Return Sam session
     */
    fun createSamAccountAndInviteToTheRoom(room: Room): Session {
        val samSession = mTestHelper.createAccount(TestConstants.USER_SAM, defaultSessionParams)

        mTestHelper.runBlockingTest {
            room.invite(samSession.myUserId, null)
        }

        mTestHelper.runBlockingTest {
            samSession.joinRoom(room.roomId, null, emptyList())
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

        // Alice sends a message
        mTestHelper.sendTextMessage(roomFromAlicePOV, messagesFromAlice[0], 1)
//        roomFromAlicePOV.sendTextMessage(messagesFromAlice[0])

        // Bob send 3 messages
        mTestHelper.sendTextMessage(roomFromBobPOV, messagesFromBob[0], 1)
        mTestHelper.sendTextMessage(roomFromBobPOV, messagesFromBob[1], 1)
        mTestHelper.sendTextMessage(roomFromBobPOV, messagesFromBob[2], 1)
        // Alice sends a message
        mTestHelper.sendTextMessage(roomFromAlicePOV, messagesFromAlice[1], 1)

        return cryptoTestData
    }

    fun checkEncryptedEvent(event: Event, roomId: String, clearMessage: String, senderSession: Session) {
        assertEquals(EventType.ENCRYPTED, event.type)
        assertNotNull(event.content)

        val eventWireContent = event.content.toContent()
        assertNotNull(eventWireContent)

        assertNull(eventWireContent["body"])
        assertEquals(MXCRYPTO_ALGORITHM_MEGOLM, eventWireContent["algorithm"])

        assertNotNull(eventWireContent["ciphertext"])
        assertNotNull(eventWireContent["session_id"])
        assertNotNull(eventWireContent["sender_key"])

        assertEquals(senderSession.sessionParams.deviceId, eventWireContent["device_id"])

        assertNotNull(event.eventId)
        assertEquals(roomId, event.roomId)
        assertEquals(EventType.MESSAGE, event.getClearType())
        // TODO assertTrue(event.getAge() < 10000)

        val eventContent = event.toContent()
        assertNotNull(eventContent)
        assertEquals(clearMessage, eventContent["body"])
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
                authData = createFakeMegolmBackupAuthData(),
                recoveryKey = "fake"
        )
    }

    @Suppress("EXPERIMENTAL_API_USAGE")
    fun createDM(alice: Session, bob: Session): String {
        val roomId = mTestHelper.runBlockingTest {
            alice.createDirectRoom(bob.myUserId)
        }

        mTestHelper.waitWithLatch { latch ->
            val bobRoomSummariesLive = runBlocking(Dispatchers.Main) {
                bob.getRoomSummariesLive(roomSummaryQueryParams { })
            }

            val newRoomObserver = object : Observer<List<RoomSummary>> {
                override fun onChanged(t: List<RoomSummary>?) {
                    val indexOfFirst = t?.indexOfFirst { it.roomId == roomId } ?: -1
                    if (indexOfFirst != -1) {
                        latch.countDown()
                        bobRoomSummariesLive.removeObserver(this)
                    }
                }
            }

            GlobalScope.launch(Dispatchers.Main) {
                bobRoomSummariesLive.observeForever(newRoomObserver)
            }
        }

        mTestHelper.waitWithLatch { latch ->
            val bobRoomSummariesLive = runBlocking(Dispatchers.Main) {
                bob.getRoomSummariesLive(roomSummaryQueryParams { })
            }

            val newRoomObserver = object : Observer<List<RoomSummary>> {
                override fun onChanged(t: List<RoomSummary>?) {
                    if (bob.getRoom(roomId)
                                    ?.getRoomMember(bob.myUserId)
                                    ?.membership == Membership.JOIN) {
                        latch.countDown()
                        bobRoomSummariesLive.removeObserver(this)
                    }
                }
            }

            GlobalScope.launch(Dispatchers.Main) {
                bobRoomSummariesLive.observeForever(newRoomObserver)
            }

            mTestHelper.runBlockingTest { bob.joinRoom(roomId) }
        }

        return roomId
    }

    fun initializeCrossSigning(session: Session) {
        mTestHelper.doSync<Unit> {
            session.cryptoService().crossSigningService()
                    .initializeCrossSigning(
                            object : UserInteractiveAuthInterceptor {
                                override fun performStage(flowResponse: RegistrationFlowResponse, errCode: String?, promise: Continuation<UIABaseAuth>) {
                                    promise.resume(
                                            UserPasswordAuth(
                                                    user = session.myUserId,
                                                    password = TestConstants.PASSWORD,
                                                    session = flowResponse.session
                                            )
                                    )
                                }
                            }, it)
        }
    }

    fun verifySASCrossSign(alice: Session, bob: Session, roomId: String) {
        assertTrue(alice.cryptoService().crossSigningService().canCrossSign())
        assertTrue(bob.cryptoService().crossSigningService().canCrossSign())

        val requestID = UUID.randomUUID().toString()
        val aliceVerificationService = alice.cryptoService().verificationService()
        val bobVerificationService = bob.cryptoService().verificationService()

        aliceVerificationService.beginKeyVerificationInDMs(
                VerificationMethod.SAS,
                requestID,
                roomId,
                bob.myUserId,
                bob.sessionParams.credentials.deviceId!!)

        // we should reach SHOW SAS on both
        var alicePovTx: OutgoingSasVerificationTransaction? = null
        var bobPovTx: IncomingSasVerificationTransaction? = null

        // wait for alice to get the ready
        mTestHelper.waitWithLatch {
            mTestHelper.retryPeriodicallyWithLatch(it) {
                bobPovTx = bobVerificationService.getExistingTransaction(alice.myUserId, requestID) as? IncomingSasVerificationTransaction
                Log.v("TEST", "== bobPovTx is ${alicePovTx?.uxState}")
                if (bobPovTx?.state == VerificationTxState.OnStarted) {
                    bobPovTx?.performAccept()
                    true
                } else {
                    false
                }
            }
        }

        mTestHelper.waitWithLatch {
            mTestHelper.retryPeriodicallyWithLatch(it) {
                alicePovTx = aliceVerificationService.getExistingTransaction(bob.myUserId, requestID) as? OutgoingSasVerificationTransaction
                Log.v("TEST", "== alicePovTx is ${alicePovTx?.uxState}")
                alicePovTx?.state == VerificationTxState.ShortCodeReady
            }
        }
        // wait for alice to get the ready
        mTestHelper.waitWithLatch {
            mTestHelper.retryPeriodicallyWithLatch(it) {
                bobPovTx = bobVerificationService.getExistingTransaction(alice.myUserId, requestID) as? IncomingSasVerificationTransaction
                Log.v("TEST", "== bobPovTx is ${alicePovTx?.uxState}")
                if (bobPovTx?.state == VerificationTxState.OnStarted) {
                    bobPovTx?.performAccept()
                }
                bobPovTx?.state == VerificationTxState.ShortCodeReady
            }
        }

        assertEquals("SAS code do not match", alicePovTx!!.getDecimalCodeRepresentation(), bobPovTx!!.getDecimalCodeRepresentation())

        bobPovTx!!.userHasVerifiedShortCode()
        alicePovTx!!.userHasVerifiedShortCode()

        mTestHelper.waitWithLatch {
            mTestHelper.retryPeriodicallyWithLatch(it) {
                alice.cryptoService().crossSigningService().isUserTrusted(bob.myUserId)
            }
        }

        mTestHelper.waitWithLatch {
            mTestHelper.retryPeriodicallyWithLatch(it) {
                alice.cryptoService().crossSigningService().isUserTrusted(bob.myUserId)
            }
        }
    }

    fun doE2ETestWithManyMembers(numberOfMembers: Int): CryptoTestData {
        val aliceSession = mTestHelper.createAccount(TestConstants.USER_ALICE, defaultSessionParams)
        aliceSession.cryptoService().setWarnOnUnknownDevices(false)

        val roomId = mTestHelper.runBlockingTest {
            aliceSession.createRoom(CreateRoomParams().apply { name = "MyRoom" })
        }
        val room = aliceSession.getRoom(roomId)!!

        mTestHelper.runBlockingTest {
            room.enableEncryption()
        }

        val sessions = mutableListOf(aliceSession)
        for (index in 1 until numberOfMembers) {
            val session = mTestHelper.createAccount("User_$index", defaultSessionParams)
            mTestHelper.runBlockingTest(timeout = 600_000) { room.invite(session.myUserId, null) }
            println("TEST -> " + session.myUserId + " invited")
            mTestHelper.runBlockingTest { session.joinRoom(room.roomId, null, emptyList()) }
            println("TEST -> " + session.myUserId + " joined")
            sessions.add(session)
        }

        return CryptoTestData(roomId, sessions)
    }
}
