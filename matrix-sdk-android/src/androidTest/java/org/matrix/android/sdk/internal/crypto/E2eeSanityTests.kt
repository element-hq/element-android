/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.crypto

import android.util.Log
import androidx.test.filters.LargeTest
import kotlinx.coroutines.delay
import org.amshove.kluent.fail
import org.amshove.kluent.internal.assertEquals
import org.junit.Assert
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.MethodSorters
import org.matrix.android.sdk.InstrumentedTest
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.MXCryptoError
import org.matrix.android.sdk.api.session.crypto.RequestResult
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysVersion
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysVersionResult
import org.matrix.android.sdk.api.session.crypto.keysbackup.MegolmBackupCreationInfo
import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.api.session.crypto.model.ImportRoomKeysResult
import org.matrix.android.sdk.api.session.crypto.verification.IncomingSasVerificationTransaction
import org.matrix.android.sdk.api.session.crypto.verification.OutgoingSasVerificationTransaction
import org.matrix.android.sdk.api.session.crypto.verification.PendingVerificationRequest
import org.matrix.android.sdk.api.session.crypto.verification.VerificationMethod
import org.matrix.android.sdk.api.session.crypto.verification.VerificationService
import org.matrix.android.sdk.api.session.crypto.verification.VerificationTransaction
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.content.EncryptedEventContent
import org.matrix.android.sdk.api.session.events.model.content.WithHeldCode
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.getTimelineEvent
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.api.session.room.timeline.TimelineSettings
import org.matrix.android.sdk.common.CommonTestHelper
import org.matrix.android.sdk.common.CommonTestHelper.Companion.runCryptoTest
import org.matrix.android.sdk.common.CommonTestHelper.Companion.runSessionTest
import org.matrix.android.sdk.common.CommonTestHelper.Companion.runSuspendingCryptoTest
import org.matrix.android.sdk.common.RetryTestRule
import org.matrix.android.sdk.common.SessionTestParams
import org.matrix.android.sdk.common.TestMatrixCallback
import org.matrix.android.sdk.mustFail
import java.util.concurrent.CountDownLatch

// @Ignore("This test fails with an unhandled exception thrown from a coroutine which terminates the entire test run.")
@RunWith(JUnit4::class)
@FixMethodOrder(MethodSorters.JVM)
@LargeTest
class E2eeSanityTests : InstrumentedTest {

    /**
     * Simple test that create an e2ee room.
     * Some new members are added, and a message is sent.
     * We check that the message is e2e and can be decrypted.
     *
     * Additional users join, we check that they can't decrypt history
     *
     * Alice sends a new message, then check that the new one can be decrypted
     */
    @Test
    fun testSendingE2EEMessages() = runSuspendingCryptoTest(context()) { cryptoTestHelper, testHelper ->

        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoom(true)
        val aliceSession = cryptoTestData.firstSession
        val e2eRoomID = cryptoTestData.roomId

        val aliceRoomPOV = aliceSession.getRoom(e2eRoomID)!!
        // we want to disable key gossiping to just check initial sending of keys
        aliceSession.cryptoService().enableKeyGossiping(false)
        cryptoTestData.secondSession?.cryptoService()?.enableKeyGossiping(false)

        // add some more users and invite them
        val otherAccounts = listOf("benoit", "valere", "ganfra") // , "adam", "manu")
                .map {
                    testHelper.createAccount(it, SessionTestParams(true)).also {
                        it.cryptoService().enableKeyGossiping(false)
                    }
                }

        Log.v("#E2E TEST", "All accounts created")
        // we want to invite them in the room
        otherAccounts.forEach {
            Log.v("#E2E TEST", "Alice invites ${it.myUserId}")
            aliceRoomPOV.membershipService().invite(it.myUserId)
        }

        // All user should accept invite
        otherAccounts.forEach { otherSession ->
            testHelper.waitForAndAcceptInviteInRoom(otherSession, e2eRoomID)
            Log.v("#E2E TEST", "${otherSession.myUserId} joined room $e2eRoomID")
        }

        // check that alice see them as joined (not really necessary?)
        ensureMembersHaveJoined(testHelper, aliceSession, otherAccounts, e2eRoomID)

        Log.v("#E2E TEST", "All users have joined the room")
        Log.v("#E2E TEST", "Alice is sending the message")

        val text = "This is my message"
        val sentEventId: String? = sendMessageInRoom(testHelper, aliceRoomPOV, text)
        //        val sentEvent = testHelper.sendTextMessage(aliceRoomPOV, "Hello all", 1).first()
        Assert.assertTrue("Message should be sent", sentEventId != null)

        // All should be able to decrypt
        otherAccounts.forEach { otherSession ->
            testHelper.retryPeriodically {
                val timeLineEvent = otherSession.getRoom(e2eRoomID)?.getTimelineEvent(sentEventId!!)
                timeLineEvent != null &&
                        timeLineEvent.isEncrypted() &&
                        timeLineEvent.root.getClearType() == EventType.MESSAGE
            }
        }

        // Add a new user to the room, and check that he can't decrypt
        val newAccount = listOf("adam") // , "adam", "manu")
                .map {
                    testHelper.createAccountSuspending(it, SessionTestParams(true))
                }

        newAccount.forEach {
            Log.v("#E2E TEST", "Alice invites ${it.myUserId}")
            aliceRoomPOV.membershipService().invite(it.myUserId)
        }

        newAccount.forEach {
            testHelper.waitForAndAcceptInviteInRoomSuspending(it, e2eRoomID)
        }

        ensureMembersHaveJoined(testHelper, aliceSession, newAccount, e2eRoomID)

        // wait a bit
        delay(3_000)

        // check that messages are encrypted (uisi)
        newAccount.forEach { otherSession ->
            testHelper.retryPeriodically {
                val timelineEvent = otherSession.getRoom(e2eRoomID)?.getTimelineEvent(sentEventId!!).also {
                    Log.v("#E2E TEST", "Event seen by new user ${it?.root?.getClearType()}|${it?.root?.mCryptoError}")
                }
                timelineEvent != null &&
                        timelineEvent.root.getClearType() == EventType.ENCRYPTED &&
                        timelineEvent.root.mCryptoError == MXCryptoError.ErrorType.UNKNOWN_INBOUND_SESSION_ID
            }
        }

        // Let alice send a new message
        Log.v("#E2E TEST", "Alice sends a new message")

        val secondMessage = "2 This is my message"
        val secondSentEventId: String? = sendMessageInRoom(testHelper, aliceRoomPOV, secondMessage)

        // new members should be able to decrypt it
        newAccount.forEach { otherSession ->
            testHelper.retryPeriodically {
                val timelineEvent = otherSession.getRoom(e2eRoomID)?.getTimelineEvent(secondSentEventId!!).also {
                    Log.v("#E2E TEST", "Second Event seen by new user ${it?.root?.getClearType()}|${it?.root?.mCryptoError}")
                }
                timelineEvent != null &&
                        timelineEvent.root.getClearType() == EventType.MESSAGE &&
                        secondMessage == timelineEvent.root.getClearContent().toModel<MessageContent>()?.body
            }
        }
    }

    @Test
    fun testKeyGossipingIsEnabledByDefault() = runSessionTest(context()) { testHelper ->
        val session = testHelper.createAccount("alice", SessionTestParams(true))
        Assert.assertTrue("Key gossiping should be enabled by default", session.cryptoService().isKeyGossipingEnabled())
    }

    /**
     * Quick test for basic key backup
     * 1. Create e2e between Alice and Bob
     * 2. Alice sends 3 messages, using 3 different sessions
     * 3. Ensure bob can decrypt
     * 4. Create backup for bob and upload keys
     *
     * 5. Sign out alice and bob to ensure no gossiping will happen
     *
     * 6. Let bob sign in with a new session
     * 7. Ensure history is UISI
     * 8. Import backup
     * 9. Check that new session can decrypt
     */
    @Test
    fun testBasicBackupImport() = runSuspendingCryptoTest(context()) { cryptoTestHelper, testHelper ->

        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoom(true)
        val aliceSession = cryptoTestData.firstSession
        val bobSession = cryptoTestData.secondSession!!
        val e2eRoomID = cryptoTestData.roomId

        Log.v("#E2E TEST", "Create and start key backup for bob ...")
        val bobKeysBackupService = bobSession.cryptoService().keysBackupService()
        val keyBackupPassword = "FooBarBaz"
        val megolmBackupCreationInfo = testHelper.doSyncSuspending<MegolmBackupCreationInfo> {
            bobKeysBackupService.prepareKeysBackupVersion(keyBackupPassword, null, it)
        }
        val version = testHelper.doSyncSuspending<KeysVersion> {
            bobKeysBackupService.createKeysBackupVersion(megolmBackupCreationInfo, it)
        }
        Log.v("#E2E TEST", "... Key backup started and enabled for bob")
        // Bob session should now have

        val aliceRoomPOV = aliceSession.getRoom(e2eRoomID)!!

        // let's send a few message to bob
        val sentEventIds = mutableListOf<String>()
        val messagesText = listOf("1. Hello", "2. Bob", "3. Good morning")
        messagesText.forEach { text ->
            val sentEventId = sendMessageInRoom(testHelper, aliceRoomPOV, text)!!.also {
                sentEventIds.add(it)
            }

            testHelper.waitWithLatch { latch ->
                testHelper.retryPeriodicallyWithLatch(latch) {
                    val timeLineEvent = bobSession.getRoom(e2eRoomID)?.getTimelineEvent(sentEventId)
                    timeLineEvent != null &&
                            timeLineEvent.isEncrypted() &&
                            timeLineEvent.root.getClearType() == EventType.MESSAGE
                }
            }
            // we want more so let's discard the session
            aliceSession.cryptoService().discardOutboundSession(e2eRoomID)

            delay(1_000)
        }
        Log.v("#E2E TEST", "Bob received all and can decrypt")

        // Let's wait a bit to be sure that bob has backed up the session

        Log.v("#E2E TEST", "Force key backup for Bob...")
        testHelper.waitWithLatch { latch ->
            bobKeysBackupService.backupAllGroupSessions(
                    null,
                    TestMatrixCallback(latch, true)
            )
        }
        Log.v("#E2E TEST", "... Key backup done for Bob")

        // Now lets logout both alice and bob to ensure that we won't have any gossiping

        val bobUserId = bobSession.myUserId
        Log.v("#E2E TEST", "Logout alice and bob...")
        testHelper.signOutAndClose(aliceSession)
        testHelper.signOutAndClose(bobSession)
        Log.v("#E2E TEST", "..Logout alice and bob...")

        delay(1_000)

        // Create a new session for bob
        Log.v("#E2E TEST", "Create a new session for Bob")
        val newBobSession = testHelper.logIntoAccount(bobUserId, SessionTestParams(true))

        // check that bob can't currently decrypt
        Log.v("#E2E TEST", "check that bob can't currently decrypt")
        sentEventIds.forEach { sentEventId ->
            testHelper.waitWithLatch { latch ->
                testHelper.retryPeriodicallyWithLatch(latch) {
                    val timelineEvent = newBobSession.getRoom(e2eRoomID)?.getTimelineEvent(sentEventId)?.also {
                        Log.v("#E2E TEST", "Event seen by new user ${it.root.getClearType()}|${it.root.mCryptoError}")
                    }
                    timelineEvent != null &&
                            timelineEvent.root.getClearType() == EventType.ENCRYPTED
                }
            }
        }
        // after initial sync events are not decrypted, so we have to try manually
        cryptoTestHelper.ensureCannotDecrypt(sentEventIds, newBobSession, e2eRoomID, MXCryptoError.ErrorType.UNKNOWN_INBOUND_SESSION_ID)

        // Let's now import keys from backup

        newBobSession.cryptoService().keysBackupService().let { kbs ->
            val keyVersionResult = testHelper.doSync<KeysVersionResult?> {
                kbs.getVersion(version.version, it)
            }

            val importedResult = testHelper.doSync<ImportRoomKeysResult> {
                kbs.restoreKeyBackupWithPassword(
                        keyVersionResult!!,
                        keyBackupPassword,
                        null,
                        null,
                        null,
                        it
                )
            }

            assertEquals(3, importedResult.totalNumberOfKeys)
        }

        // ensure bob can now decrypt
        cryptoTestHelper.ensureCanDecrypt(sentEventIds, newBobSession, e2eRoomID, messagesText)
    }

    /**
     * Check that a new verified session that was not supposed to get the keys initially will
     * get them from an older one.
     */
    @Test
    fun testSimpleGossip() = runSuspendingCryptoTest(context()) { cryptoTestHelper, testHelper ->

        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoom(true)
        val aliceSession = cryptoTestData.firstSession
        val bobSession = cryptoTestData.secondSession!!
        val e2eRoomID = cryptoTestData.roomId

        val aliceRoomPOV = aliceSession.getRoom(e2eRoomID)!!

        // let's send a few message to bob
        val sentEventIds = mutableListOf<String>()
        val messagesText = listOf("1. Hello", "2. Bob")

        Log.v("#E2E TEST", "Alice sends some messages")
        messagesText.forEach { text ->
            val sentEventId = sendMessageInRoom(testHelper, aliceRoomPOV, text)!!.also {
                sentEventIds.add(it)
            }

            testHelper.waitWithLatch { latch ->
                testHelper.retryPeriodicallyWithLatch(latch) {
                    val timeLineEvent = bobSession.getRoom(e2eRoomID)?.getTimelineEvent(sentEventId)
                    timeLineEvent != null &&
                            timeLineEvent.isEncrypted() &&
                            timeLineEvent.root.getClearType() == EventType.MESSAGE
                }
            }
        }

        // Ensure bob can decrypt
        ensureIsDecrypted(testHelper, sentEventIds, bobSession, e2eRoomID)

        // Let's now add a new bob session
        // Create a new session for bob
        Log.v("#E2E TEST", "Create a new session for Bob")
        val newBobSession = testHelper.logIntoAccount(bobSession.myUserId, SessionTestParams(true))

        // check that new bob can't currently decrypt
        Log.v("#E2E TEST", "check that new bob can't currently decrypt")

        cryptoTestHelper.ensureCannotDecrypt(sentEventIds, newBobSession, e2eRoomID, null)
//        newBobSession.cryptoService().getOutgoingRoomKeyRequests()
//                .firstOrNull {
//                    it.sessionId ==
//                }

        // Try to request
        sentEventIds.forEach { sentEventId ->
            val event = newBobSession.getRoom(e2eRoomID)!!.getTimelineEvent(sentEventId)!!.root
            newBobSession.cryptoService().requestRoomKeyForEvent(event)
        }

        // wait a bit
        // we need to wait a couple of syncs to let sharing occurs
//        testHelper.waitFewSyncs(newBobSession, 6)

        // Ensure that new bob still can't decrypt (keys must have been withheld)
        sentEventIds.forEach { sentEventId ->
            val megolmSessionId = newBobSession.getRoom(e2eRoomID)!!
                    .getTimelineEvent(sentEventId)!!
                    .root.content.toModel<EncryptedEventContent>()!!.sessionId
            testHelper.waitWithLatch { latch ->
                testHelper.retryPeriodicallyWithLatch(latch) {
                    val aliceReply = newBobSession.cryptoService().getOutgoingRoomKeyRequests()
                            .first {
                                it.sessionId == megolmSessionId &&
                                        it.roomId == e2eRoomID
                            }
                            .results.also {
                                Log.w("##TEST", "result list is $it")
                            }
                            .firstOrNull { it.userId == aliceSession.myUserId }
                            ?.result
                    aliceReply != null &&
                            aliceReply is RequestResult.Failure &&
                            WithHeldCode.UNAUTHORISED == aliceReply.code
                }
            }
        }

        cryptoTestHelper.ensureCannotDecrypt(sentEventIds, newBobSession, e2eRoomID, null)

        // Now mark new bob session as verified

        bobSession.cryptoService().verificationService().markedLocallyAsManuallyVerified(newBobSession.myUserId, newBobSession.sessionParams.deviceId!!)
        newBobSession.cryptoService().verificationService().markedLocallyAsManuallyVerified(bobSession.myUserId, bobSession.sessionParams.deviceId!!)

        // now let new session re-request
        sentEventIds.forEach { sentEventId ->
            val event = newBobSession.getRoom(e2eRoomID)!!.getTimelineEvent(sentEventId)!!.root
            newBobSession.cryptoService().reRequestRoomKeyForEvent(event)
        }

        cryptoTestHelper.ensureCanDecrypt(sentEventIds, newBobSession, e2eRoomID, messagesText)
    }

    /**
     * Test that if a better key is forwarded (lower index, it is then used)
     */
    @Test
    fun testForwardBetterKey() = runSuspendingCryptoTest(context()) { cryptoTestHelper, testHelper ->

        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoom(true)
        val aliceSession = cryptoTestData.firstSession
        val bobSessionWithBetterKey = cryptoTestData.secondSession!!
        val e2eRoomID = cryptoTestData.roomId

        val aliceRoomPOV = aliceSession.getRoom(e2eRoomID)!!

        // let's send a few message to bob
        var firstEventId: String
        val firstMessage = "1. Hello"

        Log.v("#E2E TEST", "Alice sends some messages")
        firstMessage.let { text ->
            firstEventId = sendMessageInRoom(testHelper, aliceRoomPOV, text)!!

            testHelper.waitWithLatch { latch ->
                testHelper.retryPeriodicallyWithLatch(latch) {
                    val timeLineEvent = bobSessionWithBetterKey.getRoom(e2eRoomID)?.getTimelineEvent(firstEventId)
                    timeLineEvent != null &&
                            timeLineEvent.isEncrypted() &&
                            timeLineEvent.root.getClearType() == EventType.MESSAGE
                }
            }
        }

        // Ensure bob can decrypt
        ensureIsDecrypted(testHelper, listOf(firstEventId), bobSessionWithBetterKey, e2eRoomID)

        // Let's add a new unverified session from bob
        val newBobSession = testHelper.logIntoAccount(bobSessionWithBetterKey.myUserId, SessionTestParams(true))

        // check that new bob can't currently decrypt
        Log.v("#E2E TEST", "check that new bob can't currently decrypt")
        cryptoTestHelper.ensureCannotDecrypt(listOf(firstEventId), newBobSession, e2eRoomID, null)

        // Now let alice send a new message. this time the new bob session will be able to decrypt
        var secondEventId: String
        val secondMessage = "2. New Device?"

        Log.v("#E2E TEST", "Alice sends some messages")
        secondMessage.let { text ->
            secondEventId = sendMessageInRoom(testHelper, aliceRoomPOV, text)!!

            testHelper.waitWithLatch { latch ->
                testHelper.retryPeriodicallyWithLatch(latch) {
                    val timeLineEvent = newBobSession.getRoom(e2eRoomID)?.getTimelineEvent(secondEventId)
                    timeLineEvent != null &&
                            timeLineEvent.isEncrypted() &&
                            timeLineEvent.root.getClearType() == EventType.MESSAGE
                }
            }
        }

        // check that both messages have same sessionId (it's just that we don't have index 0)
        val firstEventNewBobPov = newBobSession.getRoom(e2eRoomID)?.getTimelineEvent(firstEventId)
        val secondEventNewBobPov = newBobSession.getRoom(e2eRoomID)?.getTimelineEvent(secondEventId)

        val firstSessionId = firstEventNewBobPov!!.root.content.toModel<EncryptedEventContent>()!!.sessionId!!
        val secondSessionId = secondEventNewBobPov!!.root.content.toModel<EncryptedEventContent>()!!.sessionId!!

        Assert.assertTrue("Should be the same session id", firstSessionId == secondSessionId)

        // Confirm we can decrypt one but not the other
        testHelper.runBlockingTest {
            mustFail(message = "Should not be able to decrypt event") {
                newBobSession.cryptoService().decryptEvent(firstEventNewBobPov.root, "")
            }
        }

        testHelper.runBlockingTest {
            try {
                newBobSession.cryptoService().decryptEvent(secondEventNewBobPov.root, "")
            } catch (error: MXCryptoError) {
                fail("Should be able to decrypt event")
            }
        }

        // Now let's verify bobs session, and re-request keys
        bobSessionWithBetterKey.cryptoService()
                .verificationService()
                .markedLocallyAsManuallyVerified(newBobSession.myUserId, newBobSession.sessionParams.deviceId!!)

        newBobSession.cryptoService()
                .verificationService()
                .markedLocallyAsManuallyVerified(bobSessionWithBetterKey.myUserId, bobSessionWithBetterKey.sessionParams.deviceId!!)

        // now let new session request
        newBobSession.cryptoService().reRequestRoomKeyForEvent(firstEventNewBobPov.root)

        // We need to wait for the key request to be sent out and then a reply to be received

        // old session should have shared the key at earliest known index now
        // we should be able to decrypt both
        testHelper.waitWithLatch {
            testHelper.retryPeriodicallyWithLatch(it) {
                val canDecryptFirst = try {
                    testHelper.runBlockingTest {
                        newBobSession.cryptoService().decryptEvent(firstEventNewBobPov.root, "")
                    }
                    true
                } catch (error: MXCryptoError) {
                    false
                }
                val canDecryptSecond = try {
                    testHelper.runBlockingTest {
                        newBobSession.cryptoService().decryptEvent(secondEventNewBobPov.root, "")
                    }
                    true
                } catch (error: MXCryptoError) {
                    false
                }
                canDecryptFirst && canDecryptSecond
            }
        }
    }

    private suspend fun sendMessageInRoom(testHelper: CommonTestHelper, aliceRoomPOV: Room, text: String): String? {
        var sentEventId: String? = null
        aliceRoomPOV.sendService().sendTextMessage(text)

        val timeline = aliceRoomPOV.timelineService().createTimeline(null, TimelineSettings(60))
        timeline.start()
        testHelper.retryPeriodically {
            val decryptedMsg = timeline.getSnapshot()
                    .filter { it.root.getClearType() == EventType.MESSAGE }
                    .also { list ->
                        val message = list.joinToString(",", "[", "]") { "${it.root.type}|${it.root.sendState}" }
                        Log.v("#E2E TEST", "Timeline snapshot is $message")
                    }
                    .filter { it.root.sendState == SendState.SYNCED }
                    .firstOrNull { it.root.getClearContent().toModel<MessageContent>()?.body?.startsWith(text) == true }
            sentEventId = decryptedMsg?.eventId
            decryptedMsg != null
        }
        timeline.dispose()
        return sentEventId
    }

    /**
     * Test that if a better key is forwared (lower index, it is then used)
     */
    @Test
    fun testASelfInteractiveVerificationAndGossip() = runCryptoTest(context()) { cryptoTestHelper, testHelper ->

        val aliceSession = testHelper.createAccount("alice", SessionTestParams(true))
        cryptoTestHelper.bootstrapSecurity(aliceSession)

        // now let's create a new login from alice

        val aliceNewSession = testHelper.logIntoAccount(aliceSession.myUserId, SessionTestParams(true))

        val oldCompleteLatch = CountDownLatch(1)
        lateinit var oldCode: String
        aliceSession.cryptoService().verificationService().addListener(object : VerificationService.Listener {

            override fun verificationRequestUpdated(pr: PendingVerificationRequest) {
                val readyInfo = pr.readyInfo
                if (readyInfo != null) {
                    aliceSession.cryptoService().verificationService().beginKeyVerification(
                            VerificationMethod.SAS,
                            aliceSession.myUserId,
                            readyInfo.fromDevice,
                            readyInfo.transactionId

                    )
                }
            }

            override fun transactionUpdated(tx: VerificationTransaction) {
                Log.d("##TEST", "exitsingPov: $tx")
                val sasTx = tx as OutgoingSasVerificationTransaction
                when (sasTx.uxState) {
                    OutgoingSasVerificationTransaction.UxState.SHOW_SAS -> {
                        // for the test we just accept?
                        oldCode = sasTx.getDecimalCodeRepresentation()
                        sasTx.userHasVerifiedShortCode()
                    }
                    OutgoingSasVerificationTransaction.UxState.VERIFIED -> {
                        // we can release this latch?
                        oldCompleteLatch.countDown()
                    }
                    else -> Unit
                }
            }
        })

        val newCompleteLatch = CountDownLatch(1)
        lateinit var newCode: String
        aliceNewSession.cryptoService().verificationService().addListener(object : VerificationService.Listener {

            override fun verificationRequestCreated(pr: PendingVerificationRequest) {
                // let's ready
                aliceNewSession.cryptoService().verificationService().readyPendingVerification(
                        listOf(VerificationMethod.SAS, VerificationMethod.QR_CODE_SCAN, VerificationMethod.QR_CODE_SHOW),
                        aliceSession.myUserId,
                        pr.transactionId!!
                )
            }

            var matchOnce = true
            override fun transactionUpdated(tx: VerificationTransaction) {
                Log.d("##TEST", "newPov: $tx")

                val sasTx = tx as IncomingSasVerificationTransaction
                when (sasTx.uxState) {
                    IncomingSasVerificationTransaction.UxState.SHOW_ACCEPT -> {
                        // no need to accept as there was a request first it will auto accept
                    }
                    IncomingSasVerificationTransaction.UxState.SHOW_SAS -> {
                        if (matchOnce) {
                            sasTx.userHasVerifiedShortCode()
                            newCode = sasTx.getDecimalCodeRepresentation()
                            matchOnce = false
                        }
                    }
                    IncomingSasVerificationTransaction.UxState.VERIFIED -> {
                        newCompleteLatch.countDown()
                    }
                    else -> Unit
                }
            }
        })

        // initiate self verification
        aliceSession.cryptoService().verificationService().requestKeyVerification(
                listOf(VerificationMethod.SAS, VerificationMethod.QR_CODE_SCAN, VerificationMethod.QR_CODE_SHOW),
                aliceNewSession.myUserId,
                listOf(aliceNewSession.sessionParams.deviceId!!)
        )
        testHelper.await(oldCompleteLatch)
        testHelper.await(newCompleteLatch)
        assertEquals("Decimal code should have matched", oldCode, newCode)

        // Assert that devices are verified
        val newDeviceFromOldPov: CryptoDeviceInfo? =
                aliceSession.cryptoService().getCryptoDeviceInfo(aliceSession.myUserId, aliceNewSession.sessionParams.deviceId)
        val oldDeviceFromNewPov: CryptoDeviceInfo? =
                aliceSession.cryptoService().getCryptoDeviceInfo(aliceSession.myUserId, aliceSession.sessionParams.deviceId)

        Assert.assertTrue("new device should be verified from old point of view", newDeviceFromOldPov!!.isVerified)
        Assert.assertTrue("old device should be verified from new point of view", oldDeviceFromNewPov!!.isVerified)

        // wait for secret gossiping to happen
        testHelper.waitWithLatch { latch ->
            testHelper.retryPeriodicallyWithLatch(latch) {
                aliceNewSession.cryptoService().crossSigningService().allPrivateKeysKnown()
            }
        }

        testHelper.waitWithLatch { latch ->
            testHelper.retryPeriodicallyWithLatch(latch) {
                aliceNewSession.cryptoService().keysBackupService().getKeyBackupRecoveryKeyInfo() != null
            }
        }

        assertEquals(
                "MSK Private parts should be the same",
                aliceSession.cryptoService().crossSigningService().getCrossSigningPrivateKeys()!!.master,
                aliceNewSession.cryptoService().crossSigningService().getCrossSigningPrivateKeys()!!.master
        )
        assertEquals(
                "USK Private parts should be the same",
                aliceSession.cryptoService().crossSigningService().getCrossSigningPrivateKeys()!!.user,
                aliceNewSession.cryptoService().crossSigningService().getCrossSigningPrivateKeys()!!.user
        )

        assertEquals(
                "SSK Private parts should be the same",
                aliceSession.cryptoService().crossSigningService().getCrossSigningPrivateKeys()!!.selfSigned,
                aliceNewSession.cryptoService().crossSigningService().getCrossSigningPrivateKeys()!!.selfSigned
        )

        // Let's check that we have the megolm backup key
        assertEquals(
                "Megolm key should be the same",
                aliceSession.cryptoService().keysBackupService().getKeyBackupRecoveryKeyInfo()!!.recoveryKey,
                aliceNewSession.cryptoService().keysBackupService().getKeyBackupRecoveryKeyInfo()!!.recoveryKey
        )
        assertEquals(
                "Megolm version should be the same",
                aliceSession.cryptoService().keysBackupService().getKeyBackupRecoveryKeyInfo()!!.version,
                aliceNewSession.cryptoService().keysBackupService().getKeyBackupRecoveryKeyInfo()!!.version
        )
    }

    private suspend fun ensureMembersHaveJoined(testHelper: CommonTestHelper, aliceSession: Session, otherAccounts: List<Session>, e2eRoomID: String) {
        testHelper.retryPeriodically {
            otherAccounts.map {
                aliceSession.roomService().getRoomMember(it.myUserId, e2eRoomID)?.membership
            }.all {
                it == Membership.JOIN
            }
        }
    }

    private fun ensureIsDecrypted(testHelper: CommonTestHelper, sentEventIds: List<String>, session: Session, e2eRoomID: String) {
        testHelper.waitWithLatch { latch ->
            sentEventIds.forEach { sentEventId ->
                testHelper.retryPeriodicallyWithLatch(latch) {
                    val timeLineEvent = session.getRoom(e2eRoomID)?.getTimelineEvent(sentEventId)
                    timeLineEvent != null &&
                            timeLineEvent.isEncrypted() &&
                            timeLineEvent.root.getClearType() == EventType.MESSAGE
                }
            }
        }
    }
}
