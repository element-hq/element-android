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

import android.util.Base64
import dagger.Lazy
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.api.session.crypto.crosssigning.CrossSigningService
import im.vector.matrix.android.api.session.crypto.crosssigning.CrossSigningState
import im.vector.matrix.android.api.session.crypto.crosssigning.MXCrossSigningInfo
import im.vector.matrix.android.internal.crypto.DeviceListManager
import im.vector.matrix.android.internal.crypto.MXOlmDevice
import im.vector.matrix.android.internal.crypto.MyDeviceInfoHolder
import im.vector.matrix.android.internal.crypto.model.MXDeviceInfo
import im.vector.matrix.android.internal.crypto.model.rest.*
import im.vector.matrix.android.internal.crypto.store.IMXCryptoStore
import im.vector.matrix.android.internal.crypto.tasks.UploadSignaturesTask
import im.vector.matrix.android.internal.crypto.tasks.UploadSigningKeysTask
import im.vector.matrix.android.internal.di.MoshiProvider
import im.vector.matrix.android.internal.di.UserId
import im.vector.matrix.android.internal.task.TaskConstraints
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.configureWith
import im.vector.matrix.android.internal.util.JsonCanonicalizer
import org.matrix.olm.OlmPkSigning
import org.matrix.olm.OlmUtility
import timber.log.Timber
import javax.inject.Inject

internal class DefaultCrossSigningService @Inject constructor(
        @UserId private val userId: String,
        private val credentials: Credentials,
        private val cryptoStore: IMXCryptoStore,
        private val myDeviceInfoHolder: Lazy<MyDeviceInfoHolder>,
        private val olmDevice: MXOlmDevice,
        private val deviceListManager: DeviceListManager,
        private val uploadSigningKeysTask: UploadSigningKeysTask,
        private val uploadSignaturesTask: UploadSignaturesTask,
        private val taskExecutor: TaskExecutor) : CrossSigningService {


    private var olmUtility: OlmUtility? = null

    private var crossSigningState: CrossSigningState = CrossSigningState.Unknown

    private var masterPkSigning: OlmPkSigning? = null
    private var userPkSigning: OlmPkSigning? = null
    private var selfSigningPkSigning: OlmPkSigning? = null

    init {
        try {
            olmUtility = OlmUtility()

            //Try to get stored keys if they exist
            cryptoStore.getMyCrossSigningInfo()?.let { mxCrossSigningInfo ->
                Timber.i("## CrossSigning - Found Existing self signed keys")
                Timber.i("## CrossSigning - Checking if private keys are known")

                cryptoStore.getCrossSigningPrivateKeys()?.let { privateKeyinfo ->
                    privateKeyinfo.master?.let { privateKey ->
                        val keySeed = Base64.decode(privateKey, Base64.NO_PADDING)
                        val pkSigning = OlmPkSigning()
                        if (pkSigning.initWithSeed(keySeed) == mxCrossSigningInfo.masterKey()?.unpaddedBase64PublicKey) {
                            masterPkSigning = pkSigning
                            Timber.i("## CrossSigning - Loading master key success")
                        } else {
                            Timber.w("## CrossSigning - Public master key does not match the private key")
                            // TODO untrust
                        }
                    }
                    privateKeyinfo.user?.let { privateKey ->
                        val keySeed = Base64.decode(privateKey, Base64.NO_PADDING)
                        val pkSigning = OlmPkSigning()
                        if (pkSigning.initWithSeed(keySeed) == mxCrossSigningInfo.userKey()?.unpaddedBase64PublicKey) {
                            userPkSigning = pkSigning
                            Timber.i("## CrossSigning - Loading User Signing key success")
                        } else {
                            Timber.w("## CrossSigning - Public User key does not match the private key")
                            // TODO untrust
                        }
                    }
                    privateKeyinfo.selfSigned?.let { privateKey ->
                        val keySeed = Base64.decode(privateKey, Base64.NO_PADDING)
                        val pkSigning = OlmPkSigning()
                        if (pkSigning.initWithSeed(keySeed) == mxCrossSigningInfo.selfSigningKey()?.unpaddedBase64PublicKey) {
                            selfSigningPkSigning = pkSigning
                            Timber.i("## CrossSigning - Loading Self Signing key success")
                        } else {
                            Timber.w("## CrossSigning - Public Self Signing key does not match the private key")
                            // TODO untrust
                        }
                    }
                }

            }
        } catch (e: Throwable) {
            // Mmm this kind of a big issue
            Timber.e(e, "Failed to initialize Cross Signing")
        }
    }


    fun release() {
        olmUtility?.releaseUtility()
        listOf(masterPkSigning, userPkSigning, selfSigningPkSigning).forEach { it?.releaseSigning() }
    }


    /**
     *   - Make 3 key pairs (MSK, USK, SSK)
     *   - Save the private keys with proper security
     *   - Sign the keys and upload them
     *   - Sign the current device with SSK and sign MSK with device key (migration) and upload signatures
     */
    override fun initializeCrossSigning(authParams: UserPasswordAuth?, callback: MatrixCallback<Unit>?) {
        Timber.d("## CrossSigning  initializeCrossSigning")
        // TODO sync that
        crossSigningState = CrossSigningState.Enabling

        val myUserID = credentials.userId

        //=================
        // MASTER KEY
        //=================
        val masterPkOlm = OlmPkSigning()
        val masterKeyPrivateKey = OlmPkSigning.generateSeed()
        val masterPublicKey = masterPkOlm.initWithSeed(masterKeyPrivateKey)

        Timber.v("## CrossSigning - masterPublicKey:$masterPublicKey")

        //=================
        // USER KEY
        //=================
        val userSigningPkOlm = OlmPkSigning()
        val uskPrivateKey = OlmPkSigning.generateSeed()
        val uskPublicKey = userSigningPkOlm.initWithSeed(uskPrivateKey)

        Timber.v("## CrossSigning - uskPublicKey:$uskPublicKey")

        // Sign userSigningKey with master
        val signedUSK = JsonCanonicalizer.getCanonicalJson(Map::class.java, CrossSigningKeyInfo.Builder(myUserID, CrossSigningKeyInfo.KeyUsage.USER_SIGNING)
                .key(uskPublicKey)
                .build().signalableJSONDictionary()).let { masterPkOlm.sign(it) }

        //=================
        // SELF SIGNING KEY
        //=================
        val selfSigningPkOlm = OlmPkSigning()
        val sskPrivateKey = OlmPkSigning.generateSeed()
        val sskPublicKey = selfSigningPkOlm.initWithSeed(sskPrivateKey)

        Timber.v("## CrossSigning - sskPublicKey:$sskPublicKey")


        // Sign userSigningKey with master
        val signedSSK = JsonCanonicalizer.getCanonicalJson(Map::class.java, CrossSigningKeyInfo.Builder(myUserID, CrossSigningKeyInfo.KeyUsage.SELF_SIGNING)
                .key(sskPublicKey)
                .build().signalableJSONDictionary()).let { masterPkOlm.sign(it) }


        // I need to upload the keys
        val mskCrossSigningKeyInfo = CrossSigningKeyInfo.Builder(myUserID, CrossSigningKeyInfo.KeyUsage.MASTER)
                .key(masterPublicKey)
                .build()
        val params = UploadSigningKeysTask.Params(
                masterKey = mskCrossSigningKeyInfo,
                userKey = CrossSigningKeyInfo.Builder(myUserID, CrossSigningKeyInfo.KeyUsage.USER_SIGNING)
                        .key(uskPublicKey)
                        .signature(myUserID, masterPublicKey, signedUSK)
                        .build(),
                selfSignedKey = CrossSigningKeyInfo.Builder(myUserID, CrossSigningKeyInfo.KeyUsage.SELF_SIGNING)
                        .key(sskPublicKey)
                        .signature(myUserID, masterPublicKey, signedSSK)
                        .build(),
                userPasswordAuth = authParams
        )

        this.masterPkSigning = masterPkOlm
        this.userPkSigning = userSigningPkOlm
        this.selfSigningPkSigning = selfSigningPkOlm

        val crossSigningInfo = MXCrossSigningInfo(myUserID, listOf(params.masterKey, params.userKey, params.selfSignedKey))
        cryptoStore.setMyCrossSigningInfo(crossSigningInfo)
        cryptoStore.setUserKeysAsTrusted(myUserID)

        // TODO we should ensure that they are sent
        // TODO error handling?
        uploadSigningKeysTask.configureWith(params) {
            this.retryCount = 3
            this.constraints = TaskConstraints(true)
            this.callback = object : MatrixCallback<KeysQueryResponse> {
                override fun onSuccess(data: KeysQueryResponse) {
                    Timber.i("## CrossSigning - Keys succesfully uploaded")

                    //  Sign the current device with SSK
                    val uploadSignatureQueryBuilder = UploadSignatureQueryBuilder()

                    val myDevice = myDeviceInfoHolder.get().myDevice
                    val canonicalJson = JsonCanonicalizer.getCanonicalJson(Map::class.java, myDevice.signalableJSONDictionary())
                    val signedDevice = selfSigningPkOlm.sign(canonicalJson)
                    val updateSignatures = (myDevice.signatures?.toMutableMap() ?: HashMap()).also {
                        it[myUserID] = (it[myUserID]
                                ?: HashMap()) + mapOf("ed25519:$sskPublicKey" to signedDevice)
                    }
                    myDevice.copy(signatures = updateSignatures).let {
                        uploadSignatureQueryBuilder.withDeviceInfo(it)
                    }

                    // sign MSK with device key (migration) and upload signatures
                    olmDevice.signMessage(JsonCanonicalizer.getCanonicalJson(Map::class.java, mskCrossSigningKeyInfo.signalableJSONDictionary()))?.let { sign ->
                        val mskUpdatedSignatures = (mskCrossSigningKeyInfo.signatures?.toMutableMap()
                                ?: HashMap()).also {
                            it[myUserID] = (it[myUserID]
                                    ?: HashMap()) + mapOf("ed25519:${myDevice.deviceId}" to sign)
                        }
                        mskCrossSigningKeyInfo.copy(
                                signatures = mskUpdatedSignatures
                        ).let {
                            uploadSignatureQueryBuilder.withSigningKeyInfo(it)
                        }
                    }

                    uploadSignaturesTask.configureWith(UploadSignaturesTask.Params(uploadSignatureQueryBuilder.build())) {
                        this.retryCount = 3
                        this.constraints = TaskConstraints(true)
                        this.callback = object : MatrixCallback<SignatureUploadResponse> {
                            override fun onSuccess(data: SignatureUploadResponse) {
                                Timber.i("## CrossSigning - signatures succesfuly uploaded")
                            }

                            override fun onFailure(failure: Throwable) {
                                Timber.e(failure, "## CrossSigning - Failed to upload signatures")
                            }
                        }
                    }.executeBy(taskExecutor)


                    callback?.onSuccess(Unit)
                    crossSigningState = CrossSigningState.Trusted
                }

                override fun onFailure(failure: Throwable) {
                    Timber.e(failure, "## CrossSigning - Failed to upload signing keys")
                    callback?.onFailure(failure)
                }
            }
        }.executeBy(taskExecutor)


    }

    /**
     *
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
    override fun isUserTrusted(userId: String): Boolean {
        return cryptoStore.getCrossSigningInfo(userId)?.isTrusted == true
    }

    /**
     * Will not force a download of the key, but will verify signatures trust chain
     */
    override fun checkUserTrust(userId: String, callback: MatrixCallback<Boolean>?) {
        Timber.d("## CrossSigning  checkUserTrust for $userId")
        // I trust a user if I trust his master key
        // I can trust the master key if it is signed by my user key
        // TODO what if the master key is signed by a device key that i have verified

        // First let's get my user key
        val myUserKey = cryptoStore.getCrossSigningInfo(credentials.userId)?.userKey()
        if (myUserKey == null) {
            Timber.d("## CrossSigning checkUserTrust false, CrossSigning is not enabled (userKey not defined)")
            callback?.onSuccess(false)
            return
        }

        // Let's get the other user  master key
        val masterKey = cryptoStore.getCrossSigningInfo(userId)?.masterKey()
        if (masterKey == null) {
            Timber.d("## CrossSigning  checkUserTrust false for $userId, ")
            callback?.onSuccess(false)
            return
        }

        val masterKeySignaturesMadeByMyUserKey = masterKey.signatures
                ?.get(credentials.userId) // Signatures made by me
                ?.get("ed25519:${myUserKey.unpaddedBase64PublicKey}")


        if (masterKeySignaturesMadeByMyUserKey.isNullOrBlank()) {
            Timber.d("## CrossSigning  checkUserTrust false for $userId, not signed by my UserSigningKey")
            callback?.onSuccess(false)
            return
        }

//        olmUtility?.verifyEd25519Signature(masterKeySignaturesMadeByMyUserKey,
//                myUserKey.publicKeyBase64,
//                masterKey.publicKeyBase64)

    }

    override fun getUserCrossSigningKeys(userId: String): MXCrossSigningInfo? {
        return cryptoStore.getCrossSigningInfo(userId)
    }

    override fun getMyCrossSigningKeys(): MXCrossSigningInfo? {
        return cryptoStore.getMyCrossSigningInfo()
    }

    override fun trustUser(userId: String, callback: MatrixCallback<SignatureUploadResponse>) {
        //We should have this user keys
        val otherMasterKeys = getUserCrossSigningKeys(userId)?.masterKey()
        if (otherMasterKeys == null) {
            callback.onFailure(Throwable("Other master signing key is not known"))
            return
        }
        val myKeys = getUserCrossSigningKeys(credentials.userId)
        if (myKeys == null) {
            callback.onFailure(Throwable("CrossSigning is not setup for this account"))
            return
        }
        val userPubKey = myKeys.userKey()?.unpaddedBase64PublicKey
        if (userPubKey == null || userPkSigning == null) {
            callback.onFailure(Throwable("Cannot sign from this account, privateKeyUnknown $userPubKey"))
            return
        }

        // Sign the other MasterKey with our UserSiging key
        val newSignature = JsonCanonicalizer.getCanonicalJson(Map::class.java,
                otherMasterKeys.signalableJSONDictionary()).let { userPkSigning?.sign(it) }

        if (newSignature == null) {
            // race??
            callback.onFailure(Throwable("Failed to sign"))
            return
        }

        otherMasterKeys.addSignature(credentials.userId, userPubKey, newSignature)
        cryptoStore.setUserKeysAsTrusted(userId, true)
        // TODO update local copy with new signature directly here? kind of local echo of trust?

        val uploadQuery = UploadSignatureQueryBuilder()
                .withSigningKeyInfo(otherMasterKeys)
                .build()
        uploadSignaturesTask.configureWith(UploadSignaturesTask.Params(uploadQuery)) {
            this.callback = callback
        }.executeBy(taskExecutor)

    }


    override fun signDevice(deviceId: String, callback: MatrixCallback<SignatureUploadResponse>) {
        // This device should be yours
        val device = cryptoStore.getUserDevice(credentials.userId, deviceId)
        if (device == null) {
            callback.onFailure(IllegalArgumentException("This device  [$deviceId] is not known, or not yours"))
            return
        }

        val myKeys = getUserCrossSigningKeys(credentials.userId)
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
//        val newSignature = JsonCanonicalizer.getCanonicalJson(Map::class.java, device.signalableJSONDictionary()).let { userPkSigning?.sign(it) }
        val newSignature = selfSigningPkSigning?.sign(device.canonicalSignable())

        if (newSignature == null) {
            // race??
            callback.onFailure(Throwable("Failed to sign"))
            return
        }
        val toUpload = device.copy(
                signatures = mapOf(
                        credentials.userId
                                to
                                mapOf(
                                        "ed25519:${ssPubKey}" to newSignature
                                )
                )
        )
//        device.addSignature(credentials.userId, ssPubKey, newSignature)

        val uploadQuery = UploadSignatureQueryBuilder()
                .withDeviceInfo(toUpload)
                .build()
        uploadSignaturesTask.configureWith(UploadSignaturesTask.Params(uploadQuery)) {
            this.callback = object : MatrixCallback<SignatureUploadResponse> {
                override fun onFailure(failure: Throwable) {
                    callback.onFailure(failure)
                }

                override fun onSuccess(data: SignatureUploadResponse) {
                    val watchedFailure = data.failures?.get(userId)?.get(deviceId)
                    if (watchedFailure == null) {
                        callback.onSuccess(data)
                    } else {
                        val failure = MoshiProvider.providesMoshi().adapter(UploadResponseFailure::class.java).fromJson(watchedFailure.toString())?.message
                                ?: watchedFailure.toString()
                        callback.onFailure(Throwable(failure))
                    }
                }
            }
        }.executeBy(taskExecutor)
    }

    override fun checkDeviceTrust(userId: String, deviceId: String, callback: MatrixCallback<Unit>) {
        val otherDevice = cryptoStore.getUserDevice(userId, deviceId)
        if (otherDevice == null) {
            callback.onFailure(IllegalArgumentException("This device is not known, or not yours"))
            return
        }

        val myKeys = getUserCrossSigningKeys(credentials.userId)
        if (myKeys == null) {
            callback.onFailure(Throwable("CrossSigning is not setup for this account"))
            return
        }

        val otherKeys = getUserCrossSigningKeys(userId)
        if (otherKeys == null) {
            callback.onFailure(Throwable("CrossSigning is not setup for $userId"))
            return
        }

        // TODO should we force verification ?
        if (!otherKeys.isTrusted) {
            callback.onFailure(Throwable("$userId is not trusted"))
            return
        }

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

        val otherSSKSignature = otherDevice.signatures?.get(userId)?.get("ed25519:${otherKeys.selfSigningKey()?.unpaddedBase64PublicKey}")

        if (otherSSKSignature == null) {
            callback.onFailure(Throwable("Device ${otherDevice.deviceId} is not signed by $userId self signed key"))
            return
        }


        // Check  bob's device is signed by bob's SSK
        try {
            olmUtility?.verifyEd25519Signature(otherSSKSignature, otherKeys.selfSigningKey()?.unpaddedBase64PublicKey, otherDevice.canonicalSignable())
        } catch (e: Throwable) {
            callback.onFailure(Throwable("Invalid self signed signature for Device ${otherDevice.deviceId}"))
        }

        callback.onSuccess(Unit)

    }
}

fun MXDeviceInfo.canonicalSignable(): String {
    return JsonCanonicalizer.getCanonicalJson(Map::class.java, signalableJSONDictionary())
}

