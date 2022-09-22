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

import android.util.Log
import org.amshove.kluent.fail
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.matrix.android.sdk.api.auth.UIABaseAuth
import org.matrix.android.sdk.api.auth.UserInteractiveAuthInterceptor
import org.matrix.android.sdk.api.auth.UserPasswordAuth
import org.matrix.android.sdk.api.auth.registration.RegistrationFlowResponse
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
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomHistoryVisibility
import org.matrix.android.sdk.api.session.room.model.create.CreateRoomParams
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.session.room.roomSummaryQueryParams
import org.matrix.android.sdk.api.session.securestorage.EmptyKeySigner
import org.matrix.android.sdk.api.session.securestorage.KeyRef
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
    suspend fun doE2ETestWithAliceInARoom(encryptedRoom: Boolean = true, roomHistoryVisibility: RoomHistoryVisibility? = null): CryptoTestData {
        val aliceSession = testHelper.createAccount(TestConstants.USER_ALICE, defaultSessionParams)

        val roomId = aliceSession.roomService().createRoom(CreateRoomParams().apply {
            historyVisibility = roomHistoryVisibility
            name = "MyRoom"
        })
        if (encryptedRoom) {
            val room = aliceSession.getRoom(roomId)!!
            waitFor(
                    continueWhen = { room.onMain { getRoomSummaryLive() }.first { it.getOrNull()?.isEncrypted.orFalse() } },
                    action = { room.roomCryptoService().enableEncryption() }
            )
        }
        return CryptoTestData(roomId, listOf(aliceSession))
    }

    /**
     * @return alice and bob sessions
     */
    suspend fun doE2ETestWithAliceAndBobInARoom(encryptedRoom: Boolean = true, roomHistoryVisibility: RoomHistoryVisibility? = null): CryptoTestData {
        val cryptoTestData = doE2ETestWithAliceInARoom(encryptedRoom, roomHistoryVisibility)
        val aliceSession = cryptoTestData.firstSession
        val aliceRoomId = cryptoTestData.roomId

        val aliceRoom = aliceSession.getRoom(aliceRoomId)!!

        val bobSession = testHelper.createAccount(TestConstants.USER_BOB, defaultSessionParams)

        waitFor(
                continueWhen = { bobSession.roomService().onMain { getRoomSummariesLive(roomSummaryQueryParams { }) }.first { it.isNotEmpty() } },
                action = { aliceRoom.membershipService().invite(bobSession.myUserId) }
        )

        waitFor(
                continueWhen = {
                    bobSession.roomService().onMain { getRoomSummariesLive(roomSummaryQueryParams { }) }.first {
                        bobSession.getRoom(aliceRoomId)
                                ?.membershipService()
                                ?.getRoomMember(bobSession.myUserId)
                                ?.membership == Membership.JOIN
                    }
                },
                action = { bobSession.roomService().joinRoom(aliceRoomId) }
        )

        // Ensure bob can send messages to the room
//        val roomFromBobPOV = bobSession.getRoom(aliceRoomId)!!
//        assertNotNull(roomFromBobPOV.powerLevels)
//        assertTrue(roomFromBobPOV.powerLevels.maySendMessage(bobSession.myUserId))

        return CryptoTestData(aliceRoomId, listOf(aliceSession, bobSession))
    }

    /**
     * @return Alice and Bob sessions
     */
    suspend fun doE2ETestWithAliceAndBobInARoomWithEncryptedMessages(): CryptoTestData {
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

    private suspend fun ensureEventReceived(roomId: String, eventId: String, session: Session, andCanDecrypt: Boolean) {
        testHelper.retryPeriodically {
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

    private fun createFakeMegolmBackupAuthData(): MegolmBackupAuthData {
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

    suspend fun createDM(alice: Session, bob: Session): String {
        var roomId = ""
        waitFor(
                continueWhen = {
                    bob.roomService()
                            .onMain { getRoomSummariesLive(roomSummaryQueryParams { }) }
                            .first { it.any { it.roomId == roomId }.orFalse() }
                },
                action = { roomId = alice.roomService().createDirectRoom(bob.myUserId) }
        )

        waitFor(
                continueWhen = {
                    bob.roomService()
                            .onMain { getRoomSummariesLive(roomSummaryQueryParams { }) }
                            .first {
                                bob.getRoom(roomId)
                                        ?.membershipService()
                                        ?.getRoomMember(bob.myUserId)
                                        ?.membership == Membership.JOIN
                            }
                },
                action = { bob.roomService().joinRoom(roomId) }
        )
        return roomId
    }

    suspend fun initializeCrossSigning(session: Session) {
        testHelper.waitForCallback<Unit> {
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
    suspend fun bootstrapSecurity(session: Session) {
        initializeCrossSigning(session)
        val ssssService = session.sharedSecretStorageService()
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
        val creationInfo = testHelper.waitForCallback<MegolmBackupCreationInfo> {
            session.cryptoService().keysBackupService().prepareKeysBackupVersion(null, null, it)
        }
        val version = testHelper.waitForCallback<KeysVersion> {
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

    suspend fun verifySASCrossSign(alice: Session, bob: Session, roomId: String) {
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

        testHelper.retryPeriodically {
            bobVerificationService.getExistingVerificationRequests(alice.myUserId).firstOrNull {
                it.requestInfo?.fromDevice == alice.sessionParams.deviceId
            } != null
        }
        val incomingRequest = bobVerificationService.getExistingVerificationRequests(alice.myUserId).first {
            it.requestInfo?.fromDevice == alice.sessionParams.deviceId
        }
        bobVerificationService.readyPendingVerification(listOf(VerificationMethod.SAS), alice.myUserId, incomingRequest.transactionId!!)

        var requestID: String? = null
        // wait for it to be readied
        testHelper.retryPeriodically {
            val outgoingRequest = aliceVerificationService.getExistingVerificationRequests(bob.myUserId)
                    .firstOrNull { it.localId == localId }
            if (outgoingRequest?.isReady == true) {
                requestID = outgoingRequest.transactionId!!
                true
            } else {
                false
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

        testHelper.retryPeriodically {
            alicePovTx = aliceVerificationService.getExistingTransaction(bob.myUserId, requestID!!) as? OutgoingSasVerificationTransaction
            Log.v("TEST", "== alicePovTx is ${alicePovTx?.uxState}")
            alicePovTx?.state == VerificationTxState.ShortCodeReady
        }
        // wait for alice to get the ready
        testHelper.retryPeriodically {
            bobPovTx = bobVerificationService.getExistingTransaction(alice.myUserId, requestID!!) as? IncomingSasVerificationTransaction
            Log.v("TEST", "== bobPovTx is ${alicePovTx?.uxState}")
            if (bobPovTx?.state == VerificationTxState.OnStarted) {
                bobPovTx?.performAccept()
            }
            bobPovTx?.state == VerificationTxState.ShortCodeReady
        }

        assertEquals("SAS code do not match", alicePovTx!!.getDecimalCodeRepresentation(), bobPovTx!!.getDecimalCodeRepresentation())

        bobPovTx!!.userHasVerifiedShortCode()
        alicePovTx!!.userHasVerifiedShortCode()

        testHelper.retryPeriodically {
            alice.cryptoService().crossSigningService().isUserTrusted(bob.myUserId)
        }

        testHelper.retryPeriodically {
            alice.cryptoService().crossSigningService().isUserTrusted(bob.myUserId)
        }
    }

    suspend fun doE2ETestWithManyMembers(numberOfMembers: Int): CryptoTestData {
        val aliceSession = testHelper.createAccount(TestConstants.USER_ALICE, defaultSessionParams)
        aliceSession.cryptoService().setWarnOnUnknownDevices(false)

        val roomId = aliceSession.roomService().createRoom(CreateRoomParams().apply { name = "MyRoom" })
        val room = aliceSession.getRoom(roomId)!!

        room.roomCryptoService().enableEncryption()

        val sessions = mutableListOf(aliceSession)
        for (index in 1 until numberOfMembers) {
            val session = testHelper.createAccount("User_$index", defaultSessionParams)
            room.membershipService().invite(session.myUserId, null)
            println("TEST -> " + session.myUserId + " invited")
            session.roomService().joinRoom(room.roomId, null, emptyList())
            println("TEST -> " + session.myUserId + " joined")
            sessions.add(session)
        }

        return CryptoTestData(roomId, sessions)
    }

    suspend fun ensureCanDecrypt(sentEventIds: List<String>, session: Session, e2eRoomID: String, messagesText: List<String>) {
        sentEventIds.forEachIndexed { index, sentEventId ->
            testHelper.retryPeriodically {
                val event = session.getRoom(e2eRoomID)!!.timelineService().getTimelineEvent(sentEventId)!!.root
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
                Log.v("TEST", "ensureCanDecrypt ${event.getClearType()} is ${event.getClearContent()}")
                event.getClearType() == EventType.MESSAGE &&
                        messagesText[index] == event.getClearContent()?.toModel<MessageContent>()?.body
            }
        }
    }

    suspend fun ensureCannotDecrypt(sentEventIds: List<String>, session: Session, e2eRoomID: String, expectedError: MXCryptoError.ErrorType? = null) {
        sentEventIds.forEach { sentEventId ->
            val event = session.getRoom(e2eRoomID)!!.timelineService().getTimelineEvent(sentEventId)!!.root
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
