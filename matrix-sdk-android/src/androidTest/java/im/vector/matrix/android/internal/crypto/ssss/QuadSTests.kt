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
import im.vector.matrix.android.api.session.securestorage.SSSSKeyCreationInfo
import im.vector.matrix.android.api.session.securestorage.SecretStorageKeyContent
import im.vector.matrix.android.api.util.Optional
import im.vector.matrix.android.common.CommonTestHelper
import im.vector.matrix.android.common.CryptoTestHelper
import im.vector.matrix.android.common.SessionTestParams
import im.vector.matrix.android.common.TestConstants
import im.vector.matrix.android.common.TestMatrixCallback
import im.vector.matrix.android.internal.crypto.secrets.DefaultSharedSecureStorage
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
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class QuadSTests : InstrumentedTest {

    private val mTestHelper = CommonTestHelper(context())
    private val mCryptoTestHelper = CryptoTestHelper(mTestHelper)

    @Test
    fun test_Generate4SKey() {

        val aliceSession = mTestHelper.createAccount(TestConstants.USER_ALICE, SessionTestParams(true))

        val aliceLatch = CountDownLatch(1)

        val quadS = aliceSession.sharedSecretStorageService

        val emptyKeySigner = object : KeySigner {
            override fun sign(canonicalJson: String): Map<String, Map<String, String>>? {
                return null
            }
        }

        var recoveryKey: String? = null

        val TEST_KEY_ID = "my.test.Key"

        quadS.generateKey(TEST_KEY_ID, "Test Key", emptyKeySigner,
                object : MatrixCallback<SSSSKeyCreationInfo> {

                    override fun onSuccess(data: SSSSKeyCreationInfo) {
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
            aliceSession.getLiveAccountData("m.secret_storage.key.$TEST_KEY_ID")
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
        Assert.assertEquals("Unexpected Algorithm", DefaultSharedSecureStorage.ALGORITHM_CURVE25519_AES_SHA2, parsed!!.algorithm)
        Assert.assertEquals("Unexpected key name", "Test Key", parsed.name)
        Assert.assertNull("Key was not generated from passphrase", parsed.passphrase)
        Assert.assertNotNull("Pubkey should be defined", parsed.publicKey)

        val privateKeySpec = Curve25519AesSha2KeySpec.fromRecoveryKey(recoveryKey!!)
        DefaultSharedSecureStorage.withOlmDecryption { olmPkDecryption ->
            val pubKey = olmPkDecryption.setPrivateKey(privateKeySpec!!.privateKey)
            Assert.assertEquals("Unexpected Public Key", pubKey, parsed.publicKey)
        }

        // Set as default key
        quadS.setDefaultKey(TEST_KEY_ID, object : MatrixCallback<Unit> {})

        var defaultKeyAccountData: UserAccountDataEvent? = null
        val defaultDataLock = CountDownLatch(1)

        val liveDefAccountData = runBlocking(Dispatchers.Main) {
            aliceSession.getLiveAccountData(DefaultSharedSecureStorage.DEFAULT_KEY_ID)
        }
        val accountDefDataObserver = Observer<Optional<UserAccountDataEvent>?> { t ->
            if (t?.getOrNull()?.type == DefaultSharedSecureStorage.DEFAULT_KEY_ID) {
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

        val secretAccountData = assertAccountData(aliceSession,"secret.of.life" )

        val encryptedContent = secretAccountData.content.get("encrypted") as? Map<*,*>
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
        aliceSession.sharedSecretStorageService.getSecret("secret.of.life" ,
                 null, //default key
                keySpec!!,
                null,
                object : MatrixCallback<String> {
                    override fun onFailure(failure: Throwable) {
                        fail("Fail to decrypt -> " +failure.localizedMessage)
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

    private fun assertAccountData(session: Session, type: String): UserAccountDataEvent {
        val accountDataLock = CountDownLatch(1)
        var accountData: UserAccountDataEvent? = null

        val liveAccountData = runBlocking(Dispatchers.Main) {
            session.getLiveAccountData(type)
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

    private fun generatedSecret(session: Session, keyId: String, asDefault: Boolean = true): SSSSKeyCreationInfo {

        val quadS = session.sharedSecretStorageService

        val emptyKeySigner = object : KeySigner {
            override fun sign(canonicalJson: String): Map<String, Map<String, String>>? {
                return null
            }
        }

        var creationInfo: SSSSKeyCreationInfo? = null

        val generateLatch = CountDownLatch(1)

        quadS.generateKey(keyId, keyId, emptyKeySigner,
                object : MatrixCallback<SSSSKeyCreationInfo> {

                    override fun onSuccess(data: SSSSKeyCreationInfo) {
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
            assertAccountData(session, DefaultSharedSecureStorage.DEFAULT_KEY_ID)
        }

        return creationInfo!!
    }
}
