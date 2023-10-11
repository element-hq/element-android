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

package org.matrix.android.sdk.internal.crypto.keysbackup

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import kotlinx.coroutines.suspendCancellableCoroutine
import org.amshove.kluent.internal.assertFails
import org.amshove.kluent.internal.assertFailsWith
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.FixMethodOrder
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.matrix.android.sdk.InstrumentedTest
import org.matrix.android.sdk.api.crypto.MXCRYPTO_ALGORITHM_MEGOLM_BACKUP
import org.matrix.android.sdk.api.listeners.StepProgressListener
import org.matrix.android.sdk.api.session.crypto.keysbackup.BackupUtils
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysBackupState
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysBackupStateListener
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysBackupVersionTrustSignature
import org.matrix.android.sdk.api.session.crypto.keysbackup.toKeysVersionResult
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.common.CommonTestHelper.Companion.runCryptoTest
import org.matrix.android.sdk.common.CommonTestHelper.Companion.runSessionTest
import org.matrix.android.sdk.common.SessionTestParams
import org.matrix.android.sdk.common.TestConstants
import java.security.InvalidParameterException
import kotlin.coroutines.resume

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.JVM)
@LargeTest
class KeysBackupTest : InstrumentedTest {

    // @get:Rule val rule = RetryTestRule(3)

    /**
     * - From doE2ETestWithAliceAndBobInARoomWithEncryptedMessages, we should have no backed up keys
     * - Check backup keys after having marked one as backed up
     * - Reset keys backup markers
     */
    @Test
    @Ignore("Uses internal APIs")
    fun roomKeysTest_testBackupStore_ok() = runCryptoTest(context()) { _, _ ->

//        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoomWithEncryptedMessages()
//
//        // From doE2ETestWithAliceAndBobInARoomWithEncryptedMessages, we should have no backed up keys
//        val cryptoStore = (cryptoTestData.firstSession.cryptoService().keysBackupService() as DefaultKeysBackupService).store
//        val sessions = cryptoStore.inboundGroupSessionsToBackup(100)
//        val sessionsCount = sessions.size
//
//        assertFalse(sessions.isEmpty())
//        assertEquals(sessionsCount, cryptoTestData.firstSession.cryptoService().inboundGroupSessionsCount(false))
//        assertEquals(0, cryptoTestData.firstSession.cryptoService().inboundGroupSessionsCount(true))
//
//        // - Check backup keys after having marked one as backed up
//        val session = sessions[0]
//
//        cryptoStore.markBackupDoneForInboundGroupSessions(listOf(session))
//
//        assertEquals(sessionsCount, cryptoTestData.firstSession.cryptoService().inboundGroupSessionsCount(false))
//        assertEquals(1, cryptoTestData.firstSession.cryptoService().inboundGroupSessionsCount(true))
//
//        val sessions2 = cryptoStore.inboundGroupSessionsToBackup(100)
//        assertEquals(sessionsCount - 1, sessions2.size)
//
//        // - Reset keys backup markers
//        cryptoStore.resetBackupMarkers()
//
//        val sessions3 = cryptoStore.inboundGroupSessionsToBackup(100)
//        assertEquals(sessionsCount, sessions3.size)
//        assertEquals(sessionsCount, cryptoTestData.firstSession.cryptoService().inboundGroupSessionsCount(false))
//        assertEquals(0, cryptoTestData.firstSession.cryptoService().inboundGroupSessionsCount(true))

//        cryptoTestData.cleanUp(testHelper)
    }

    /**
     * Check that prepareKeysBackupVersionWithPassword returns valid data
     */
    @Test
    fun prepareKeysBackupVersionTest() = runSessionTest(context()) { testHelper ->

        val bobSession = testHelper.createAccount(TestConstants.USER_BOB, KeysBackupTestConstants.defaultSessionParams)

        assertNotNull(bobSession.cryptoService().keysBackupService())

        val keysBackup = bobSession.cryptoService().keysBackupService()

        val stateObserver = StateObserver(keysBackup)

        assertFalse(keysBackup.isEnabled())

        val megolmBackupCreationInfo = keysBackup.prepareKeysBackupVersion(null, null)

        assertEquals(MXCRYPTO_ALGORITHM_MEGOLM_BACKUP, megolmBackupCreationInfo.algorithm)
        assertNotNull(megolmBackupCreationInfo.authData.publicKey)
        assertNotNull(megolmBackupCreationInfo.authData.signatures)
        assertNotNull(megolmBackupCreationInfo.recoveryKey)

        stateObserver.stopAndCheckStates(null)
    }

    /**
     * Test creating a keys backup version and check that createKeysBackupVersion() returns valid data
     */
    @Test
    fun createKeysBackupVersionTest() = runCryptoTest(context()) { cryptoTestHelper, testHelper ->
        val bobSession = testHelper.createAccount(TestConstants.USER_BOB, KeysBackupTestConstants.defaultSessionParams)
        Log.d("#E2E", "Initializing crosssigning for ${bobSession.myUserId.take(8)}")
        cryptoTestHelper.initializeCrossSigning(bobSession)

        val keysBackup = bobSession.cryptoService().keysBackupService()

        val stateObserver = StateObserver(keysBackup)

        assertFalse(keysBackup.isEnabled())

        Log.d("#E2E", "prepareKeysBackupVersion")
        val megolmBackupCreationInfo =
                keysBackup.prepareKeysBackupVersion(null, null)

        assertFalse(keysBackup.isEnabled())

        // Create the version
        Log.d("#E2E", "createKeysBackupVersion")
        val version = keysBackup.createKeysBackupVersion(megolmBackupCreationInfo)

        // Backup must be enable now
        assertTrue(keysBackup.isEnabled())

        // Check that it's signed with MSK
        val versionResult = keysBackup.getVersion(version.version)
        val trust = keysBackup.getKeysBackupTrust(versionResult!!)

        Log.d("#E2E", "Check backup signatures")
        assertEquals("Should have 2 signatures", 2, trust.signatures.size)

        trust.signatures
                .firstOrNull { it is KeysBackupVersionTrustSignature.DeviceSignature }
                .let {
                    assertNotNull("Should be signed by a device", it)
                    it as KeysBackupVersionTrustSignature.DeviceSignature
                }.let {
                    assertEquals("Should be signed by current device", bobSession.sessionParams.deviceId, it.deviceId)
                    assertTrue("Signature should be valid", it.valid)
                }

        trust.signatures
                .firstOrNull { it is KeysBackupVersionTrustSignature.UserSignature }
                .let {
                    assertNotNull("Should be signed by a user", it)
                    it as KeysBackupVersionTrustSignature.UserSignature
                }.let {
                    val msk = bobSession.cryptoService().crossSigningService()
                            .getMyCrossSigningKeys()?.masterKey()?.unpaddedBase64PublicKey
                    assertEquals("Should be signed by my msk 1", msk, it.keyId)
                    assertEquals("Should be signed by my msk 2", msk, it.cryptoCrossSigningKey?.unpaddedBase64PublicKey)
                    assertTrue("Signature should be valid", it.valid)
                }

        stateObserver.stopAndCheckStates(null)
    }

    /**
     * - Check that createKeysBackupVersion() launches the backup
     * - Check the backup completes
     */
    @Test
    fun backupAfterCreateKeysBackupVersionTest() = runCryptoTest(context()) { cryptoTestHelper, testHelper ->
        val keysBackupTestHelper = KeysBackupTestHelper(testHelper, cryptoTestHelper)

        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoomWithEncryptedMessages()

        val keysBackup = cryptoTestData.firstSession.cryptoService().keysBackupService()

        assertEquals(2, cryptoTestData.firstSession.cryptoService().inboundGroupSessionsCount(false))
        assertEquals(0, cryptoTestData.firstSession.cryptoService().inboundGroupSessionsCount(true))

        val stateObserver = BackupStateHelper(keysBackup).hasBackedUpOnce

        keysBackupTestHelper.prepareAndCreateKeysBackupData(keysBackup)
        Log.d("#E2E", "Wait for a backup cycle")
        stateObserver.await()
        Log.d("#E2E", ".. Ok")

        val nbOfKeys = cryptoTestData.firstSession.cryptoService().inboundGroupSessionsCount(false)
        val backedUpKeys = cryptoTestData.firstSession.cryptoService().inboundGroupSessionsCount(true)

        assertEquals(2, nbOfKeys)
        assertEquals("All keys must have been marked as backed up", nbOfKeys, backedUpKeys)

        // Check the several backup state changes
//        stateObserver.stopAndCheckStates(
//                listOf(
//                        KeysBackupState.Enabling,
//                        KeysBackupState.ReadyToBackUp,
//                        KeysBackupState.WillBackUp,
//                        KeysBackupState.BackingUp,
//                        KeysBackupState.ReadyToBackUp
//                )
//        )
    }

    /**
     * Check that backupAllGroupSessions() returns valid data
     */
    @Test
    fun backupAllGroupSessionsTest() = runCryptoTest(context()) { cryptoTestHelper, testHelper ->
        val keysBackupTestHelper = KeysBackupTestHelper(testHelper, cryptoTestHelper)
        Log.d("#E2E", "Setting up Alice Bob with messages")
        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoomWithEncryptedMessages()

        val keysBackup = cryptoTestData.firstSession.cryptoService().keysBackupService()

        val stateObserver = StateObserver(keysBackup)

        Log.d("#E2E", "Creating key backup...")
        keysBackupTestHelper.prepareAndCreateKeysBackupData(keysBackup)
        Log.d("#E2E", "... created")

        // Check that backupAllGroupSessions returns valid data
        val nbOfKeys = cryptoTestData.firstSession.cryptoService().inboundGroupSessionsCount(false)

        assertEquals(2, nbOfKeys)

        testHelper.retryWithBackoff {
            Log.d("#E2E", "Backup ${keysBackup.getTotalNumbersOfBackedUpKeys()}/${keysBackup.getTotalNumbersOfBackedUpKeys()}")
            keysBackup.getTotalNumbersOfKeys() == keysBackup.getTotalNumbersOfBackedUpKeys()
        }

        val backedUpKeys = cryptoTestData.firstSession.cryptoService().inboundGroupSessionsCount(true)

        assertEquals("All keys must have been marked as backed up", nbOfKeys, backedUpKeys)

        stateObserver.stopAndCheckStates(null)
    }

    /**
     * Check encryption and decryption of megolm keys in the backup.
     * - Pick a megolm key
     * - Check [MXKeyBackup encryptGroupSession] returns stg
     * - Check [MXKeyBackup pkDecryptionFromRecoveryKey] is able to create a OLMPkDecryption
     * - Check [MXKeyBackup decryptKeyBackupData] returns stg
     * - Compare the decrypted megolm key with the original one
     */
    @Test
    @Ignore("Uses internal API")
    fun testEncryptAndDecryptKeysBackupData() = runCryptoTest(context()) { _, _ ->
//        val keysBackupTestHelper = KeysBackupTestHelper(testHelper, cryptoTestHelper)
//
//        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoomWithEncryptedMessages()
//
//        val keysBackup = cryptoTestData.firstSession.cryptoService().keysBackupService() as DefaultKeysBackupService
//
//        val stateObserver = StateObserver(keysBackup)
//
//        // - Pick a megolm key
//        val session = keysBackup.store.inboundGroupSessionsToBackup(1)[0]
//
//        val keyBackupCreationInfo = keysBackupTestHelper.prepareAndCreateKeysBackupData(keysBackup).megolmBackupCreationInfo
//
//        // - Check encryptGroupSession() returns stg
//        val keyBackupData = keysBackup.encryptGroupSession(session)
//        assertNotNull(keyBackupData)
//        assertNotNull(keyBackupData!!.sessionData)
//
//        // - Check pkDecryptionFromRecoveryKey() is able to create a OlmPkDecryption
//        val decryption = keysBackup.pkDecryptionFromRecoveryKey(keyBackupCreationInfo.recoveryKey.toBase58())
//        assertNotNull(decryption)
//        // - Check decryptKeyBackupData() returns stg
//        val sessionData = keysBackup
//                .decryptKeyBackupData(
//                        keyBackupData,
//                        session.safeSessionId!!,
//                        cryptoTestData.roomId,
//                        keyBackupCreationInfo.recoveryKey
//                )
//        assertNotNull(sessionData)
//        // - Compare the decrypted megolm key with the original one
//        keysBackupTestHelper.assertKeysEquals(session.exportKeys(), sessionData)
//
//        stateObserver.stopAndCheckStates(null)
    }

    /**
     * - Do an e2e backup to the homeserver with a recovery key
     * - Log Alice on a new device
     * - Restore the e2e backup from the homeserver with the recovery key
     * - Restore must be successful
     */
    @Test
    fun restoreKeysBackupTest() = runCryptoTest(context()) { cryptoTestHelper, testHelper ->
        val keysBackupTestHelper = KeysBackupTestHelper(testHelper, cryptoTestHelper)

        val testData = keysBackupTestHelper.createKeysBackupScenarioWithPassword(null)

        // - Restore the e2e backup from the homeserver
        val importRoomKeysResult = testData.aliceSession2.cryptoService().keysBackupService().restoreKeysWithRecoveryKey(
                testData.aliceSession2.cryptoService().keysBackupService().keysBackupVersion!!,
                testData.prepareKeysBackupDataResult.megolmBackupCreationInfo.recoveryKey,
                null,
                null,
                null
        )

        Log.d("#E2E", "importRoomKeysResult is $importRoomKeysResult")

        keysBackupTestHelper.checkRestoreSuccess(testData, importRoomKeysResult.totalNumberOfKeys, importRoomKeysResult.successfullyNumberOfImportedKeys)

        testData.cleanUp(testHelper)
    }

    /**
     *
     * This is the same as `testRestoreKeyBackup` but this test checks that pending key
     * share requests are cancelled.
     *
     * - Do an e2e backup to the homeserver with a recovery key
     * - Log Alice on a new device
     * - *** Check the SDK sent key share requests
     * - Restore the e2e backup from the homeserver with the recovery key
     * - Restore must be successful
     * - *** There must be no more pending key share requests
     */
//    @Test
//    fun restoreKeysBackupAndKeyShareRequestTest() {
//        fail("Check with Valere for this test. I think we do not send key share request")
//
//        val testData = mKeysBackupTestHelper.createKeysBackupScenarioWithPassword(null)
//
//        // - Check the SDK sent key share requests
//        val cryptoStore2 = (testData.aliceSession2.cryptoService().keysBackupService() as DefaultKeysBackupService).store
//        val unsentRequest = cryptoStore2
//                .getOutgoingRoomKeyRequestByState(setOf(ShareRequestState.UNSENT))
//        val sentRequest = cryptoStore2
//                .getOutgoingRoomKeyRequestByState(setOf(ShareRequestState.SENT))
//
//        // Request is either sent or unsent
//        assertTrue(unsentRequest != null || sentRequest != null)
//
//        // - Restore the e2e backup from the homeserver
//        val importRoomKeysResult = mTestHelper.doSyncSuspending<> {  }<ImportRoomKeysResult> {
//            testData.aliceSession2.cryptoService().keysBackupService().restoreKeysWithRecoveryKey(testData.aliceSession2.cryptoService().keysBackupService().keysBackupVersion!!,
//                    testData.prepareKeysBackupDataResult.megolmBackupCreationInfo.recoveryKey,
//                    null,
//                    null,
//                    null,
//                    it
//            )
//        }
//
//        mKeysBackupTestHelper.checkRestoreSuccess(testData, importRoomKeysResult.totalNumberOfKeys, importRoomKeysResult.successfullyNumberOfImportedKeys)
//
//        // - There must be no more pending key share requests
//        val unsentRequestAfterRestoration = cryptoStore2
//                .getOutgoingRoomKeyRequestByState(setOf(ShareRequestState.UNSENT))
//        val sentRequestAfterRestoration = cryptoStore2
//                .getOutgoingRoomKeyRequestByState(setOf(ShareRequestState.SENT))
//
//        // Request is either sent or unsent
//        assertTrue(unsentRequestAfterRestoration == null && sentRequestAfterRestoration == null)
//
//        testData.cleanUp(testHelper)
//    }

    /**
     * - Do an e2e backup to the homeserver with a recovery key
     * - And log Alice on a new device
     * - The new device must see the previous backup as not trusted
     * - Trust the backup from the new device
     * - Backup must be enabled on the new device
     * - Retrieve the last version from the server
     * - It must be the same
     * - It must be trusted and must have with 2 signatures now
     */
    @Test
    fun trustKeyBackupVersionTest() = runCryptoTest(context()) { cryptoTestHelper, testHelper ->
        val keysBackupTestHelper = KeysBackupTestHelper(testHelper, cryptoTestHelper)

        // - Do an e2e backup to the homeserver with a recovery key
        // - And log Alice on a new device
        val testData = keysBackupTestHelper.createKeysBackupScenarioWithPassword(null)

        val stateObserver = StateObserver(testData.aliceSession2.cryptoService().keysBackupService())

        // - The new device must see the previous backup as not trusted
        assertNotNull(testData.aliceSession2.cryptoService().keysBackupService().keysBackupVersion)
        assertFalse(testData.aliceSession2.cryptoService().keysBackupService().isEnabled())
        assertEquals(KeysBackupState.NotTrusted, testData.aliceSession2.cryptoService().keysBackupService().getState())

        // - Trust the backup from the new device
        testData.aliceSession2.cryptoService().keysBackupService().trustKeysBackupVersion(
                testData.aliceSession2.cryptoService().keysBackupService().keysBackupVersion!!,
                true
        )

        // Wait for backup state to be ReadyToBackUp
        keysBackupTestHelper.waitForKeysBackupToBeInState(testData.aliceSession2, KeysBackupState.ReadyToBackUp)

        // - Backup must be enabled on the new device, on the same version
        assertEquals(testData.prepareKeysBackupDataResult.version, testData.aliceSession2.cryptoService().keysBackupService().keysBackupVersion?.version)
        assertTrue(testData.aliceSession2.cryptoService().keysBackupService().isEnabled())

        // - Retrieve the last version from the server
        val keysVersionResult = testData.aliceSession2.cryptoService()
                .keysBackupService()
                .getCurrentVersion()!!
                .toKeysVersionResult()

        // - It must be the same
        assertEquals(testData.prepareKeysBackupDataResult.version, keysVersionResult!!.version)

        val keysBackupVersionTrust = testData.aliceSession2.cryptoService()
                .keysBackupService()
                .getKeysBackupTrust(keysVersionResult)

        // The backup should have a valid signature from that device now
        assertTrue(keysBackupVersionTrust.usable)
        val signature = keysBackupVersionTrust.signatures
                .filterIsInstance<KeysBackupVersionTrustSignature.DeviceSignature>()
                .firstOrNull { it.deviceId == testData.aliceSession2.cryptoService().getMyCryptoDevice().deviceId }
        assertNotNull(signature)
        assertTrue(signature!!.valid)

        stateObserver.stopAndCheckStates(null)
    }

    /**
     * - Do an e2e backup to the homeserver with a recovery key
     * - And log Alice on a new device
     * - The new device must see the previous backup as not trusted
     * - Trust the backup from the new device with the recovery key
     * - Backup must be enabled on the new device
     * - Retrieve the last version from the server
     * - It must be the same
     * - It must be trusted and must have with 2 signatures now
     */
    @Test
    fun trustKeyBackupVersionWithRecoveryKeyTest() = runCryptoTest(context()) { cryptoTestHelper, testHelper ->
        val keysBackupTestHelper = KeysBackupTestHelper(testHelper, cryptoTestHelper)

        // - Do an e2e backup to the homeserver with a recovery key
        // - And log Alice on a new device
        val testData = keysBackupTestHelper.createKeysBackupScenarioWithPassword(null)

        val stateObserver = StateObserver(testData.aliceSession2.cryptoService().keysBackupService())

        // - The new device must see the previous backup as not trusted
        assertNotNull(testData.aliceSession2.cryptoService().keysBackupService().keysBackupVersion)
        assertFalse(testData.aliceSession2.cryptoService().keysBackupService().isEnabled())
        assertEquals(KeysBackupState.NotTrusted, testData.aliceSession2.cryptoService().keysBackupService().getState())

        // - Trust the backup from the new device with the recovery key
        testData.aliceSession2.cryptoService().keysBackupService().trustKeysBackupVersionWithRecoveryKey(
                testData.aliceSession2.cryptoService().keysBackupService().keysBackupVersion!!,
                testData.prepareKeysBackupDataResult.megolmBackupCreationInfo.recoveryKey
        )

        // Wait for backup state to be ReadyToBackUp
        keysBackupTestHelper.waitForKeysBackupToBeInState(testData.aliceSession2, KeysBackupState.ReadyToBackUp)

        // - Backup must be enabled on the new device, on the same version
        assertEquals(
                testData.prepareKeysBackupDataResult.version,
                testData.aliceSession2.cryptoService().keysBackupService().keysBackupVersion?.version
        )
        assertTrue(testData.aliceSession2.cryptoService().keysBackupService().isEnabled())

        // - Retrieve the last version from the server
        val keysVersionResult = testData.aliceSession2.cryptoService().keysBackupService()
                .getCurrentVersion()!!
                .toKeysVersionResult()

        // - It must be the same
        assertEquals(testData.prepareKeysBackupDataResult.version, keysVersionResult!!.version)

        val keysBackupVersionTrust = testData.aliceSession2.cryptoService()
                .keysBackupService()
                .getKeysBackupTrust(keysVersionResult)

//        // - It must be trusted and must have 2 signatures now
//        assertTrue(keysBackupVersionTrust.usable)
//        assertEquals(2, keysBackupVersionTrust.signatures.size)
        // The backup should have a valid signature from that device now
        assertTrue(keysBackupVersionTrust.usable)
        val signature = keysBackupVersionTrust.signatures
                .filterIsInstance<KeysBackupVersionTrustSignature.DeviceSignature>()
                .firstOrNull { it.deviceId == testData.aliceSession2.cryptoService().getMyCryptoDevice().deviceId }
        assertNotNull(signature)
        assertTrue(signature!!.valid)

        stateObserver.stopAndCheckStates(null)
    }

    /**
     * - Do an e2e backup to the homeserver with a recovery key
     * - And log Alice on a new device
     * - The new device must see the previous backup as not trusted
     * - Try to trust the backup from the new device with a wrong recovery key
     * - It must fail
     * - The backup must still be untrusted and disabled
     */
    @Test
    fun trustKeyBackupVersionWithWrongRecoveryKeyTest() = runCryptoTest(context()) { cryptoTestHelper, testHelper ->
        val keysBackupTestHelper = KeysBackupTestHelper(testHelper, cryptoTestHelper)

        // - Do an e2e backup to the homeserver with a recovery key
        // - And log Alice on a new device
        val testData = keysBackupTestHelper.createKeysBackupScenarioWithPassword(null)

        val stateObserver = StateObserver(testData.aliceSession2.cryptoService().keysBackupService())

        // - The new device must see the previous backup as not trusted
        assertNotNull(testData.aliceSession2.cryptoService().keysBackupService().keysBackupVersion)
        assertFalse(testData.aliceSession2.cryptoService().keysBackupService().isEnabled())
        assertEquals(KeysBackupState.NotTrusted, testData.aliceSession2.cryptoService().keysBackupService().getState())

        // - Try to trust the backup from the new device with a wrong recovery key
        assertFails {
            testData.aliceSession2.cryptoService().keysBackupService().trustKeysBackupVersionWithRecoveryKey(
                    testData.aliceSession2.cryptoService().keysBackupService().keysBackupVersion!!,
                    BackupUtils.recoveryKeyFromPassphrase("Bad recovery key"),
            )
        }

        // - The new device must still see the previous backup as not trusted
        assertNotNull(testData.aliceSession2.cryptoService().keysBackupService().keysBackupVersion)
        assertFalse(testData.aliceSession2.cryptoService().keysBackupService().isEnabled())
        assertEquals(KeysBackupState.NotTrusted, testData.aliceSession2.cryptoService().keysBackupService().getState())

        stateObserver.stopAndCheckStates(null)
    }

    /**
     * - Do an e2e backup to the homeserver with a password
     * - And log Alice on a new device
     * - The new device must see the previous backup as not trusted
     * - Trust the backup from the new device with the password
     * - Backup must be enabled on the new device
     * - Retrieve the last version from the server
     * - It must be the same
     * - It must be trusted and must have with 2 signatures now
     */
    @Test
    fun trustKeyBackupVersionWithPasswordTest() = runCryptoTest(context()) { cryptoTestHelper, testHelper ->
        val keysBackupTestHelper = KeysBackupTestHelper(testHelper, cryptoTestHelper)

        val password = "Password"

        // - Do an e2e backup to the homeserver with a password
        // - And log Alice on a new device
        val testData = keysBackupTestHelper.createKeysBackupScenarioWithPassword(password)

        val stateObserver = StateObserver(testData.aliceSession2.cryptoService().keysBackupService())

        // - The new device must see the previous backup as not trusted
        assertNotNull(testData.aliceSession2.cryptoService().keysBackupService().keysBackupVersion)
        assertFalse(testData.aliceSession2.cryptoService().keysBackupService().isEnabled())
        assertEquals(KeysBackupState.NotTrusted, testData.aliceSession2.cryptoService().keysBackupService().getState())

        // - Trust the backup from the new device with the password
        testData.aliceSession2.cryptoService().keysBackupService().trustKeysBackupVersionWithPassphrase(
                testData.aliceSession2.cryptoService().keysBackupService().keysBackupVersion!!,
                password
        )

        // Wait for backup state to be ReadyToBackUp
        keysBackupTestHelper.waitForKeysBackupToBeInState(testData.aliceSession2, KeysBackupState.ReadyToBackUp)

        // - Backup must be enabled on the new device, on the same version
        assertEquals(testData.prepareKeysBackupDataResult.version, testData.aliceSession2.cryptoService().keysBackupService().keysBackupVersion?.version)
        assertTrue(testData.aliceSession2.cryptoService().keysBackupService().isEnabled())

        // - Retrieve the last version from the server
        val keysVersionResult = testData.aliceSession2.cryptoService().keysBackupService()
                .getCurrentVersion()!!
                .toKeysVersionResult()

        // - It must be the same
        assertEquals(testData.prepareKeysBackupDataResult.version, keysVersionResult!!.version)

        val keysBackupVersionTrust = testData.aliceSession2.cryptoService()
                .keysBackupService()
                .getKeysBackupTrust(keysVersionResult)

//        // - It must be trusted and must have 2 signatures now
//        assertTrue(keysBackupVersionTrust.usable)
//        assertEquals(2, keysBackupVersionTrust.signatures.size)

        // - It must be trusted and signed by current device
        assertTrue(keysBackupVersionTrust.usable)
        val signature = keysBackupVersionTrust.signatures
                .filterIsInstance<KeysBackupVersionTrustSignature.DeviceSignature>()
                .firstOrNull { it.deviceId == testData.aliceSession2.cryptoService().getMyCryptoDevice().deviceId }
        assertNotNull(signature)
        assertTrue(signature!!.valid)

        stateObserver.stopAndCheckStates(null)
    }

    /**
     * - Do an e2e backup to the homeserver with a password
     * - And log Alice on a new device
     * - The new device must see the previous backup as not trusted
     * - Try to trust the backup from the new device with a wrong password
     * - It must fail
     * - The backup must still be untrusted and disabled
     */
    @Test
    fun trustKeyBackupVersionWithWrongPasswordTest() = runCryptoTest(context()) { cryptoTestHelper, testHelper ->
        val keysBackupTestHelper = KeysBackupTestHelper(testHelper, cryptoTestHelper)

        val password = "Password"
        val badPassword = "Bad Password"

        // - Do an e2e backup to the homeserver with a password
        // - And log Alice on a new device
        val testData = keysBackupTestHelper.createKeysBackupScenarioWithPassword(password)

        val stateObserver = StateObserver(testData.aliceSession2.cryptoService().keysBackupService())

        // - The new device must see the previous backup as not trusted
        assertNotNull(testData.aliceSession2.cryptoService().keysBackupService().keysBackupVersion)
        assertFalse(testData.aliceSession2.cryptoService().keysBackupService().isEnabled())
        assertEquals(KeysBackupState.NotTrusted, testData.aliceSession2.cryptoService().keysBackupService().getState())

        // - Try to trust the backup from the new device with a wrong password
        assertFails {
            testData.aliceSession2.cryptoService().keysBackupService().trustKeysBackupVersionWithPassphrase(
                    testData.aliceSession2.cryptoService().keysBackupService().keysBackupVersion!!,
                    badPassword,
            )
        }

        // - The new device must still see the previous backup as not trusted
        assertNotNull(testData.aliceSession2.cryptoService().keysBackupService().keysBackupVersion)
        assertFalse(testData.aliceSession2.cryptoService().keysBackupService().isEnabled())
        assertEquals(KeysBackupState.NotTrusted, testData.aliceSession2.cryptoService().keysBackupService().getState())

        stateObserver.stopAndCheckStates(null)
    }

    /**
     * - Do an e2e backup to the homeserver with a recovery key
     * - Log Alice on a new device
     * - Try to restore the e2e backup with a wrong recovery key
     * - It must fail
     */
    @Test
    fun restoreKeysBackupWithAWrongRecoveryKeyTest() = runCryptoTest(context()) { cryptoTestHelper, testHelper ->
        val keysBackupTestHelper = KeysBackupTestHelper(testHelper, cryptoTestHelper)

        val testData = keysBackupTestHelper.createKeysBackupScenarioWithPassword(null)
        val keysBackupService = testData.aliceSession2.cryptoService().keysBackupService()

        // - Try to restore the e2e backup with a wrong recovery key
        assertFailsWith<InvalidParameterException> {
            keysBackupService.restoreKeysWithRecoveryKey(
                    keysBackupService.keysBackupVersion!!,
                    BackupUtils.recoveryKeyFromBase58("EsTc LW2K PGiF wKEA 3As5 g5c4 BXwk qeeJ ZJV8 Q9fu gUMN UE4d"),
                    null,
                    null,
                    null,
            )
        }
    }

    /**
     * - Do an e2e backup to the homeserver with a password
     * - Log Alice on a new device
     * - Restore the e2e backup with the password
     * - Restore must be successful
     */
    @Test
    fun testBackupWithPassword() = runCryptoTest(context()) { cryptoTestHelper, testHelper ->

        val keysBackupTestHelper = KeysBackupTestHelper(testHelper, cryptoTestHelper)

        val password = "password"

        val testData = keysBackupTestHelper.createKeysBackupScenarioWithPassword(password)
        Assume.assumeTrue(
                "Can't report progress same way in rust",
                testData.cryptoTestData.firstSession.cryptoService().name() != "rust-sdk"
        )

        // - Restore the e2e backup with the password
        val steps = ArrayList<StepProgressListener.Step>()

        val importRoomKeysResult = testData.aliceSession2.cryptoService().keysBackupService().restoreKeyBackupWithPassword(
                testData.aliceSession2.cryptoService().keysBackupService().keysBackupVersion!!,
                password,
                null,
                null,
                object : StepProgressListener {
                    override fun onStepProgress(step: StepProgressListener.Step) {
                        steps.add(step)
                    }
                }
        )

        // Check steps
        assertEquals(105, steps.size)

        for (i in 0..100) {
            assertTrue(steps[i] is StepProgressListener.Step.ComputingKey)
            assertEquals(i, (steps[i] as StepProgressListener.Step.ComputingKey).progress)
            assertEquals(100, (steps[i] as StepProgressListener.Step.ComputingKey).total)
        }

        assertTrue(steps[101] is StepProgressListener.Step.DownloadingKey)

        // 2 Keys to import, value will be 0%, 50%, 100%
        for (i in 102..104) {
            assertTrue(steps[i] is StepProgressListener.Step.ImportingKey)
            assertEquals(100, (steps[i] as StepProgressListener.Step.ImportingKey).total)
        }

        assertEquals(0, (steps[102] as StepProgressListener.Step.ImportingKey).progress)
        assertEquals(50, (steps[103] as StepProgressListener.Step.ImportingKey).progress)
        assertEquals(100, (steps[104] as StepProgressListener.Step.ImportingKey).progress)

        keysBackupTestHelper.checkRestoreSuccess(testData, importRoomKeysResult.totalNumberOfKeys, importRoomKeysResult.successfullyNumberOfImportedKeys)
    }

    /**
     * - Do an e2e backup to the homeserver with a password
     * - Log Alice on a new device
     * - Try to restore the e2e backup with a wrong password
     * - It must fail
     */
    @Test
    fun restoreKeysBackupWithAWrongPasswordTest() = runCryptoTest(context()) { cryptoTestHelper, testHelper ->
        val keysBackupTestHelper = KeysBackupTestHelper(testHelper, cryptoTestHelper)

        val password = "password"
        val wrongPassword = "passw0rd"

        val testData = keysBackupTestHelper.createKeysBackupScenarioWithPassword(password)
        val keysBackupService = testData.aliceSession2.cryptoService().keysBackupService()

        // - Try to restore the e2e backup with a wrong password
        assertFailsWith<InvalidParameterException> {
            keysBackupService.restoreKeyBackupWithPassword(
                    keysBackupService.keysBackupVersion!!,
                    wrongPassword,
                    null,
                    null,
                    null,
            )
        }
    }

    /**
     * - Do an e2e backup to the homeserver with a password
     * - Log Alice on a new device
     * - Restore the e2e backup with the recovery key.
     * - Restore must be successful
     */
    @Test
    fun testUseRecoveryKeyToRestoreAPasswordBasedKeysBackup() = runCryptoTest(context()) { cryptoTestHelper, testHelper ->
        val keysBackupTestHelper = KeysBackupTestHelper(testHelper, cryptoTestHelper)

        val password = "password"

        val testData = keysBackupTestHelper.createKeysBackupScenarioWithPassword(password)

        // - Restore the e2e backup with the recovery key.
        val importRoomKeysResult = testData.aliceSession2.cryptoService().keysBackupService().restoreKeysWithRecoveryKey(
                testData.aliceSession2.cryptoService().keysBackupService().keysBackupVersion!!,
                testData.prepareKeysBackupDataResult.megolmBackupCreationInfo.recoveryKey,
                null,
                null,
                null
        )

        keysBackupTestHelper.checkRestoreSuccess(testData, importRoomKeysResult.totalNumberOfKeys, importRoomKeysResult.successfullyNumberOfImportedKeys)
    }

    /**
     * - Do an e2e backup to the homeserver with a recovery key
     * - And log Alice on a new device
     * - Try to restore the e2e backup with a password
     * - It must fail
     */
    @Test
    fun testUsePasswordToRestoreARecoveryKeyBasedKeysBackup() = runCryptoTest(context()) { cryptoTestHelper, testHelper ->
        val keysBackupTestHelper = KeysBackupTestHelper(testHelper, cryptoTestHelper)

        val testData = keysBackupTestHelper.createKeysBackupScenarioWithPassword("password")
        val keysBackupService = testData.aliceSession2.cryptoService().keysBackupService()

        // - Try to restore the e2e backup with a password
        val importRoomKeysResult = keysBackupService.restoreKeyBackupWithPassword(
                keysBackupService.keysBackupVersion!!,
                "password",
                null,
                null,
                null,
        )

        assertTrue(importRoomKeysResult.importedSessionInfo.isNotEmpty())
    }

    /**
     * - Create a backup version
     * - Check the returned KeysVersionResult is trusted
     */
    @Test
    fun testIsKeysBackupTrusted() = runCryptoTest(context()) { cryptoTestHelper, testHelper ->
        val keysBackupTestHelper = KeysBackupTestHelper(testHelper, cryptoTestHelper)

        // - Create a backup version
        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoomWithEncryptedMessages()

        val keysBackup = cryptoTestData.firstSession.cryptoService().keysBackupService()

        val stateObserver = StateObserver(keysBackup)

        // - Do an e2e backup to the homeserver
        keysBackupTestHelper.prepareAndCreateKeysBackupData(keysBackup)

        // Get key backup version from the homeserver
        val keysVersionResult = keysBackup.getCurrentVersion()!!.toKeysVersionResult()

        // - Check the returned KeyBackupVersion is trusted
        val keysBackupVersionTrust = keysBackup.getKeysBackupTrust(keysVersionResult!!)

        assertNotNull(keysBackupVersionTrust)
        assertTrue(keysBackupVersionTrust.usable)
        assertEquals(1, keysBackupVersionTrust.signatures.size)

        val signature = keysBackupVersionTrust.signatures[0] as KeysBackupVersionTrustSignature.DeviceSignature
        assertTrue(signature.valid)
        assertNotNull(signature.device)
        assertEquals(cryptoTestData.firstSession.cryptoService().getMyCryptoDevice().deviceId, signature.deviceId)
        assertEquals(signature.device!!.deviceId, cryptoTestData.firstSession.sessionParams.deviceId)

        stateObserver.stopAndCheckStates(null)
    }

    /**
     * Check WrongBackUpVersion state
     *
     * - Make alice back up her keys to her homeserver
     * - Create a new backup with fake data on the homeserver
     * - Make alice back up all her keys again
     * -> That must fail and her backup state must be WrongBackUpVersion or Not trusted?
     */
    @Test
    fun testBackupWhenAnotherBackupWasCreated() = runCryptoTest(context()) { cryptoTestHelper, testHelper ->
        val keysBackupTestHelper = KeysBackupTestHelper(testHelper, cryptoTestHelper)

        // - Create a backup version
        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoomWithEncryptedMessages()

        val keysBackup = cryptoTestData.firstSession.cryptoService().keysBackupService()

        assertFalse(keysBackup.isEnabled())

        val backupWaitHelper = BackupStateHelper(keysBackup)
        keysBackupTestHelper.prepareAndCreateKeysBackupData(keysBackup)
        assertTrue(keysBackup.isEnabled())

        backupWaitHelper.hasBackedUpOnce.await()

        val newSession = testHelper.logIntoAccount(cryptoTestData.firstSession.myUserId, SessionTestParams(true))
        keysBackupTestHelper.prepareAndCreateKeysBackupData(newSession.cryptoService().keysBackupService())

        // Make a new key for alice to backup
        cryptoTestData.firstSession.cryptoService().discardOutboundSession(cryptoTestData.roomId)
        testHelper.sendMessageInRoom(cryptoTestData.firstSession.getRoom(cryptoTestData.roomId)!!, "new")

        // - Alice first session should not be able to backup
        testHelper.retryPeriodically {
            Log.d("#E2E", "backup state is ${keysBackup.getState()}")
            KeysBackupState.NotTrusted == keysBackup.getState()
        }

        assertFalse(keysBackup.isEnabled())
    }

    /**
     * - Do an e2e backup to the homeserver
     * - Log Alice on a new device
     * - Post a message to have a new megolm session
     * - Try to backup all
     * -> It must fail. Backup state must be NotTrusted
     * - Validate the old device from the new one
     * -> Backup should automatically enable on the new device
     * -> It must use the same backup version
     * - Try to backup all again
     * -> It must success
     */
    @Test
    @Ignore("Instable on both flavors")
    fun testBackupAfterVerifyingADevice() = runCryptoTest(context()) { cryptoTestHelper, testHelper ->
        val keysBackupTestHelper = KeysBackupTestHelper(testHelper, cryptoTestHelper)

        // - Create a backup version
        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoomWithEncryptedMessages()
        cryptoTestHelper.initializeCrossSigning(cryptoTestData.firstSession)

        val keysBackup = cryptoTestData.firstSession.cryptoService().keysBackupService()

        // - Make alice back up her keys to her homeserver
        keysBackupTestHelper.prepareAndCreateKeysBackupData(keysBackup)

        // Wait for keys backup to finish by asking again to backup keys.
        testHelper.retryWithBackoff {
            keysBackup.getTotalNumbersOfKeys() == keysBackup.getTotalNumbersOfBackedUpKeys()
        }
        testHelper.retryWithBackoff {
            keysBackup.getState() == KeysBackupState.ReadyToBackUp
        }

        val oldKeyBackupVersion = keysBackup.currentBackupVersion
        val aliceUserId = cryptoTestData.firstSession.myUserId

        // - Log Alice on a new device
        Log.d("#E2E", "Log Alice on a new device")
        val aliceSession2 = testHelper.logIntoAccount(aliceUserId, KeysBackupTestConstants.defaultSessionParamsWithInitialSync)

        // - Post a message to have a new megolm session
        Log.d("#E2E", "Post a message to have a new megolm session")
        aliceSession2.cryptoService().setWarnOnUnknownDevices(false)
        val room2 = aliceSession2.getRoom(cryptoTestData.roomId)!!

        testHelper.sendMessageInRoom(room2, "New key")

        // - Try to backup all in aliceSession2, it must fail
        val keysBackup2 = aliceSession2.cryptoService().keysBackupService()

        assertFalse("Backup should not be enabled", keysBackup2.isEnabled())

        // Backup state must be NotTrusted
        assertEquals("Backup state must be NotTrusted", KeysBackupState.NotTrusted, keysBackup2.getState())
        assertFalse("Backup should not be enabled", keysBackup2.isEnabled())

        val signatures = keysBackup2.getCurrentVersion()?.toKeysVersionResult()?.getAuthDataAsMegolmBackupAuthData()?.signatures
        Log.d("#E2E", "keysBackup2 signatures: $signatures")

        // - Validate the old device from the new one
        cryptoTestHelper.verifyNewSession(cryptoTestData.firstSession, aliceSession2)

        cryptoTestData.firstSession.cryptoService().keysBackupService().checkAndStartKeysBackup()
        // -> Backup should automatically enable on the new device
        suspendCancellableCoroutine<Unit> { continuation ->
            val listener = object : KeysBackupStateListener {
                override fun onStateChange(newState: KeysBackupState) {
                    Log.d("#E2E", "keysBackup2 onStateChange: $newState")
                    // Check the backup completes
                    if (keysBackup2.getState() == KeysBackupState.ReadyToBackUp) {
                        // Remove itself from the list of listeners
                        keysBackup2.removeListener(this)
                        continuation.resume(Unit)
                    }
                }
            }
            keysBackup2.addListener(listener)
            continuation.invokeOnCancellation { keysBackup2.removeListener(listener) }
        }

        // -> It must use the same backup version
        assertEquals(oldKeyBackupVersion, aliceSession2.cryptoService().keysBackupService().currentBackupVersion)

        // aliceSession2.cryptoService().keysBackupService().backupAllGroupSessions(null, it)
        testHelper.retryWithBackoff {
            keysBackup2.getTotalNumbersOfKeys() == keysBackup2.getTotalNumbersOfBackedUpKeys()
        }

        testHelper.retryWithBackoff {
            aliceSession2.cryptoService().keysBackupService().getState() == KeysBackupState.ReadyToBackUp
        }

        // -> It must success
        assertTrue(aliceSession2.cryptoService().keysBackupService().isEnabled())
    }

    /**
     * - Do an e2e backup to the homeserver with a recovery key
     * - Delete the backup
     */
    @Test
    fun deleteKeysBackupTest() = runCryptoTest(context()) { cryptoTestHelper, testHelper ->
        val keysBackupTestHelper = KeysBackupTestHelper(testHelper, cryptoTestHelper)

        // - Create a backup version
        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoomWithEncryptedMessages()

        val keysBackup = cryptoTestData.firstSession.cryptoService().keysBackupService()

        val stateObserver = StateObserver(keysBackup)

        assertFalse(keysBackup.isEnabled())

        val keyBackupCreationInfo = keysBackupTestHelper.prepareAndCreateKeysBackupData(keysBackup)

        assertTrue(keysBackup.isEnabled())

        // Delete the backup
        keysBackup.deleteBackup(keyBackupCreationInfo.version)

        // Backup is now disabled
        assertFalse(keysBackup.isEnabled())

        stateObserver.stopAndCheckStates(null)
    }
}
