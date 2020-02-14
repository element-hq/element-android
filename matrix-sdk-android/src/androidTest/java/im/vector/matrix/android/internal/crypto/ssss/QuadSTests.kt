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

import android.util.Base64
import androidx.lifecycle.Observer
import androidx.test.ext.junit.runners.AndroidJUnit4
import im.vector.matrix.android.InstrumentedTest
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.securestorage.Curve25519AesSha2KeySpec
import im.vector.matrix.android.api.session.securestorage.EncryptedSecretContent
import im.vector.matrix.android.api.session.securestorage.KeySigner
import im.vector.matrix.android.api.session.securestorage.SecretStorageKeyContent
import im.vector.matrix.android.api.session.securestorage.SsssKeyCreationInfo
import im.vector.matrix.android.api.util.Optional
import im.vector.matrix.android.common.CommonTestHelper
import im.vector.matrix.android.common.SessionTestParams
import im.vector.matrix.android.common.TestConstants
import im.vector.matrix.android.common.TestMatrixCallback
import im.vector.matrix.android.internal.crypto.SSSS_ALGORITHM_CURVE25519_AES_SHA2
import im.vector.matrix.android.internal.crypto.crosssigning.toBase64NoPadding
import im.vector.matrix.android.internal.crypto.secrets.DefaultSharedSecretStorageService
import im.vector.matrix.android.internal.session.sync.model.accountdata.UserAccountDataEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Assert.fail
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

        val aliceLatch = CountDownLatch(1)

        val quadS = aliceSession.sharedSecretStorageService

        var recoveryKey: String? = null

        val TEST_KEY_ID = "my.test.Key"

        quadS.generateKey(TEST_KEY_ID, "Test Key", emptyKeySigner,
                object : MatrixCallback<SsssKeyCreationInfo> {
                    override fun onSuccess(data: SsssKeyCreationInfo) {
                        recoveryKey = data.recoveryKey
                        aliceLatch.countDown()
                    }

                    override fun onFailure(failure: Throwable) {
                        Assert.fail("onFailure " + failure.localizedMessage)
                        aliceLatch.countDown()
                    }
                })

        mTestHelper.await(aliceLatch)

        // Assert Account data is updated
        val accountDataLock = CountDownLatch(1)
        var accountData: UserAccountDataEvent? = null

        val liveAccountData = runBlocking(Dispatchers.Main) {
            aliceSession.getLiveAccountDataEvent("m.secret_storage.key.$TEST_KEY_ID")
        }
        val accountDataObserver = Observer<Optional<UserAccountDataEvent>?> { t ->
            if (t?.getOrNull()?.type == "m.secret_storage.key.$TEST_KEY_ID") {
                accountData = t.getOrNull()
                accountDataLock.countDown()
            }
        }
        GlobalScope.launch(Dispatchers.Main) { liveAccountData.observeForever(accountDataObserver) }

        mTestHelper.await(accountDataLock)

        Assert.assertNotNull("Key should be stored in account data", accountData)
        val parsed = SecretStorageKeyContent.fromJson(accountData!!.content)
        Assert.assertNotNull("Key Content cannot be parsed", parsed)
        Assert.assertEquals("Unexpected Algorithm", SSSS_ALGORITHM_CURVE25519_AES_SHA2, parsed!!.algorithm)
        Assert.assertEquals("Unexpected key name", "Test Key", parsed.name)
        Assert.assertNull("Key was not generated from passphrase", parsed.passphrase)
        Assert.assertNotNull("Pubkey should be defined", parsed.publicKey)

        val privateKeySpec = Curve25519AesSha2KeySpec.fromRecoveryKey(recoveryKey!!)
        DefaultSharedSecretStorageService.withOlmDecryption { olmPkDecryption ->
            val pubKey = olmPkDecryption.setPrivateKey(privateKeySpec!!.privateKey)
            Assert.assertEquals("Unexpected Public Key", pubKey, parsed.publicKey)
        }

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

        Assert.assertNotNull(defaultKeyAccountData?.content)
        Assert.assertEquals("Unexpected default key ${defaultKeyAccountData?.content}", TEST_KEY_ID, defaultKeyAccountData?.content?.get("key"))

        mTestHelper.signout(aliceSession)
    }

    @Test
    fun test_StoreSecret() {
        val aliceSession = mTestHelper.createAccount(TestConstants.USER_ALICE, SessionTestParams(true))
        val keyId = "My.Key"
        val info = generatedSecret(aliceSession, keyId, true)

        // Store a secret

        val storeCountDownLatch = CountDownLatch(1)
        val clearSecret = Base64.encodeToString("42".toByteArray(), Base64.NO_PADDING or Base64.NO_WRAP)
        aliceSession.sharedSecretStorageService.storeSecret(
                "secret.of.life",
                clearSecret,
                null, // default key
                TestMatrixCallback(storeCountDownLatch)
        )

        val secretAccountData = assertAccountData(aliceSession, "secret.of.life")

        val encryptedContent = secretAccountData.content.get("encrypted") as? Map<*, *>
        Assert.assertNotNull("Element should be encrypted", encryptedContent)
        Assert.assertNotNull("Secret should be encrypted with default key", encryptedContent?.get(keyId))

        val secret = EncryptedSecretContent.fromJson(encryptedContent?.get(keyId))
        Assert.assertNotNull(secret?.ciphertext)
        Assert.assertNotNull(secret?.mac)
        Assert.assertNotNull(secret?.ephemeral)

        // Try to decrypt??

        val keySpec = Curve25519AesSha2KeySpec.fromRecoveryKey(info.recoveryKey)

        var decryptedSecret: String? = null

        val decryptCountDownLatch = CountDownLatch(1)
        aliceSession.sharedSecretStorageService.getSecret("secret.of.life",
                null, // default key
                keySpec!!,
                object : MatrixCallback<String> {
                    override fun onFailure(failure: Throwable) {
                        fail("Fail to decrypt -> " + failure.localizedMessage)
                        decryptCountDownLatch.countDown()
                    }

                    override fun onSuccess(data: String) {
                        decryptedSecret = data
                        decryptCountDownLatch.countDown()
                    }
                }
        )
        mTestHelper.await(decryptCountDownLatch)

        Assert.assertEquals("Secret mismatch", clearSecret, decryptedSecret)
        mTestHelper.signout(aliceSession)
    }

    @Test
    fun test_SetDefaultLocalEcho() {
        val aliceSession = mTestHelper.createAccount(TestConstants.USER_ALICE, SessionTestParams(true))

        val quadS = aliceSession.sharedSecretStorageService

        val TEST_KEY_ID = "my.test.Key"

        val countDownLatch = CountDownLatch(1)
        quadS.generateKey(TEST_KEY_ID, "Test Key", emptyKeySigner,
                TestMatrixCallback(countDownLatch))

        mTestHelper.await(countDownLatch)

        // Test that we don't need to wait for an account data sync to access directly the keyid from DB
        val defaultLatch = CountDownLatch(1)
        quadS.setDefaultKey(TEST_KEY_ID, TestMatrixCallback(defaultLatch))
        mTestHelper.await(defaultLatch)

        mTestHelper.signout(aliceSession)
    }

    @Test
    fun test_StoreSecretWithMultipleKey() {
        val aliceSession = mTestHelper.createAccount(TestConstants.USER_ALICE, SessionTestParams(true))
        val keyId1 = "Key.1"
        val key1Info = generatedSecret(aliceSession, keyId1, true)
        val keyId2 = "Key2"
        val key2Info = generatedSecret(aliceSession, keyId2, true)

        val mySecretText = "Lorem ipsum dolor sit amet, consectetur adipiscing elit"

        val storeLatch = CountDownLatch(1)
        aliceSession.sharedSecretStorageService.storeSecret(
                "my.secret",
                mySecretText.toByteArray().toBase64NoPadding(),
                listOf(keyId1, keyId2),
                TestMatrixCallback(storeLatch)
        )
        mTestHelper.await(storeLatch)

        val accountDataEvent = aliceSession.getAccountDataEvent("my.secret")
        val encryptedContent = accountDataEvent?.content?.get("encrypted") as? Map<*, *>

        Assert.assertEquals("Content should contains two encryptions", 2, encryptedContent?.keys?.size ?: 0)

        Assert.assertNotNull(encryptedContent?.get(keyId1))
        Assert.assertNotNull(encryptedContent?.get(keyId2))

        // Assert that can decrypt with both keys
        val decryptCountDownLatch = CountDownLatch(2)
        aliceSession.sharedSecretStorageService.getSecret("my.secret",
                keyId1,
                Curve25519AesSha2KeySpec.fromRecoveryKey(key1Info.recoveryKey)!!,
                TestMatrixCallback(decryptCountDownLatch)
        )

        aliceSession.sharedSecretStorageService.getSecret("my.secret",
                keyId2,
                Curve25519AesSha2KeySpec.fromRecoveryKey(key2Info.recoveryKey)!!,
                TestMatrixCallback(decryptCountDownLatch)
        )

        mTestHelper.await(decryptCountDownLatch)

        mTestHelper.signout(aliceSession)
    }

    @Test
    fun test_GetSecretWithBadPassphrase() {
        val aliceSession = mTestHelper.createAccount(TestConstants.USER_ALICE, SessionTestParams(true))
        val keyId1 = "Key.1"
        val passphrase = "The good pass phrase"
        val key1Info = generatedSecretFromPassphrase(aliceSession, passphrase, keyId1, true)

        val mySecretText = "Lorem ipsum dolor sit amet, consectetur adipiscing elit"

        val storeLatch = CountDownLatch(1)
        aliceSession.sharedSecretStorageService.storeSecret(
                "my.secret",
                mySecretText.toByteArray().toBase64NoPadding(),
                listOf(keyId1),
                TestMatrixCallback(storeLatch)
        )
        mTestHelper.await(storeLatch)

        val decryptCountDownLatch = CountDownLatch(2)
        aliceSession.sharedSecretStorageService.getSecret("my.secret",
                keyId1,
                Curve25519AesSha2KeySpec.fromPassphrase(
                        "A bad passphrase",
                        key1Info.content?.passphrase?.salt ?: "",
                        key1Info.content?.passphrase?.iterations ?: 0,
                        null),
                object : MatrixCallback<String> {
                    override fun onSuccess(data: String) {
                        decryptCountDownLatch.countDown()
                        fail("Should not be able to decrypt")
                    }

                    override fun onFailure(failure: Throwable) {
                        Assert.assertTrue(true)
                        decryptCountDownLatch.countDown()
                    }
                }
        )

        // Now try with correct key
        aliceSession.sharedSecretStorageService.getSecret("my.secret",
                keyId1,
                Curve25519AesSha2KeySpec.fromPassphrase(
                        passphrase,
                        key1Info.content?.passphrase?.salt ?: "",
                        key1Info.content?.passphrase?.iterations ?: 0,
                        null),
                TestMatrixCallback(decryptCountDownLatch)
        )

        mTestHelper.await(decryptCountDownLatch)

        mTestHelper.signout(aliceSession)
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

        Assert.assertNotNull("Account Data type:$type should be found", accountData)

        return accountData!!
    }

    private fun generatedSecret(session: Session, keyId: String, asDefault: Boolean = true): SsssKeyCreationInfo {
        val quadS = session.sharedSecretStorageService

        var creationInfo: SsssKeyCreationInfo? = null

        val generateLatch = CountDownLatch(1)

        quadS.generateKey(keyId, keyId, emptyKeySigner,
                object : MatrixCallback<SsssKeyCreationInfo> {
                    override fun onSuccess(data: SsssKeyCreationInfo) {
                        creationInfo = data
                        generateLatch.countDown()
                    }

                    override fun onFailure(failure: Throwable) {
                        Assert.fail("onFailure " + failure.localizedMessage)
                        generateLatch.countDown()
                    }
                })

        mTestHelper.await(generateLatch)

        Assert.assertNotNull(creationInfo)

        assertAccountData(session, "m.secret_storage.key.$keyId")
        if (asDefault) {
            val setDefaultLatch = CountDownLatch(1)
            quadS.setDefaultKey(keyId, TestMatrixCallback(setDefaultLatch))
            mTestHelper.await(setDefaultLatch)
            assertAccountData(session, DefaultSharedSecretStorageService.DEFAULT_KEY_ID)
        }

        return creationInfo!!
    }

    private fun generatedSecretFromPassphrase(session: Session, passphrase: String, keyId: String, asDefault: Boolean = true): SsssKeyCreationInfo {
        val quadS = session.sharedSecretStorageService

        var creationInfo: SsssKeyCreationInfo? = null

        val generateLatch = CountDownLatch(1)

        quadS.generateKeyWithPassphrase(keyId, keyId,
                passphrase,
                emptyKeySigner,
                null,
                object : MatrixCallback<SsssKeyCreationInfo> {
                    override fun onSuccess(data: SsssKeyCreationInfo) {
                        creationInfo = data
                        generateLatch.countDown()
                    }

                    override fun onFailure(failure: Throwable) {
                        Assert.fail("onFailure " + failure.localizedMessage)
                        generateLatch.countDown()
                    }
                })

        mTestHelper.await(generateLatch)

        Assert.assertNotNull(creationInfo)

        assertAccountData(session, "m.secret_storage.key.$keyId")
        if (asDefault) {
            val setDefaultLatch = CountDownLatch(1)
            quadS.setDefaultKey(keyId, TestMatrixCallback(setDefaultLatch))
            mTestHelper.await(setDefaultLatch)
            assertAccountData(session, DefaultSharedSecretStorageService.DEFAULT_KEY_ID)
        }

        return creationInfo!!
    }
}
