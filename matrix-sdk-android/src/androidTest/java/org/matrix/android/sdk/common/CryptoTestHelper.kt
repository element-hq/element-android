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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.launch
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
import org.matrix.android.sdk.api.session.crypto.keysbackup.BackupUtils
import org.matrix.android.sdk.api.session.crypto.keysbackup.MegolmBackupAuthData
import org.matrix.android.sdk.api.session.crypto.keysbackup.MegolmBackupCreationInfo
import org.matrix.android.sdk.api.session.crypto.model.OlmDecryptionResult
import org.matrix.android.sdk.api.session.crypto.verification.EVerificationState
import org.matrix.android.sdk.api.session.crypto.verification.PendingVerificationRequest
import org.matrix.android.sdk.api.session.crypto.verification.SasTransactionState
import org.matrix.android.sdk.api.session.crypto.verification.SasVerificationTransaction
import org.matrix.android.sdk.api.session.crypto.verification.VerificationMethod
import org.matrix.android.sdk.api.session.crypto.verification.dbgState
import org.matrix.android.sdk.api.session.crypto.verification.getRequest
import org.matrix.android.sdk.api.session.crypto.verification.getTransaction
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.getRoomSummary
import org.matrix.android.sdk.api.session.room.failure.JoinRoomFailure
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomHistoryVisibility
import org.matrix.android.sdk.api.session.room.model.create.CreateRoomParams
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.session.room.roomSummaryQueryParams
import org.matrix.android.sdk.api.session.securestorage.EmptyKeySigner
import org.matrix.android.sdk.api.session.securestorage.KeyRef
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

    suspend fun inviteNewUsersAndWaitForThemToJoin(session: Session, roomId: String, usernames: List<String>): List<Session> {
        val newSessions = usernames.map { username ->
            testHelper.createAccount(username, SessionTestParams(true)).also {
                if (it.cryptoService().supportsDisablingKeyGossiping()) {
                    it.cryptoService().enableKeyGossiping(false)
                }
            }
        }

        val room = session.getRoom(roomId)!!

        Log.v("#E2E TEST", "accounts for ${usernames.joinToString(",") { it.take(10) }} created")
        // we want to invite them in the room
        newSessions.forEach { newSession ->
            Log.v("#E2E TEST", "${session.myUserId.take(10)} invites ${newSession.myUserId.take(10)}")
            room.membershipService().invite(newSession.myUserId)
        }

        // All user should accept invite
        newSessions.forEach { newSession ->
            waitForAndAcceptInviteInRoom(newSession, roomId)
            Log.v("#E2E TEST", "${newSession.myUserId.take(10)} joined room $roomId")
        }
        ensureMembersHaveJoined(session, newSessions, roomId)
        return newSessions
    }

    private suspend fun ensureMembersHaveJoined(session: Session, invitedUserSessions: List<Session>, roomId: String) {
        testHelper.retryWithBackoff(
                onFail = {
                    fail("Members ${invitedUserSessions.map { it.myUserId.take(10) }} should have join from the pov of ${session.myUserId.take(10)}")
                }
        ) {
            invitedUserSessions.map { invitedUserSession ->
                session.roomService().getRoomMember(invitedUserSession.myUserId, roomId)?.membership?.also {
                    Log.v("#E2E TEST", "${invitedUserSession.myUserId.take(10)} membership is $it")
                }
            }.all {
                it == Membership.JOIN
            }
        }
    }

    private suspend fun waitForAndAcceptInviteInRoom(session: Session, roomId: String) {
        testHelper.retryWithBackoff(
                onFail = {
                    fail("${session.myUserId} cannot see the invite from ${session.myUserId.take(10)}")
                }
        ) {
            val roomSummary = session.getRoomSummary(roomId)
            (roomSummary != null && roomSummary.membership == Membership.INVITE).also {
                if (it) {
                    Log.v("#E2E TEST", "${session.myUserId.take(10)} can see the invite from ${roomSummary?.inviterId}")
                }
            }
        }

        // not sure why it's taking so long :/
        Log.v("#E2E TEST", "${session.myUserId.take(10)} tries to join room $roomId")
        try {
            session.roomService().joinRoom(roomId)
        } catch (ex: JoinRoomFailure.JoinedWithTimeout) {
            // it's ok we will wait after
        }

        Log.v("#E2E TEST", "${session.myUserId} waiting for join echo ...")
        testHelper.retryWithBackoff(
                onFail = {
                    fail("${session.myUserId.take(10)} cannot see the join echo for ${roomId}")
                }
        ) {
            val roomSummary = session.getRoomSummary(roomId)
            roomSummary != null && roomSummary.membership == Membership.JOIN
        }
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
        ensureEventReceived(aliceRoomId, testHelper.sendMessageInRoom(roomFromAlicePOV, messagesFromAlice[0]), bobSession, true)

        // Bob send 3 messages
        for (msg in messagesFromBob) {
            ensureEventReceived(aliceRoomId, testHelper.sendMessageInRoom(roomFromBobPOV, msg), aliceSession, true)
        }

        // Alice sends a message
        ensureEventReceived(aliceRoomId, testHelper.sendMessageInRoom(roomFromAlicePOV, messagesFromAlice[1]), bobSession, true)
        return cryptoTestData
    }

    private suspend fun ensureEventReceived(roomId: String, eventId: String, session: Session, andCanDecrypt: Boolean) {
        testHelper.retryWithBackoff {
            val timeLineEvent = session.getRoom(roomId)?.timelineService()?.getTimelineEvent(eventId)
            Log.d("#E2E", "ensureEventReceived $eventId => ${timeLineEvent?.senderInfo?.userId}| ${timeLineEvent?.root?.getClearType()}")
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
                recoveryKey = BackupUtils.recoveryKeyFromPassphrase("3cnTdW")
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
                            })
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
        val creationInfo = session.cryptoService().keysBackupService().prepareKeysBackupVersion(null, null)
        val version = session.cryptoService().keysBackupService().createKeysBackupVersion(creationInfo)

        // Save it for gossiping
        session.cryptoService().keysBackupService().saveBackupRecoveryKey(creationInfo.recoveryKey, version = version.version)

        creationInfo.recoveryKey.toBase64().let { secret ->
            ssssService.storeSecret(
                    KEYBACKUP_SECRET_SSSS_NAME,
                    secret,
                    listOf(KeyRef(keyInfo.keyId, keyInfo.keySpec))
            )
        }
    }

    suspend fun verifySASCrossSign(alice: Session, bob: Session, roomId: String) {
        val scope = CoroutineScope(SupervisorJob())

        assertTrue(alice.cryptoService().crossSigningService().canCrossSign())
        assertTrue(bob.cryptoService().crossSigningService().canCrossSign())

        val aliceVerificationService = alice.cryptoService().verificationService()
        val bobVerificationService = bob.cryptoService().verificationService()

        val bobSeesVerification = CompletableDeferred<PendingVerificationRequest>()
        scope.launch(Dispatchers.IO) {
            bobVerificationService.requestEventFlow()
                    .cancellable()
                    .collect {
                        val request = it.getRequest()
                        if (request != null) {
                            bobSeesVerification.complete(request)
                            return@collect cancel()
                        }
                    }
        }

        val aliceReady = CompletableDeferred<PendingVerificationRequest>()
        scope.launch(Dispatchers.IO) {
            aliceVerificationService.requestEventFlow()
                    .cancellable()
                    .collect {
                        val request = it.getRequest()
                        if (request?.state == EVerificationState.Ready) {
                            aliceReady.complete(request)
                            return@collect cancel()
                        }
                    }
        }
        val bobReady = CompletableDeferred<PendingVerificationRequest>()
        scope.launch(Dispatchers.IO) {
            bobVerificationService.requestEventFlow()
                    .cancellable()
                    .collect {
                        val request = it.getRequest()
                        if (request?.state == EVerificationState.Ready) {
                            bobReady.complete(request)
                            return@collect cancel()
                        }
                    }
        }

        val requestID = aliceVerificationService.requestKeyVerificationInDMs(
                methods = listOf(VerificationMethod.SAS, VerificationMethod.QR_CODE_SCAN, VerificationMethod.QR_CODE_SHOW),
                otherUserId = bob.myUserId,
                roomId = roomId
        ).transactionId

        bobSeesVerification.await()
        bobVerificationService.readyPendingVerification(
                listOf(VerificationMethod.SAS),
                alice.myUserId,
                requestID
        )
        aliceReady.await()
        bobReady.await()

        val bobCode = CompletableDeferred<SasVerificationTransaction>()

        scope.launch(Dispatchers.IO) {
            bobVerificationService.requestEventFlow()
                    .cancellable()
                    .collect {
                        val transaction = it.getTransaction()
                        Log.v("#E2E TEST", "#TEST flow ${bob.myUserId.take(5)} ${transaction?.transactionId}|${transaction?.dbgState()}")
                        val tx = transaction as? SasVerificationTransaction
                        if (tx?.state() == SasTransactionState.SasShortCodeReady) {
                            Log.v("#E2E TEST", "COMPLETE BOB CODE")
                            bobCode.complete(tx)
                            return@collect cancel()
                        }
                        if (it.getRequest()?.state == EVerificationState.Cancelled) {
                            Log.v("#E2E TEST", "EXCEPTION BOB CODE")
                            bobCode.completeExceptionally(AssertionError("Request as been cancelled"))
                            return@collect cancel()
                        }
                    }
        }

        val aliceCode = CompletableDeferred<SasVerificationTransaction>()

        scope.launch(Dispatchers.IO) {
            aliceVerificationService.requestEventFlow()
                    .cancellable()
                    .collect {
                        val transaction = it.getTransaction()
                        Log.v("#E2E TEST", "#TEST flow ${alice.myUserId.take(5)}  ${transaction?.transactionId}|${transaction?.dbgState()}")
                        val tx = transaction as? SasVerificationTransaction
                        if (tx?.state() == SasTransactionState.SasShortCodeReady) {
                            Log.v("#E2E TEST", "COMPLETE ALICE CODE")
                            aliceCode.complete(tx)
                            return@collect cancel()
                        }
                        if (it.getRequest()?.state == EVerificationState.Cancelled) {
                            Log.v("#E2E TEST", "EXCEPTION ALICE CODE")
                            aliceCode.completeExceptionally(AssertionError("Request as been cancelled"))
                            return@collect cancel()
                        }
                    }
        }

        Log.v("#E2E TEST", "#TEST let alice start the verification")
        val id = aliceVerificationService.startKeyVerification(
                VerificationMethod.SAS,
                bob.myUserId,
                requestID,
        )
        Log.v("#E2E TEST", "#TEST alice started: $id")

        val bobTx = bobCode.await()
        val aliceTx = aliceCode.await()
        Log.v("#E2E TEST", "#TEST Alice code ${aliceTx.getDecimalCodeRepresentation()}")
        Log.v("#E2E TEST", "#TEST Bob code ${bobTx.getDecimalCodeRepresentation()}")
        assertEquals("SAS code do not match", aliceTx.getDecimalCodeRepresentation()!!, bobTx.getDecimalCodeRepresentation())

        val aliceDone = CompletableDeferred<Unit>()
        scope.launch(Dispatchers.IO) {
            aliceVerificationService.requestEventFlow()
                    .cancellable()
                    .collect {
                        val transaction = it.getTransaction()
                        Log.v("#E2E TEST", "#TEST flow ${alice.myUserId.take(5)}  ${transaction?.transactionId}|${transaction?.dbgState()}")

                        val request = it.getRequest()
                        Log.v("#E2E TEST", "#TEST flow request ${alice.myUserId.take(5)}  ${request?.transactionId}|${request?.state}")
                        if (request?.state == EVerificationState.Done || request?.state == EVerificationState.WaitingForDone) {
                            aliceDone.complete(Unit)
                            return@collect cancel()
                        }
                    }
        }
        val bobDone = CompletableDeferred<Unit>()
        scope.launch(Dispatchers.IO) {
            bobVerificationService.requestEventFlow()
                    .cancellable()
                    .collect {
                        val transaction = it.getTransaction()
                        Log.v("#E2E TEST", "#TEST flow ${bob.myUserId.take(5)}  ${transaction?.transactionId}|${transaction?.dbgState()}")

                        val request = it.getRequest()
                        Log.v("#E2E TEST", "#TEST flow request ${bob.myUserId.take(5)}  ${request?.transactionId}|${request?.state}")

                        if (request?.state == EVerificationState.Done || request?.state == EVerificationState.WaitingForDone) {
                            bobDone.complete(Unit)
                            return@collect cancel()
                        }
                    }
        }

        Log.v("#E2E TEST", "#TEST Bob confirm sas code")
        bobTx.userHasVerifiedShortCode()
        Log.v("#E2E TEST", "#TEST Alice confirm sas code")
        aliceTx.userHasVerifiedShortCode()

        Log.v("#E2E TEST", "#TEST Waiting for Done..")
        bobDone.await()
        aliceDone.await()
        Log.v("#E2E TEST", "#TEST .. ok")

        alice.cryptoService().crossSigningService().isUserTrusted(bob.myUserId)
        bob.cryptoService().crossSigningService().isUserTrusted(alice.myUserId)

        scope.cancel()
    }

    suspend fun verifyNewSession(oldDevice: Session, newDevice: Session) {
        val scope = CoroutineScope(SupervisorJob())

        assertTrue(oldDevice.cryptoService().crossSigningService().canCrossSign())

        val verificationServiceOld = oldDevice.cryptoService().verificationService()
        val verificationServiceNew = newDevice.cryptoService().verificationService()

        val oldSeesVerification = CompletableDeferred<PendingVerificationRequest>()
        scope.launch(Dispatchers.IO) {
            verificationServiceOld.requestEventFlow()
                    .cancellable()
                    .collect {
                        val request = it.getRequest()
                        Log.d("#E2E", "Verification request received: $request")
                        if (request != null) {
                            oldSeesVerification.complete(request)
                            return@collect cancel()
                        }
                    }
        }

        val newReady = CompletableDeferred<PendingVerificationRequest>()
        scope.launch(Dispatchers.IO) {
            verificationServiceNew.requestEventFlow()
                    .cancellable()
                    .collect {
                        val request = it.getRequest()
                        Log.d("#E2E", "new state: ${request?.state}")
                        if (request?.state == EVerificationState.Ready) {
                            newReady.complete(request)
                            return@collect cancel()
                        }
                    }
        }

        val txId = verificationServiceNew.requestSelfKeyVerification(listOf(VerificationMethod.SAS)).transactionId
        oldSeesVerification.await()

        verificationServiceOld.readyPendingVerification(
                listOf(VerificationMethod.SAS),
                oldDevice.myUserId,
                txId
        )

        newReady.await()

        val newConfirmed = CompletableDeferred<Unit>()
        scope.launch(Dispatchers.IO) {
            verificationServiceNew.requestEventFlow()
                    .cancellable()
                    .collect {
                        val tx = it.getTransaction() as? SasVerificationTransaction
                        Log.d("#E2E", "new tx state: ${tx?.state()}")
                        if (tx?.state() == SasTransactionState.SasShortCodeReady) {
                            tx.userHasVerifiedShortCode()
                            newConfirmed.complete(Unit)
                            return@collect cancel()
                        }
                    }
        }

        val oldConfirmed = CompletableDeferred<Unit>()
        scope.launch(Dispatchers.IO) {
            verificationServiceOld.requestEventFlow()
                    .cancellable()
                    .collect {
                        val tx = it.getTransaction() as? SasVerificationTransaction
                        Log.d("#E2E", "old tx state: ${tx?.state()}")
                        if (tx?.state() == SasTransactionState.SasShortCodeReady) {
                            tx.userHasVerifiedShortCode()
                            oldConfirmed.complete(Unit)
                            return@collect cancel()
                        }
                    }
        }

        verificationServiceNew.startKeyVerification(VerificationMethod.SAS, newDevice.myUserId, txId)

        newConfirmed.await()
        oldConfirmed.await()

        testHelper.retryPeriodically {
            oldDevice.cryptoService().crossSigningService().isCrossSigningVerified()
        }

        Log.d("#E2E", "New session is trusted")
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
            testHelper.retryWithBackoff {
                val event = session.getRoom(e2eRoomID)?.timelineService()?.getTimelineEvent(sentEventId)?.root
                        ?: return@retryWithBackoff false
                try {
                    session.cryptoService().decryptEvent(event, "").let { result ->
                        event.mxDecryptionResult = OlmDecryptionResult(
                                payload = result.clearEvent,
                                senderKey = result.senderCurve25519Key,
                                keysClaimed = result.claimedEd25519Key?.let { mapOf("ed25519" to it) },
                                forwardingCurve25519KeyChain = result.forwardingCurve25519KeyChain,
                                verificationState = result.messageVerificationState
                        )
                    }
                } catch (error: MXCryptoError) {
                    // nop
                }
                Log.v("#E2E TEST", "ensureCanDecrypt ${event.getClearType()} is ${event.getClearContent()}")
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
