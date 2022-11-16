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

package org.matrix.android.sdk.internal.crypto.crosssigning

import androidx.lifecycle.LiveData
import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.MatrixCoroutineDispatchers
import org.matrix.android.sdk.api.auth.UserInteractiveAuthInterceptor
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.crypto.crosssigning.CrossSigningService
import org.matrix.android.sdk.api.session.crypto.crosssigning.DeviceTrustLevel
import org.matrix.android.sdk.api.session.crypto.crosssigning.DeviceTrustResult
import org.matrix.android.sdk.api.session.crypto.crosssigning.MXCrossSigningInfo
import org.matrix.android.sdk.api.session.crypto.crosssigning.PrivateKeysInfo
import org.matrix.android.sdk.api.session.crypto.crosssigning.UserTrustResult
import org.matrix.android.sdk.api.session.crypto.crosssigning.isCrossSignedVerified
import org.matrix.android.sdk.api.session.crypto.crosssigning.isLocallyVerified
import org.matrix.android.sdk.api.session.crypto.crosssigning.isVerified
import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.api.session.crypto.model.RoomEncryptionTrustLevel
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.api.util.fromBase64
import org.matrix.android.sdk.internal.crypto.DeviceListManager
import org.matrix.android.sdk.internal.crypto.OutgoingKeyRequestManager
import org.matrix.android.sdk.internal.crypto.model.rest.UploadSignatureQueryBuilder
import org.matrix.android.sdk.internal.crypto.store.IMXCryptoStore
import org.matrix.android.sdk.internal.crypto.tasks.InitializeCrossSigningTask
import org.matrix.android.sdk.internal.crypto.tasks.UploadSignaturesTask
import org.matrix.android.sdk.internal.di.SessionId
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.di.WorkManagerProvider
import org.matrix.android.sdk.internal.session.SessionScope
import org.matrix.android.sdk.internal.task.TaskExecutor
import org.matrix.android.sdk.internal.util.JsonCanonicalizer
import org.matrix.android.sdk.internal.util.logLimit
import org.matrix.android.sdk.internal.worker.WorkerParamsFactory
import org.matrix.olm.OlmPkSigning
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@SessionScope
internal class DefaultCrossSigningService @Inject constructor(
        @UserId private val myUserId: String,
        @SessionId private val sessionId: String,
        private val cryptoStore: IMXCryptoStore,
        private val deviceListManager: DeviceListManager,
        private val initializeCrossSigningTask: InitializeCrossSigningTask,
        private val uploadSignaturesTask: UploadSignaturesTask,
        private val taskExecutor: TaskExecutor,
        private val coroutineDispatchers: MatrixCoroutineDispatchers,
        private val cryptoCoroutineScope: CoroutineScope,
        private val workManagerProvider: WorkManagerProvider,
        private val outgoingKeyRequestManager: OutgoingKeyRequestManager,
        private val crossSigningOlm: CrossSigningOlm,
        private val updateTrustWorkerDataRepository: UpdateTrustWorkerDataRepository
) : CrossSigningService,
        DeviceListManager.UserDevicesUpdateListener {

    init {
        try {

            // Try to get stored keys if they exist
            cryptoStore.getMyCrossSigningInfo()?.let { mxCrossSigningInfo ->
                Timber.i("## CrossSigning - Found Existing self signed keys")
                Timber.i("## CrossSigning - Checking if private keys are known")

                cryptoStore.getCrossSigningPrivateKeys()?.let { privateKeysInfo ->
                    privateKeysInfo.master
                            ?.fromBase64()
                            ?.let { privateKeySeed ->
                                val pkSigning = OlmPkSigning()
                                if (pkSigning.initWithSeed(privateKeySeed) == mxCrossSigningInfo.masterKey()?.unpaddedBase64PublicKey) {
                                    crossSigningOlm.masterPkSigning = pkSigning
                                    Timber.i("## CrossSigning - Loading master key success")
                                } else {
                                    Timber.w("## CrossSigning - Public master key does not match the private key")
                                    pkSigning.releaseSigning()
                                    // TODO untrust?
                                }
                            }
                    privateKeysInfo.user
                            ?.fromBase64()
                            ?.let { privateKeySeed ->
                                val pkSigning = OlmPkSigning()
                                if (pkSigning.initWithSeed(privateKeySeed) == mxCrossSigningInfo.userKey()?.unpaddedBase64PublicKey) {
                                    crossSigningOlm.userPkSigning = pkSigning
                                    Timber.i("## CrossSigning - Loading User Signing key success")
                                } else {
                                    Timber.w("## CrossSigning - Public User key does not match the private key")
                                    pkSigning.releaseSigning()
                                    // TODO untrust?
                                }
                            }
                    privateKeysInfo.selfSigned
                            ?.fromBase64()
                            ?.let { privateKeySeed ->
                                val pkSigning = OlmPkSigning()
                                if (pkSigning.initWithSeed(privateKeySeed) == mxCrossSigningInfo.selfSigningKey()?.unpaddedBase64PublicKey) {
                                    crossSigningOlm.selfSigningPkSigning = pkSigning
                                    Timber.i("## CrossSigning - Loading Self Signing key success")
                                } else {
                                    Timber.w("## CrossSigning - Public Self Signing key does not match the private key")
                                    pkSigning.releaseSigning()
                                    // TODO untrust?
                                }
                            }
                }

                // Recover local trust in case private key are there?
                cryptoCoroutineScope.launch(coroutineDispatchers.crypto) {
                    setUserKeysAsTrusted(myUserId, checkUserTrust(myUserId).isVerified())
                }
            }
        } catch (e: Throwable) {
            // Mmm this kind of a big issue
            Timber.e(e, "Failed to initialize Cross Signing")
        }

        deviceListManager.addListener(this)
    }

    fun release() {
        crossSigningOlm.release()
        deviceListManager.removeListener(this)
    }

    protected fun finalize() {
        release()
    }

    /**
     * - Make 3 key pairs (MSK, USK, SSK)
     * - Save the private keys with proper security
     * - Sign the keys and upload them
     * - Sign the current device with SSK and sign MSK with device key (migration) and upload signatures.
     */
    override suspend fun initializeCrossSigning(uiaInterceptor: UserInteractiveAuthInterceptor?) {
        Timber.d("## CrossSigning  initializeCrossSigning")

        val params = InitializeCrossSigningTask.Params(
                interactiveAuthInterceptor = uiaInterceptor
        )
        val data = initializeCrossSigningTask
                .execute(params)
        val crossSigningInfo = MXCrossSigningInfo(
                myUserId,
                listOf(data.masterKeyInfo, data.userKeyInfo, data.selfSignedKeyInfo),
                true
        )
        withContext(coroutineDispatchers.crypto) {
            cryptoStore.setMyCrossSigningInfo(crossSigningInfo)
            setUserKeysAsTrusted(myUserId, true)
            cryptoStore.storePrivateKeysInfo(data.masterKeyPK, data.userKeyPK, data.selfSigningKeyPK)
            crossSigningOlm.masterPkSigning = OlmPkSigning().apply { initWithSeed(data.masterKeyPK.fromBase64()) }
            crossSigningOlm.userPkSigning = OlmPkSigning().apply { initWithSeed(data.userKeyPK.fromBase64()) }
            crossSigningOlm.selfSigningPkSigning = OlmPkSigning().apply { initWithSeed(data.selfSigningKeyPK.fromBase64()) }
        }
    }

    override suspend fun onSecretMSKGossip(mskPrivateKey: String) {
        Timber.i("## CrossSigning - onSecretSSKGossip")
        val mxCrossSigningInfo = getMyCrossSigningKeys() ?: return Unit.also {
            Timber.e("## CrossSigning - onSecretMSKGossip() received secret but public key is not known")
        }

        mskPrivateKey.fromBase64()
                .let { privateKeySeed ->
                    val pkSigning = OlmPkSigning()
                    try {
                        if (pkSigning.initWithSeed(privateKeySeed) == mxCrossSigningInfo.masterKey()?.unpaddedBase64PublicKey) {
                            crossSigningOlm.masterPkSigning?.releaseSigning()
                            crossSigningOlm.masterPkSigning = pkSigning
                            Timber.i("## CrossSigning - Loading MSK success")
                            cryptoStore.storeMSKPrivateKey(mskPrivateKey)
                            return
                        } else {
                            Timber.e("## CrossSigning - onSecretMSKGossip() private key do not match public key")
                            pkSigning.releaseSigning()
                        }
                    } catch (failure: Throwable) {
                        Timber.e("## CrossSigning - onSecretMSKGossip() ${failure.localizedMessage}")
                        pkSigning.releaseSigning()
                    }
                }
    }

    override suspend fun onSecretSSKGossip(sskPrivateKey: String) {
        Timber.i("## CrossSigning - onSecretSSKGossip")
        val mxCrossSigningInfo = getMyCrossSigningKeys() ?: return Unit.also {
            Timber.e("## CrossSigning - onSecretSSKGossip() received secret but public key is not known")
        }

        sskPrivateKey.fromBase64()
                .let { privateKeySeed ->
                    val pkSigning = OlmPkSigning()
                    try {
                        if (pkSigning.initWithSeed(privateKeySeed) == mxCrossSigningInfo.selfSigningKey()?.unpaddedBase64PublicKey) {
                            crossSigningOlm.selfSigningPkSigning?.releaseSigning()
                            crossSigningOlm.selfSigningPkSigning = pkSigning
                            Timber.i("## CrossSigning - Loading SSK success")
                            cryptoStore.storeSSKPrivateKey(sskPrivateKey)
                            return
                        } else {
                            Timber.e("## CrossSigning - onSecretSSKGossip() private key do not match public key")
                            pkSigning.releaseSigning()
                        }
                    } catch (failure: Throwable) {
                        Timber.e("## CrossSigning - onSecretSSKGossip() ${failure.localizedMessage}")
                        pkSigning.releaseSigning()
                    }
                }
    }

    override suspend fun onSecretUSKGossip(uskPrivateKey: String) {
        Timber.i("## CrossSigning - onSecretUSKGossip")
        val mxCrossSigningInfo = getMyCrossSigningKeys() ?: return Unit.also {
            Timber.e("## CrossSigning - onSecretUSKGossip() received secret but public key is not knwow ")
        }

        uskPrivateKey.fromBase64()
                .let { privateKeySeed ->
                    val pkSigning = OlmPkSigning()
                    try {
                        if (pkSigning.initWithSeed(privateKeySeed) == mxCrossSigningInfo.userKey()?.unpaddedBase64PublicKey) {
                            crossSigningOlm.userPkSigning?.releaseSigning()
                            crossSigningOlm.userPkSigning = pkSigning
                            Timber.i("## CrossSigning - Loading USK success")
                            cryptoStore.storeUSKPrivateKey(uskPrivateKey)
                            return
                        } else {
                            Timber.e("## CrossSigning - onSecretUSKGossip() private key do not match public key")
                            pkSigning.releaseSigning()
                        }
                    } catch (failure: Throwable) {
                        pkSigning.releaseSigning()
                    }
                }
    }

    override suspend fun checkTrustFromPrivateKeys(
            masterKeyPrivateKey: String?,
            uskKeyPrivateKey: String?,
            sskPrivateKey: String?
    ): UserTrustResult {
        val mxCrossSigningInfo = getMyCrossSigningKeys() ?: return UserTrustResult.CrossSigningNotConfigured(myUserId)

        var masterKeyIsTrusted = false
        var userKeyIsTrusted = false
        var selfSignedKeyIsTrusted = false

        masterKeyPrivateKey?.fromBase64()
                ?.let { privateKeySeed ->
                    val pkSigning = OlmPkSigning()
                    try {
                        if (pkSigning.initWithSeed(privateKeySeed) == mxCrossSigningInfo.masterKey()?.unpaddedBase64PublicKey) {
                            crossSigningOlm.masterPkSigning?.releaseSigning()
                            crossSigningOlm.masterPkSigning = pkSigning
                            masterKeyIsTrusted = true
                            Timber.i("## CrossSigning - Loading master key success")
                        } else {
                            pkSigning.releaseSigning()
                        }
                    } catch (failure: Throwable) {
                        pkSigning.releaseSigning()
                    }
                }

        uskKeyPrivateKey?.fromBase64()
                ?.let { privateKeySeed ->
                    val pkSigning = OlmPkSigning()
                    try {
                        if (pkSigning.initWithSeed(privateKeySeed) == mxCrossSigningInfo.userKey()?.unpaddedBase64PublicKey) {
                            crossSigningOlm.userPkSigning?.releaseSigning()
                            crossSigningOlm.userPkSigning = pkSigning
                            userKeyIsTrusted = true
                            Timber.i("## CrossSigning - Loading master key success")
                        } else {
                            pkSigning.releaseSigning()
                        }
                    } catch (failure: Throwable) {
                        pkSigning.releaseSigning()
                    }
                }

        sskPrivateKey?.fromBase64()
                ?.let { privateKeySeed ->
                    val pkSigning = OlmPkSigning()
                    try {
                        if (pkSigning.initWithSeed(privateKeySeed) == mxCrossSigningInfo.selfSigningKey()?.unpaddedBase64PublicKey) {
                            crossSigningOlm.selfSigningPkSigning?.releaseSigning()
                            crossSigningOlm.selfSigningPkSigning = pkSigning
                            selfSignedKeyIsTrusted = true
                            Timber.i("## CrossSigning - Loading master key success")
                        } else {
                            pkSigning.releaseSigning()
                        }
                    } catch (failure: Throwable) {
                        pkSigning.releaseSigning()
                    }
                }

        if (!masterKeyIsTrusted || !userKeyIsTrusted || !selfSignedKeyIsTrusted) {
            return UserTrustResult.Failure("Keys not trusted $mxCrossSigningInfo") // UserTrustResult.KeysNotTrusted(mxCrossSigningInfo)
        } else {
            cryptoStore.markMyMasterKeyAsLocallyTrusted(true)
            val checkSelfTrust = checkSelfTrust()
            if (checkSelfTrust.isVerified()) {
                cryptoStore.storePrivateKeysInfo(masterKeyPrivateKey, uskKeyPrivateKey, sskPrivateKey)
                setUserKeysAsTrusted(myUserId, true)
            }
            return checkSelfTrust
        }
    }

    /**
     *
     *  ┏━━━━━━━━┓                             ┏━━━━━━━━┓
     *  ┃ ALICE  ┃                             ┃  BOB   ┃
     *  ┗━━━━━━━━┛                             ┗━━━━━━━━┛
     *   MSK                      ┌────────────▶  MSK
     *                            │
     *     │                      │
     *     │    SSK               │
     *     │                      │
     *     │                      │
     *     └──▶ USK   ────────────┘
     * .
     */
    override suspend fun isUserTrusted(otherUserId: String): Boolean {
        return withContext(coroutineDispatchers.io) {
            cryptoStore.getCrossSigningInfo(otherUserId)?.isTrusted() == true
        }
    }

    override suspend fun isCrossSigningVerified(): Boolean {
        return withContext(coroutineDispatchers.io) {
            checkSelfTrust().isVerified()
        }
    }

    /**
     * Will not force a download of the key, but will verify signatures trust chain.
     */
    override suspend fun checkUserTrust(otherUserId: String): UserTrustResult {
        Timber.v("## CrossSigning  checkUserTrust for $otherUserId")
        if (otherUserId == myUserId) {
            return checkSelfTrust()
        }
        // I trust a user if I trust his master key
        // I can trust the master key if it is signed by my user key
        // TODO what if the master key is signed by a device key that i have verified

        // First let's get my user key
        val myCrossSigningInfo = cryptoStore.getCrossSigningInfo(myUserId)

        return checkOtherMSKTrusted(myCrossSigningInfo, cryptoStore.getCrossSigningInfo(otherUserId))
    }

    override fun checkOtherMSKTrusted(myCrossSigningInfo: MXCrossSigningInfo?, otherInfo: MXCrossSigningInfo?): UserTrustResult {
        val myUserKey = myCrossSigningInfo?.userKey()
                ?: return UserTrustResult.CrossSigningNotConfigured(myUserId)

        if (!myCrossSigningInfo.isTrusted()) {
            return UserTrustResult.Failure("Keys not trusted $myCrossSigningInfo") // UserTrustResult.KeysNotTrusted(myCrossSigningInfo)
        }

        // Let's get the other user  master key
        val otherMasterKey = otherInfo?.masterKey()
                ?: return UserTrustResult.Failure("Unknown MSK for ${otherInfo?.userId}") // UserTrustResult.UnknownCrossSignatureInfo(otherInfo?.userId ?: "")

        val masterKeySignaturesMadeByMyUserKey = otherMasterKey.signatures
                ?.get(myUserId) // Signatures made by me
                ?.get("ed25519:${myUserKey.unpaddedBase64PublicKey}")

        if (masterKeySignaturesMadeByMyUserKey.isNullOrBlank()) {
            Timber.d("## CrossSigning  checkUserTrust false for ${otherInfo.userId}, not signed by my UserSigningKey")
            return UserTrustResult.Failure("MSK not signed by my USK $otherMasterKey") // UserTrustResult.KeyNotSigned(otherMasterKey)
        }

        // Check that Alice USK signature of Bob MSK is valid
        try {
            crossSigningOlm.olmUtility.verifyEd25519Signature(
                    masterKeySignaturesMadeByMyUserKey,
                    myUserKey.unpaddedBase64PublicKey,
                    otherMasterKey.canonicalSignable()
            )
        } catch (failure: Throwable) {
            return UserTrustResult.Failure("Invalid signature $masterKeySignaturesMadeByMyUserKey") // UserTrustResult.InvalidSignature(myUserKey, masterKeySignaturesMadeByMyUserKey)
        }

        return UserTrustResult.Success
    }

    private fun checkSelfTrust(): UserTrustResult {
        // Special case when it's me,
        // I have to check that MSK -> USK -> SSK
        // and that MSK is trusted (i know the private key, or is signed by a trusted device)
        val myCrossSigningInfo = cryptoStore.getCrossSigningInfo(myUserId)

        return checkSelfTrust(myCrossSigningInfo, cryptoStore.getUserDeviceList(myUserId))
    }

    override fun checkSelfTrust(myCrossSigningInfo: MXCrossSigningInfo?, myDevices: List<CryptoDeviceInfo>?): UserTrustResult {
        // Special case when it's me,
        // I have to check that MSK -> USK -> SSK
        // and that MSK is trusted (i know the private key, or is signed by a trusted device)
//        val myCrossSigningInfo = cryptoStore.getCrossSigningInfo(userId)

        val myMasterKey = myCrossSigningInfo?.masterKey()
                ?: return UserTrustResult.CrossSigningNotConfigured(myUserId)

        // Is the master key trusted
        // 1) check if I know the private key
        val masterPrivateKey = cryptoStore.getCrossSigningPrivateKeys()
                ?.master
                ?.fromBase64()

        var isMaterKeyTrusted = false
        if (myMasterKey.trustLevel?.locallyVerified == true) {
            isMaterKeyTrusted = true
        } else if (masterPrivateKey != null) {
            // Check if private match public
            var olmPkSigning: OlmPkSigning? = null
            try {
                olmPkSigning = OlmPkSigning()
                val expectedPK = olmPkSigning.initWithSeed(masterPrivateKey)
                isMaterKeyTrusted = myMasterKey.unpaddedBase64PublicKey == expectedPK
            } catch (failure: Throwable) {
                Timber.e(failure)
            }
            olmPkSigning?.releaseSigning()
        } else {
            // Maybe it's signed by a locally trusted device?
            myMasterKey.signatures?.get(myUserId)?.forEach { (key, value) ->
                val potentialDeviceId = key.removePrefix("ed25519:")
                val potentialDevice = myDevices?.firstOrNull { it.deviceId == potentialDeviceId } // cryptoStore.getUserDevice(userId, potentialDeviceId)
                if (potentialDevice != null && potentialDevice.isVerified) {
                    // Check signature validity?
                    try {
                        crossSigningOlm.olmUtility.verifyEd25519Signature(value, potentialDevice.fingerprint(), myMasterKey.canonicalSignable())
                        isMaterKeyTrusted = true
                        return@forEach
                    } catch (failure: Throwable) {
                        // log
                        Timber.w(failure, "Signature not valid?")
                    }
                }
            }
        }

        if (!isMaterKeyTrusted) {
            return UserTrustResult.Failure("Keys not trusted $myCrossSigningInfo") // UserTrustResult.KeysNotTrusted(myCrossSigningInfo)
        }

        val myUserKey = myCrossSigningInfo.userKey()
                ?: return UserTrustResult.CrossSigningNotConfigured(myUserId)

        val userKeySignaturesMadeByMyMasterKey = myUserKey.signatures
                ?.get(myUserId) // Signatures made by me
                ?.get("ed25519:${myMasterKey.unpaddedBase64PublicKey}")

        if (userKeySignaturesMadeByMyMasterKey.isNullOrBlank()) {
            Timber.d("## CrossSigning  checkUserTrust false for $myUserId, USK not signed by MSK")
            return UserTrustResult.Failure("USK not signed by MSK") // UserTrustResult.KeyNotSigned(myUserKey)
        }

        // Check that Alice USK signature of Alice MSK is valid
        try {
            crossSigningOlm.olmUtility.verifyEd25519Signature(
                    userKeySignaturesMadeByMyMasterKey,
                    myMasterKey.unpaddedBase64PublicKey,
                    myUserKey.canonicalSignable()
            )
        } catch (failure: Throwable) {
            return UserTrustResult.Failure("Invalid MSK signature of USK") // UserTrustResult.InvalidSignature(myUserKey, userKeySignaturesMadeByMyMasterKey)
        }

        val mySSKey = myCrossSigningInfo.selfSigningKey()
                ?: return UserTrustResult.CrossSigningNotConfigured(myUserId)

        val ssKeySignaturesMadeByMyMasterKey = mySSKey.signatures
                ?.get(myUserId) // Signatures made by me
                ?.get("ed25519:${myMasterKey.unpaddedBase64PublicKey}")

        if (ssKeySignaturesMadeByMyMasterKey.isNullOrBlank()) {
            Timber.d("## CrossSigning  checkUserTrust false for $myUserId, SSK not signed by MSK")
            return UserTrustResult.Failure("SSK not signed by MSK") // UserTrustResult.KeyNotSigned(mySSKey)
        }

        // Check that Alice USK signature of Alice MSK is valid
        try {
            crossSigningOlm.olmUtility.verifyEd25519Signature(
                    ssKeySignaturesMadeByMyMasterKey,
                    myMasterKey.unpaddedBase64PublicKey,
                    mySSKey.canonicalSignable()
            )
        } catch (failure: Throwable) {
            return UserTrustResult.Failure("Invalid signature $ssKeySignaturesMadeByMyMasterKey") // UserTrustResult.InvalidSignature(mySSKey, ssKeySignaturesMadeByMyMasterKey)
        }

        return UserTrustResult.Success
    }

    override suspend fun getUserCrossSigningKeys(otherUserId: String): MXCrossSigningInfo? {
        return withContext(coroutineDispatchers.io) {
            cryptoStore.getCrossSigningInfo(otherUserId)
        }
    }

    override fun getLiveCrossSigningKeys(userId: String): LiveData<Optional<MXCrossSigningInfo>> {
        return cryptoStore.getLiveCrossSigningInfo(userId)
    }

    override suspend fun getMyCrossSigningKeys(): MXCrossSigningInfo? {
        return withContext(coroutineDispatchers.io) {
            cryptoStore.getMyCrossSigningInfo()
        }
    }

    override suspend fun getCrossSigningPrivateKeys(): PrivateKeysInfo? {
        return withContext(coroutineDispatchers.io) {
            cryptoStore.getCrossSigningPrivateKeys()
        }
    }

    override fun getLiveCrossSigningPrivateKeys(): LiveData<Optional<PrivateKeysInfo>> {
        return cryptoStore.getLiveCrossSigningPrivateKeys()
    }

    override fun canCrossSign(): Boolean {
        return checkSelfTrust().isVerified() && cryptoStore.getCrossSigningPrivateKeys()?.selfSigned != null &&
                cryptoStore.getCrossSigningPrivateKeys()?.user != null
    }

    override fun allPrivateKeysKnown(): Boolean {
        return checkSelfTrust().isVerified() &&
                cryptoStore.getCrossSigningPrivateKeys()?.allKnown().orFalse()
    }

    override suspend fun trustUser(otherUserId: String) {
        withContext(coroutineDispatchers.crypto) {
            Timber.d("## CrossSigning - Mark user $otherUserId as trusted ")
            // We should have this user keys
            val otherMasterKeys = getUserCrossSigningKeys(otherUserId)?.masterKey()
            if (otherMasterKeys == null) {
                throw Throwable("## CrossSigning - Other master signing key is not known")
            }
            val myKeys = getUserCrossSigningKeys(myUserId)
                    ?: throw Throwable("## CrossSigning - CrossSigning is not setup for this account")

            val userPubKey = myKeys.userKey()?.unpaddedBase64PublicKey
            if (userPubKey == null || crossSigningOlm.userPkSigning == null) {
                throw Throwable("## CrossSigning - Cannot sign from this account, privateKeyUnknown $userPubKey")
            }

            // Sign the other MasterKey with our UserSigning key
            val newSignature = JsonCanonicalizer.getCanonicalJson(
                    Map::class.java,
                    otherMasterKeys.signalableJSONDictionary()
            ).let { crossSigningOlm.userPkSigning?.sign(it) }
                    ?: // race??
                    throw Throwable("## CrossSigning - Failed to sign")

            cryptoStore.setUserKeysAsTrusted(otherUserId, true)

            Timber.d("## CrossSigning - Upload signature of $otherUserId MSK signed by USK")
            val uploadQuery = UploadSignatureQueryBuilder()
                    .withSigningKeyInfo(otherMasterKeys.copyForSignature(myUserId, userPubKey, newSignature))
                    .build()

            uploadSignaturesTask.execute(UploadSignaturesTask.Params(uploadQuery))

            // Local echo for device cross trust, to avoid having to wait for a notification of key change
            cryptoStore.getUserDeviceList(otherUserId)?.forEach { device ->
                val updatedTrust = checkDeviceTrust(device.userId, device.deviceId, device.trustLevel?.isLocallyVerified() ?: false)
                Timber.v("## CrossSigning - update trust for device ${device.deviceId} of user $otherUserId , verified=$updatedTrust")
                cryptoStore.setDeviceTrust(device.userId, device.deviceId, updatedTrust.isCrossSignedVerified(), updatedTrust.isLocallyVerified())
            }
        }
    }

    override suspend fun markMyMasterKeyAsTrusted() {
        withContext(coroutineDispatchers.crypto) {
            cryptoStore.markMyMasterKeyAsLocallyTrusted(true)
            checkSelfTrust()
            // re-verify all trusts
            onUsersDeviceUpdate(listOf(myUserId))
        }
    }

    override suspend fun trustDevice(deviceId: String) {
        withContext(coroutineDispatchers.crypto) {
            // This device should be yours
            val device = cryptoStore.getUserDevice(myUserId, deviceId)
            if (device == null) {
                throw IllegalArgumentException("This device [$deviceId] is not known, or not yours")
            }

            val myKeys = getUserCrossSigningKeys(myUserId)
                    ?: throw Throwable("CrossSigning is not setup for this account")

            val ssPubKey = myKeys.selfSigningKey()?.unpaddedBase64PublicKey
            if (ssPubKey == null || crossSigningOlm.selfSigningPkSigning == null) {
                throw Throwable("Cannot sign from this account, public and/or privateKey Unknown $ssPubKey")
            }

            // Sign with self signing
            val newSignature = crossSigningOlm.selfSigningPkSigning?.sign(device.canonicalSignable())
                    ?: throw Throwable("Failed to sign")

            val toUpload = device.copy(
                    signatures = mapOf(
                            myUserId
                                    to
                                    mapOf(
                                            "ed25519:$ssPubKey" to newSignature
                                    )
                    )
            )

            val uploadQuery = UploadSignatureQueryBuilder()
                    .withDeviceInfo(toUpload)
                    .build()
            uploadSignaturesTask.execute(UploadSignaturesTask.Params(uploadQuery))
        }
    }

    override suspend fun shieldForGroup(userIds: List<String>): RoomEncryptionTrustLevel {
        // Not used in kotlin SDK?
        TODO("Not yet implemented")
    }

    override suspend fun checkDeviceTrust(otherUserId: String, otherDeviceId: String, locallyTrusted: Boolean?): DeviceTrustResult {
        val otherDevice = cryptoStore.getUserDevice(otherUserId, otherDeviceId)
                ?: return DeviceTrustResult.UnknownDevice(otherDeviceId)

        val myKeys = getUserCrossSigningKeys(myUserId)
                ?: return legacyFallbackTrust(locallyTrusted, DeviceTrustResult.CrossSigningNotConfigured(myUserId))

        if (!myKeys.isTrusted()) return legacyFallbackTrust(locallyTrusted, DeviceTrustResult.KeysNotTrusted(myKeys))

        val otherKeys = getUserCrossSigningKeys(otherUserId)
                ?: return legacyFallbackTrust(locallyTrusted, DeviceTrustResult.CrossSigningNotConfigured(otherUserId))

        // TODO should we force verification ?
        if (!otherKeys.isTrusted()) return legacyFallbackTrust(locallyTrusted, DeviceTrustResult.KeysNotTrusted(otherKeys))

        // Check if the trust chain is valid
        /*
         *  ┏━━━━━━━━┓                             ┏━━━━━━━━┓
         *  ┃ ALICE  ┃                             ┃  BOB   ┃
         *  ┗━━━━━━━━┛                             ┗━━━━━━━━┛
         *   MSK                      ┌────────────▶MSK
         *                            │
         *     │                      │               │
         *     │    SSK               │               └──▶ SSK  ──────────────────┐
         *     │                      │                                           │
         *     │                      │                    USK                    │
         *     └──▶ USK   ────────────┘              (not visible by              │
         *                                                Alice)                  │
         *                                                                        ▼
         *                                                                ┌──────────────┐
         *                                                                │ BOB's Device │
         *                                                                └──────────────┘
         */

        val otherSSKSignature = otherDevice.signatures?.get(otherUserId)?.get("ed25519:${otherKeys.selfSigningKey()?.unpaddedBase64PublicKey}")
                ?: return legacyFallbackTrust(
                        locallyTrusted,
                        DeviceTrustResult.MissingDeviceSignature(
                                otherDeviceId, otherKeys.selfSigningKey()
                                ?.unpaddedBase64PublicKey
                                ?: ""
                        )
                )

        // Check  bob's device is signed by bob's SSK
        try {
            crossSigningOlm.olmUtility.verifyEd25519Signature(
                    otherSSKSignature,
                    otherKeys.selfSigningKey()?.unpaddedBase64PublicKey,
                    otherDevice.canonicalSignable()
            )
        } catch (e: Throwable) {
            return legacyFallbackTrust(locallyTrusted, DeviceTrustResult.InvalidDeviceSignature(otherDeviceId, otherSSKSignature, e))
        }

        return DeviceTrustResult.Success(DeviceTrustLevel(crossSigningVerified = true, locallyVerified = locallyTrusted))
    }

    fun checkDeviceTrust(myKeys: MXCrossSigningInfo?, otherKeys: MXCrossSigningInfo?, otherDevice: CryptoDeviceInfo): DeviceTrustResult {
        val locallyTrusted = otherDevice.trustLevel?.isLocallyVerified()
        myKeys ?: return legacyFallbackTrust(locallyTrusted, DeviceTrustResult.CrossSigningNotConfigured(myUserId))

        if (!myKeys.isTrusted()) return legacyFallbackTrust(locallyTrusted, DeviceTrustResult.KeysNotTrusted(myKeys))

        otherKeys ?: return legacyFallbackTrust(locallyTrusted, DeviceTrustResult.CrossSigningNotConfigured(otherDevice.userId))

        // TODO should we force verification ?
        if (!otherKeys.isTrusted()) return legacyFallbackTrust(locallyTrusted, DeviceTrustResult.KeysNotTrusted(otherKeys))

        // Check if the trust chain is valid
        /*
         *  ┏━━━━━━━━┓                             ┏━━━━━━━━┓
         *  ┃ ALICE  ┃                             ┃  BOB   ┃
         *  ┗━━━━━━━━┛                             ┗━━━━━━━━┛
         *   MSK                      ┌────────────▶MSK
         *                            │
         *     │                      │               │
         *     │    SSK               │               └──▶ SSK  ──────────────────┐
         *     │                      │                                           │
         *     │                      │                    USK                    │
         *     └──▶ USK   ────────────┘              (not visible by              │
         *                                                Alice)                  │
         *                                                                        ▼
         *                                                                ┌──────────────┐
         *                                                                │ BOB's Device │
         *                                                                └──────────────┘
         */

        val otherSSKSignature = otherDevice.signatures?.get(otherKeys.userId)?.get("ed25519:${otherKeys.selfSigningKey()?.unpaddedBase64PublicKey}")
                ?: return legacyFallbackTrust(
                        locallyTrusted,
                        DeviceTrustResult.MissingDeviceSignature(
                                otherDevice.deviceId, otherKeys.selfSigningKey()
                                ?.unpaddedBase64PublicKey
                                ?: ""
                        )
                )

        // Check  bob's device is signed by bob's SSK
        try {
            crossSigningOlm.olmUtility.verifyEd25519Signature(
                    otherSSKSignature,
                    otherKeys.selfSigningKey()?.unpaddedBase64PublicKey,
                    otherDevice.canonicalSignable()
            )
        } catch (e: Throwable) {
            return legacyFallbackTrust(locallyTrusted, DeviceTrustResult.InvalidDeviceSignature(otherDevice.deviceId, otherSSKSignature, e))
        }

        return DeviceTrustResult.Success(DeviceTrustLevel(crossSigningVerified = true, locallyVerified = locallyTrusted))
    }

    private fun legacyFallbackTrust(locallyTrusted: Boolean?, crossSignTrustFail: DeviceTrustResult): DeviceTrustResult {
        return if (locallyTrusted == true) {
            DeviceTrustResult.Success(DeviceTrustLevel(crossSigningVerified = false, locallyVerified = true))
        } else {
            crossSignTrustFail
        }
    }

    override fun onUsersDeviceUpdate(userIds: List<String>) {
        Timber.d("## CrossSigning - onUsersDeviceUpdate for users: ${userIds.logLimit()}")
        runBlocking {
            checkTrustAndAffectedRoomShields(userIds)
        }
    }

    override suspend fun checkTrustAndAffectedRoomShields(userIds: List<String>) {
        Timber.d("## CrossSigning - checkTrustAndAffectedRoomShields for users: ${userIds.logLimit()}")
        val workerParams = UpdateTrustWorker.Params(
                sessionId = sessionId,
                filename = updateTrustWorkerDataRepository.createParam(userIds)
        )
        val workerData = WorkerParamsFactory.toData(workerParams)

        val workRequest = workManagerProvider.matrixOneTimeWorkRequestBuilder<UpdateTrustWorker>()
                .setInputData(workerData)
                .setBackoffCriteria(BackoffPolicy.LINEAR, WorkManagerProvider.BACKOFF_DELAY_MILLIS, TimeUnit.MILLISECONDS)
                .build()

        workManagerProvider.workManager
                .beginUniqueWork("TRUST_UPDATE_QUEUE", ExistingWorkPolicy.APPEND_OR_REPLACE, workRequest)
                .enqueue()
    }

    private suspend fun setUserKeysAsTrusted(otherUserId: String, trusted: Boolean) {
        val currentTrust = cryptoStore.getCrossSigningInfo(otherUserId)?.isTrusted()
        cryptoStore.setUserKeysAsTrusted(otherUserId, trusted)
        // If it's me, recheck trust of all users and devices?
        val users = ArrayList<String>()
        if (otherUserId == myUserId && currentTrust != trusted) {
            // notify key requester
            outgoingKeyRequestManager.onSelfCrossSigningTrustChanged(trusted)
            cryptoStore.updateUsersTrust {
                users.add(it)
                // called within a real transaction, has to block
                runBlocking {
                    checkUserTrust(it).isVerified()
                }
            }

            users.forEach {
                cryptoStore.getUserDeviceList(it)?.forEach { device ->
                    val updatedTrust = checkDeviceTrust(it, device.deviceId, device.trustLevel?.isLocallyVerified() ?: false)
                    Timber.v("## CrossSigning - update trust for device ${device.deviceId} of user $otherUserId , verified=$updatedTrust")
                    cryptoStore.setDeviceTrust(it, device.deviceId, updatedTrust.isCrossSignedVerified(), updatedTrust.isLocallyVerified())
                }
            }
        }
    }
}
