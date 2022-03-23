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
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.MethodSorters
import org.matrix.android.sdk.InstrumentedTest
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.MXCryptoError
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.failure.JoinRoomFailure
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.api.session.room.timeline.TimelineSettings
import org.matrix.android.sdk.common.CommonTestHelper
import org.matrix.android.sdk.common.CryptoTestHelper
import org.matrix.android.sdk.common.SessionTestParams
import org.matrix.android.sdk.common.TestConstants
import org.matrix.android.sdk.common.TestMatrixCallback
import org.matrix.android.sdk.internal.crypto.algorithms.olm.OlmDecryptionResult
import org.matrix.android.sdk.internal.crypto.keysbackup.model.MegolmBackupCreationInfo
import org.matrix.android.sdk.internal.crypto.keysbackup.model.rest.KeysVersion
import org.matrix.android.sdk.internal.crypto.keysbackup.model.rest.KeysVersionResult
import org.matrix.android.sdk.internal.crypto.model.ImportRoomKeysResult
import org.matrix.android.sdk.internal.crypto.model.event.EncryptedEventContent

@RunWith(JUnit4::class)
@FixMethodOrder(MethodSorters.JVM)
@LargeTest
class E2eeSanityTests : InstrumentedTest {

    private val testHelper = CommonTestHelper(context())
    private val cryptoTestHelper = CryptoTestHelper(testHelper)

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
    fun testSendingE2EEMessages() {
        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoom(true)
        val aliceSession = cryptoTestData.firstSession
        val e2eRoomID = cryptoTestData.roomId

        val aliceRoomPOV = aliceSession.getRoom(e2eRoomID)!!

        // add some more users and invite them
        val otherAccounts = listOf("benoit", "valere", "ganfra") // , "adam", "manu")
                .map {
                    testHelper.createAccount(it, SessionTestParams(true))
                }

        Log.v("#E2E TEST", "All accounts created")
        // we want to invite them in the room
        otherAccounts.forEach {
            testHelper.runBlockingTest {
                Log.v("#E2E TEST", "Alice invites ${it.myUserId}")
                aliceRoomPOV.invite(it.myUserId)
            }
        }

        // All user should accept invite
        otherAccounts.forEach { otherSession ->
            waitForAndAcceptInviteInRoom(otherSession, e2eRoomID)
            Log.v("#E2E TEST", "${otherSession.myUserId} joined room $e2eRoomID")
        }

        // check that alice see them as joined (not really necessary?)
        ensureMembersHaveJoined(aliceSession, otherAccounts, e2eRoomID)

        Log.v("#E2E TEST", "All users have joined the room")
        Log.v("#E2E TEST", "Alice is sending the message")

        val text = "This is my message"
        val sentEventId: String? = sendMessageInRoom(aliceRoomPOV, text)
        //        val sentEvent = testHelper.sendTextMessage(aliceRoomPOV, "Hello all", 1).first()
        Assert.assertTrue("Message should be sent", sentEventId != null)

        // All should be able to decrypt
        otherAccounts.forEach { otherSession ->
            testHelper.waitWithLatch { latch ->
                testHelper.retryPeriodicallyWithLatch(latch) {
                    val timelineEvent = otherSession.getRoom(e2eRoomID)?.getTimelineEvent(sentEventId!!)
                    timelineEvent != null &&
                            timelineEvent.isEncrypted() &&
                            timelineEvent.root.getClearType() == EventType.MESSAGE
                }
            }
        }

        // Add a new user to the room, and check that he can't decrypt
        val newAccount = listOf("adam") // , "adam", "manu")
                .map {
                    testHelper.createAccount(it, SessionTestParams(true))
                }

        newAccount.forEach {
            testHelper.runBlockingTest {
                Log.v("#E2E TEST", "Alice invites ${it.myUserId}")
                aliceRoomPOV.invite(it.myUserId)
            }
        }

        newAccount.forEach {
            waitForAndAcceptInviteInRoom(it, e2eRoomID)
        }

        ensureMembersHaveJoined(aliceSession, newAccount, e2eRoomID)

        // wait a bit
        testHelper.runBlockingTest {
            delay(3_000)
        }

        // check that messages are encrypted (uisi)
        newAccount.forEach { otherSession ->
            testHelper.waitWithLatch { latch ->
                testHelper.retryPeriodicallyWithLatch(latch) {
                    val timelineEvent = otherSession.getRoom(e2eRoomID)?.getTimelineEvent(sentEventId!!).also {
                        Log.v("#E2E TEST", "Event seen by new user ${it?.root?.getClearType()}|${it?.root?.mCryptoError}")
                    }
                    timelineEvent != null &&
                            timelineEvent.root.getClearType() == EventType.ENCRYPTED &&
                            timelineEvent.root.mCryptoError == MXCryptoError.ErrorType.UNKNOWN_INBOUND_SESSION_ID
                }
            }
        }

        // Let alice send a new message
        Log.v("#E2E TEST", "Alice sends a new message")

        val secondMessage = "2 This is my message"
        val secondSentEventId: String? = sendMessageInRoom(aliceRoomPOV, secondMessage)

        // new members should be able to decrypt it
        newAccount.forEach { otherSession ->
            testHelper.waitWithLatch { latch ->
                testHelper.retryPeriodicallyWithLatch(latch) {
                    val timelineEvent = otherSession.getRoom(e2eRoomID)?.getTimelineEvent(secondSentEventId!!).also {
                        Log.v("#E2E TEST", "Second Event seen by new user ${it?.root?.getClearType()}|${it?.root?.mCryptoError}")
                    }
                    timelineEvent != null &&
                            timelineEvent.root.getClearType() == EventType.MESSAGE &&
                            secondMessage == timelineEvent.root.getClearContent().toModel<MessageContent>()?.body
                }
            }
        }

        otherAccounts.forEach {
            testHelper.signOutAndClose(it)
        }
        newAccount.forEach { testHelper.signOutAndClose(it) }

        cryptoTestData.cleanUp(testHelper)
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
    fun testBasicBackupImport() {
        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoom(true)
        val aliceSession = cryptoTestData.firstSession
        val bobSession = cryptoTestData.secondSession!!
        val e2eRoomID = cryptoTestData.roomId

        Log.v("#E2E TEST", "Create and start key backup for bob ...")
        val bobKeysBackupService = bobSession.cryptoService().keysBackupService()
        val keyBackupPassword = "FooBarBaz"
        val megolmBackupCreationInfo = testHelper.doSync<MegolmBackupCreationInfo> {
            bobKeysBackupService.prepareKeysBackupVersion(keyBackupPassword, null, it)
        }
        val version = testHelper.doSync<KeysVersion> {
            bobKeysBackupService.createKeysBackupVersion(megolmBackupCreationInfo, it)
        }
        Log.v("#E2E TEST", "... Key backup started and enabled for bob")
        // Bob session should now have

        val aliceRoomPOV = aliceSession.getRoom(e2eRoomID)!!

        // let's send a few message to bob
        val sentEventIds = mutableListOf<String>()
        val messagesText = listOf("1. Hello", "2. Bob", "3. Good morning")
        messagesText.forEach { text ->
            val sentEventId = sendMessageInRoom(aliceRoomPOV, text)!!.also {
                sentEventIds.add(it)
            }

            testHelper.waitWithLatch { latch ->
                testHelper.retryPeriodicallyWithLatch(latch) {
                    val timelineEvent = bobSession.getRoom(e2eRoomID)?.getTimelineEvent(sentEventId)
                    timelineEvent != null &&
                            timelineEvent.isEncrypted() &&
                            timelineEvent.root.getClearType() == EventType.MESSAGE
                }
            }
            // we want more so let's discard the session
            aliceSession.cryptoService().discardOutboundSession(e2eRoomID)

            testHelper.runBlockingTest {
                delay(1_000)
            }
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

        testHelper.runBlockingTest {
            delay(1_000)
        }

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
        ensureCannotDecrypt(sentEventIds, newBobSession, e2eRoomID, MXCryptoError.ErrorType.UNKNOWN_INBOUND_SESSION_ID)

        // Let's now import keys from backup

        newBobSession.cryptoService().keysBackupService().let { keysBackupService ->
            val keyVersionResult = testHelper.doSync<KeysVersionResult?> {
                keysBackupService.getVersion(version.version, it)
            }

            val importedResult = testHelper.doSync<ImportRoomKeysResult> {
                keysBackupService.restoreKeyBackupWithPassword(keyVersionResult!!,
                        keyBackupPassword,
                        null,
                        null,
                        null, it)
            }

            assertEquals(3, importedResult.totalNumberOfKeys)
        }

        // ensure bob can now decrypt
        ensureCanDecrypt(sentEventIds, newBobSession, e2eRoomID, messagesText)

        testHelper.signOutAndClose(newBobSession)
    }

    /**
     * Check that a new verified session that was not supposed to get the keys initially will
     * get them from an older one.
     */
    @Test
    fun testSimpleGossip() {
        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoom(true)
        val aliceSession = cryptoTestData.firstSession
        val bobSession = cryptoTestData.secondSession!!
        val e2eRoomID = cryptoTestData.roomId

        val aliceRoomPOV = aliceSession.getRoom(e2eRoomID)!!

        cryptoTestHelper.initializeCrossSigning(bobSession)

        // let's send a few message to bob
        val sentEventIds = mutableListOf<String>()
        val messagesText = listOf("1. Hello", "2. Bob")

        Log.v("#E2E TEST", "Alice sends some messages")
        messagesText.forEach { text ->
            val sentEventId = sendMessageInRoom(aliceRoomPOV, text)!!.also {
                sentEventIds.add(it)
            }

            testHelper.waitWithLatch { latch ->
                testHelper.retryPeriodicallyWithLatch(latch) {
                    val timelineEvent = bobSession.getRoom(e2eRoomID)?.getTimelineEvent(sentEventId)
                    timelineEvent != null &&
                            timelineEvent.isEncrypted() &&
                            timelineEvent.root.getClearType() == EventType.MESSAGE
                }
            }
        }

        // Ensure bob can decrypt
        ensureIsDecrypted(sentEventIds, bobSession, e2eRoomID)

        // Let's now add a new bob session
        // Create a new session for bob
        Log.v("#E2E TEST", "Create a new session for Bob")
        val newBobSession = testHelper.logIntoAccount(bobSession.myUserId, SessionTestParams(true))

        // check that new bob can't currently decrypt
        Log.v("#E2E TEST", "check that new bob can't currently decrypt")

        ensureCannotDecrypt(sentEventIds, newBobSession, e2eRoomID, MXCryptoError.ErrorType.UNKNOWN_INBOUND_SESSION_ID)

        // Try to request
        sentEventIds.forEach { sentEventId ->
            val event = newBobSession.getRoom(e2eRoomID)!!.getTimelineEvent(sentEventId)!!.root
            newBobSession.cryptoService().requestRoomKeyForEvent(event)
        }

        // wait a bit
        testHelper.runBlockingTest {
            delay(10_000)
        }

        // Ensure that new bob still can't decrypt (keys must have been withheld)
        ensureCannotDecrypt(sentEventIds, newBobSession, e2eRoomID, MXCryptoError.ErrorType.KEYS_WITHHELD)

        // Now mark new bob session as verified

        bobSession.cryptoService().verificationService().markedLocallyAsManuallyVerified(newBobSession.myUserId, newBobSession.sessionParams.deviceId!!)
        newBobSession.cryptoService().verificationService().markedLocallyAsManuallyVerified(bobSession.myUserId, bobSession.sessionParams.deviceId!!)

        // now let new session re-request
        sentEventIds.forEach { sentEventId ->
            val event = newBobSession.getRoom(e2eRoomID)!!.getTimelineEvent(sentEventId)!!.root
            newBobSession.cryptoService().reRequestRoomKeyForEvent(event)
        }

        // wait a bit
        testHelper.runBlockingTest {
            delay(10_000)
        }

        ensureCanDecrypt(sentEventIds, newBobSession, e2eRoomID, messagesText)

        cryptoTestData.cleanUp(testHelper)
        testHelper.signOutAndClose(newBobSession)
    }

    /**
     * Test that if a better key is forwarded (lower index, it is then used)
     */
    @Test
    fun testForwardBetterKey() {
        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoom(true)
        val aliceSession = cryptoTestData.firstSession
        val bobSessionWithBetterKey = cryptoTestData.secondSession!!
        val e2eRoomID = cryptoTestData.roomId

        val aliceRoomPOV = aliceSession.getRoom(e2eRoomID)!!

        cryptoTestHelper.initializeCrossSigning(bobSessionWithBetterKey)

        // let's send a few message to bob
        var firstEventId: String
        val firstMessage = "1. Hello"

        Log.v("#E2E TEST", "Alice sends some messages")
        firstMessage.let { text ->
            firstEventId = sendMessageInRoom(aliceRoomPOV, text)!!

            testHelper.waitWithLatch { latch ->
                testHelper.retryPeriodicallyWithLatch(latch) {
                    val timelineEvent = bobSessionWithBetterKey.getRoom(e2eRoomID)?.getTimelineEvent(firstEventId)
                    timelineEvent != null &&
                            timelineEvent.isEncrypted() &&
                            timelineEvent.root.getClearType() == EventType.MESSAGE
                }
            }
        }

        // Ensure bob can decrypt
        ensureIsDecrypted(listOf(firstEventId), bobSessionWithBetterKey, e2eRoomID)

        // Let's add a new unverified session from bob
        val newBobSession = testHelper.logIntoAccount(bobSessionWithBetterKey.myUserId, SessionTestParams(true))

        // check that new bob can't currently decrypt
        Log.v("#E2E TEST", "check that new bob can't currently decrypt")
        ensureCannotDecrypt(listOf(firstEventId), newBobSession, e2eRoomID, MXCryptoError.ErrorType.UNKNOWN_INBOUND_SESSION_ID)

        // Now let alice send a new message. this time the new bob session will be able to decrypt
        var secondEventId: String
        val secondMessage = "2. New Device?"

        Log.v("#E2E TEST", "Alice sends some messages")
        secondMessage.let { text ->
            secondEventId = sendMessageInRoom(aliceRoomPOV, text)!!

            testHelper.waitWithLatch { latch ->
                testHelper.retryPeriodicallyWithLatch(latch) {
                    val timelineEvent = newBobSession.getRoom(e2eRoomID)?.getTimelineEvent(secondEventId)
                    timelineEvent != null &&
                            timelineEvent.isEncrypted() &&
                            timelineEvent.root.getClearType() == EventType.MESSAGE
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
            try {
                newBobSession.cryptoService().decryptEvent(firstEventNewBobPov.root, "")
                fail("Should not be able to decrypt event")
            } catch (error: MXCryptoError) {
                val errorType = (error as? MXCryptoError.Base)?.errorType
                assertEquals(MXCryptoError.ErrorType.UNKNOWN_MESSAGE_INDEX, errorType)
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
        newBobSession.cryptoService().requestRoomKeyForEvent(firstEventNewBobPov.root)

        // wait a bit
        testHelper.runBlockingTest {
            delay(10_000)
        }

        // old session should have shared the key at earliest known index now
        // we should be able to decrypt both
        testHelper.runBlockingTest {
            try {
                newBobSession.cryptoService().decryptEvent(firstEventNewBobPov.root, "")
            } catch (error: MXCryptoError) {
                fail("Should be able to decrypt first event now $error")
            }
        }
        testHelper.runBlockingTest {
            try {
                newBobSession.cryptoService().decryptEvent(secondEventNewBobPov.root, "")
            } catch (error: MXCryptoError) {
                fail("Should be able to decrypt event $error")
            }
        }

        cryptoTestData.cleanUp(testHelper)
        testHelper.signOutAndClose(newBobSession)
    }

    private fun sendMessageInRoom(aliceRoomPOV: Room, text: String): String? {
        aliceRoomPOV.sendTextMessage(text)
        var sentEventId: String? = null
        testHelper.waitWithLatch(4 * TestConstants.timeOutMillis) { latch ->
            val timeline = aliceRoomPOV.createTimeline(null, TimelineSettings(60))
            timeline.start()

            testHelper.retryPeriodicallyWithLatch(latch) {
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
        }
        return sentEventId
    }

    private fun ensureMembersHaveJoined(aliceSession: Session, otherAccounts: List<Session>, e2eRoomID: String) {
        testHelper.waitWithLatch { latch ->
            testHelper.retryPeriodicallyWithLatch(latch) {
                otherAccounts.map {
                    aliceSession.getRoomMember(it.myUserId, e2eRoomID)?.membership
                }.all {
                    it == Membership.JOIN
                }
            }
        }
    }

    private fun waitForAndAcceptInviteInRoom(otherSession: Session, e2eRoomID: String) {
        testHelper.waitWithLatch { latch ->
            testHelper.retryPeriodicallyWithLatch(latch) {
                val roomSummary = otherSession.getRoomSummary(e2eRoomID)
                (roomSummary != null && roomSummary.membership == Membership.INVITE).also {
                    if (it) {
                        Log.v("#E2E TEST", "${otherSession.myUserId} can see the invite from alice")
                    }
                }
            }
        }

        testHelper.runBlockingTest(60_000) {
            Log.v("#E2E TEST", "${otherSession.myUserId} tries to join room $e2eRoomID")
            try {
                otherSession.joinRoom(e2eRoomID)
            } catch (ex: JoinRoomFailure.JoinedWithTimeout) {
                // it's ok we will wait after
            }
        }

        Log.v("#E2E TEST", "${otherSession.myUserId} waiting for join echo ...")
        testHelper.waitWithLatch {
            testHelper.retryPeriodicallyWithLatch(it) {
                val roomSummary = otherSession.getRoomSummary(e2eRoomID)
                roomSummary != null && roomSummary.membership == Membership.JOIN
            }
        }
    }

    private fun ensureCanDecrypt(sentEventIds: MutableList<String>, session: Session, e2eRoomID: String, messagesText: List<String>) {
        sentEventIds.forEachIndexed { index, sentEventId ->
            testHelper.waitWithLatch { latch ->
                testHelper.retryPeriodicallyWithLatch(latch) {
                    val event = session.getRoom(e2eRoomID)!!.getTimelineEvent(sentEventId)!!.root
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
                    event.getClearType() == EventType.MESSAGE &&
                            messagesText[index] == event.getClearContent()?.toModel<MessageContent>()?.body
                }
            }
        }
    }

    private fun ensureIsDecrypted(sentEventIds: List<String>, session: Session, e2eRoomID: String) {
        testHelper.waitWithLatch { latch ->
            sentEventIds.forEach { sentEventId ->
                testHelper.retryPeriodicallyWithLatch(latch) {
                    val timelineEvent = session.getRoom(e2eRoomID)?.getTimelineEvent(sentEventId)
                    timelineEvent != null &&
                            timelineEvent.isEncrypted() &&
                            timelineEvent.root.getClearType() == EventType.MESSAGE
                }
            }
        }
    }

    private fun ensureCannotDecrypt(sentEventIds: List<String>, newBobSession: Session, e2eRoomID: String, expectedError: MXCryptoError.ErrorType?) {
        sentEventIds.forEach { sentEventId ->
            val event = newBobSession.getRoom(e2eRoomID)!!.getTimelineEvent(sentEventId)!!.root
            testHelper.runBlockingTest {
                try {
                    newBobSession.cryptoService().decryptEvent(event, "")
                    fail("Should not be able to decrypt event")
                } catch (error: MXCryptoError) {
                    val errorType = (error as? MXCryptoError.Base)?.errorType
                    if (expectedError == null) {
                        Assert.assertNotNull(errorType)
                    } else {
                        assertEquals(expectedError, errorType, "Message expected to be UISI")
                    }
                }
            }
        }
    }
}
