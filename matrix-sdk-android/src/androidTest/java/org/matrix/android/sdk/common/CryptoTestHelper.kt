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
import org.amshove.kluent.fail
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.matrix.android.sdk.api.auth.UIABaseAuth
import org.matrix.android.sdk.api.auth.UserInteractiveAuthInterceptor
import org.matrix.android.sdk.api.auth.UserPasswordAuth
import org.matrix.android.sdk.api.auth.registration.RegistrationFlowResponse
import org.matrix.android.sdk.api.crypto.MXCRYPTO_ALGORITHM_MEGOLM
import org.matrix.android.sdk.api.crypto.MXCRYPTO_ALGORITHM_MEGOLM_BACKUP
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.MXCryptoError
import org.matrix.android.sdk.api.session.crypto.crosssigning.KEYBACKUP_SECRET_SSSS_NAME
import org.matrix.android.sdk.api.session.crypto.crosssigning.MASTER_KEY_SSSS_NAME
import org.matrix.android.sdk.api.session.crypto.crosssigning.SELF_SIGNING_KEY_SSSS_NAME
import org.matrix.android.sdk.api.session.crypto.crosssigning.USER_SIGNING_KEY_SSSS_NAME
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysVersion
import org.matrix.android.sdk.api.session.crypto.keysbackup.MegolmBackupAuthData
import org.matrix.android.sdk.api.session.crypto.keysbackup.MegolmBackupCreationInfo
import org.matrix.android.sdk.api.session.crypto.keysbackup.extractCurveKeyFromRecoveryKey
import org.matrix.android.sdk.api.session.crypto.model.OlmDecryptionResult
import org.matrix.android.sdk.api.session.crypto.verification.IncomingSasVerificationTransaction
import org.matrix.android.sdk.api.session.crypto.verification.OutgoingSasVerificationTransaction
import org.matrix.android.sdk.api.session.crypto.verification.VerificationMethod
import org.matrix.android.sdk.api.session.crypto.verification.VerificationTxState
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomHistoryVisibility
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.model.create.CreateRoomParams
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.session.room.roomSummaryQueryParams
import org.matrix.android.sdk.api.session.securestorage.EmptyKeySigner
import org.matrix.android.sdk.api.session.securestorage.KeyRef
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.api.util.awaitCallback
import org.matrix.android.sdk.api.util.toBase64NoPadding
import java.util.UUID
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class CryptoTestHelper(val testHelper: CommonTestHelper) {

    private val messagesFromAlice: List<String> = listOf("0 - Hello I'm Alice!", "4 - Go!")
    private val messagesFromBob: List<String> = listOf("1 - Hello I'm Bob!", "2 - Isn't life grand?", "3 - Let's go to the opera.")

    private val defaultSessionParams = SessionTestParams(true)

    /**
     * @return alice session
     */
    fun doE2ETestWithAliceInARoom(encryptedRoom: Boolean = true, roomHistoryVisibility: RoomHistoryVisibility? = null): CryptoTestData {
        val aliceSession = testHelper.createAccount(TestConstants.USER_ALICE, defaultSessionParams)

        val roomId = testHelper.runBlockingTest {
            aliceSession.roomService().createRoom(CreateRoomParams().apply {
                historyVisibility = roomHistoryVisibility
                name = "MyRoom"
            })
        }
        if (encryptedRoom) {
            testHelper.waitWithLatch { latch ->
                val room = aliceSession.getRoom(roomId)!!
                room.roomCryptoService().enableEncryption()
                val roomSummaryLive = room.getRoomSummaryLive()
                val roomSummaryObserver = object : Observer<Optional<RoomSummary>> {
                    override fun onChanged(roomSummary: Optional<RoomSummary>) {
                        if (roomSummary.getOrNull()?.isEncrypted.orFalse()) {
                            roomSummaryLive.removeObserver(this)
                            latch.countDown()
                        }
                    }
                }
                roomSummaryLive.observeForever(roomSummaryObserver)
            }
        }
        return CryptoTestData(roomId, listOf(aliceSession))
    }

    /**
     * @return alice and bob sessions
     */
    fun doE2ETestWithAliceAndBobInARoom(encryptedRoom: Boolean = true, roomHistoryVisibility: RoomHistoryVisibility? = null): CryptoTestData {
        val cryptoTestData = doE2ETestWithAliceInARoom(encryptedRoom, roomHistoryVisibility)
        val aliceSession = cryptoTestData.firstSession
        val aliceRoomId = cryptoTestData.roomId

        val aliceRoom = aliceSession.getRoom(aliceRoomId)!!

        val bobSession = testHelper.createAccount(TestConstants.USER_BOB, defaultSessionParams)

        testHelper.waitWithLatch { latch ->
            val bobRoomSummariesLive = bobSession.roomService().getRoomSummariesLive(roomSummaryQueryParams { })
            val newRoomObserver = object : Observer<List<RoomSummary>> {
                override fun onChanged(t: List<RoomSummary>?) {
                    if (t?.isNotEmpty() == true) {
                        bobRoomSummariesLive.removeObserver(this)
                        latch.countDown()
                    }
                }
            }
            bobRoomSummariesLive.observeForever(newRoomObserver)
            aliceRoom.membershipService().invite(bobSession.myUserId)
        }

        testHelper.waitWithLatch { latch ->
            val bobRoomSummariesLive = bobSession.roomService().getRoomSummariesLive(roomSummaryQueryParams { })
            val roomJoinedObserver = object : Observer<List<RoomSummary>> {
                override fun onChanged(t: List<RoomSummary>?) {
                    if (bobSession.getRoom(aliceRoomId)
                                    ?.membershipService()
                                    ?.getRoomMember(bobSession.myUserId)
                                    ?.membership == Membership.JOIN) {
                        bobRoomSummariesLive.removeObserver(this)
                        latch.countDown()
                    }
                }
            }
            bobRoomSummariesLive.observeForever(roomJoinedObserver)
            bobSession.roomService().joinRoom(aliceRoomId)
        }
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
        val samSession = testHelper.createAccount(TestConstants.USER_SAM, defaultSessionParams)

        testHelper.runBlockingTest {
            room.membershipService().invite(samSession.myUserId, null)
        }

        testHelper.runBlockingTest {
            samSession.roomService().joinRoom(room.roomId, null, emptyList())
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
        testHelper.sendTextMessage(roomFromAlicePOV, messagesFromAlice[0], 1).first().eventId.let { sentEventId ->
            // ensure bob got it
            ensureEventReceived(aliceRoomId, sentEventId, bobSession, true)
        }

        // Bob send 3 messages
        testHelper.sendTextMessage(roomFromBobPOV, messagesFromBob[0], 1).first().eventId.let { sentEventId ->
            // ensure alice got it
            ensureEventReceived(aliceRoomId, sentEventId, aliceSession, true)
        }

        testHelper.sendTextMessage(roomFromBobPOV, messagesFromBob[1], 1).first().eventId.let { sentEventId ->
            // ensure alice got it
            ensureEventReceived(aliceRoomId, sentEventId, aliceSession, true)
        }
        testHelper.sendTextMessage(roomFromBobPOV, messagesFromBob[2], 1).first().eventId.let { sentEventId ->
            // ensure alice got it
            ensureEventReceived(aliceRoomId, sentEventId, aliceSession, true)
        }

        // Alice sends a message
        testHelper.sendTextMessage(roomFromAlicePOV, messagesFromAlice[1], 1).first().eventId.let { sentEventId ->
            // ensure bob got it
            ensureEventReceived(aliceRoomId, sentEventId, bobSession, true)
        }
        return cryptoTestData
    }

    private fun ensureEventReceived(roomId: String, eventId: String, session: Session, andCanDecrypt: Boolean) {
        testHelper.waitWithLatch { latch ->
            testHelper.retryPeriodicallyWithLatch(latch) {
                val timeLineEvent = session.getRoom(roomId)?.timelineService()?.getTimelineEvent(eventId)
                if (andCanDecrypt) {
                    timeLineEvent != null &&
                            timeLineEvent.isEncrypted() &&
                            timeLineEvent.root.getClearType() == EventType.MESSAGE
                } else {
                    timeLineEvent != null
                }
            }
        }
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

    fun createDM(alice: Session, bob: Session): String {
        var roomId: String = ""
        testHelper.waitWithLatch { latch ->
            roomId = alice.roomService().createDirectRoom(bob.myUserId)
            val bobRoomSummariesLive = bob.roomService().getRoomSummariesLive(roomSummaryQueryParams { })
            val newRoomObserver = object : Observer<List<RoomSummary>> {
                override fun onChanged(t: List<RoomSummary>?) {
                    if (t?.any { it.roomId == roomId }.orFalse()) {
                        bobRoomSummariesLive.removeObserver(this)
                        latch.countDown()
                    }
                }
            }
            bobRoomSummariesLive.observeForever(newRoomObserver)
        }

        testHelper.waitWithLatch { latch ->
            val bobRoomSummariesLive = bob.roomService().getRoomSummariesLive(roomSummaryQueryParams { })
            val newRoomObserver = object : Observer<List<RoomSummary>> {
                override fun onChanged(t: List<RoomSummary>?) {
                    if (bob.getRoom(roomId)
                                    ?.membershipService()
                                    ?.getRoomMember(bob.myUserId)
                                    ?.membership == Membership.JOIN) {
                        bobRoomSummariesLive.removeObserver(this)
                        latch.countDown()
                    }
                }
            }
            bobRoomSummariesLive.observeForever(newRoomObserver)
            bob.roomService().joinRoom(roomId)
        }

        return roomId
    }

    fun initializeCrossSigning(session: Session) {
        testHelper.doSync<Unit> {
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
                            }, it
                    )
        }
    }

    /**
     * Initialize cross-signing, set up megolm backup and save all in 4S
     */
    fun bootstrapSecurity(session: Session) {
        initializeCrossSigning(session)
        val ssssService = session.sharedSecretStorageService()
        testHelper.runBlockingTest {
            val keyInfo = ssssService.generateKey(
                    UUID.randomUUID().toString(),
                    null,
                    "ssss_key",
                    EmptyKeySigner()
            )
            ssssService.setDefaultKey(keyInfo.keyId)

            ssssService.storeSecret(
                    MASTER_KEY_SSSS_NAME,
                    session.cryptoService().crossSigningService().getCrossSigningPrivateKeys()!!.master!!,
                    listOf(KeyRef(keyInfo.keyId, keyInfo.keySpec))
            )

            ssssService.storeSecret(
                    SELF_SIGNING_KEY_SSSS_NAME,
                    session.cryptoService().crossSigningService().getCrossSigningPrivateKeys()!!.selfSigned!!,
                    listOf(KeyRef(keyInfo.keyId, keyInfo.keySpec))
            )

            ssssService.storeSecret(
                    USER_SIGNING_KEY_SSSS_NAME,
                    session.cryptoService().crossSigningService().getCrossSigningPrivateKeys()!!.user!!,
                    listOf(KeyRef(keyInfo.keyId, keyInfo.keySpec))
            )

            // set up megolm backup
            val creationInfo = awaitCallback<MegolmBackupCreationInfo> {
                session.cryptoService().keysBackupService().prepareKeysBackupVersion(null, null, it)
            }
            val version = awaitCallback<KeysVersion> {
                session.cryptoService().keysBackupService().createKeysBackupVersion(creationInfo, it)
            }
            // Save it for gossiping
            session.cryptoService().keysBackupService().saveBackupRecoveryKey(creationInfo.recoveryKey, version = version.version)

            extractCurveKeyFromRecoveryKey(creationInfo.recoveryKey)?.toBase64NoPadding()?.let { secret ->
                ssssService.storeSecret(
                        KEYBACKUP_SECRET_SSSS_NAME,
                        secret,
                        listOf(KeyRef(keyInfo.keyId, keyInfo.keySpec))
                )
            }
        }
    }

    fun verifySASCrossSign(alice: Session, bob: Session, roomId: String) {
        assertTrue(alice.cryptoService().crossSigningService().canCrossSign())
        assertTrue(bob.cryptoService().crossSigningService().canCrossSign())

        val aliceVerificationService = alice.cryptoService().verificationService()
        val bobVerificationService = bob.cryptoService().verificationService()

        val localId = UUID.randomUUID().toString()
        aliceVerificationService.requestKeyVerificationInDMs(
                localId = localId,
                methods = listOf(VerificationMethod.SAS, VerificationMethod.QR_CODE_SCAN, VerificationMethod.QR_CODE_SHOW),
                otherUserId = bob.myUserId,
                roomId = roomId
        ).transactionId

        testHelper.waitWithLatch {
            testHelper.retryPeriodicallyWithLatch(it) {
                bobVerificationService.getExistingVerificationRequests(alice.myUserId).firstOrNull {
                    it.requestInfo?.fromDevice == alice.sessionParams.deviceId
                } != null
            }
        }
        val incomingRequest = bobVerificationService.getExistingVerificationRequests(alice.myUserId).first {
            it.requestInfo?.fromDevice == alice.sessionParams.deviceId
        }
        bobVerificationService.readyPendingVerification(listOf(VerificationMethod.SAS), alice.myUserId, incomingRequest.transactionId!!)

        var requestID: String? = null
        // wait for it to be readied
        testHelper.waitWithLatch {
            testHelper.retryPeriodicallyWithLatch(it) {
                val outgoingRequest = aliceVerificationService.getExistingVerificationRequests(bob.myUserId)
                        .firstOrNull { it.localId == localId }
                if (outgoingRequest?.isReady == true) {
                    requestID = outgoingRequest.transactionId!!
                    true
                } else {
                    false
                }
            }
        }

        aliceVerificationService.beginKeyVerificationInDMs(
                VerificationMethod.SAS,
                requestID!!,
                roomId,
                bob.myUserId,
                bob.sessionParams.credentials.deviceId!!
        )

        // we should reach SHOW SAS on both
        var alicePovTx: OutgoingSasVerificationTransaction? = null
        var bobPovTx: IncomingSasVerificationTransaction? = null

        testHelper.waitWithLatch {
            testHelper.retryPeriodicallyWithLatch(it) {
                alicePovTx = aliceVerificationService.getExistingTransaction(bob.myUserId, requestID!!) as? OutgoingSasVerificationTransaction
                Log.v("TEST", "== alicePovTx is ${alicePovTx?.uxState}")
                alicePovTx?.state == VerificationTxState.ShortCodeReady
            }
        }
        // wait for alice to get the ready
        testHelper.waitWithLatch {
            testHelper.retryPeriodicallyWithLatch(it) {
                bobPovTx = bobVerificationService.getExistingTransaction(alice.myUserId, requestID!!) as? IncomingSasVerificationTransaction
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

        testHelper.waitWithLatch {
            testHelper.retryPeriodicallyWithLatch(it) {
                alice.cryptoService().crossSigningService().isUserTrusted(bob.myUserId)
            }
        }

        testHelper.waitWithLatch {
            testHelper.retryPeriodicallyWithLatch(it) {
                alice.cryptoService().crossSigningService().isUserTrusted(bob.myUserId)
            }
        }
    }

    fun doE2ETestWithManyMembers(numberOfMembers: Int): CryptoTestData {
        val aliceSession = testHelper.createAccount(TestConstants.USER_ALICE, defaultSessionParams)
        aliceSession.cryptoService().setWarnOnUnknownDevices(false)

        val roomId = testHelper.runBlockingTest {
            aliceSession.roomService().createRoom(CreateRoomParams().apply { name = "MyRoom" })
        }
        val room = aliceSession.getRoom(roomId)!!

        testHelper.runBlockingTest {
            room.roomCryptoService().enableEncryption()
        }

        val sessions = mutableListOf(aliceSession)
        for (index in 1 until numberOfMembers) {
            val session = testHelper.createAccount("User_$index", defaultSessionParams)
            testHelper.runBlockingTest(timeout = 600_000) { room.membershipService().invite(session.myUserId, null) }
            println("TEST -> " + session.myUserId + " invited")
            testHelper.runBlockingTest { session.roomService().joinRoom(room.roomId, null, emptyList()) }
            println("TEST -> " + session.myUserId + " joined")
            sessions.add(session)
        }

        return CryptoTestData(roomId, sessions)
    }

    fun ensureCanDecrypt(sentEventIds: List<String>, session: Session, e2eRoomID: String, messagesText: List<String>) {
        sentEventIds.forEachIndexed { index, sentEventId ->
            testHelper.waitWithLatch { latch ->
                testHelper.retryPeriodicallyWithLatch(latch) {
                    val event = session.getRoom(e2eRoomID)!!.timelineService().getTimelineEvent(sentEventId)!!.root
                    testHelper.runBlockingTest {
                        try {
                            session.cryptoService().decryptEvent(event, "").let { result ->
                                event.mxDecryptionResult = OlmDecryptionResult(
                                        payload = result.clearEvent,
                                        senderKey = result.senderCurve25519Key,
                                        keysClaimed = result.claimedEd25519Key?.let { mapOf("ed25519" to it) },
                                        forwardingCurve25519KeyChain = result.forwardingCurve25519KeyChain
                                )
                            }
                        } catch (error: MXCryptoError) {
                            // nop
                        }
                    }
                    Log.v("TEST", "ensureCanDecrypt ${event.getClearType()} is ${event.getClearContent()}")
                    event.getClearType() == EventType.MESSAGE &&
                            messagesText[index] == event.getClearContent()?.toModel<MessageContent>()?.body
                }
            }
        }
    }

    fun ensureCannotDecrypt(sentEventIds: List<String>, session: Session, e2eRoomID: String, expectedError: MXCryptoError.ErrorType? = null) {
        sentEventIds.forEach { sentEventId ->
            val event = session.getRoom(e2eRoomID)!!.timelineService().getTimelineEvent(sentEventId)!!.root
            testHelper.runBlockingTest {
                try {
                    session.cryptoService().decryptEvent(event, "")
                    fail("Should not be able to decrypt event")
                } catch (error: MXCryptoError) {
                    val errorType = (error as? MXCryptoError.Base)?.errorType
                    if (expectedError == null) {
                        assertNotNull(errorType)
                    } else {
                        assertEquals("Unexpected reason", expectedError, errorType)
                    }
                }
            }
        }
    }
}
