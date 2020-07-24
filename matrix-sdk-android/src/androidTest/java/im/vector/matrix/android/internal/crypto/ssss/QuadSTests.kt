/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.matrix.android.internal.crypto.ssss

import androidx.lifecycle.Observer
import androidx.test.ext.junit.runners.AndroidJUnit4
import im.vector.matrix.android.InstrumentedTest
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.securestorage.EncryptedSecretContent
import im.vector.matrix.android.api.session.securestorage.KeySigner
import im.vector.matrix.android.api.session.securestorage.RawBytesKeySpec
import im.vector.matrix.android.api.session.securestorage.SecretStorageKeyContent
import im.vector.matrix.android.api.session.securestorage.SharedSecretStorageService
import im.vector.matrix.android.api.session.securestorage.SsssKeyCreationInfo
import im.vector.matrix.android.api.util.Optional
import im.vector.matrix.android.common.CommonTestHelper
import im.vector.matrix.android.common.SessionTestParams
import im.vector.matrix.android.common.TestConstants
import im.vector.matrix.android.common.TestMatrixCallback
import im.vector.matrix.android.internal.crypto.SSSS_ALGORITHM_AES_HMAC_SHA2
import im.vector.matrix.android.internal.crypto.crosssigning.toBase64NoPadding
import im.vector.matrix.android.internal.crypto.secrets.DefaultSharedSecretStorageService
import im.vector.matrix.android.api.session.accountdata.UserAccountDataEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.util.concurrent.CountDownLatch

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.JVM)
class QuadSTests : InstrumentedTest {

    private val mTestHelper = CommonTestHelper(context())

    private val emptyKeySigner = object : KeySigner {
        override fun sign(canonicalJson: String): Map<String, Map<String, String>>? {
            return null
        }
    }

    @Test
    fun test_Generate4SKey() {
        val aliceSession = mTestHelper.createAccount(TestConstants.USER_ALICE, SessionTestParams(true))

        val quadS = aliceSession.sharedSecretStorageService

        val TEST_KEY_ID = "my.test.Key"

        mTestHelper.doSync<SsssKeyCreationInfo> {
            quadS.generateKey(TEST_KEY_ID, null, "Test Key", emptyKeySigner, it)
        }

        // Assert Account data is updated
        val accountDataLock = CountDownLatch(1)
        var accountData: UserAccountDataEvent? = null

        val liveAccountData = runBlocking(Dispatchers.Main) {
            aliceSession.getLiveAccountDataEvent("${DefaultSharedSecretStorageService.KEY_ID_BASE}.$TEST_KEY_ID")
        }
        val accountDataObserver = Observer<Optional<UserAccountDataEvent>?> { t ->
            if (t?.getOrNull()?.type == "${DefaultSharedSecretStorageService.KEY_ID_BASE}.$TEST_KEY_ID") {
                accountData = t.getOrNull()
                accountDataLock.countDown()
            }
        }
        GlobalScope.launch(Dispatchers.Main) { liveAccountData.observeForever(accountDataObserver) }

        mTestHelper.await(accountDataLock)

        assertNotNull("Key should be stored in account data", accountData)
        val parsed = SecretStorageKeyContent.fromJson(accountData!!.content)
        assertNotNull("Key Content cannot be parsed", parsed)
        assertEquals("Unexpected Algorithm", SSSS_ALGORITHM_AES_HMAC_SHA2, parsed!!.algorithm)
        assertEquals("Unexpected key name", "Test Key", parsed.name)
        assertNull("Key was not generated from passphrase", parsed.passphrase)

        // Set as default key
        quadS.setDefaultKey(TEST_KEY_ID, object : MatrixCallback<Unit> {})

        var defaultKeyAccountData: UserAccountDataEvent? = null
        val defaultDataLock = CountDownLatch(1)

        val liveDefAccountData = runBlocking(Dispatchers.Main) {
            aliceSession.getLiveAccountDataEvent(DefaultSharedSecretStorageService.DEFAULT_KEY_ID)
        }
        val accountDefDataObserver = Observer<Optional<UserAccountDataEvent>?> { t ->
            if (t?.getOrNull()?.type == DefaultSharedSecretStorageService.DEFAULT_KEY_ID) {
                defaultKeyAccountData = t.getOrNull()!!
                defaultDataLock.countDown()
            }
        }
        GlobalScope.launch(Dispatchers.Main) { liveDefAccountData.observeForever(accountDefDataObserver) }

        mTestHelper.await(defaultDataLock)

        assertNotNull(defaultKeyAccountData?.content)
        assertEquals("Unexpected default key ${defaultKeyAccountData?.content}", TEST_KEY_ID, defaultKeyAccountData?.content?.get("key"))

        mTestHelper.signOutAndClose(aliceSession)
    }

    @Test
    fun test_StoreSecret() {
        val aliceSession = mTestHelper.createAccount(TestConstants.USER_ALICE, SessionTestParams(true))
        val keyId = "My.Key"
        val info = generatedSecret(aliceSession, keyId, true)

        val keySpec = RawBytesKeySpec.fromRecoveryKey(info.recoveryKey)

        // Store a secret
        val clearSecret = "42".toByteArray().toBase64NoPadding()
        mTestHelper.doSync<Unit> {
            aliceSession.sharedSecretStorageService.storeSecret(
                    "secret.of.life",
                    clearSecret,
                    listOf(SharedSecretStorageService.KeyRef(null, keySpec)), // default key
                    it
            )
        }

        val secretAccountData = assertAccountData(aliceSession, "secret.of.life")

        val encryptedContent = secretAccountData.content["encrypted"] as? Map<*, *>
        assertNotNull("Element should be encrypted", encryptedContent)
        assertNotNull("Secret should be encrypted with default key", encryptedContent?.get(keyId))

        val secret = EncryptedSecretContent.fromJson(encryptedContent?.get(keyId))
        assertNotNull(secret?.ciphertext)
        assertNotNull(secret?.mac)
        assertNotNull(secret?.initializationVector)

        // Try to decrypt??

        val decryptedSecret = mTestHelper.doSync<String> {
            aliceSession.sharedSecretStorageService.getSecret(
                    "secret.of.life",
                    null, // default key
                    keySpec!!,
                    it
            )
        }

        assertEquals("Secret mismatch", clearSecret, decryptedSecret)
        mTestHelper.signOutAndClose(aliceSession)
    }

    @Test
    fun test_SetDefaultLocalEcho() {
        val aliceSession = mTestHelper.createAccount(TestConstants.USER_ALICE, SessionTestParams(true))

        val quadS = aliceSession.sharedSecretStorageService

        val TEST_KEY_ID = "my.test.Key"

        mTestHelper.doSync<SsssKeyCreationInfo> {
            quadS.generateKey(TEST_KEY_ID, null, "Test Key", emptyKeySigner, it)
        }

        // Test that we don't need to wait for an account data sync to access directly the keyid from DB
        mTestHelper.doSync<Unit> {
            quadS.setDefaultKey(TEST_KEY_ID, it)
        }

        mTestHelper.signOutAndClose(aliceSession)
    }

    @Test
    fun test_StoreSecretWithMultipleKey() {
        val aliceSession = mTestHelper.createAccount(TestConstants.USER_ALICE, SessionTestParams(true))
        val keyId1 = "Key.1"
        val key1Info = generatedSecret(aliceSession, keyId1, true)
        val keyId2 = "Key2"
        val key2Info = generatedSecret(aliceSession, keyId2, true)

        val mySecretText = "Lorem ipsum dolor sit amet, consectetur adipiscing elit"

        mTestHelper.doSync<Unit> {
            aliceSession.sharedSecretStorageService.storeSecret(
                    "my.secret",
                    mySecretText.toByteArray().toBase64NoPadding(),
                    listOf(
                            SharedSecretStorageService.KeyRef(keyId1, RawBytesKeySpec.fromRecoveryKey(key1Info.recoveryKey)),
                            SharedSecretStorageService.KeyRef(keyId2, RawBytesKeySpec.fromRecoveryKey(key2Info.recoveryKey))
                    ),
                    it
            )
        }

        val accountDataEvent = aliceSession.getAccountDataEvent("my.secret")
        val encryptedContent = accountDataEvent?.content?.get("encrypted") as? Map<*, *>

        assertEquals("Content should contains two encryptions", 2, encryptedContent?.keys?.size ?: 0)

        assertNotNull(encryptedContent?.get(keyId1))
        assertNotNull(encryptedContent?.get(keyId2))

        // Assert that can decrypt with both keys
        mTestHelper.doSync<String> {
            aliceSession.sharedSecretStorageService.getSecret("my.secret",
                    keyId1,
                    RawBytesKeySpec.fromRecoveryKey(key1Info.recoveryKey)!!,
                    it
            )
        }

        mTestHelper.doSync<String> {
            aliceSession.sharedSecretStorageService.getSecret("my.secret",
                    keyId2,
                    RawBytesKeySpec.fromRecoveryKey(key2Info.recoveryKey)!!,
                    it
            )
        }

        mTestHelper.signOutAndClose(aliceSession)
    }

    @Test
    fun test_GetSecretWithBadPassphrase() {
        val aliceSession = mTestHelper.createAccount(TestConstants.USER_ALICE, SessionTestParams(true))
        val keyId1 = "Key.1"
        val passphrase = "The good pass phrase"
        val key1Info = generatedSecretFromPassphrase(aliceSession, passphrase, keyId1, true)

        val mySecretText = "Lorem ipsum dolor sit amet, consectetur adipiscing elit"

        mTestHelper.doSync<Unit> {
            aliceSession.sharedSecretStorageService.storeSecret(
                    "my.secret",
                    mySecretText.toByteArray().toBase64NoPadding(),
                    listOf(SharedSecretStorageService.KeyRef(keyId1, RawBytesKeySpec.fromRecoveryKey(key1Info.recoveryKey))),
                    it
            )
        }

        val decryptCountDownLatch = CountDownLatch(1)
        var error = false
        aliceSession.sharedSecretStorageService.getSecret("my.secret",
                keyId1,
                RawBytesKeySpec.fromPassphrase(
                        "A bad passphrase",
                        key1Info.content?.passphrase?.salt ?: "",
                        key1Info.content?.passphrase?.iterations ?: 0,
                        null),
                object : MatrixCallback<String> {
                    override fun onSuccess(data: String) {
                        decryptCountDownLatch.countDown()
                    }

                    override fun onFailure(failure: Throwable) {
                        error = true
                        decryptCountDownLatch.countDown()
                    }
                }
        )

        mTestHelper.await(decryptCountDownLatch)

        error shouldBe true

        // Now try with correct key
        mTestHelper.doSync<String> {
            aliceSession.sharedSecretStorageService.getSecret("my.secret",
                    keyId1,
                    RawBytesKeySpec.fromPassphrase(
                            passphrase,
                            key1Info.content?.passphrase?.salt ?: "",
                            key1Info.content?.passphrase?.iterations ?: 0,
                            null),
                    it
            )
        }

        mTestHelper.signOutAndClose(aliceSession)
    }

    private fun assertAccountData(session: Session, type: String): UserAccountDataEvent {
        val accountDataLock = CountDownLatch(1)
        var accountData: UserAccountDataEvent? = null

        val liveAccountData = runBlocking(Dispatchers.Main) {
            session.getLiveAccountDataEvent(type)
        }
        val accountDataObserver = Observer<Optional<UserAccountDataEvent>?> { t ->
            if (t?.getOrNull()?.type == type) {
                accountData = t.getOrNull()
                accountDataLock.countDown()
            }
        }
        GlobalScope.launch(Dispatchers.Main) { liveAccountData.observeForever(accountDataObserver) }
        mTestHelper.await(accountDataLock)

        assertNotNull("Account Data type:$type should be found", accountData)

        return accountData!!
    }

    private fun generatedSecret(session: Session, keyId: String, asDefault: Boolean = true): SsssKeyCreationInfo {
        val quadS = session.sharedSecretStorageService

        val creationInfo = mTestHelper.doSync<SsssKeyCreationInfo> {
            quadS.generateKey(keyId, null, keyId, emptyKeySigner, it)
        }

        assertAccountData(session, "${DefaultSharedSecretStorageService.KEY_ID_BASE}.$keyId")

        if (asDefault) {
            mTestHelper.doSync<Unit> {
                quadS.setDefaultKey(keyId, it)
            }
            assertAccountData(session, DefaultSharedSecretStorageService.DEFAULT_KEY_ID)
        }

        return creationInfo
    }

    private fun generatedSecretFromPassphrase(session: Session, passphrase: String, keyId: String, asDefault: Boolean = true): SsssKeyCreationInfo {
        val quadS = session.sharedSecretStorageService

        val creationInfo = mTestHelper.doSync<SsssKeyCreationInfo> {
            quadS.generateKeyWithPassphrase(
                    keyId,
                    keyId,
                    passphrase,
                    emptyKeySigner,
                    null,
                    it)
        }

        assertAccountData(session, "${DefaultSharedSecretStorageService.KEY_ID_BASE}.$keyId")
        if (asDefault) {
            val setDefaultLatch = CountDownLatch(1)
            quadS.setDefaultKey(keyId, TestMatrixCallback(setDefaultLatch))
            mTestHelper.await(setDefaultLatch)
            assertAccountData(session, DefaultSharedSecretStorageService.DEFAULT_KEY_ID)
        }

        return creationInfo
    }
}
