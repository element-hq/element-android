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
import org.matrix.android.sdk.api.auth.UIABaseAuth
import org.matrix.android.sdk.api.auth.UserInteractiveAuthInterceptor
import org.matrix.android.sdk.api.auth.UserPasswordAuth
import org.matrix.android.sdk.api.auth.registration.RegistrationFlowResponse
import org.matrix.android.sdk.api.crypto.MXCryptoConfig
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.MXCryptoError
import org.matrix.android.sdk.api.session.crypto.model.MessageVerificationState
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.content.EncryptedEventContent
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.room.getTimelineEvent
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.common.CommonTestHelper
import org.matrix.android.sdk.common.CommonTestHelper.Companion.runCryptoTest
import org.matrix.android.sdk.common.CommonTestHelper.Companion.runSessionTest
import org.matrix.android.sdk.common.SessionTestParams
import org.matrix.android.sdk.common.TestConstants
import org.matrix.android.sdk.mustFail
import timber.log.Timber
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

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
    fun testSendingE2EEMessages() = runCryptoTest(context()) { cryptoTestHelper, testHelper ->

        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoom(true)
        val aliceSession = cryptoTestData.firstSession
        val e2eRoomID = cryptoTestData.roomId
        val aliceRoomPOV = aliceSession.getRoom(e2eRoomID)!!
        // we want to disable key gossiping to just check initial sending of keys
        if (aliceSession.cryptoService().supportsDisablingKeyGossiping()) {
            aliceSession.cryptoService().enableKeyGossiping(false)
        }
        if (cryptoTestData.secondSession?.cryptoService()?.supportsDisablingKeyGossiping() == true) {
            cryptoTestData.secondSession?.cryptoService()?.enableKeyGossiping(false)
        }

        // add some more users and invite them
        val otherAccounts = listOf("benoit", "valere", "ganfra") // , "adam", "manu")
                .let {
                    cryptoTestHelper.inviteNewUsersAndWaitForThemToJoin(aliceSession, e2eRoomID, it)
                }

        Log.v("#E2E TEST", "All users have joined the room")
        Log.v("#E2E TEST", "Alice is sending the message")

        val text = "This is my message"
        val sentEventId: String = testHelper.sendMessageInRoom(aliceRoomPOV, text)
        Log.v("#E2E TEST", "Alice just sent message with id:$sentEventId")

        // All should be able to decrypt
        otherAccounts.forEach { otherSession ->
            val room = otherSession.getRoom(e2eRoomID)!!
            testHelper.ensureMessage(room, sentEventId) {
                it.isEncrypted() &&
                        it.root.getClearType() == EventType.MESSAGE &&
                        it.root.mxDecryptionResult?.verificationState == MessageVerificationState.UN_SIGNED_DEVICE
            }
        }
        Log.v("#E2E TEST", "Everybody received the encrypted message and could decrypt")
        // Add a new user to the room, and check that he can't decrypt
        Log.v("#E2E TEST", "Create some new accounts and invite them")
        val newAccount = listOf("adam") // , "adam", "manu")
                .let {
                    cryptoTestHelper.inviteNewUsersAndWaitForThemToJoin(aliceSession, e2eRoomID, it)
                }

        // wait a bit
        delay(3_000)

        // check that messages are encrypted (uisi)
        newAccount.forEach { otherSession ->
            testHelper.retryWithBackoff(
                    onFail = {
                        fail("New Users shouldn't be able to decrypt history")
                    }
            ) {
                val timelineEvent = otherSession.getRoom(e2eRoomID)?.getTimelineEvent(sentEventId).also {
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
        val secondSentEventId: String = testHelper.sendMessageInRoom(aliceRoomPOV, secondMessage)

        // new members should be able to decrypt it
        newAccount.forEach { otherSession ->
            // ("${otherSession.myUserId} should be able to decrypt")
            testHelper.retryWithBackoff(
                    onFail = {
                        fail("New user ${otherSession.myUserId.take(10)} should be able to decrypt the second message")
                    }
            ) {
                val timelineEvent = otherSession.getRoom(e2eRoomID)?.getTimelineEvent(secondSentEventId).also {
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
    fun testBasicBackupImport() = runCryptoTest(context()) { cryptoTestHelper, testHelper ->

        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoom(true)
        val aliceSession = cryptoTestData.firstSession
        val bobSession = cryptoTestData.secondSession!!
        val e2eRoomID = cryptoTestData.roomId

        Log.v("#E2E TEST", "Create and start key backup for bob ...")
        val bobKeysBackupService = bobSession.cryptoService().keysBackupService()
        val keyBackupPassword = "FooBarBaz"
        val megolmBackupCreationInfo = bobKeysBackupService.prepareKeysBackupVersion(keyBackupPassword, null)
        val version = bobKeysBackupService.createKeysBackupVersion(megolmBackupCreationInfo)

        Log.v("#E2E TEST", "... Key backup started and enabled for bob: version:$version")
        // Bob session should now have

        val aliceRoomPOV = aliceSession.getRoom(e2eRoomID)!!

        // let's send a few message to bob
        val sentEventIds = mutableListOf<String>()
        val messagesText = listOf("1. Hello", "2. Bob", "3. Good morning")
        messagesText.forEach { text ->
            val sentEventId = testHelper.sendMessageInRoom(aliceRoomPOV, text).also {
                sentEventIds.add(it)
            }

            testHelper.retryWithBackoff(
                    onFail = {
                        fail("Bob should be able to decrypt all messages")
                    }
            ) {
                val timeLineEvent = bobSession.getRoom(e2eRoomID)?.getTimelineEvent(sentEventId)
                timeLineEvent != null &&
                        timeLineEvent.isEncrypted() &&
                        timeLineEvent.root.getClearType() == EventType.MESSAGE
            }
            // we want more so let's discard the session
            aliceSession.cryptoService().discardOutboundSession(e2eRoomID)
        }
        Log.v("#E2E TEST", "Bob received all and can decrypt")

        // Let's wait a bit to be sure that bob has backed up the session

        Log.v("#E2E TEST", "Force key backup for Bob...")
        testHelper.retryWithBackoff(
                onFail = {
                    fail("All keys should be backedup")
                }
        ) {
            Log.v("#E2E TEST", "backedUp=${ bobKeysBackupService.getTotalNumbersOfBackedUpKeys()}, known=${bobKeysBackupService.getTotalNumbersOfKeys()}")
            bobKeysBackupService.getTotalNumbersOfBackedUpKeys() == bobKeysBackupService.getTotalNumbersOfKeys()
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
            testHelper.retryWithBackoff {
                val timelineEvent = newBobSession.getRoom(e2eRoomID)?.getTimelineEvent(sentEventId)?.also {
                    Log.v("#E2E TEST", "Event seen by new user ${it.root.getClearType()}|${it.root.mCryptoError}")
                }
                timelineEvent != null && timelineEvent.root.getClearType() == EventType.ENCRYPTED
            }
        }
        // after initial sync events are not decrypted, so we have to try manually
        // TODO CHANGE WHEN AVAILABLE FROM RUST
        cryptoTestHelper.ensureCannotDecrypt(
                sentEventIds,
                newBobSession,
                e2eRoomID,
                MXCryptoError.ErrorType.UNKNOWN_INBOUND_SESSION_ID
        ) // MXCryptoError.ErrorType.UNKNOWN_INBOUND_SESSION_ID)

        // Let's now import keys from backup
        Log.v("#E2E TEST", "Restore backup for the new session")
        newBobSession.cryptoService().keysBackupService().let { kbs ->
            val keyVersionResult = kbs.getVersion(version.version)

            val importedResult = kbs.restoreKeyBackupWithPassword(
                    keyVersionResult!!,
                    keyBackupPassword,
                    null,
                    null,
                    null,
            )

            assertEquals(3, importedResult.totalNumberOfKeys)
        }

        // ensure bob can now decrypt

        Log.v("#E2E TEST", "Check that bob can decrypt now")
        cryptoTestHelper.ensureCanDecrypt(sentEventIds, newBobSession, e2eRoomID, messagesText)

        // Check key trust
        Log.v("#E2E TEST", "Check key safety")
        sentEventIds.forEach { sentEventId ->
            val timelineEvent = newBobSession.getRoom(e2eRoomID)?.getTimelineEvent(sentEventId)!!
            val result = newBobSession.cryptoService().decryptEvent(timelineEvent.root, "")
            assertEquals("Keys from history should be deniable", MessageVerificationState.UNSAFE_SOURCE, result.messageVerificationState)
        }
    }

    /**
     * Check that a new verified session that was not supposed to get the keys initially will
     * get them from an older one.
     */
    @Test
    fun testSimpleGossip() = runCryptoTest(context()) { cryptoTestHelper, testHelper ->

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
            val sentEventId = testHelper.sendMessageInRoom(aliceRoomPOV, text).also {
                sentEventIds.add(it)
            }

            testHelper.retryWithBackoff(
                    onFail = {
                        fail("${bobSession.myUserId.take(10)} should be able to decrypt message sent by alice}")
                    }
            ) {
                val timeLineEvent = bobSession.getRoom(e2eRoomID)?.getTimelineEvent(sentEventId)
                timeLineEvent != null &&
                        timeLineEvent.isEncrypted() &&
                        timeLineEvent.root.getClearType() == EventType.MESSAGE
            }
        }

        // Ensure bob can decrypt
        ensureIsDecrypted(testHelper, sentEventIds, bobSession, e2eRoomID)

        // Let's now add a new bob session
        // Create a new session for bob
        Log.v("#E2E TEST", "Create a new session for Bob")
        val newBobSession = testHelper.logIntoAccount(bobSession.myUserId, SessionTestParams(true))

        // ensure first session is aware of the new one
        bobSession.cryptoService().downloadKeysIfNeeded(listOf(bobSession.myUserId), true)

        // check that new bob can't currently decrypt
        Log.v("#E2E TEST", "check that new bob can't currently decrypt")

        cryptoTestHelper.ensureCannotDecrypt(sentEventIds, newBobSession, e2eRoomID, null)

        // Try to request
//
//        Log.v("#E2E TEST", "Let bob re-request")
//        sentEventIds.forEach { sentEventId ->
//            val event = newBobSession.getRoom(e2eRoomID)!!.getTimelineEvent(sentEventId)!!.root
//            newBobSession.cryptoService().reRequestRoomKeyForEvent(event)
//        }
//
//        Log.v("#E2E TEST", "Should not be able to decrypt as not verified")
//        cryptoTestHelper.ensureCannotDecrypt(sentEventIds, newBobSession, e2eRoomID, null)

        // Now mark new bob session as verified

        Log.v("#E2E TEST", "Mark all as verified")
        bobSession.cryptoService().verificationService().markedLocallyAsManuallyVerified(newBobSession.myUserId, newBobSession.sessionParams.deviceId)
        newBobSession.cryptoService().verificationService().markedLocallyAsManuallyVerified(bobSession.myUserId, bobSession.sessionParams.deviceId)

        // now let new session re-request

        Log.v("#E2E TEST", "Re-request")
        sentEventIds.forEach { sentEventId ->
            val event = newBobSession.getRoom(e2eRoomID)!!.getTimelineEvent(sentEventId)!!.root
            newBobSession.cryptoService().reRequestRoomKeyForEvent(event)
        }

        Log.v("#E2E TEST", "Now should be able to decrypt")
        cryptoTestHelper.ensureCanDecrypt(sentEventIds, newBobSession, e2eRoomID, messagesText)
    }

    /**
     * Test that if a better key is forwarded (lower index, it is then used)
     */
    @Test
    fun testForwardBetterKey() = runCryptoTest(
            context(),
            cryptoConfig = MXCryptoConfig(limitRoomKeyRequestsToMyDevices = false)
    ) { cryptoTestHelper, testHelper ->

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
            firstEventId = testHelper.sendMessageInRoom(aliceRoomPOV, text)

            testHelper.retryWithBackoff {
                val timeLineEvent = bobSessionWithBetterKey.getRoom(e2eRoomID)?.getTimelineEvent(firstEventId)
                timeLineEvent != null &&
                        timeLineEvent.isEncrypted() &&
                        timeLineEvent.root.getClearType() == EventType.MESSAGE
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
            secondEventId = testHelper.sendMessageInRoom(aliceRoomPOV, text)

            testHelper.retryWithBackoff {
                val timeLineEvent = newBobSession.getRoom(e2eRoomID)?.getTimelineEvent(secondEventId)
                timeLineEvent != null &&
                        timeLineEvent.isEncrypted() &&
                        timeLineEvent.root.getClearType() == EventType.MESSAGE
            }
        }

        // check that both messages have same sessionId (it's just that we don't have index 0)
        val firstEventNewBobPov = newBobSession.getRoom(e2eRoomID)?.getTimelineEvent(firstEventId)
        val secondEventNewBobPov = newBobSession.getRoom(e2eRoomID)?.getTimelineEvent(secondEventId)

        val firstSessionId = firstEventNewBobPov!!.root.content.toModel<EncryptedEventContent>()!!.sessionId!!
        val secondSessionId = secondEventNewBobPov!!.root.content.toModel<EncryptedEventContent>()!!.sessionId!!

        Assert.assertTrue("Should be the same session id", firstSessionId == secondSessionId)

        // Confirm we can decrypt one but not the other
        mustFail(message = "Should not be able to decrypt event") {
            newBobSession.cryptoService().decryptEvent(firstEventNewBobPov.root, "")
        }

        try {
            newBobSession.cryptoService().decryptEvent(secondEventNewBobPov.root, "")
        } catch (error: MXCryptoError) {
            fail("Should be able to decrypt event")
        }

        // Now let's verify bobs session, and re-request keys
        bobSessionWithBetterKey.cryptoService()
                .verificationService()
                .markedLocallyAsManuallyVerified(newBobSession.myUserId, newBobSession.sessionParams.deviceId)

        newBobSession.cryptoService()
                .verificationService()
                .markedLocallyAsManuallyVerified(bobSessionWithBetterKey.myUserId, bobSessionWithBetterKey.sessionParams.deviceId)

        // now let new session request
        newBobSession.cryptoService().reRequestRoomKeyForEvent(firstEventNewBobPov.root)

        // We need to wait for the key request to be sent out and then a reply to be received

        // old session should have shared the key at earliest known index now
        // we should be able to decrypt both
        testHelper.retryWithBackoff {
            val canDecryptFirst = try {
                newBobSession.cryptoService().decryptEvent(firstEventNewBobPov.root, "")
                true
            } catch (error: MXCryptoError) {
                false
            }
            val canDecryptSecond = try {
                newBobSession.cryptoService().decryptEvent(secondEventNewBobPov.root, "")
                true
            } catch (error: MXCryptoError) {
                false
            }
            canDecryptFirst && canDecryptSecond
        }
    }

    /**
     * Test that if a better key is forwared (lower index, it is then used)
     */
//    @Test
//    fun testASelfInteractiveVerificationAndGossip() = runCryptoTest(context()) { cryptoTestHelper, testHelper ->
//
//        val aliceSession = testHelper.createAccount("alice", SessionTestParams(true))
//        cryptoTestHelper.bootstrapSecurity(aliceSession)
//
//        // now let's create a new login from alice
//
//        val aliceNewSession = testHelper.logIntoAccount(aliceSession.myUserId, SessionTestParams(true))
//
//        val deferredOldCode = aliceSession.cryptoService().verificationService().readOldVerificationCodeAsync(this, aliceSession.myUserId)
//        val deferredNewCode = aliceNewSession.cryptoService().verificationService().readNewVerificationCodeAsync(this, aliceSession.myUserId)
//        // initiate self verification
//        aliceSession.cryptoService().verificationService().requestSelfKeyVerification(
//                listOf(VerificationMethod.SAS, VerificationMethod.QR_CODE_SCAN, VerificationMethod.QR_CODE_SHOW),
// //                aliceNewSession.myUserId,
// //                listOf(aliceNewSession.sessionParams.deviceId!!)
//        )
//
//        val (oldCode, newCode) = awaitAll(deferredOldCode, deferredNewCode)
//
//        assertEquals("Decimal code should have matched", oldCode, newCode)
//
//        // Assert that devices are verified
//        val newDeviceFromOldPov: CryptoDeviceInfo? =
//                aliceSession.cryptoService().getCryptoDeviceInfo(aliceSession.myUserId, aliceNewSession.sessionParams.deviceId)
//        val oldDeviceFromNewPov: CryptoDeviceInfo? =
//                aliceSession.cryptoService().getCryptoDeviceInfo(aliceSession.myUserId, aliceSession.sessionParams.deviceId)
//
//        Assert.assertTrue("new device should be verified from old point of view", newDeviceFromOldPov!!.isVerified)
//        Assert.assertTrue("old device should be verified from new point of view", oldDeviceFromNewPov!!.isVerified)
//
//        // wait for secret gossiping to happen
//        testHelper.retryPeriodically {
//            aliceNewSession.cryptoService().crossSigningService().allPrivateKeysKnown()
//        }
//
//        testHelper.retryPeriodically {
//            aliceNewSession.cryptoService().keysBackupService().getKeyBackupRecoveryKeyInfo() != null
//        }
//
//        assertEquals(
//                "MSK Private parts should be the same",
//                aliceSession.cryptoService().crossSigningService().getCrossSigningPrivateKeys()!!.master,
//                aliceNewSession.cryptoService().crossSigningService().getCrossSigningPrivateKeys()!!.master
//        )
//        assertEquals(
//                "USK Private parts should be the same",
//                aliceSession.cryptoService().crossSigningService().getCrossSigningPrivateKeys()!!.user,
//                aliceNewSession.cryptoService().crossSigningService().getCrossSigningPrivateKeys()!!.user
//        )
//
//        assertEquals(
//                "SSK Private parts should be the same",
//                aliceSession.cryptoService().crossSigningService().getCrossSigningPrivateKeys()!!.selfSigned,
//                aliceNewSession.cryptoService().crossSigningService().getCrossSigningPrivateKeys()!!.selfSigned
//        )
//
//        // Let's check that we have the megolm backup key
//        assertEquals(
//                "Megolm key should be the same",
//                aliceSession.cryptoService().keysBackupService().getKeyBackupRecoveryKeyInfo()!!.recoveryKey,
//                aliceNewSession.cryptoService().keysBackupService().getKeyBackupRecoveryKeyInfo()!!.recoveryKey
//        )
//        assertEquals(
//                "Megolm version should be the same",
//                aliceSession.cryptoService().keysBackupService().getKeyBackupRecoveryKeyInfo()!!.version,
//                aliceNewSession.cryptoService().keysBackupService().getKeyBackupRecoveryKeyInfo()!!.version
//        )
//    }

    @Test
    fun test_EncryptionDoesNotHinderVerification() = runCryptoTest(context()) { cryptoTestHelper, testHelper ->
        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoom()

        val aliceSession = cryptoTestData.firstSession
        val bobSession = cryptoTestData.secondSession

        val aliceAuthParams = UserPasswordAuth(
                user = aliceSession.myUserId,
                password = TestConstants.PASSWORD
        )

        val bobAuthParams = UserPasswordAuth(
                user = bobSession!!.myUserId,
                password = TestConstants.PASSWORD
        )

        aliceSession.cryptoService().crossSigningService().initializeCrossSigning(object : UserInteractiveAuthInterceptor {
            override fun performStage(flowResponse: RegistrationFlowResponse, errCode: String?, promise: Continuation<UIABaseAuth>) {
                promise.resume(aliceAuthParams)
            }
        })

        bobSession.cryptoService().crossSigningService().initializeCrossSigning(object : UserInteractiveAuthInterceptor {
            override fun performStage(flowResponse: RegistrationFlowResponse, errCode: String?, promise: Continuation<UIABaseAuth>) {
                promise.resume(bobAuthParams)
            }
        })

        // add a second session for bob but not cross signed

        val secondBobSession = testHelper.logIntoAccount(bobSession.myUserId, SessionTestParams(true))

        aliceSession.cryptoService().setGlobalBlacklistUnverifiedDevices(true)

        // The two bob session should not be able to decrypt any message

        val roomFromAlicePOV = aliceSession.getRoom(cryptoTestData.roomId)!!
        Timber.v("#TEST: Send a first message that should be withheld")
        val sentEvent = testHelper.sendMessageInRoom(roomFromAlicePOV, "Hello")

        // wait for it to be synced back the other side
        Timber.v("#TEST: Wait for message to be synced back")
        testHelper.retryWithBackoff {
            bobSession.roomService().getRoom(cryptoTestData.roomId)?.timelineService()?.getTimelineEvent(sentEvent) != null
        }

        testHelper.retryWithBackoff {
            secondBobSession.roomService().getRoom(cryptoTestData.roomId)?.timelineService()?.getTimelineEvent(sentEvent) != null
        }

        // bob should not be able to decrypt
        Timber.v("#TEST: Ensure cannot be decrytped")
        cryptoTestHelper.ensureCannotDecrypt(listOf(sentEvent), bobSession, cryptoTestData.roomId)
        cryptoTestHelper.ensureCannotDecrypt(listOf(sentEvent), secondBobSession, cryptoTestData.roomId)

        // let's try to verify, it should work even if bob devices are untrusted
        Timber.v("#TEST: Do the verification")
        cryptoTestHelper.verifySASCrossSign(aliceSession, bobSession, cryptoTestData.roomId)

        Timber.v("#TEST: Send a second message, outbound session should have rotated and only bob 1rst session should decrypt")

        val secondEvent = testHelper.sendMessageInRoom(roomFromAlicePOV, "World")
        Timber.v("#TEST: Wait for message to be synced back")
        testHelper.retryWithBackoff {
            bobSession.roomService().getRoom(cryptoTestData.roomId)?.timelineService()?.getTimelineEvent(secondEvent) != null
        }

        testHelper.retryWithBackoff {
            secondBobSession.roomService().getRoom(cryptoTestData.roomId)?.timelineService()?.getTimelineEvent(secondEvent) != null
        }

        cryptoTestHelper.ensureCanDecrypt(listOf(secondEvent), bobSession, cryptoTestData.roomId, listOf("World"))
        cryptoTestHelper.ensureCannotDecrypt(listOf(secondEvent), secondBobSession, cryptoTestData.roomId)
    }

//    private suspend fun VerificationService.readOldVerificationCodeAsync(scope: CoroutineScope, userId: String): Deferred<String> {
//        return scope.async {
//            suspendCancellableCoroutine { continuation ->
//                var oldCode: String? = null
//                val listener = object : VerificationService.Listener {
//
//                    override fun verificationRequestUpdated(pr: PendingVerificationRequest) {
//                        val readyInfo = pr.readyInfo
//                        if (readyInfo != null) {
//                            beginKeyVerification(
//                                    VerificationMethod.SAS,
//                                    userId,
//                                    readyInfo.fromDevice,
//                                    readyInfo.transactionId
//
//                            )
//                        }
//                    }
//
//                    override fun transactionUpdated(tx: VerificationTransaction) {
//                        Log.d("##TEST", "exitsingPov: $tx")
//                        val sasTx = tx as OutgoingSasVerificationTransaction
//                        when (sasTx.uxState) {
//                            OutgoingSasVerificationTransaction.UxState.SHOW_SAS -> {
//                                // for the test we just accept?
//                                oldCode = sasTx.getDecimalCodeRepresentation()
//                                sasTx.userHasVerifiedShortCode()
//                            }
//                            OutgoingSasVerificationTransaction.UxState.VERIFIED -> {
//                                removeListener(this)
//                                // we can release this latch?
//                                continuation.resume(oldCode!!)
//                            }
//                            else                                                -> Unit
//                        }
//                    }
//                }
//                addListener(listener)
//                continuation.invokeOnCancellation { removeListener(listener) }
//            }
//        }
//    }
//
//    private suspend fun VerificationService.readNewVerificationCodeAsync(scope: CoroutineScope, userId: String): Deferred<String> {
//        return scope.async {
//            suspendCancellableCoroutine { continuation ->
//                var newCode: String? = null
//
//                val listener = object : VerificationService.Listener {
//
//                    override fun verificationRequestCreated(pr: PendingVerificationRequest) {
//                        // let's ready
//                        readyPendingVerification(
//                                listOf(VerificationMethod.SAS, VerificationMethod.QR_CODE_SCAN, VerificationMethod.QR_CODE_SHOW),
//                                userId,
//                                pr.transactionId!!
//                        )
//                    }
//
//                    var matchOnce = true
//                    override fun transactionUpdated(tx: VerificationTransaction) {
//                        Log.d("##TEST", "newPov: $tx")
//
//                        val sasTx = tx as IncomingSasVerificationTransaction
//                        when (sasTx.uxState) {
//                            IncomingSasVerificationTransaction.UxState.SHOW_ACCEPT -> {
//                                // no need to accept as there was a request first it will auto accept
//                            }
//                            IncomingSasVerificationTransaction.UxState.SHOW_SAS    -> {
//                                if (matchOnce) {
//                                    sasTx.userHasVerifiedShortCode()
//                                    newCode = sasTx.getDecimalCodeRepresentation()
//                                    matchOnce = false
//                                }
//                            }
//                            IncomingSasVerificationTransaction.UxState.VERIFIED    -> {
//                                removeListener(this)
//                                continuation.resume(newCode!!)
//                            }
//                            else                                                   -> Unit
//                        }
//                    }
//                }
//                addListener(listener)
//                continuation.invokeOnCancellation { removeListener(listener) }
//            }
//        }
//    }

    private suspend fun ensureIsDecrypted(testHelper: CommonTestHelper, sentEventIds: List<String>, session: Session, e2eRoomID: String) {
        sentEventIds.forEach { sentEventId ->
            testHelper.retryPeriodically {
                val timeLineEvent = session.getRoom(e2eRoomID)?.getTimelineEvent(sentEventId)
                timeLineEvent != null &&
                        timeLineEvent.isEncrypted() &&
                        timeLineEvent.root.getClearType() == EventType.MESSAGE
            }
        }
    }
}
