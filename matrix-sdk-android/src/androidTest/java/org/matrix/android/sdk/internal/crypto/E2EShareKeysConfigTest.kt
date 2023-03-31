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
import org.amshove.kluent.internal.assertEquals
import org.junit.Assert
import org.junit.Assume
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.MethodSorters
import org.matrix.android.sdk.InstrumentedTest
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.room.getTimelineEvent
import org.matrix.android.sdk.api.session.room.model.RoomHistoryVisibility
import org.matrix.android.sdk.api.session.room.model.create.CreateRoomParams
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.common.CommonTestHelper
import org.matrix.android.sdk.common.CommonTestHelper.Companion.runCryptoTest
import org.matrix.android.sdk.common.CryptoTestData
import org.matrix.android.sdk.common.SessionTestParams
import org.matrix.android.sdk.common.TestConstants

@RunWith(JUnit4::class)
@FixMethodOrder(MethodSorters.JVM)
@LargeTest
class E2EShareKeysConfigTest : InstrumentedTest {

    @Test
    fun msc3061ShouldBeDisabledByDefault() = runCryptoTest(context()) { _, commonTestHelper ->
        val aliceSession = commonTestHelper.createAccount(TestConstants.USER_ALICE, SessionTestParams(withInitialSync = false))
        Assert.assertFalse("MSC3061 is lab and should be disabled by default", aliceSession.cryptoService().isShareKeysOnInviteEnabled())
    }

    @Test
    fun ensureKeysAreNotSharedIfOptionDisabled() = runCryptoTest(context()) { cryptoTestHelper, commonTestHelper ->
        val aliceSession = commonTestHelper.createAccount(TestConstants.USER_ALICE, SessionTestParams(withInitialSync = true))
        aliceSession.cryptoService().enableShareKeyOnInvite(false)
        val roomId = aliceSession.roomService().createRoom(CreateRoomParams().apply {
            historyVisibility = RoomHistoryVisibility.SHARED
            name = "MyRoom"
            enableEncryption()
        })

        commonTestHelper.retryWithBackoff {
            aliceSession.roomService().getRoomSummary(roomId)?.isEncrypted == true
        }
        val roomAlice = aliceSession.roomService().getRoom(roomId)!!

        // send some messages
        val withSession1 = commonTestHelper.sendMessageInRoom(roomAlice, "Hello")
        aliceSession.cryptoService().discardOutboundSession(roomId)
        val withSession2 = commonTestHelper.sendMessageInRoom(roomAlice, "World")

        // Create bob account
        val bobSession = commonTestHelper.createAccount(TestConstants.USER_BOB, SessionTestParams(withInitialSync = true))

        // Let alice invite bob
        roomAlice.membershipService().invite(bobSession.myUserId)

        commonTestHelper.waitForAndAcceptInviteInRoom(bobSession, roomId)

        // Bob has join but should not be able to decrypt history
        cryptoTestHelper.ensureCannotDecrypt(
                listOf(withSession1, withSession2),
                bobSession,
                roomId
        )

        // We don't need bob anymore
        commonTestHelper.signOutAndClose(bobSession)

        if (aliceSession.cryptoService().supportsShareKeysOnInvite()) {
            // Now let's enable history key sharing on alice side
            aliceSession.cryptoService().enableShareKeyOnInvite(true)

            // let's add a new message first
            val afterFlagOn = commonTestHelper.sendMessageInRoom(roomAlice, "After")

            // Worth nothing to check that the session was rotated
            Assert.assertNotEquals(
                    "Session should have been rotated",
                    aliceSession.roomService().getRoom(roomId)?.getTimelineEvent(withSession1)?.root?.content?.get("session_id")!!,
                    aliceSession.roomService().getRoom(roomId)?.getTimelineEvent(afterFlagOn)?.root?.content?.get("session_id")!!
            )

            // Invite a new user
            val samSession = commonTestHelper.createAccount(TestConstants.USER_SAM, SessionTestParams(withInitialSync = true))

            // Let alice invite sam
            roomAlice.membershipService().invite(samSession.myUserId)

            commonTestHelper.waitForAndAcceptInviteInRoom(samSession, roomId)

            // Sam shouldn't be able to decrypt messages with the first session, but should decrypt the one with 3rd session
            cryptoTestHelper.ensureCannotDecrypt(
                    listOf(withSession1, withSession2),
                    samSession,
                    roomId
            )

            cryptoTestHelper.ensureCanDecrypt(
                    listOf(afterFlagOn),
                    samSession,
                    roomId,
                    listOf(aliceSession.roomService().getRoom(roomId)?.getTimelineEvent(afterFlagOn)?.root?.getClearContent()?.get("body") as String)
            )
        }
    }

    @Test
    fun ifSharingDisabledOnAliceSideBobShouldNotShareAliceHistory() = runCryptoTest(context()) { cryptoTestHelper, commonTestHelper ->

        val testData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoom(roomHistoryVisibility = RoomHistoryVisibility.SHARED)

        Assume.assumeTrue("Shared key on invite needed to test this",
                testData.firstSession.cryptoService().supportsShareKeysOnInvite()
        )

        val aliceSession = testData.firstSession.also {
            it.cryptoService().enableShareKeyOnInvite(false)
        }
        val bobSession = testData.secondSession!!.also {
            it.cryptoService().enableShareKeyOnInvite(true)
        }

        val (fromAliceNotSharable, fromBobSharable, samSession) = commonAliceAndBobSendMessages(commonTestHelper, aliceSession, testData, bobSession)

        // Bob should have shared history keys to sam.
        // But has alice hasn't enabled sharing, bob shouldn't send her sessions
        cryptoTestHelper.ensureCannotDecrypt(
                fromAliceNotSharable.map { it.eventId },
                samSession,
                testData.roomId
        )

        cryptoTestHelper.ensureCanDecrypt(
                fromBobSharable.map { it.eventId },
                samSession,
                testData.roomId,
                fromBobSharable.map { it.root.getClearContent()?.get("body") as String })
    }

    @Test
    fun ifSharingEnabledOnAliceSideBobShouldShareAliceHistory() = runCryptoTest(context()) { cryptoTestHelper, commonTestHelper ->
        val testData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoom(roomHistoryVisibility = RoomHistoryVisibility.SHARED)

        Assume.assumeTrue("Shared key on invite needed to test this",
                testData.firstSession.cryptoService().supportsShareKeysOnInvite()
        )

        val aliceSession = testData.firstSession.also {
            it.cryptoService().enableShareKeyOnInvite(true)
        }
        val bobSession = testData.secondSession!!.also {
            it.cryptoService().enableShareKeyOnInvite(true)
        }

        val (fromAliceNotSharable, fromBobSharable, samSession) = commonAliceAndBobSendMessages(commonTestHelper, aliceSession, testData, bobSession)

        cryptoTestHelper.ensureCanDecrypt(
                fromAliceNotSharable.map { it.eventId },
                samSession,
                testData.roomId,
                fromAliceNotSharable.map { it.root.getClearContent()?.get("body") as String })

        cryptoTestHelper.ensureCanDecrypt(
                fromBobSharable.map { it.eventId },
                samSession,
                testData.roomId,
                fromBobSharable.map { it.root.getClearContent()?.get("body") as String })
    }

    private suspend fun commonAliceAndBobSendMessages(commonTestHelper: CommonTestHelper, aliceSession: Session, testData: CryptoTestData, bobSession: Session): Triple<List<TimelineEvent>, List<TimelineEvent>, Session> {
        val fromAliceNotSharable = commonTestHelper.sendTextMessage(aliceSession.getRoom(testData.roomId)!!, "Hello from alice", 1)
        val fromBobSharable = commonTestHelper.sendTextMessage(bobSession.getRoom(testData.roomId)!!, "Hello from bob", 1)

        // Now let bob invite Sam
        // Invite a new user
        val samSession = commonTestHelper.createAccount(TestConstants.USER_SAM, SessionTestParams(withInitialSync = true))

        // Let bob invite sam
        bobSession.getRoom(testData.roomId)!!.membershipService().invite(samSession.myUserId)

        commonTestHelper.waitForAndAcceptInviteInRoom(samSession, testData.roomId)
        return Triple(fromAliceNotSharable, fromBobSharable, samSession)
    }

    // test flag on backup is correct

    @Test
    fun testBackupFlagIsCorrect() = runCryptoTest(context()) { cryptoTestHelper, commonTestHelper ->
        val aliceSession = commonTestHelper.createAccount(TestConstants.USER_ALICE, SessionTestParams(withInitialSync = true))

        Assume.assumeTrue("Shared key on invite needed to test this",
                aliceSession.cryptoService().supportsShareKeysOnInvite()
        )

        aliceSession.cryptoService().enableShareKeyOnInvite(false)
        val roomId = aliceSession.roomService().createRoom(CreateRoomParams().apply {
            historyVisibility = RoomHistoryVisibility.SHARED
            name = "MyRoom"
            enableEncryption()
        })

        commonTestHelper.retryWithBackoff {
            aliceSession.roomService().getRoomSummary(roomId)?.isEncrypted == true
        }
        val roomAlice = aliceSession.roomService().getRoom(roomId)!!

        // send some messages
        val notSharableMessage = commonTestHelper.sendMessageInRoom(roomAlice, "Hello")

        aliceSession.cryptoService().enableShareKeyOnInvite(true)
        val sharableMessage = commonTestHelper.sendMessageInRoom(roomAlice, "World")

        Log.v("#E2E TEST", "Create and start key backup for bob ...")
        val keysBackupService = aliceSession.cryptoService().keysBackupService()
        val keyBackupPassword = "FooBarBaz"
        val megolmBackupCreationInfo = keysBackupService.prepareKeysBackupVersion(keyBackupPassword, null)
        val version = keysBackupService.createKeysBackupVersion(megolmBackupCreationInfo)

        Log.v("#E2E TEST", "... Backup created.")

        commonTestHelper.retryPeriodically {
            Log.v("#E2E TEST", "Backup status ${keysBackupService.getTotalNumbersOfBackedUpKeys()}/${keysBackupService.getTotalNumbersOfKeys()}")
            keysBackupService.getTotalNumbersOfKeys() == keysBackupService.getTotalNumbersOfBackedUpKeys()
        }

        val aliceId = aliceSession.myUserId
        // signout

        Log.v("#E2E TEST", "Sign out alice")
        commonTestHelper.signOutAndClose(aliceSession)

        Log.v("#E2E TEST", "Sign in a new alice device")
        val newAliceSession = commonTestHelper.logIntoAccount(aliceId, SessionTestParams(true))

        newAliceSession.cryptoService().enableShareKeyOnInvite(true)

        newAliceSession.cryptoService().keysBackupService().let { kbs ->
            val keyVersionResult = kbs.getVersion(version.version)

            Log.v("#E2E TEST", "Restore new backup")
            val importedResult =  kbs.restoreKeyBackupWithPassword(
                        keyVersionResult!!,
                        keyBackupPassword,
                        null,
                        null,
                        null,
                )

            assertEquals(2, importedResult.totalNumberOfKeys)
        }

        // Now let's invite sam
        // Invite a new user

        Log.v("#E2E TEST", "Create Sam account")
        val samSession = commonTestHelper.createAccount(TestConstants.USER_SAM, SessionTestParams(withInitialSync = true))

        // Let alice invite sam
        Log.v("#E2E TEST", "Let alice invite sam")
        newAliceSession.getRoom(roomId)!!.membershipService().invite(samSession.myUserId)

        commonTestHelper.waitForAndAcceptInviteInRoom(samSession, roomId)

        // Sam shouldn't be able to decrypt messages with the first session, but should decrypt the one with 3rd session
        cryptoTestHelper.ensureCannotDecrypt(
                listOf(notSharableMessage),
                samSession,
                roomId
        )

        cryptoTestHelper.ensureCanDecrypt(
                listOf(sharableMessage),
                samSession,
                roomId,
                listOf(newAliceSession.getRoom(roomId)!!
                        .getTimelineEvent(sharableMessage)
                        ?.root
                        ?.getClearContent()
                        ?.get("body") as String
                )
        )
    }
}
