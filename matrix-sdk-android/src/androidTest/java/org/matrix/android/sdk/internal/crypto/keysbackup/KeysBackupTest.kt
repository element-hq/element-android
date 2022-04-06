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

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.FixMethodOrder
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.matrix.android.sdk.InstrumentedTest
import org.matrix.android.sdk.api.listeners.ProgressListener
import org.matrix.android.sdk.api.listeners.StepProgressListener
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysBackupState
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysBackupStateListener
import org.matrix.android.sdk.common.CommonTestHelper
import org.matrix.android.sdk.common.CryptoTestHelper
import org.matrix.android.sdk.common.TestConstants
import org.matrix.android.sdk.common.TestMatrixCallback
import org.matrix.android.sdk.internal.crypto.MXCRYPTO_ALGORITHM_MEGOLM_BACKUP
import org.matrix.android.sdk.internal.crypto.crosssigning.DeviceTrustLevel
import org.matrix.android.sdk.internal.crypto.keysbackup.model.KeysBackupLastVersionResult
import org.matrix.android.sdk.internal.crypto.keysbackup.model.KeysBackupVersionTrust
import org.matrix.android.sdk.internal.crypto.keysbackup.model.MegolmBackupCreationInfo
import org.matrix.android.sdk.internal.crypto.keysbackup.model.rest.KeysVersion
import org.matrix.android.sdk.internal.crypto.keysbackup.model.toKeysVersionResult
import org.matrix.android.sdk.internal.crypto.model.ImportRoomKeysResult
import java.util.Collections
import java.util.concurrent.CountDownLatch

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.JVM)
@LargeTest
class KeysBackupTest : InstrumentedTest {

    private val testHelper = CommonTestHelper(context())
    private val cryptoTestHelper = CryptoTestHelper(testHelper)
    private val keysBackupTestHelper = KeysBackupTestHelper(testHelper, cryptoTestHelper)

    /**
     * - From doE2ETestWithAliceAndBobInARoomWithEncryptedMessages, we should have no backed up keys
     * - Check backup keys after having marked one as backed up
     * - Reset keys backup markers
     */
    @Test
    @Ignore("This test will be ignored until it is fixed")
    fun roomKeysTest_testBackupStore_ok() {
        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoomWithEncryptedMessages()

        // From doE2ETestWithAliceAndBobInARoomWithEncryptedMessages, we should have no backed up keys
        val cryptoStore = (cryptoTestData.firstSession.cryptoService().keysBackupService() as DefaultKeysBackupService).store
        val sessions = cryptoStore.inboundGroupSessionsToBackup(100)
        val sessionsCount = sessions.size

        assertFalse(sessions.isEmpty())
        assertEquals(sessionsCount, cryptoTestData.firstSession.cryptoService().inboundGroupSessionsCount(false))
        assertEquals(0, cryptoTestData.firstSession.cryptoService().inboundGroupSessionsCount(true))

        // - Check backup keys after having marked one as backed up
        val session = sessions[0]

        cryptoStore.markBackupDoneForInboundGroupSessions(Collections.singletonList(session))

        assertEquals(sessionsCount, cryptoTestData.firstSession.cryptoService().inboundGroupSessionsCount(false))
        assertEquals(1, cryptoTestData.firstSession.cryptoService().inboundGroupSessionsCount(true))

        val sessions2 = cryptoStore.inboundGroupSessionsToBackup(100)
        assertEquals(sessionsCount - 1, sessions2.size)

        // - Reset keys backup markers
        cryptoStore.resetBackupMarkers()

        val sessions3 = cryptoStore.inboundGroupSessionsToBackup(100)
        assertEquals(sessionsCount, sessions3.size)
        assertEquals(sessionsCount, cryptoTestData.firstSession.cryptoService().inboundGroupSessionsCount(false))
        assertEquals(0, cryptoTestData.firstSession.cryptoService().inboundGroupSessionsCount(true))

        cryptoTestData.cleanUp(testHelper)
    }

    /**
     * Check that prepareKeysBackupVersionWithPassword returns valid data
     */
    @Test
    fun prepareKeysBackupVersionTest() {
        val bobSession = testHelper.createAccount(TestConstants.USER_BOB, KeysBackupTestConstants.defaultSessionParams)

        assertNotNull(bobSession.cryptoService().keysBackupService())

        val keysBackup = bobSession.cryptoService().keysBackupService()

        val stateObserver = StateObserver(keysBackup)

        assertFalse(keysBackup.isEnabled)

        val megolmBackupCreationInfo = testHelper.doSync<MegolmBackupCreationInfo> {
            keysBackup.prepareKeysBackupVersion(null, null, it)
        }

        assertEquals(MXCRYPTO_ALGORITHM_MEGOLM_BACKUP, megolmBackupCreationInfo.algorithm)
        assertNotNull(megolmBackupCreationInfo.authData.publicKey)
        assertNotNull(megolmBackupCreationInfo.authData.signatures)
        assertNotNull(megolmBackupCreationInfo.recoveryKey)

        stateObserver.stopAndCheckStates(null)
        testHelper.signOutAndClose(bobSession)
    }

    /**
     * Test creating a keys backup version and check that createKeysBackupVersion() returns valid data
     */
    @Test
    fun createKeysBackupVersionTest() {
        val bobSession = testHelper.createAccount(TestConstants.USER_BOB, KeysBackupTestConstants.defaultSessionParams)

        val keysBackup = bobSession.cryptoService().keysBackupService()

        val stateObserver = StateObserver(keysBackup)

        assertFalse(keysBackup.isEnabled)

        val megolmBackupCreationInfo = testHelper.doSync<MegolmBackupCreationInfo> {
            keysBackup.prepareKeysBackupVersion(null, null, it)
        }

        assertFalse(keysBackup.isEnabled)

        // Create the version
        testHelper.doSync<KeysVersion> {
            keysBackup.createKeysBackupVersion(megolmBackupCreationInfo, it)
        }

        // Backup must be enable now
        assertTrue(keysBackup.isEnabled)

        stateObserver.stopAndCheckStates(null)
        testHelper.signOutAndClose(bobSession)
    }

    /**
     * - Check that createKeysBackupVersion() launches the backup
     * - Check the backup completes
     */
    @Test
    @Ignore("This test will be ignored until it is fixed")
    fun backupAfterCreateKeysBackupVersionTest() {
        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoomWithEncryptedMessages()

        keysBackupTestHelper.waitForKeybackUpBatching()
        val keysBackup = cryptoTestData.firstSession.cryptoService().keysBackupService()

        val latch = CountDownLatch(1)

        assertEquals(2, cryptoTestData.firstSession.cryptoService().inboundGroupSessionsCount(false))
        assertEquals(0, cryptoTestData.firstSession.cryptoService().inboundGroupSessionsCount(true))

        val stateObserver = StateObserver(keysBackup, latch, 5)

        keysBackupTestHelper.prepareAndCreateKeysBackupData(keysBackup)

        testHelper.await(latch)

        val nbOfKeys = cryptoTestData.firstSession.cryptoService().inboundGroupSessionsCount(false)
        val backedUpKeys = cryptoTestData.firstSession.cryptoService().inboundGroupSessionsCount(true)

        assertEquals(2, nbOfKeys)
        assertEquals("All keys must have been marked as backed up", nbOfKeys, backedUpKeys)

        // Check the several backup state changes
        stateObserver.stopAndCheckStates(
                listOf(
                        KeysBackupState.Enabling,
                        KeysBackupState.ReadyToBackUp,
                        KeysBackupState.WillBackUp,
                        KeysBackupState.BackingUp,
                        KeysBackupState.ReadyToBackUp
                )
        )
        cryptoTestData.cleanUp(testHelper)
    }

    /**
     * Check that backupAllGroupSessions() returns valid data
     */
    @Test
    @Ignore("This test will be ignored until it is fixed")
    fun backupAllGroupSessionsTest() {
        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoomWithEncryptedMessages()

        val keysBackup = cryptoTestData.firstSession.cryptoService().keysBackupService()

        val stateObserver = StateObserver(keysBackup)

        keysBackupTestHelper.prepareAndCreateKeysBackupData(keysBackup)

        // Check that backupAllGroupSessions returns valid data
        val nbOfKeys = cryptoTestData.firstSession.cryptoService().inboundGroupSessionsCount(false)

        assertEquals(2, nbOfKeys)

        var lastBackedUpKeysProgress = 0

        testHelper.doSync<Unit> {
            keysBackup.backupAllGroupSessions(object : ProgressListener {
                override fun onProgress(progress: Int, total: Int) {
                    assertEquals(nbOfKeys, total)
                    lastBackedUpKeysProgress = progress
                }
            }, it)
        }

        assertEquals(nbOfKeys, lastBackedUpKeysProgress)

        val backedUpKeys = cryptoTestData.firstSession.cryptoService().inboundGroupSessionsCount(true)

        assertEquals("All keys must have been marked as backed up", nbOfKeys, backedUpKeys)

        stateObserver.stopAndCheckStates(null)
        cryptoTestData.cleanUp(testHelper)
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
    @Ignore("This test will be ignored until it is fixed")
    fun testEncryptAndDecryptKeysBackupData() {
        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoomWithEncryptedMessages()

        val keysBackup = cryptoTestData.firstSession.cryptoService().keysBackupService() as DefaultKeysBackupService

        val stateObserver = StateObserver(keysBackup)

        // - Pick a megolm key
        val session = keysBackup.store.inboundGroupSessionsToBackup(1)[0]

        val keyBackupCreationInfo = keysBackupTestHelper.prepareAndCreateKeysBackupData(keysBackup).megolmBackupCreationInfo

        // - Check encryptGroupSession() returns stg
        val keyBackupData = keysBackup.encryptGroupSession(session)
        assertNotNull(keyBackupData)
        assertNotNull(keyBackupData!!.sessionData)

        // - Check pkDecryptionFromRecoveryKey() is able to create a OlmPkDecryption
        val decryption = keysBackup.pkDecryptionFromRecoveryKey(keyBackupCreationInfo.recoveryKey)
        assertNotNull(decryption)
        // - Check decryptKeyBackupData() returns stg
        val sessionData = keysBackup
                .decryptKeyBackupData(keyBackupData,
                        session.olmInboundGroupSession!!.sessionIdentifier(),
                        cryptoTestData.roomId,
                        decryption!!)
        assertNotNull(sessionData)
        // - Compare the decrypted megolm key with the original one
        keysBackupTestHelper.assertKeysEquals(session.exportKeys(), sessionData)

        stateObserver.stopAndCheckStates(null)
        cryptoTestData.cleanUp(testHelper)
    }

    /**
     * - Do an e2e backup to the homeserver with a recovery key
     * - Log Alice on a new device
     * - Restore the e2e backup from the homeserver with the recovery key
     * - Restore must be successful
     */
    @Test
    @Ignore("This test will be ignored until it is fixed")
    fun restoreKeysBackupTest() {
        val testData = keysBackupTestHelper.createKeysBackupScenarioWithPassword(null)

        // - Restore the e2e backup from the homeserver
        val importRoomKeysResult = testHelper.doSync<ImportRoomKeysResult> {
            testData.aliceSession2.cryptoService().keysBackupService().restoreKeysWithRecoveryKey(testData.aliceSession2.cryptoService().keysBackupService().keysBackupVersion!!,
                    testData.prepareKeysBackupDataResult.megolmBackupCreationInfo.recoveryKey,
                    null,
                    null,
                    null,
                    it
            )
        }

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
//        val importRoomKeysResult = mTestHelper.doSync<ImportRoomKeysResult> {
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
//        testData.cleanUp(mTestHelper)
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
    @Ignore("This test will be ignored until it is fixed")
    fun trustKeyBackupVersionTest() {
        // - Do an e2e backup to the homeserver with a recovery key
        // - And log Alice on a new device
        val testData = keysBackupTestHelper.createKeysBackupScenarioWithPassword(null)

        val stateObserver = StateObserver(testData.aliceSession2.cryptoService().keysBackupService())

        // - The new device must see the previous backup as not trusted
        assertNotNull(testData.aliceSession2.cryptoService().keysBackupService().keysBackupVersion)
        assertFalse(testData.aliceSession2.cryptoService().keysBackupService().isEnabled)
        assertEquals(KeysBackupState.NotTrusted, testData.aliceSession2.cryptoService().keysBackupService().state)

        // - Trust the backup from the new device
        testHelper.doSync<Unit> {
            testData.aliceSession2.cryptoService().keysBackupService().trustKeysBackupVersion(
                    testData.aliceSession2.cryptoService().keysBackupService().keysBackupVersion!!,
                    true,
                    it
            )
        }

        // Wait for backup state to be ReadyToBackUp
        keysBackupTestHelper.waitForKeysBackupToBeInState(testData.aliceSession2, KeysBackupState.ReadyToBackUp)

        // - Backup must be enabled on the new device, on the same version
        assertEquals(testData.prepareKeysBackupDataResult.version, testData.aliceSession2.cryptoService().keysBackupService().keysBackupVersion?.version)
        assertTrue(testData.aliceSession2.cryptoService().keysBackupService().isEnabled)

        // - Retrieve the last version from the server
        val keysVersionResult = testHelper.doSync<KeysBackupLastVersionResult> {
            testData.aliceSession2.cryptoService().keysBackupService().getCurrentVersion(it)
        }.toKeysVersionResult()

        // - It must be the same
        assertEquals(testData.prepareKeysBackupDataResult.version, keysVersionResult!!.version)

        val keysBackupVersionTrust = testHelper.doSync<KeysBackupVersionTrust> {
            testData.aliceSession2.cryptoService().keysBackupService().getKeysBackupTrust(keysVersionResult, it)
        }

        // - It must be trusted and must have 2 signatures now
        assertTrue(keysBackupVersionTrust.usable)
        assertEquals(2, keysBackupVersionTrust.signatures.size)

        stateObserver.stopAndCheckStates(null)
        testData.cleanUp(testHelper)
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
    @Ignore("This test will be ignored until it is fixed")
    fun trustKeyBackupVersionWithRecoveryKeyTest() {
        // - Do an e2e backup to the homeserver with a recovery key
        // - And log Alice on a new device
        val testData = keysBackupTestHelper.createKeysBackupScenarioWithPassword(null)

        val stateObserver = StateObserver(testData.aliceSession2.cryptoService().keysBackupService())

        // - The new device must see the previous backup as not trusted
        assertNotNull(testData.aliceSession2.cryptoService().keysBackupService().keysBackupVersion)
        assertFalse(testData.aliceSession2.cryptoService().keysBackupService().isEnabled)
        assertEquals(KeysBackupState.NotTrusted, testData.aliceSession2.cryptoService().keysBackupService().state)

        // - Trust the backup from the new device with the recovery key
        testHelper.doSync<Unit> {
            testData.aliceSession2.cryptoService().keysBackupService().trustKeysBackupVersionWithRecoveryKey(
                    testData.aliceSession2.cryptoService().keysBackupService().keysBackupVersion!!,
                    testData.prepareKeysBackupDataResult.megolmBackupCreationInfo.recoveryKey,
                    it
            )
        }

        // Wait for backup state to be ReadyToBackUp
        keysBackupTestHelper.waitForKeysBackupToBeInState(testData.aliceSession2, KeysBackupState.ReadyToBackUp)

        // - Backup must be enabled on the new device, on the same version
        assertEquals(testData.prepareKeysBackupDataResult.version, testData.aliceSession2.cryptoService().keysBackupService().keysBackupVersion?.version)
        assertTrue(testData.aliceSession2.cryptoService().keysBackupService().isEnabled)

        // - Retrieve the last version from the server
        val keysVersionResult = testHelper.doSync<KeysBackupLastVersionResult> {
            testData.aliceSession2.cryptoService().keysBackupService().getCurrentVersion(it)
        }.toKeysVersionResult()

        // - It must be the same
        assertEquals(testData.prepareKeysBackupDataResult.version, keysVersionResult!!.version)

        val keysBackupVersionTrust = testHelper.doSync<KeysBackupVersionTrust> {
            testData.aliceSession2.cryptoService().keysBackupService().getKeysBackupTrust(keysVersionResult, it)
        }

        // - It must be trusted and must have 2 signatures now
        assertTrue(keysBackupVersionTrust.usable)
        assertEquals(2, keysBackupVersionTrust.signatures.size)

        stateObserver.stopAndCheckStates(null)
        testData.cleanUp(testHelper)
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
    @Ignore("This test will be ignored until it is fixed")
    fun trustKeyBackupVersionWithWrongRecoveryKeyTest() {
        // - Do an e2e backup to the homeserver with a recovery key
        // - And log Alice on a new device
        val testData = keysBackupTestHelper.createKeysBackupScenarioWithPassword(null)

        val stateObserver = StateObserver(testData.aliceSession2.cryptoService().keysBackupService())

        // - The new device must see the previous backup as not trusted
        assertNotNull(testData.aliceSession2.cryptoService().keysBackupService().keysBackupVersion)
        assertFalse(testData.aliceSession2.cryptoService().keysBackupService().isEnabled)
        assertEquals(KeysBackupState.NotTrusted, testData.aliceSession2.cryptoService().keysBackupService().state)

        // - Try to trust the backup from the new device with a wrong recovery key
        val latch = CountDownLatch(1)
        testData.aliceSession2.cryptoService().keysBackupService().trustKeysBackupVersionWithRecoveryKey(
                testData.aliceSession2.cryptoService().keysBackupService().keysBackupVersion!!,
                "Bad recovery key",
                TestMatrixCallback(latch, false)
        )
        testHelper.await(latch)

        // - The new device must still see the previous backup as not trusted
        assertNotNull(testData.aliceSession2.cryptoService().keysBackupService().keysBackupVersion)
        assertFalse(testData.aliceSession2.cryptoService().keysBackupService().isEnabled)
        assertEquals(KeysBackupState.NotTrusted, testData.aliceSession2.cryptoService().keysBackupService().state)

        stateObserver.stopAndCheckStates(null)
        testData.cleanUp(testHelper)
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
    @Ignore("This test will be ignored until it is fixed")
    fun trustKeyBackupVersionWithPasswordTest() {
        val password = "Password"

        // - Do an e2e backup to the homeserver with a password
        // - And log Alice on a new device
        val testData = keysBackupTestHelper.createKeysBackupScenarioWithPassword(password)

        val stateObserver = StateObserver(testData.aliceSession2.cryptoService().keysBackupService())

        // - The new device must see the previous backup as not trusted
        assertNotNull(testData.aliceSession2.cryptoService().keysBackupService().keysBackupVersion)
        assertFalse(testData.aliceSession2.cryptoService().keysBackupService().isEnabled)
        assertEquals(KeysBackupState.NotTrusted, testData.aliceSession2.cryptoService().keysBackupService().state)

        // - Trust the backup from the new device with the password
        testHelper.doSync<Unit> {
            testData.aliceSession2.cryptoService().keysBackupService().trustKeysBackupVersionWithPassphrase(
                    testData.aliceSession2.cryptoService().keysBackupService().keysBackupVersion!!,
                    password,
                    it
            )
        }

        // Wait for backup state to be ReadyToBackUp
        keysBackupTestHelper.waitForKeysBackupToBeInState(testData.aliceSession2, KeysBackupState.ReadyToBackUp)

        // - Backup must be enabled on the new device, on the same version
        assertEquals(testData.prepareKeysBackupDataResult.version, testData.aliceSession2.cryptoService().keysBackupService().keysBackupVersion?.version)
        assertTrue(testData.aliceSession2.cryptoService().keysBackupService().isEnabled)

        // - Retrieve the last version from the server
        val keysVersionResult = testHelper.doSync<KeysBackupLastVersionResult> {
            testData.aliceSession2.cryptoService().keysBackupService().getCurrentVersion(it)
        }.toKeysVersionResult()

        // - It must be the same
        assertEquals(testData.prepareKeysBackupDataResult.version, keysVersionResult!!.version)

        val keysBackupVersionTrust = testHelper.doSync<KeysBackupVersionTrust> {
            testData.aliceSession2.cryptoService().keysBackupService().getKeysBackupTrust(keysVersionResult, it)
        }

        // - It must be trusted and must have 2 signatures now
        assertTrue(keysBackupVersionTrust.usable)
        assertEquals(2, keysBackupVersionTrust.signatures.size)

        stateObserver.stopAndCheckStates(null)
        testData.cleanUp(testHelper)
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
    @Ignore("This test will be ignored until it is fixed")
    fun trustKeyBackupVersionWithWrongPasswordTest() {
        val password = "Password"
        val badPassword = "Bad Password"

        // - Do an e2e backup to the homeserver with a password
        // - And log Alice on a new device
        val testData = keysBackupTestHelper.createKeysBackupScenarioWithPassword(password)

        val stateObserver = StateObserver(testData.aliceSession2.cryptoService().keysBackupService())

        // - The new device must see the previous backup as not trusted
        assertNotNull(testData.aliceSession2.cryptoService().keysBackupService().keysBackupVersion)
        assertFalse(testData.aliceSession2.cryptoService().keysBackupService().isEnabled)
        assertEquals(KeysBackupState.NotTrusted, testData.aliceSession2.cryptoService().keysBackupService().state)

        // - Try to trust the backup from the new device with a wrong password
        val latch = CountDownLatch(1)
        testData.aliceSession2.cryptoService().keysBackupService().trustKeysBackupVersionWithPassphrase(
                testData.aliceSession2.cryptoService().keysBackupService().keysBackupVersion!!,
                badPassword,
                TestMatrixCallback(latch, false)
        )
        testHelper.await(latch)

        // - The new device must still see the previous backup as not trusted
        assertNotNull(testData.aliceSession2.cryptoService().keysBackupService().keysBackupVersion)
        assertFalse(testData.aliceSession2.cryptoService().keysBackupService().isEnabled)
        assertEquals(KeysBackupState.NotTrusted, testData.aliceSession2.cryptoService().keysBackupService().state)

        stateObserver.stopAndCheckStates(null)
        testData.cleanUp(testHelper)
    }

    /**
     * - Do an e2e backup to the homeserver with a recovery key
     * - Log Alice on a new device
     * - Try to restore the e2e backup with a wrong recovery key
     * - It must fail
     */
    @Test
    @Ignore("This test will be ignored until it is fixed")
    fun restoreKeysBackupWithAWrongRecoveryKeyTest() {
        val testData = keysBackupTestHelper.createKeysBackupScenarioWithPassword(null)

        // - Try to restore the e2e backup with a wrong recovery key
        val latch2 = CountDownLatch(1)
        var importRoomKeysResult: ImportRoomKeysResult? = null
        testData.aliceSession2.cryptoService().keysBackupService().restoreKeysWithRecoveryKey(testData.aliceSession2.cryptoService().keysBackupService().keysBackupVersion!!,
                "EsTc LW2K PGiF wKEA 3As5 g5c4 BXwk qeeJ ZJV8 Q9fu gUMN UE4d",
                null,
                null,
                null,
                object : TestMatrixCallback<ImportRoomKeysResult>(latch2, false) {
                    override fun onSuccess(data: ImportRoomKeysResult) {
                        importRoomKeysResult = data
                        super.onSuccess(data)
                    }
                }
        )
        testHelper.await(latch2)

        // onSuccess may not have been called
        assertNull(importRoomKeysResult)

        testData.cleanUp(testHelper)
    }

    /**
     * - Do an e2e backup to the homeserver with a password
     * - Log Alice on a new device
     * - Restore the e2e backup with the password
     * - Restore must be successful
     */
    @Test
    @Ignore("This test will be ignored until it is fixed")
    fun testBackupWithPassword() {
        val password = "password"

        val testData = keysBackupTestHelper.createKeysBackupScenarioWithPassword(password)

        // - Restore the e2e backup with the password
        val steps = ArrayList<StepProgressListener.Step>()

        val importRoomKeysResult = testHelper.doSync<ImportRoomKeysResult> {
            testData.aliceSession2.cryptoService().keysBackupService().restoreKeyBackupWithPassword(testData.aliceSession2.cryptoService().keysBackupService().keysBackupVersion!!,
                    password,
                    null,
                    null,
                    object : StepProgressListener {
                        override fun onStepProgress(step: StepProgressListener.Step) {
                            steps.add(step)
                        }
                    },
                    it
            )
        }

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

        testData.cleanUp(testHelper)
    }

    /**
     * - Do an e2e backup to the homeserver with a password
     * - Log Alice on a new device
     * - Try to restore the e2e backup with a wrong password
     * - It must fail
     */
    @Test
    @Ignore("This test will be ignored until it is fixed")
    fun restoreKeysBackupWithAWrongPasswordTest() {
        val password = "password"
        val wrongPassword = "passw0rd"

        val testData = keysBackupTestHelper.createKeysBackupScenarioWithPassword(password)

        // - Try to restore the e2e backup with a wrong password
        val latch2 = CountDownLatch(1)
        var importRoomKeysResult: ImportRoomKeysResult? = null
        testData.aliceSession2.cryptoService().keysBackupService().restoreKeyBackupWithPassword(testData.aliceSession2.cryptoService().keysBackupService().keysBackupVersion!!,
                wrongPassword,
                null,
                null,
                null,
                object : TestMatrixCallback<ImportRoomKeysResult>(latch2, false) {
                    override fun onSuccess(data: ImportRoomKeysResult) {
                        importRoomKeysResult = data
                        super.onSuccess(data)
                    }
                }
        )
        testHelper.await(latch2)

        // onSuccess may not have been called
        assertNull(importRoomKeysResult)

        testData.cleanUp(testHelper)
    }

    /**
     * - Do an e2e backup to the homeserver with a password
     * - Log Alice on a new device
     * - Restore the e2e backup with the recovery key.
     * - Restore must be successful
     */
    @Test
    @Ignore("This test will be ignored until it is fixed")
    fun testUseRecoveryKeyToRestoreAPasswordBasedKeysBackup() {
        val password = "password"

        val testData = keysBackupTestHelper.createKeysBackupScenarioWithPassword(password)

        // - Restore the e2e backup with the recovery key.
        val importRoomKeysResult = testHelper.doSync<ImportRoomKeysResult> {
            testData.aliceSession2.cryptoService().keysBackupService().restoreKeysWithRecoveryKey(testData.aliceSession2.cryptoService().keysBackupService().keysBackupVersion!!,
                    testData.prepareKeysBackupDataResult.megolmBackupCreationInfo.recoveryKey,
                    null,
                    null,
                    null,
                    it
            )
        }

        keysBackupTestHelper.checkRestoreSuccess(testData, importRoomKeysResult.totalNumberOfKeys, importRoomKeysResult.successfullyNumberOfImportedKeys)

        testData.cleanUp(testHelper)
    }

    /**
     * - Do an e2e backup to the homeserver with a recovery key
     * - And log Alice on a new device
     * - Try to restore the e2e backup with a password
     * - It must fail
     */
    @Test
    @Ignore("This test will be ignored until it is fixed")
    fun testUsePasswordToRestoreARecoveryKeyBasedKeysBackup() {
        val testData = keysBackupTestHelper.createKeysBackupScenarioWithPassword(null)

        // - Try to restore the e2e backup with a password
        val latch2 = CountDownLatch(1)
        var importRoomKeysResult: ImportRoomKeysResult? = null
        testData.aliceSession2.cryptoService().keysBackupService().restoreKeyBackupWithPassword(testData.aliceSession2.cryptoService().keysBackupService().keysBackupVersion!!,
                "password",
                null,
                null,
                null,
                object : TestMatrixCallback<ImportRoomKeysResult>(latch2, false) {
                    override fun onSuccess(data: ImportRoomKeysResult) {
                        importRoomKeysResult = data
                        super.onSuccess(data)
                    }
                }
        )
        testHelper.await(latch2)

        // onSuccess may not have been called
        assertNull(importRoomKeysResult)

        testData.cleanUp(testHelper)
    }

    /**
     * - Create a backup version
     * - Check the returned KeysVersionResult is trusted
     */
    @Test
    @Ignore("This test will be ignored until it is fixed")
    fun testIsKeysBackupTrusted() {
        // - Create a backup version
        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoomWithEncryptedMessages()

        val keysBackup = cryptoTestData.firstSession.cryptoService().keysBackupService()

        val stateObserver = StateObserver(keysBackup)

        // - Do an e2e backup to the homeserver
        keysBackupTestHelper.prepareAndCreateKeysBackupData(keysBackup)

        // Get key backup version from the homeserver
        val keysVersionResult = testHelper.doSync<KeysBackupLastVersionResult> {
            keysBackup.getCurrentVersion(it)
        }.toKeysVersionResult()

        // - Check the returned KeyBackupVersion is trusted
        val keysBackupVersionTrust = testHelper.doSync<KeysBackupVersionTrust> {
            keysBackup.getKeysBackupTrust(keysVersionResult!!, it)
        }

        assertNotNull(keysBackupVersionTrust)
        assertTrue(keysBackupVersionTrust.usable)
        assertEquals(1, keysBackupVersionTrust.signatures.size)

        val signature = keysBackupVersionTrust.signatures[0]
        assertTrue(signature.valid)
        assertNotNull(signature.device)
        assertEquals(cryptoTestData.firstSession.cryptoService().getMyDevice().deviceId, signature.deviceId)
        assertEquals(signature.device!!.deviceId, cryptoTestData.firstSession.sessionParams.deviceId)

        stateObserver.stopAndCheckStates(null)
        cryptoTestData.cleanUp(testHelper)
    }

    /**
     * Check backup starts automatically if there is an existing and compatible backup
     * version on the homeserver.
     * - Create a backup version
     * - Restart alice session
     * -> The new alice session must back up to the same version
     */
    @Test
    @Ignore("This test will be ignored until it is fixed")
    fun testCheckAndStartKeysBackupWhenRestartingAMatrixSession() {
        // - Create a backup version
        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoomWithEncryptedMessages()

        val keysBackup = cryptoTestData.firstSession.cryptoService().keysBackupService()

        val stateObserver = StateObserver(keysBackup)

        assertFalse(keysBackup.isEnabled)

        val keyBackupCreationInfo = keysBackupTestHelper.prepareAndCreateKeysBackupData(keysBackup)

        assertTrue(keysBackup.isEnabled)

        // - Restart alice session
        // - Log Alice on a new device
        val aliceSession2 = testHelper.logIntoAccount(cryptoTestData.firstSession.myUserId, KeysBackupTestConstants.defaultSessionParamsWithInitialSync)

        cryptoTestData.cleanUp(testHelper)

        val keysBackup2 = aliceSession2.cryptoService().keysBackupService()

        val stateObserver2 = StateObserver(keysBackup2)

        // -> The new alice session must back up to the same version
        val latch = CountDownLatch(1)
        var count = 0
        keysBackup2.addListener(object : KeysBackupStateListener {
            override fun onStateChange(newState: KeysBackupState) {
                // Check the backup completes
                if (newState == KeysBackupState.ReadyToBackUp) {
                    count++

                    if (count == 2) {
                        // Remove itself from the list of listeners
                        keysBackup2.removeListener(this)

                        latch.countDown()
                    }
                }
            }
        })
        testHelper.await(latch)

        assertEquals(keyBackupCreationInfo.version, keysBackup2.currentBackupVersion)

        stateObserver.stopAndCheckStates(null)
        stateObserver2.stopAndCheckStates(null)
        testHelper.signOutAndClose(aliceSession2)
    }

    /**
     * Check WrongBackUpVersion state
     *
     * - Make alice back up her keys to her homeserver
     * - Create a new backup with fake data on the homeserver
     * - Make alice back up all her keys again
     * -> That must fail and her backup state must be WrongBackUpVersion
     */
    @Test
    fun testBackupWhenAnotherBackupWasCreated() {
        // - Create a backup version
        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoomWithEncryptedMessages()

        val keysBackup = cryptoTestData.firstSession.cryptoService().keysBackupService()

        val stateObserver = StateObserver(keysBackup)

        assertFalse(keysBackup.isEnabled)

        // Wait for keys backup to be finished
        val latch0 = CountDownLatch(1)
        var count = 0
        keysBackup.addListener(object : KeysBackupStateListener {
            override fun onStateChange(newState: KeysBackupState) {
                // Check the backup completes
                if (newState == KeysBackupState.ReadyToBackUp) {
                    count++

                    if (count == 2) {
                        // Remove itself from the list of listeners
                        keysBackup.removeListener(this)

                        latch0.countDown()
                    }
                }
            }
        })

        // - Make alice back up her keys to her homeserver
        keysBackupTestHelper.prepareAndCreateKeysBackupData(keysBackup)

        assertTrue(keysBackup.isEnabled)

        testHelper.await(latch0)

        // - Create a new backup with fake data on the homeserver, directly using the rest client
        val megolmBackupCreationInfo = cryptoTestHelper.createFakeMegolmBackupCreationInfo()
        testHelper.doSync<KeysVersion> {
            (keysBackup as DefaultKeysBackupService).createFakeKeysBackupVersion(megolmBackupCreationInfo, it)
        }

        // Reset the store backup status for keys
        (cryptoTestData.firstSession.cryptoService().keysBackupService() as DefaultKeysBackupService).store.resetBackupMarkers()

        // - Make alice back up all her keys again
        val latch2 = CountDownLatch(1)
        keysBackup.backupAllGroupSessions(null, TestMatrixCallback(latch2, false))
        testHelper.await(latch2)

        // -> That must fail and her backup state must be WrongBackUpVersion
        assertEquals(KeysBackupState.WrongBackUpVersion, keysBackup.state)
        assertFalse(keysBackup.isEnabled)

        stateObserver.stopAndCheckStates(null)
        cryptoTestData.cleanUp(testHelper)
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
    @Ignore("This test will be ignored until it is fixed")
    fun testBackupAfterVerifyingADevice() {
        // - Create a backup version
        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoomWithEncryptedMessages()

        val keysBackup = cryptoTestData.firstSession.cryptoService().keysBackupService()

        val stateObserver = StateObserver(keysBackup)

        // - Make alice back up her keys to her homeserver
        keysBackupTestHelper.prepareAndCreateKeysBackupData(keysBackup)

        // Wait for keys backup to finish by asking again to backup keys.
        testHelper.doSync<Unit> {
            keysBackup.backupAllGroupSessions(null, it)
        }

        val oldDeviceId = cryptoTestData.firstSession.sessionParams.deviceId!!
        val oldKeyBackupVersion = keysBackup.currentBackupVersion
        val aliceUserId = cryptoTestData.firstSession.myUserId

        // - Log Alice on a new device
        val aliceSession2 = testHelper.logIntoAccount(aliceUserId, KeysBackupTestConstants.defaultSessionParamsWithInitialSync)

        // - Post a message to have a new megolm session
        aliceSession2.cryptoService().setWarnOnUnknownDevices(false)

        val room2 = aliceSession2.getRoom(cryptoTestData.roomId)!!

        testHelper.sendTextMessage(room2, "New key", 1)

        // - Try to backup all in aliceSession2, it must fail
        val keysBackup2 = aliceSession2.cryptoService().keysBackupService()

        val stateObserver2 = StateObserver(keysBackup2)

        var isSuccessful = false
        val latch2 = CountDownLatch(1)
        keysBackup2.backupAllGroupSessions(
                null,
                object : TestMatrixCallback<Unit>(latch2, false) {
                    override fun onSuccess(data: Unit) {
                        isSuccessful = true
                        super.onSuccess(data)
                    }
                })
        testHelper.await(latch2)

        assertFalse(isSuccessful)

        // Backup state must be NotTrusted
        assertEquals(KeysBackupState.NotTrusted, keysBackup2.state)
        assertFalse(keysBackup2.isEnabled)

        // - Validate the old device from the new one
        aliceSession2.cryptoService().setDeviceVerification(DeviceTrustLevel(crossSigningVerified = false, locallyVerified = true), aliceSession2.myUserId, oldDeviceId)

        // -> Backup should automatically enable on the new device
        val latch4 = CountDownLatch(1)
        keysBackup2.addListener(object : KeysBackupStateListener {
            override fun onStateChange(newState: KeysBackupState) {
                // Check the backup completes
                if (keysBackup2.state == KeysBackupState.ReadyToBackUp) {
                    // Remove itself from the list of listeners
                    keysBackup2.removeListener(this)

                    latch4.countDown()
                }
            }
        })
        testHelper.await(latch4)

        // -> It must use the same backup version
        assertEquals(oldKeyBackupVersion, aliceSession2.cryptoService().keysBackupService().currentBackupVersion)

        testHelper.doSync<Unit> {
            aliceSession2.cryptoService().keysBackupService().backupAllGroupSessions(null, it)
        }

        // -> It must success
        assertTrue(aliceSession2.cryptoService().keysBackupService().isEnabled)

        stateObserver.stopAndCheckStates(null)
        stateObserver2.stopAndCheckStates(null)
        testHelper.signOutAndClose(aliceSession2)
        cryptoTestData.cleanUp(testHelper)
    }

    /**
     * - Do an e2e backup to the homeserver with a recovery key
     * - Delete the backup
     */
    @Test
    fun deleteKeysBackupTest() {
        // - Create a backup version
        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoomWithEncryptedMessages()

        val keysBackup = cryptoTestData.firstSession.cryptoService().keysBackupService()

        val stateObserver = StateObserver(keysBackup)

        assertFalse(keysBackup.isEnabled)

        val keyBackupCreationInfo = keysBackupTestHelper.prepareAndCreateKeysBackupData(keysBackup)

        assertTrue(keysBackup.isEnabled)

        // Delete the backup
        testHelper.doSync<Unit> { keysBackup.deleteBackup(keyBackupCreationInfo.version, it) }

        // Backup is now disabled
        assertFalse(keysBackup.isEnabled)

        stateObserver.stopAndCheckStates(null)
        cryptoTestData.cleanUp(testHelper)
    }
}
