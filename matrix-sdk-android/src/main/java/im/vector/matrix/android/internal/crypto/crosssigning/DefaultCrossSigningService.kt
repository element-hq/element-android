/*
 * Copyright 2020 New Vector Ltd
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

package im.vector.matrix.android.internal.crypto.crosssigning

import androidx.lifecycle.LiveData
import dagger.Lazy
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.crypto.crosssigning.CrossSigningService
import im.vector.matrix.android.api.session.crypto.crosssigning.MXCrossSigningInfo
import im.vector.matrix.android.api.util.Optional
import im.vector.matrix.android.internal.crypto.DeviceListManager
import im.vector.matrix.android.internal.crypto.MXOlmDevice
import im.vector.matrix.android.internal.crypto.MyDeviceInfoHolder
import im.vector.matrix.android.internal.crypto.model.CryptoCrossSigningKey
import im.vector.matrix.android.internal.crypto.model.KeyUsage
import im.vector.matrix.android.internal.crypto.model.rest.UploadSignatureQueryBuilder
import im.vector.matrix.android.internal.crypto.model.rest.UserPasswordAuth
import im.vector.matrix.android.internal.crypto.store.IMXCryptoStore
import im.vector.matrix.android.internal.crypto.tasks.UploadSignaturesTask
import im.vector.matrix.android.internal.crypto.tasks.UploadSigningKeysTask
import im.vector.matrix.android.internal.di.UserId
import im.vector.matrix.android.internal.session.SessionScope
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.TaskThread
import im.vector.matrix.android.internal.task.configureWith
import im.vector.matrix.android.internal.util.JsonCanonicalizer
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import im.vector.matrix.android.internal.util.withoutPrefix
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.matrix.olm.OlmPkSigning
import org.matrix.olm.OlmUtility
import timber.log.Timber
import javax.inject.Inject

@SessionScope
internal class DefaultCrossSigningService @Inject constructor(
        @UserId private val userId: String,
        private val cryptoStore: IMXCryptoStore,
        private val myDeviceInfoHolder: Lazy<MyDeviceInfoHolder>,
        private val olmDevice: MXOlmDevice,
        private val deviceListManager: DeviceListManager,
        private val uploadSigningKeysTask: UploadSigningKeysTask,
        private val uploadSignaturesTask: UploadSignaturesTask,
        private val computeTrustTask: ComputeTrustTask,
        private val taskExecutor: TaskExecutor,
        private val coroutineDispatchers: MatrixCoroutineDispatchers,
        private val cryptoCoroutineScope: CoroutineScope,
        private val eventBus: EventBus) : CrossSigningService, DeviceListManager.UserDevicesUpdateListener {

    private var olmUtility: OlmUtility? = null

    private var masterPkSigning: OlmPkSigning? = null
    private var userPkSigning: OlmPkSigning? = null
    private var selfSigningPkSigning: OlmPkSigning? = null

    init {
        try {
            olmUtility = OlmUtility()

            // Try to get stored keys if they exist
            cryptoStore.getMyCrossSigningInfo()?.let { mxCrossSigningInfo ->
                Timber.i("## CrossSigning - Found Existing self signed keys")
                Timber.i("## CrossSigning - Checking if private keys are known")

                cryptoStore.getCrossSigningPrivateKeys()?.let { privateKeysInfo ->
                    privateKeysInfo.master
                            ?.fromBase64NoPadding()
                            ?.let { privateKeySeed ->
                                val pkSigning = OlmPkSigning()
                                if (pkSigning.initWithSeed(privateKeySeed) == mxCrossSigningInfo.masterKey()?.unpaddedBase64PublicKey) {
                                    masterPkSigning = pkSigning
                                    Timber.i("## CrossSigning - Loading master key success")
                                } else {
                                    Timber.w("## CrossSigning - Public master key does not match the private key")
                                    // TODO untrust
                                }
                            }
                    privateKeysInfo.user
                            ?.fromBase64NoPadding()
                            ?.let { privateKeySeed ->
                                val pkSigning = OlmPkSigning()
                                if (pkSigning.initWithSeed(privateKeySeed) == mxCrossSigningInfo.userKey()?.unpaddedBase64PublicKey) {
                                    userPkSigning = pkSigning
                                    Timber.i("## CrossSigning - Loading User Signing key success")
                                } else {
                                    Timber.w("## CrossSigning - Public User key does not match the private key")
                                    // TODO untrust
                                }
                            }
                    privateKeysInfo.selfSigned
                            ?.fromBase64NoPadding()
                            ?.let { privateKeySeed ->
                                val pkSigning = OlmPkSigning()
                                if (pkSigning.initWithSeed(privateKeySeed) == mxCrossSigningInfo.selfSigningKey()?.unpaddedBase64PublicKey) {
                                    selfSigningPkSigning = pkSigning
                                    Timber.i("## CrossSigning - Loading Self Signing key success")
                                } else {
                                    Timber.w("## CrossSigning - Public Self Signing key does not match the private key")
                                    // TODO untrust
                                }
                            }
                }

                // Recover local trust in case private key are there?
                setUserKeysAsTrusted(userId, checkUserTrust(userId).isVerified())
            }
        } catch (e: Throwable) {
            // Mmm this kind of a big issue
            Timber.e(e, "Failed to initialize Cross Signing")
        }

        deviceListManager.addListener(this)
    }

    fun release() {
        olmUtility?.releaseUtility()
        listOf(masterPkSigning, userPkSigning, selfSigningPkSigning).forEach { it?.releaseSigning() }
        deviceListManager.removeListener(this)
    }

    protected fun finalize() {
        release()
    }

    /**
     *   - Make 3 key pairs (MSK, USK, SSK)
     *   - Save the private keys with proper security
     *   - Sign the keys and upload them
     *   - Sign the current device with SSK and sign MSK with device key (migration) and upload signatures
     */
    override fun initializeCrossSigning(authParams: UserPasswordAuth?, callback: MatrixCallback<Unit>?) {
        Timber.d("## CrossSigning  initializeCrossSigning")

        // =================
        // MASTER KEY
        // =================
        val masterPkOlm = OlmPkSigning()
        val masterKeyPrivateKey = OlmPkSigning.generateSeed()
        val masterPublicKey = masterPkOlm.initWithSeed(masterKeyPrivateKey)

        Timber.v("## CrossSigning - masterPublicKey:$masterPublicKey")

        // =================
        // USER KEY
        // =================
        val userSigningPkOlm = OlmPkSigning()
        val uskPrivateKey = OlmPkSigning.generateSeed()
        val uskPublicKey = userSigningPkOlm.initWithSeed(uskPrivateKey)

        Timber.v("## CrossSigning - uskPublicKey:$uskPublicKey")

        // Sign userSigningKey with master
        val signedUSK = CryptoCrossSigningKey.Builder(userId, KeyUsage.USER_SIGNING)
                .key(uskPublicKey)
                .build()
                .canonicalSignable()
                .let { masterPkOlm.sign(it) }

        // =================
        // SELF SIGNING KEY
        // =================
        val selfSigningPkOlm = OlmPkSigning()
        val sskPrivateKey = OlmPkSigning.generateSeed()
        val sskPublicKey = selfSigningPkOlm.initWithSeed(sskPrivateKey)

        Timber.v("## CrossSigning - sskPublicKey:$sskPublicKey")

        // Sign userSigningKey with master
        val signedSSK = JsonCanonicalizer.getCanonicalJson(Map::class.java, CryptoCrossSigningKey.Builder(userId, KeyUsage.SELF_SIGNING)
                .key(sskPublicKey)
                .build().signalableJSONDictionary()).let { masterPkOlm.sign(it) }

        // I need to upload the keys
        val mskCrossSigningKeyInfo = CryptoCrossSigningKey.Builder(userId, KeyUsage.MASTER)
                .key(masterPublicKey)
                .build()
        val params = UploadSigningKeysTask.Params(
                masterKey = mskCrossSigningKeyInfo,
                userKey = CryptoCrossSigningKey.Builder(userId, KeyUsage.USER_SIGNING)
                        .key(uskPublicKey)
                        .signature(userId, masterPublicKey, signedUSK)
                        .build(),
                selfSignedKey = CryptoCrossSigningKey.Builder(userId, KeyUsage.SELF_SIGNING)
                        .key(sskPublicKey)
                        .signature(userId, masterPublicKey, signedSSK)
                        .build(),
                userPasswordAuth = authParams
        )

        this.masterPkSigning = masterPkOlm
        this.userPkSigning = userSigningPkOlm
        this.selfSigningPkSigning = selfSigningPkOlm

        val crossSigningInfo = MXCrossSigningInfo(userId, listOf(params.masterKey, params.userKey, params.selfSignedKey))
        cryptoStore.setMyCrossSigningInfo(crossSigningInfo)
        setUserKeysAsTrusted(userId, true)
        cryptoStore.storePrivateKeysInfo(masterKeyPrivateKey?.toBase64NoPadding(), uskPrivateKey?.toBase64NoPadding(), sskPrivateKey?.toBase64NoPadding())

        uploadSigningKeysTask.configureWith(params) {
            this.executionThread = TaskThread.CRYPTO
            this.callback = object : MatrixCallback<Unit> {
                override fun onSuccess(data: Unit) {
                    Timber.i("## CrossSigning - Keys successfully uploaded")

                    //  Sign the current device with SSK
                    val uploadSignatureQueryBuilder = UploadSignatureQueryBuilder()

                    val myDevice = myDeviceInfoHolder.get().myDevice
                    val canonicalJson = JsonCanonicalizer.getCanonicalJson(Map::class.java, myDevice.signalableJSONDictionary())
                    val signedDevice = selfSigningPkOlm.sign(canonicalJson)
                    val updateSignatures = (myDevice.signatures?.toMutableMap() ?: HashMap()).also {
                        it[userId] = (it[userId]
                                ?: HashMap()) + mapOf("ed25519:$sskPublicKey" to signedDevice)
                    }
                    myDevice.copy(signatures = updateSignatures).let {
                        uploadSignatureQueryBuilder.withDeviceInfo(it)
                    }

                    // sign MSK with device key (migration) and upload signatures
                    olmDevice.signMessage(JsonCanonicalizer.getCanonicalJson(Map::class.java, mskCrossSigningKeyInfo.signalableJSONDictionary()))?.let { sign ->
                        val mskUpdatedSignatures = (mskCrossSigningKeyInfo.signatures?.toMutableMap()
                                ?: HashMap()).also {
                            it[userId] = (it[userId]
                                    ?: HashMap()) + mapOf("ed25519:${myDevice.deviceId}" to sign)
                        }
                        mskCrossSigningKeyInfo.copy(
                                signatures = mskUpdatedSignatures
                        ).let {
                            uploadSignatureQueryBuilder.withSigningKeyInfo(it)
                        }
                    }

                    resetTrustOnKeyChange()
                    uploadSignaturesTask.configureWith(UploadSignaturesTask.Params(uploadSignatureQueryBuilder.build())) {
                        // this.retryCount = 3
                        this.executionThread = TaskThread.CRYPTO
                        this.callback = object : MatrixCallback<Unit> {
                            override fun onSuccess(data: Unit) {
                                Timber.i("## CrossSigning - signatures successfully uploaded")
                                callback?.onSuccess(Unit)
                            }

                            override fun onFailure(failure: Throwable) {
                                // Clear
                                Timber.e(failure, "## CrossSigning - Failed to upload signatures")
                                clearSigningKeys()
                            }
                        }
                    }.executeBy(taskExecutor)
                }

                override fun onFailure(failure: Throwable) {
                    Timber.e(failure, "## CrossSigning - Failed to upload signing keys")
                    clearSigningKeys()
                    callback?.onFailure(failure)
                }
            }
        }.executeBy(taskExecutor)
    }

    private fun clearSigningKeys() {
        masterPkSigning?.releaseSigning()
        userPkSigning?.releaseSigning()
        selfSigningPkSigning?.releaseSigning()

        masterPkSigning = null
        userPkSigning = null
        selfSigningPkSigning = null

        cryptoStore.setMyCrossSigningInfo(null)
        cryptoStore.storePrivateKeysInfo(null, null, null)
    }

    private fun resetTrustOnKeyChange() {
        Timber.i("## CrossSigning - Clear all other user trust")
        cryptoStore.clearOtherUserTrust()
    }

    override fun checkTrustFromPrivateKeys(masterKeyPrivateKey: String?, uskKeyPrivateKey: String?, sskPrivateKey: String?, callback: MatrixCallback<Unit>?): UserTrustResult {
        val mxCrossSigningInfo = getMyCrossSigningKeys() ?: return UserTrustResult.CrossSigningNotConfigured(userId)

        var masterKeyIsTrusted = false
        var userKeyIsTrusted = false
        var selfSignedKeyIsTrusted = false

        masterKeyPrivateKey?.fromBase64NoPadding()
                ?.let { privateKeySeed ->
                    val pkSigning = OlmPkSigning()
                    try {
                        if (pkSigning.initWithSeed(privateKeySeed) == mxCrossSigningInfo.masterKey()?.unpaddedBase64PublicKey) {
                            masterPkSigning?.releaseSigning()
                            masterPkSigning = pkSigning
                            masterKeyIsTrusted = true
                            Timber.i("## CrossSigning - Loading master key success")
                        } else {
                            pkSigning.releaseSigning()
                        }
                    } catch (failure: Throwable) {
                        pkSigning.releaseSigning()
                    }
                }

        uskKeyPrivateKey?.fromBase64NoPadding()
                ?.let { privateKeySeed ->
                    val pkSigning = OlmPkSigning()
                    try {
                        if (pkSigning.initWithSeed(privateKeySeed) == mxCrossSigningInfo.userKey()?.unpaddedBase64PublicKey) {
                            userPkSigning?.releaseSigning()
                            userPkSigning = pkSigning
                            userKeyIsTrusted = true
                            Timber.i("## CrossSigning - Loading master key success")
                        } else {
                            pkSigning.releaseSigning()
                        }
                    } catch (failure: Throwable) {
                        pkSigning.releaseSigning()
                    }
                }

        sskPrivateKey?.fromBase64NoPadding()
                ?.let { privateKeySeed ->
                    val pkSigning = OlmPkSigning()
                    try {
                        if (pkSigning.initWithSeed(privateKeySeed) == mxCrossSigningInfo.selfSigningKey()?.unpaddedBase64PublicKey) {
                            selfSigningPkSigning?.releaseSigning()
                            selfSigningPkSigning = pkSigning
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
            return UserTrustResult.KeysNotTrusted(mxCrossSigningInfo)
        } else {
            cryptoStore.markMyMasterKeyAsLocallyTrusted(true)
            val checkSelfTrust = checkSelfTrust()
            if (checkSelfTrust.isVerified()) {
                cryptoStore.storePrivateKeysInfo(masterKeyPrivateKey, uskKeyPrivateKey, sskPrivateKey)
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
     */
    override fun isUserTrusted(otherUserId: String): Boolean {
        return cryptoStore.getCrossSigningInfo(userId)?.isTrusted() == true
    }

    override fun isCrossSigningVerified(): Boolean {
        return checkSelfTrust().isVerified()
    }

    /**
     * Will not force a download of the key, but will verify signatures trust chain
     */
    override fun checkUserTrust(otherUserId: String): UserTrustResult {
        Timber.d("## CrossSigning  checkUserTrust for $otherUserId")
        if (otherUserId == userId) {
            return checkSelfTrust()
        }
        // I trust a user if I trust his master key
        // I can trust the master key if it is signed by my user key
        // TODO what if the master key is signed by a device key that i have verified

        // First let's get my user key
        val myCrossSigningInfo = cryptoStore.getCrossSigningInfo(userId)

        val myUserKey = myCrossSigningInfo?.userKey()
                ?: return UserTrustResult.CrossSigningNotConfigured(userId)

        if (!myCrossSigningInfo.isTrusted()) {
            return UserTrustResult.KeysNotTrusted(myCrossSigningInfo)
        }

        // Let's get the other user  master key
        val otherMasterKey = cryptoStore.getCrossSigningInfo(otherUserId)?.masterKey()
                ?: return UserTrustResult.UnknownCrossSignatureInfo(otherUserId)

        val masterKeySignaturesMadeByMyUserKey = otherMasterKey.signatures
                ?.get(userId) // Signatures made by me
                ?.get("ed25519:${myUserKey.unpaddedBase64PublicKey}")

        if (masterKeySignaturesMadeByMyUserKey.isNullOrBlank()) {
            Timber.d("## CrossSigning  checkUserTrust false for $otherUserId, not signed by my UserSigningKey")
            return UserTrustResult.KeyNotSigned(otherMasterKey)
        }

        // Check that Alice USK signature of Bob MSK is valid
        try {
            olmUtility!!.verifyEd25519Signature(masterKeySignaturesMadeByMyUserKey, myUserKey.unpaddedBase64PublicKey, otherMasterKey.canonicalSignable())
        } catch (failure: Throwable) {
            return UserTrustResult.InvalidSignature(myUserKey, masterKeySignaturesMadeByMyUserKey)
        }

        return UserTrustResult.Success
    }

    private fun checkSelfTrust(): UserTrustResult {
        // Special case when it's me,
        // I have to check that MSK -> USK -> SSK
        // and that MSK is trusted (i know the private key, or is signed by a trusted device)
        val myCrossSigningInfo = cryptoStore.getCrossSigningInfo(userId)

        val myMasterKey = myCrossSigningInfo?.masterKey()
                ?: return UserTrustResult.CrossSigningNotConfigured(userId)

        // Is the master key trusted
        // 1) check if I know the private key
        val masterPrivateKey = cryptoStore.getCrossSigningPrivateKeys()
                ?.master
                ?.fromBase64NoPadding()

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
            myMasterKey.signatures?.get(userId)?.forEach { (key, value) ->
                val potentialDeviceId = key.withoutPrefix("ed25519:")
                val potentialDevice = cryptoStore.getUserDevice(userId, potentialDeviceId)
                if (potentialDevice != null && potentialDevice.isVerified) {
                    // Check signature validity?
                    try {
                        olmUtility?.verifyEd25519Signature(value, potentialDevice.fingerprint(), myMasterKey.canonicalSignable())
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
            return UserTrustResult.KeysNotTrusted(myCrossSigningInfo)
        }

        val myUserKey = myCrossSigningInfo.userKey()
                ?: return UserTrustResult.CrossSigningNotConfigured(userId)

        val userKeySignaturesMadeByMyMasterKey = myUserKey.signatures
                ?.get(userId) // Signatures made by me
                ?.get("ed25519:${myMasterKey.unpaddedBase64PublicKey}")

        if (userKeySignaturesMadeByMyMasterKey.isNullOrBlank()) {
            Timber.d("## CrossSigning  checkUserTrust false for $userId, USK not signed by MSK")
            return UserTrustResult.KeyNotSigned(myUserKey)
        }

        // Check that Alice USK signature of Alice MSK is valid
        try {
            olmUtility!!.verifyEd25519Signature(userKeySignaturesMadeByMyMasterKey, myMasterKey.unpaddedBase64PublicKey, myUserKey.canonicalSignable())
        } catch (failure: Throwable) {
            return UserTrustResult.InvalidSignature(myUserKey, userKeySignaturesMadeByMyMasterKey)
        }

        val mySSKey = myCrossSigningInfo.selfSigningKey()
                ?: return UserTrustResult.CrossSigningNotConfigured(userId)

        val ssKeySignaturesMadeByMyMasterKey = mySSKey.signatures
                ?.get(userId) // Signatures made by me
                ?.get("ed25519:${myMasterKey.unpaddedBase64PublicKey}")

        if (ssKeySignaturesMadeByMyMasterKey.isNullOrBlank()) {
            Timber.d("## CrossSigning  checkUserTrust false for $userId, SSK not signed by MSK")
            return UserTrustResult.KeyNotSigned(mySSKey)
        }

        // Check that Alice USK signature of Alice MSK is valid
        try {
            olmUtility!!.verifyEd25519Signature(ssKeySignaturesMadeByMyMasterKey, myMasterKey.unpaddedBase64PublicKey, mySSKey.canonicalSignable())
        } catch (failure: Throwable) {
            return UserTrustResult.InvalidSignature(mySSKey, ssKeySignaturesMadeByMyMasterKey)
        }

        return UserTrustResult.Success
    }

    override fun getUserCrossSigningKeys(otherUserId: String): MXCrossSigningInfo? {
        return cryptoStore.getCrossSigningInfo(otherUserId)
    }

    override fun getLiveCrossSigningKeys(userId: String): LiveData<Optional<MXCrossSigningInfo>> {
        return cryptoStore.getLiveCrossSigningInfo(userId)
    }

    override fun getMyCrossSigningKeys(): MXCrossSigningInfo? {
        return cryptoStore.getMyCrossSigningInfo()
    }

    override fun canCrossSign(): Boolean {
        return checkSelfTrust().isVerified() && cryptoStore.getCrossSigningPrivateKeys()?.selfSigned != null
    }

    override fun trustUser(otherUserId: String, callback: MatrixCallback<Unit>) {
        Timber.d("## CrossSigning - Mark user $userId as trusted ")
        // We should have this user keys
        val otherMasterKeys = getUserCrossSigningKeys(otherUserId)?.masterKey()
        if (otherMasterKeys == null) {
            callback.onFailure(Throwable("## CrossSigning - Other master signing key is not known"))
            return
        }
        val myKeys = getUserCrossSigningKeys(userId)
        if (myKeys == null) {
            callback.onFailure(Throwable("## CrossSigning - CrossSigning is not setup for this account"))
            return
        }
        val userPubKey = myKeys.userKey()?.unpaddedBase64PublicKey
        if (userPubKey == null || userPkSigning == null) {
            callback.onFailure(Throwable("## CrossSigning - Cannot sign from this account, privateKeyUnknown $userPubKey"))
            return
        }

        // Sign the other MasterKey with our UserSigning key
        val newSignature = JsonCanonicalizer.getCanonicalJson(Map::class.java,
                otherMasterKeys.signalableJSONDictionary()).let { userPkSigning?.sign(it) }

        if (newSignature == null) {
            // race??
            callback.onFailure(Throwable("## CrossSigning - Failed to sign"))
            return
        }

        cryptoStore.setUserKeysAsTrusted(otherUserId, true)
        // TODO update local copy with new signature directly here? kind of local echo of trust?

        Timber.d("## CrossSigning - Upload signature of $userId MSK signed by USK")
        val uploadQuery = UploadSignatureQueryBuilder()
                .withSigningKeyInfo(otherMasterKeys.copyForSignature(userId, userPubKey, newSignature))
                .build()
        uploadSignaturesTask.configureWith(UploadSignaturesTask.Params(uploadQuery)) {
            this.executionThread = TaskThread.CRYPTO
            this.callback = callback
        }.executeBy(taskExecutor)
    }

    override fun markMyMasterKeyAsTrusted() {
        cryptoStore.markMyMasterKeyAsLocallyTrusted(true)
        checkSelfTrust()
    }

    override fun signDevice(deviceId: String, callback: MatrixCallback<Unit>) {
        // This device should be yours
        val device = cryptoStore.getUserDevice(userId, deviceId)
        if (device == null) {
            callback.onFailure(IllegalArgumentException("This device [$deviceId] is not known, or not yours"))
            return
        }

        val myKeys = getUserCrossSigningKeys(userId)
        if (myKeys == null) {
            callback.onFailure(Throwable("CrossSigning is not setup for this account"))
            return
        }

        val ssPubKey = myKeys.selfSigningKey()?.unpaddedBase64PublicKey
        if (ssPubKey == null || selfSigningPkSigning == null) {
            callback.onFailure(Throwable("Cannot sign from this account, public and/or privateKey Unknown $ssPubKey"))
            return
        }

        // Sign with self signing
        val newSignature = selfSigningPkSigning?.sign(device.canonicalSignable())

        if (newSignature == null) {
            // race??
            callback.onFailure(Throwable("Failed to sign"))
            return
        }
        val toUpload = device.copy(
                signatures = mapOf(
                        userId
                                to
                                mapOf(
                                        "ed25519:$ssPubKey" to newSignature
                                )
                )
        )

        val uploadQuery = UploadSignatureQueryBuilder()
                .withDeviceInfo(toUpload)
                .build()
        uploadSignaturesTask.configureWith(UploadSignaturesTask.Params(uploadQuery)) {
            this.executionThread = TaskThread.CRYPTO
            this.callback = callback
        }.executeBy(taskExecutor)
    }

    override fun checkDeviceTrust(otherUserId: String, otherDeviceId: String, locallyTrusted: Boolean?): DeviceTrustResult {
        val otherDevice = cryptoStore.getUserDevice(otherUserId, otherDeviceId)
                ?: return DeviceTrustResult.UnknownDevice(otherDeviceId)

        val myKeys = getUserCrossSigningKeys(userId)
                ?: return legacyFallbackTrust(locallyTrusted, DeviceTrustResult.CrossSigningNotConfigured(userId))

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
                        DeviceTrustResult.MissingDeviceSignature(otherDeviceId, otherKeys.selfSigningKey()
                                ?.unpaddedBase64PublicKey
                                ?: ""
                        )
                )

        // Check  bob's device is signed by bob's SSK
        try {
            olmUtility!!.verifyEd25519Signature(otherSSKSignature, otherKeys.selfSigningKey()?.unpaddedBase64PublicKey, otherDevice.canonicalSignable())
        } catch (e: Throwable) {
            return legacyFallbackTrust(locallyTrusted, DeviceTrustResult.InvalidDeviceSignature(otherDeviceId, otherSSKSignature, e))
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
        cryptoCoroutineScope.launch(coroutineDispatchers.crypto) {
            Timber.d("## CrossSigning - onUsersDeviceUpdate for ${userIds.size} users")
            userIds.forEach { otherUserId ->
                checkUserTrust(otherUserId).let {
                    Timber.d("## CrossSigning - update trust for $otherUserId , verified=${it.isVerified()}")
                    setUserKeysAsTrusted(otherUserId, it.isVerified())
                }

                // TODO if my keys have changes, i should recheck all devices of all users?
                val devices = cryptoStore.getUserDeviceList(otherUserId)
                devices?.forEach { device ->
                    val updatedTrust = checkDeviceTrust(otherUserId, device.deviceId, device.trustLevel?.isLocallyVerified() ?: false)
                    Timber.d("## CrossSigning - update trust for device ${device.deviceId} of user $otherUserId , verified=$updatedTrust")
                    cryptoStore.setDeviceTrust(otherUserId, device.deviceId, updatedTrust.isCrossSignedVerified(), updatedTrust.isLocallyVerified())
                }

                if (otherUserId == userId) {
                    // It's me, i should check if a newly trusted device is signing my master key
                    // In this case it will change my MSK trust, and should then re-trigger a check of all other user trust
                    setUserKeysAsTrusted(otherUserId, checkSelfTrust().isVerified())
                }
            }

            eventBus.post(CryptoToSessionUserTrustChange(userIds))
        }
    }

    private fun setUserKeysAsTrusted(otherUserId: String, trusted: Boolean) {
        cryptoCoroutineScope.launch(coroutineDispatchers.crypto) {
            val currentTrust = cryptoStore.getCrossSigningInfo(otherUserId)?.isTrusted()
            cryptoStore.setUserKeysAsTrusted(otherUserId, trusted)
            // If it's me, recheck trust of all users and devices?
            val users = ArrayList<String>()
            if (otherUserId == userId && currentTrust != trusted) {
                cryptoStore.updateUsersTrust {
                    users.add(it)
                    checkUserTrust(it).isVerified()
                }

                users.forEach {
                    cryptoStore.getUserDeviceList(it)?.forEach { device ->
                        val updatedTrust = checkDeviceTrust(it, device.deviceId, device.trustLevel?.isLocallyVerified() ?: false)
                        Timber.d("## CrossSigning - update trust for device ${device.deviceId} of user $otherUserId , verified=$updatedTrust")
                        cryptoStore.setDeviceTrust(it, device.deviceId, updatedTrust.isCrossSignedVerified(), updatedTrust.isLocallyVerified())
                    }
                }
            }
        }
    }
}
