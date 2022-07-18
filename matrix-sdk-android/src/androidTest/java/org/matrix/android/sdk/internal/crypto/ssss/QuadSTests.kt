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

package org.matrix.android.sdk.internal.crypto.ssss

import androidx.lifecycle.Observer
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.FixMethodOrder
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.matrix.android.sdk.InstrumentedTest
import org.matrix.android.sdk.api.crypto.SSSS_ALGORITHM_AES_HMAC_SHA2
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.accountdata.UserAccountDataEvent
import org.matrix.android.sdk.api.session.securestorage.EncryptedSecretContent
import org.matrix.android.sdk.api.session.securestorage.KeyRef
import org.matrix.android.sdk.api.session.securestorage.KeySigner
import org.matrix.android.sdk.api.session.securestorage.RawBytesKeySpec
import org.matrix.android.sdk.api.session.securestorage.SecretStorageKeyContent
import org.matrix.android.sdk.api.session.securestorage.SharedSecretStorageError
import org.matrix.android.sdk.api.session.securestorage.SsssKeyCreationInfo
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.api.util.toBase64NoPadding
import org.matrix.android.sdk.common.CommonTestHelper
import org.matrix.android.sdk.common.CommonTestHelper.Companion.runSessionTest
import org.matrix.android.sdk.common.SessionTestParams
import org.matrix.android.sdk.common.TestConstants
import org.matrix.android.sdk.internal.crypto.secrets.DefaultSharedSecretStorageService

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.JVM)
class QuadSTests : InstrumentedTest {

    private val emptyKeySigner = object : KeySigner {
        override fun sign(canonicalJson: String): Map<String, Map<String, String>>? {
            return null
        }
    }

    @Test
    fun test_Generate4SKey() = runSessionTest(context()) { testHelper ->

        val aliceSession = testHelper.createAccount(TestConstants.USER_ALICE, SessionTestParams(true))

        val quadS = aliceSession.sharedSecretStorageService()

        val TEST_KEY_ID = "my.test.Key"

        testHelper.runBlockingTest {
            quadS.generateKey(TEST_KEY_ID, null, "Test Key", emptyKeySigner)
        }

        var accountData: UserAccountDataEvent? = null
        // Assert Account data is updated
        testHelper.waitWithLatch {
            val liveAccountData = aliceSession.accountDataService().getLiveUserAccountDataEvent("${DefaultSharedSecretStorageService.KEY_ID_BASE}.$TEST_KEY_ID")
            val accountDataObserver = Observer<Optional<UserAccountDataEvent>?> { t ->
                if (t?.getOrNull()?.type == "${DefaultSharedSecretStorageService.KEY_ID_BASE}.$TEST_KEY_ID") {
                    accountData = t.getOrNull()
                }
                it.countDown()
            }
            liveAccountData.observeForever(accountDataObserver)
        }
        assertNotNull("Key should be stored in account data", accountData)
        val parsed = SecretStorageKeyContent.fromJson(accountData!!.content)
        assertNotNull("Key Content cannot be parsed", parsed)
        assertEquals("Unexpected Algorithm", SSSS_ALGORITHM_AES_HMAC_SHA2, parsed!!.algorithm)
        assertEquals("Unexpected key name", "Test Key", parsed.name)
        assertNull("Key was not generated from passphrase", parsed.passphrase)

        var defaultKeyAccountData: UserAccountDataEvent? = null
        // Set as default key
        testHelper.waitWithLatch { latch ->
            quadS.setDefaultKey(TEST_KEY_ID)
            val liveDefAccountData =
                    aliceSession.accountDataService().getLiveUserAccountDataEvent(DefaultSharedSecretStorageService.DEFAULT_KEY_ID)
            val accountDefDataObserver = Observer<Optional<UserAccountDataEvent>?> { t ->
                if (t?.getOrNull()?.type == DefaultSharedSecretStorageService.DEFAULT_KEY_ID) {
                    defaultKeyAccountData = t.getOrNull()!!
                    latch.countDown()
                }
            }
            liveDefAccountData.observeForever(accountDefDataObserver)
        }
        assertNotNull(defaultKeyAccountData?.content)
        assertEquals("Unexpected default key ${defaultKeyAccountData?.content}", TEST_KEY_ID, defaultKeyAccountData?.content?.get("key"))

        testHelper.signOutAndClose(aliceSession)
    }

    @Test
    fun test_StoreSecret() = runSessionTest(context()) { testHelper ->

        val aliceSession = testHelper.createAccount(TestConstants.USER_ALICE, SessionTestParams(true))
        val keyId = "My.Key"
        val info = generatedSecret(testHelper, aliceSession, keyId, true)

        val keySpec = RawBytesKeySpec.fromRecoveryKey(info.recoveryKey)

        // Store a secret
        val clearSecret = "42".toByteArray().toBase64NoPadding()
        testHelper.runBlockingTest {
            aliceSession.sharedSecretStorageService().storeSecret(
                    "secret.of.life",
                    clearSecret,
                    listOf(KeyRef(null, keySpec)) // default key
            )
        }

        val secretAccountData = assertAccountData(testHelper, aliceSession, "secret.of.life")

        val encryptedContent = secretAccountData.content["encrypted"] as? Map<*, *>
        assertNotNull("Element should be encrypted", encryptedContent)
        assertNotNull("Secret should be encrypted with default key", encryptedContent?.get(keyId))

        val secret = EncryptedSecretContent.fromJson(encryptedContent?.get(keyId))
        assertNotNull(secret?.ciphertext)
        assertNotNull(secret?.mac)
        assertNotNull(secret?.initializationVector)

        // Try to decrypt??

        val decryptedSecret = testHelper.runBlockingTest {
            aliceSession.sharedSecretStorageService().getSecret(
                    "secret.of.life",
                    null, // default key
                    keySpec!!
            )
        }

        assertEquals("Secret mismatch", clearSecret, decryptedSecret)
    }

    @Test
    fun test_SetDefaultLocalEcho() = runSessionTest(context()) { testHelper ->

        val aliceSession = testHelper.createAccount(TestConstants.USER_ALICE, SessionTestParams(true))

        val quadS = aliceSession.sharedSecretStorageService()

        val TEST_KEY_ID = "my.test.Key"

        testHelper.runBlockingTest {
            quadS.generateKey(TEST_KEY_ID, null, "Test Key", emptyKeySigner)
        }

        // Test that we don't need to wait for an account data sync to access directly the keyid from DB
        testHelper.runBlockingTest {
            quadS.setDefaultKey(TEST_KEY_ID)
        }
    }

    @Test
    fun test_StoreSecretWithMultipleKey() = runSessionTest(context()) { testHelper ->

        val aliceSession = testHelper.createAccount(TestConstants.USER_ALICE, SessionTestParams(true))
        val keyId1 = "Key.1"
        val key1Info = generatedSecret(testHelper, aliceSession, keyId1, true)
        val keyId2 = "Key2"
        val key2Info = generatedSecret(testHelper, aliceSession, keyId2, true)

        val mySecretText = "Lorem ipsum dolor sit amet, consectetur adipiscing elit"

        testHelper.runBlockingTest {
            aliceSession.sharedSecretStorageService().storeSecret(
                    "my.secret",
                    mySecretText.toByteArray().toBase64NoPadding(),
                    listOf(
                            KeyRef(keyId1, RawBytesKeySpec.fromRecoveryKey(key1Info.recoveryKey)),
                            KeyRef(keyId2, RawBytesKeySpec.fromRecoveryKey(key2Info.recoveryKey))
                    )
            )
        }

        val accountDataEvent = aliceSession.accountDataService().getUserAccountDataEvent("my.secret")
        val encryptedContent = accountDataEvent?.content?.get("encrypted") as? Map<*, *>

        assertEquals("Content should contains two encryptions", 2, encryptedContent?.keys?.size ?: 0)

        assertNotNull(encryptedContent?.get(keyId1))
        assertNotNull(encryptedContent?.get(keyId2))

        // Assert that can decrypt with both keys
        testHelper.runBlockingTest {
            aliceSession.sharedSecretStorageService().getSecret(
                    "my.secret",
                    keyId1,
                    RawBytesKeySpec.fromRecoveryKey(key1Info.recoveryKey)!!
            )
        }

        testHelper.runBlockingTest {
            aliceSession.sharedSecretStorageService().getSecret(
                    "my.secret",
                    keyId2,
                    RawBytesKeySpec.fromRecoveryKey(key2Info.recoveryKey)!!
            )
        }
    }

    @Test
    @Ignore("Test is working locally, not in GitHub actions")
    fun test_GetSecretWithBadPassphrase() = runSessionTest(context()) { testHelper ->

        val aliceSession = testHelper.createAccount(TestConstants.USER_ALICE, SessionTestParams(true))
        val keyId1 = "Key.1"
        val passphrase = "The good pass phrase"
        val key1Info = generatedSecretFromPassphrase(testHelper, aliceSession, passphrase, keyId1, true)

        val mySecretText = "Lorem ipsum dolor sit amet, consectetur adipiscing elit"

        testHelper.runBlockingTest {
            aliceSession.sharedSecretStorageService().storeSecret(
                    "my.secret",
                    mySecretText.toByteArray().toBase64NoPadding(),
                    listOf(KeyRef(keyId1, RawBytesKeySpec.fromRecoveryKey(key1Info.recoveryKey)))
            )
        }

        testHelper.runBlockingTest {
            try {
                aliceSession.sharedSecretStorageService().getSecret(
                        "my.secret",
                        keyId1,
                        RawBytesKeySpec.fromPassphrase(
                                "A bad passphrase",
                                key1Info.content?.passphrase?.salt ?: "",
                                key1Info.content?.passphrase?.iterations ?: 0,
                                null
                        )
                )
            } catch (throwable: Throwable) {
                assert(throwable is SharedSecretStorageError.BadMac)
            }
        }

        // Now try with correct key
        testHelper.runBlockingTest {
            aliceSession.sharedSecretStorageService().getSecret(
                    "my.secret",
                    keyId1,
                    RawBytesKeySpec.fromPassphrase(
                            passphrase,
                            key1Info.content?.passphrase?.salt ?: "",
                            key1Info.content?.passphrase?.iterations ?: 0,
                            null
                    )
            )
        }
    }

    private fun assertAccountData(testHelper: CommonTestHelper, session: Session, type: String): UserAccountDataEvent {
        var accountData: UserAccountDataEvent? = null
        testHelper.waitWithLatch {
            val liveAccountData = session.accountDataService().getLiveUserAccountDataEvent(type)
            val accountDataObserver = Observer<Optional<UserAccountDataEvent>?> { t ->
                if (t?.getOrNull()?.type == type) {
                    accountData = t.getOrNull()
                    it.countDown()
                }
            }
            liveAccountData.observeForever(accountDataObserver)
        }
        assertNotNull("Account Data type:$type should be found", accountData)
        return accountData!!
    }

    private fun generatedSecret(testHelper: CommonTestHelper, session: Session, keyId: String, asDefault: Boolean = true): SsssKeyCreationInfo {
        val quadS = session.sharedSecretStorageService()

        val creationInfo = testHelper.runBlockingTest {
            quadS.generateKey(keyId, null, keyId, emptyKeySigner)
        }

        assertAccountData(testHelper, session, "${DefaultSharedSecretStorageService.KEY_ID_BASE}.$keyId")

        if (asDefault) {
            testHelper.runBlockingTest {
                quadS.setDefaultKey(keyId)
            }
            assertAccountData(testHelper, session, DefaultSharedSecretStorageService.DEFAULT_KEY_ID)
        }

        return creationInfo
    }

    private fun generatedSecretFromPassphrase(testHelper: CommonTestHelper, session: Session, passphrase: String, keyId: String, asDefault: Boolean = true): SsssKeyCreationInfo {
        val quadS = session.sharedSecretStorageService()

        val creationInfo = testHelper.runBlockingTest {
            quadS.generateKeyWithPassphrase(
                    keyId,
                    keyId,
                    passphrase,
                    emptyKeySigner,
                    null
            )
        }

        assertAccountData(testHelper, session, "${DefaultSharedSecretStorageService.KEY_ID_BASE}.$keyId")
        if (asDefault) {
            testHelper.runBlockingTest {
                quadS.setDefaultKey(keyId)
            }
            assertAccountData(testHelper, session, DefaultSharedSecretStorageService.DEFAULT_KEY_ID)
        }

        return creationInfo
    }
}
