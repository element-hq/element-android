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

package org.matrix.android.sdk.internal.crypto.store.db

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.zhuinden.monarchy.Monarchy
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.Sort
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import org.matrix.android.sdk.api.crypto.MXCRYPTO_ALGORITHM_MEGOLM
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.logger.LoggerTag
import org.matrix.android.sdk.api.session.crypto.NewSessionListener
import org.matrix.android.sdk.api.session.crypto.OutgoingKeyRequest
import org.matrix.android.sdk.api.session.crypto.OutgoingRoomKeyRequestState
import org.matrix.android.sdk.api.session.crypto.crosssigning.CryptoCrossSigningKey
import org.matrix.android.sdk.api.session.crypto.crosssigning.MXCrossSigningInfo
import org.matrix.android.sdk.api.session.crypto.crosssigning.PrivateKeysInfo
import org.matrix.android.sdk.api.session.crypto.keysbackup.SavedKeyBackupKeyInfo
import org.matrix.android.sdk.api.session.crypto.model.AuditTrail
import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.api.session.crypto.model.DeviceInfo
import org.matrix.android.sdk.api.session.crypto.model.ForwardInfo
import org.matrix.android.sdk.api.session.crypto.model.IncomingKeyRequestInfo
import org.matrix.android.sdk.api.session.crypto.model.MXUsersDevicesMap
import org.matrix.android.sdk.api.session.crypto.model.RoomKeyRequestBody
import org.matrix.android.sdk.api.session.crypto.model.TrailType
import org.matrix.android.sdk.api.session.crypto.model.WithheldInfo
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.content.RoomKeyWithHeldContent
import org.matrix.android.sdk.api.session.events.model.content.WithHeldCode
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.api.util.toOptional
import org.matrix.android.sdk.internal.crypto.model.MXInboundMegolmSessionWrapper
import org.matrix.android.sdk.internal.crypto.model.OlmSessionWrapper
import org.matrix.android.sdk.internal.crypto.model.OutboundGroupSessionWrapper
import org.matrix.android.sdk.internal.crypto.store.IMXCryptoStore
import org.matrix.android.sdk.internal.crypto.store.db.mapper.CrossSigningKeysMapper
import org.matrix.android.sdk.internal.crypto.store.db.model.AuditTrailEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.AuditTrailEntityFields
import org.matrix.android.sdk.internal.crypto.store.db.model.AuditTrailMapper
import org.matrix.android.sdk.internal.crypto.store.db.model.CrossSigningInfoEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.CrossSigningInfoEntityFields
import org.matrix.android.sdk.internal.crypto.store.db.model.CryptoMapper
import org.matrix.android.sdk.internal.crypto.store.db.model.CryptoMetadataEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.CryptoRoomEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.CryptoRoomEntityFields
import org.matrix.android.sdk.internal.crypto.store.db.model.DeviceInfoEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.DeviceInfoEntityFields
import org.matrix.android.sdk.internal.crypto.store.db.model.KeysBackupDataEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.MyDeviceLastSeenInfoEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.OlmInboundGroupSessionEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.OlmInboundGroupSessionEntityFields
import org.matrix.android.sdk.internal.crypto.store.db.model.OlmSessionEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.OlmSessionEntityFields
import org.matrix.android.sdk.internal.crypto.store.db.model.OutboundGroupSessionInfoEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.OutgoingKeyRequestEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.OutgoingKeyRequestEntityFields
import org.matrix.android.sdk.internal.crypto.store.db.model.SharedSessionEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.TrustLevelEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.UserEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.UserEntityFields
import org.matrix.android.sdk.internal.crypto.store.db.model.WithHeldSessionEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.createPrimaryKey
import org.matrix.android.sdk.internal.crypto.store.db.model.deleteOnCascade
import org.matrix.android.sdk.internal.crypto.store.db.query.create
import org.matrix.android.sdk.internal.crypto.store.db.query.delete
import org.matrix.android.sdk.internal.crypto.store.db.query.get
import org.matrix.android.sdk.internal.crypto.store.db.query.getById
import org.matrix.android.sdk.internal.crypto.store.db.query.getOrCreate
import org.matrix.android.sdk.internal.crypto.util.RequestIdHelper
import org.matrix.android.sdk.internal.di.CryptoDatabase
import org.matrix.android.sdk.internal.di.DeviceId
import org.matrix.android.sdk.internal.di.MoshiProvider
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.extensions.clearWith
import org.matrix.android.sdk.internal.session.SessionScope
import org.matrix.android.sdk.internal.util.time.Clock
import org.matrix.olm.OlmAccount
import org.matrix.olm.OlmException
import org.matrix.olm.OlmOutboundGroupSession
import timber.log.Timber
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private val loggerTag = LoggerTag("RealmCryptoStore", LoggerTag.CRYPTO)

@SessionScope
internal class RealmCryptoStore @Inject constructor(
        @CryptoDatabase private val realmConfiguration: RealmConfiguration,
        private val crossSigningKeysMapper: CrossSigningKeysMapper,
        @UserId private val userId: String,
        @DeviceId private val deviceId: String?,
        private val clock: Clock,
) : IMXCryptoStore {

    /* ==========================================================================================
     * Memory cache, to correctly release JNI objects
     * ========================================================================================== */

    // A realm instance, for faster future getInstance. Do not use it
    private var realmLocker: Realm? = null

    // The olm account
    private var olmAccount: OlmAccount? = null

    private val newSessionListeners = ArrayList<NewSessionListener>()

    override fun addNewSessionListener(listener: NewSessionListener) {
        if (!newSessionListeners.contains(listener)) newSessionListeners.add(listener)
    }

    override fun removeSessionListener(listener: NewSessionListener) {
        newSessionListeners.remove(listener)
    }

    private val monarchyWriteAsyncExecutor = Executors.newSingleThreadExecutor()

    private val monarchy = Monarchy.Builder()
            .setRealmConfiguration(realmConfiguration)
            .setWriteAsyncExecutor(monarchyWriteAsyncExecutor)
            .build()

    init {
        // Ensure CryptoMetadataEntity is inserted in DB
        doRealmTransaction(realmConfiguration) { realm ->
            var currentMetadata = realm.where<CryptoMetadataEntity>().findFirst()

            var deleteAll = false

            if (currentMetadata != null) {
                // Check credentials
                // The device id may not have been provided in credentials.
                // Check it only if provided, else trust the stored one.
                if (currentMetadata.userId != userId ||
                        (deviceId != null && deviceId != currentMetadata.deviceId)) {
                    Timber.w("## open() : Credentials do not match, close this store and delete data")
                    deleteAll = true
                    currentMetadata = null
                }
            }

            if (currentMetadata == null) {
                if (deleteAll) {
                    realm.deleteAll()
                }

                // Metadata not found, or database cleaned, create it
                realm.createObject(CryptoMetadataEntity::class.java, userId).apply {
                    deviceId = this@RealmCryptoStore.deviceId
                }
            }
        }
    }
    /* ==========================================================================================
     * Other data
     * ========================================================================================== */

    override fun hasData(): Boolean {
        return doWithRealm(realmConfiguration) {
            !it.isEmpty &&
                    // Check if there is a MetaData object
                    it.where<CryptoMetadataEntity>().count() > 0
        }
    }

    override fun deleteStore() {
        doRealmTransaction(realmConfiguration) {
            it.deleteAll()
        }
    }

    override fun open() {
        synchronized(this) {
            if (realmLocker == null) {
                realmLocker = Realm.getInstance(realmConfiguration)
            }
        }
    }

    override fun close() {
        // Ensure no async request will be run later
        val tasks = monarchyWriteAsyncExecutor.shutdownNow()
        Timber.w("Closing RealmCryptoStore, ${tasks.size} async task(s) cancelled")
        tryOrNull("Interrupted") {
            // Wait 1 minute max
            monarchyWriteAsyncExecutor.awaitTermination(1, TimeUnit.MINUTES)
        }

        olmAccount?.releaseAccount()

        realmLocker?.close()
        realmLocker = null
    }

    override fun storeDeviceId(deviceId: String) {
        doRealmTransaction(realmConfiguration) {
            it.where<CryptoMetadataEntity>().findFirst()?.deviceId = deviceId
        }
    }

    override fun getDeviceId(): String {
        return doWithRealm(realmConfiguration) {
            it.where<CryptoMetadataEntity>().findFirst()?.deviceId
        } ?: ""
    }

    override fun saveOlmAccount() {
        doRealmTransaction(realmConfiguration) {
            it.where<CryptoMetadataEntity>().findFirst()?.putOlmAccount(olmAccount)
        }
    }

    /**
     * Olm account access should be synchronized.
     */
    override fun <T> doWithOlmAccount(block: (OlmAccount) -> T): T {
        return olmAccount!!.let { olmAccount ->
            synchronized(olmAccount) {
                block.invoke(olmAccount)
            }
        }
    }

    @Synchronized
    override fun getOrCreateOlmAccount(): OlmAccount {
        doRealmTransaction(realmConfiguration) {
            val metaData = it.where<CryptoMetadataEntity>().findFirst()
            val existing = metaData!!.getOlmAccount()
            if (existing == null) {
                Timber.d("## Crypto Creating olm account")
                val created = OlmAccount()
                metaData.putOlmAccount(created)
                olmAccount = created
            } else {
                Timber.d("## Crypto Access existing account")
                olmAccount = existing
            }
        }
        return olmAccount!!
    }

    override fun getUserDevice(userId: String, deviceId: String): CryptoDeviceInfo? {
        return doWithRealm(realmConfiguration) {
            it.where<DeviceInfoEntity>()
                    .equalTo(DeviceInfoEntityFields.PRIMARY_KEY, DeviceInfoEntity.createPrimaryKey(userId, deviceId))
                    .findFirst()
                    ?.let { deviceInfo ->
                        CryptoMapper.mapToModel(deviceInfo)
                    }
        }
    }

    override fun deviceWithIdentityKey(identityKey: String): CryptoDeviceInfo? {
        return doWithRealm(realmConfiguration) { realm ->
            realm.where<DeviceInfoEntity>()
                    .contains(DeviceInfoEntityFields.KEYS_MAP_JSON, identityKey)
                    .findAll()
                    .mapNotNull { CryptoMapper.mapToModel(it) }
                    .firstOrNull {
                        it.identityKey() == identityKey
                    }
        }
    }

    override fun storeUserDevices(userId: String, devices: Map<String, CryptoDeviceInfo>?) {
        doRealmTransaction(realmConfiguration) { realm ->
            if (devices == null) {
                Timber.d("Remove user $userId")
                // Remove the user
                UserEntity.delete(realm, userId)
            } else {
                val userEntity = UserEntity.getOrCreate(realm, userId)
                // First delete the removed devices
                val deviceIds = devices.keys
                userEntity.devices.toTypedArray().iterator().let {
                    while (it.hasNext()) {
                        val deviceInfoEntity = it.next()
                        if (deviceInfoEntity.deviceId !in deviceIds) {
                            Timber.d("Remove device ${deviceInfoEntity.deviceId} of user $userId")
                            deviceInfoEntity.deleteOnCascade()
                        }
                    }
                }
                // Then update existing devices or add new one
                devices.values.forEach { cryptoDeviceInfo ->
                    val existingDeviceInfoEntity = userEntity.devices.firstOrNull { it.deviceId == cryptoDeviceInfo.deviceId }
                    if (existingDeviceInfoEntity == null) {
                        // Add the device
                        Timber.d("Add device ${cryptoDeviceInfo.deviceId} of user $userId")
                        val newEntity = CryptoMapper.mapToEntity(cryptoDeviceInfo)
                        newEntity.firstTimeSeenLocalTs = clock.epochMillis()
                        userEntity.devices.add(newEntity)
                    } else {
                        // Update the device
                        Timber.d("Update device ${cryptoDeviceInfo.deviceId} of user $userId")
                        CryptoMapper.updateDeviceInfoEntity(existingDeviceInfoEntity, cryptoDeviceInfo)
                    }
                }
            }
        }
    }

    override fun storeUserCrossSigningKeys(
            userId: String,
            masterKey: CryptoCrossSigningKey?,
            selfSigningKey: CryptoCrossSigningKey?,
            userSigningKey: CryptoCrossSigningKey?
    ) {
        doRealmTransaction(realmConfiguration) { realm ->
            UserEntity.getOrCreate(realm, userId)
                    .let { userEntity ->
                        if (masterKey == null || selfSigningKey == null) {
                            // The user has disabled cross signing?
                            userEntity.crossSigningInfoEntity?.deleteOnCascade()
                            userEntity.crossSigningInfoEntity = null
                        } else {
                            var shouldResetMyDevicesLocalTrust = false
                            CrossSigningInfoEntity.getOrCreate(realm, userId).let { signingInfo ->
                                // What should we do if we detect a change of the keys?
                                val existingMaster = signingInfo.getMasterKey()
                                if (existingMaster != null && existingMaster.publicKeyBase64 == masterKey.unpaddedBase64PublicKey) {
                                    crossSigningKeysMapper.update(existingMaster, masterKey)
                                } else {
                                    Timber.d("## CrossSigning  MSK change for $userId")
                                    val keyEntity = crossSigningKeysMapper.map(masterKey)
                                    signingInfo.setMasterKey(keyEntity)
                                    if (userId == this.userId) {
                                        shouldResetMyDevicesLocalTrust = true
                                        // my msk has changed! clear my private key
                                        // Could we have some race here? e.g I am the one that did change the keys
                                        // could i get this update to early and clear the private keys?
                                        // -> initializeCrossSigning is guarding for that by storing all at once
                                        realm.where<CryptoMetadataEntity>().findFirst()?.apply {
                                            xSignMasterPrivateKey = null
                                        }
                                    }
                                }

                                val existingSelfSigned = signingInfo.getSelfSignedKey()
                                if (existingSelfSigned != null && existingSelfSigned.publicKeyBase64 == selfSigningKey.unpaddedBase64PublicKey) {
                                    crossSigningKeysMapper.update(existingSelfSigned, selfSigningKey)
                                } else {
                                    Timber.d("## CrossSigning  SSK change for $userId")
                                    val keyEntity = crossSigningKeysMapper.map(selfSigningKey)
                                    signingInfo.setSelfSignedKey(keyEntity)
                                    if (userId == this.userId) {
                                        shouldResetMyDevicesLocalTrust = true
                                        // my ssk has changed! clear my private key
                                        realm.where<CryptoMetadataEntity>().findFirst()?.apply {
                                            xSignSelfSignedPrivateKey = null
                                        }
                                    }
                                }

                                // Only for me
                                if (userSigningKey != null) {
                                    val existingUSK = signingInfo.getUserSigningKey()
                                    if (existingUSK != null && existingUSK.publicKeyBase64 == userSigningKey.unpaddedBase64PublicKey) {
                                        crossSigningKeysMapper.update(existingUSK, userSigningKey)
                                    } else {
                                        Timber.d("## CrossSigning  USK change for $userId")
                                        val keyEntity = crossSigningKeysMapper.map(userSigningKey)
                                        signingInfo.setUserSignedKey(keyEntity)
                                        if (userId == this.userId) {
                                            shouldResetMyDevicesLocalTrust = true
                                            // my usk has changed! clear my private key
                                            realm.where<CryptoMetadataEntity>().findFirst()?.apply {
                                                xSignUserPrivateKey = null
                                            }
                                        }
                                    }
                                }

                                // When my cross signing keys are reset, we consider clearing all existing device trust
                                if (shouldResetMyDevicesLocalTrust) {
                                    realm.where<UserEntity>()
                                            .equalTo(UserEntityFields.USER_ID, this.userId)
                                            .findFirst()
                                            ?.devices?.forEach {
                                                it?.trustLevelEntity?.crossSignedVerified = false
                                                it?.trustLevelEntity?.locallyVerified = it.deviceId == deviceId
                                            }
                                }
                                userEntity.crossSigningInfoEntity = signingInfo
                            }
                        }
                    }
        }
    }

    override fun getCrossSigningPrivateKeys(): PrivateKeysInfo? {
        return doWithRealm(realmConfiguration) { realm ->
            realm.where<CryptoMetadataEntity>()
                    .findFirst()
                    ?.let {
                        PrivateKeysInfo(
                                master = it.xSignMasterPrivateKey,
                                selfSigned = it.xSignSelfSignedPrivateKey,
                                user = it.xSignUserPrivateKey
                        )
                    }
        }
    }

    override fun getLiveCrossSigningPrivateKeys(): LiveData<Optional<PrivateKeysInfo>> {
        val liveData = monarchy.findAllMappedWithChanges(
                { realm: Realm ->
                    realm
                            .where<CryptoMetadataEntity>()
                },
                {
                    PrivateKeysInfo(
                            master = it.xSignMasterPrivateKey,
                            selfSigned = it.xSignSelfSignedPrivateKey,
                            user = it.xSignUserPrivateKey
                    )
                }
        )
        return Transformations.map(liveData) {
            it.firstOrNull().toOptional()
        }
    }

    override fun storePrivateKeysInfo(msk: String?, usk: String?, ssk: String?) {
        Timber.v("## CRYPTO | *** storePrivateKeysInfo ${msk != null}, ${usk != null}, ${ssk != null}")
        doRealmTransaction(realmConfiguration) { realm ->
            realm.where<CryptoMetadataEntity>().findFirst()?.apply {
                xSignMasterPrivateKey = msk
                xSignUserPrivateKey = usk
                xSignSelfSignedPrivateKey = ssk
            }
        }
    }

    override fun saveBackupRecoveryKey(recoveryKey: String?, version: String?) {
        doRealmTransaction(realmConfiguration) { realm ->
            realm.where<CryptoMetadataEntity>().findFirst()?.apply {
                keyBackupRecoveryKey = recoveryKey
                keyBackupRecoveryKeyVersion = version
            }
        }
    }

    override fun getKeyBackupRecoveryKeyInfo(): SavedKeyBackupKeyInfo? {
        return doWithRealm(realmConfiguration) { realm ->
            realm.where<CryptoMetadataEntity>()
                    .findFirst()
                    ?.let {
                        val key = it.keyBackupRecoveryKey
                        val version = it.keyBackupRecoveryKeyVersion
                        if (!key.isNullOrBlank() && !version.isNullOrBlank()) {
                            SavedKeyBackupKeyInfo(recoveryKey = key, version = version)
                        } else {
                            null
                        }
                    }
        }
    }

    override fun storeMSKPrivateKey(msk: String?) {
        Timber.v("## CRYPTO | *** storeMSKPrivateKey ${msk != null} ")
        doRealmTransaction(realmConfiguration) { realm ->
            realm.where<CryptoMetadataEntity>().findFirst()?.apply {
                xSignMasterPrivateKey = msk
            }
        }
    }

    override fun storeSSKPrivateKey(ssk: String?) {
        Timber.v("## CRYPTO | *** storeSSKPrivateKey ${ssk != null} ")
        doRealmTransaction(realmConfiguration) { realm ->
            realm.where<CryptoMetadataEntity>().findFirst()?.apply {
                xSignSelfSignedPrivateKey = ssk
            }
        }
    }

    override fun storeUSKPrivateKey(usk: String?) {
        Timber.v("## CRYPTO | *** storeUSKPrivateKey ${usk != null} ")
        doRealmTransaction(realmConfiguration) { realm ->
            realm.where<CryptoMetadataEntity>().findFirst()?.apply {
                xSignUserPrivateKey = usk
            }
        }
    }

    override fun getUserDevices(userId: String): Map<String, CryptoDeviceInfo>? {
        return doWithRealm(realmConfiguration) {
            it.where<UserEntity>()
                    .equalTo(UserEntityFields.USER_ID, userId)
                    .findFirst()
                    ?.devices
                    ?.map { deviceInfo ->
                        CryptoMapper.mapToModel(deviceInfo)
                    }
                    ?.associateBy { cryptoDevice ->
                        cryptoDevice.deviceId
                    }
        }
    }

    override fun getUserDeviceList(userId: String): List<CryptoDeviceInfo>? {
        return doWithRealm(realmConfiguration) {
            it.where<UserEntity>()
                    .equalTo(UserEntityFields.USER_ID, userId)
                    .findFirst()
                    ?.devices
                    ?.map { deviceInfo ->
                        CryptoMapper.mapToModel(deviceInfo)
                    }
        }
    }

    override fun getLiveDeviceList(userId: String): LiveData<List<CryptoDeviceInfo>> {
        val liveData = monarchy.findAllMappedWithChanges(
                { realm: Realm ->
                    realm
                            .where<UserEntity>()
                            .equalTo(UserEntityFields.USER_ID, userId)
                },
                { entity ->
                    entity.devices.map { CryptoMapper.mapToModel(it) }
                }
        )
        return Transformations.map(liveData) {
            it.firstOrNull().orEmpty()
        }
    }

    override fun getLiveDeviceList(userIds: List<String>): LiveData<List<CryptoDeviceInfo>> {
        val liveData = monarchy.findAllMappedWithChanges(
                { realm: Realm ->
                    realm
                            .where<UserEntity>()
                            .`in`(UserEntityFields.USER_ID, userIds.distinct().toTypedArray())
                },
                { entity ->
                    entity.devices.map { CryptoMapper.mapToModel(it) }
                }
        )
        return Transformations.map(liveData) {
            it.flatten()
        }
    }

    override fun getLiveDeviceList(): LiveData<List<CryptoDeviceInfo>> {
        val liveData = monarchy.findAllMappedWithChanges(
                { realm: Realm ->
                    realm.where<UserEntity>()
                },
                { entity ->
                    entity.devices.map { CryptoMapper.mapToModel(it) }
                }
        )
        return Transformations.map(liveData) {
            it.firstOrNull().orEmpty()
        }
    }

    override fun getMyDevicesInfo(): List<DeviceInfo> {
        return monarchy.fetchAllCopiedSync {
            it.where<MyDeviceLastSeenInfoEntity>()
        }.map {
            DeviceInfo(
                    deviceId = it.deviceId,
                    lastSeenIp = it.lastSeenIp,
                    lastSeenTs = it.lastSeenTs,
                    displayName = it.displayName
            )
        }
    }

    override fun getLiveMyDevicesInfo(): LiveData<List<DeviceInfo>> {
        return monarchy.findAllMappedWithChanges(
                { realm: Realm ->
                    realm.where<MyDeviceLastSeenInfoEntity>()
                },
                { entity ->
                    DeviceInfo(
                            deviceId = entity.deviceId,
                            lastSeenIp = entity.lastSeenIp,
                            lastSeenTs = entity.lastSeenTs,
                            displayName = entity.displayName
                    )
                }
        )
    }

    override fun saveMyDevicesInfo(info: List<DeviceInfo>) {
        val entities = info.map {
            MyDeviceLastSeenInfoEntity(
                    lastSeenTs = it.lastSeenTs,
                    lastSeenIp = it.lastSeenIp,
                    displayName = it.displayName,
                    deviceId = it.deviceId
            )
        }
        doRealmTransactionAsync(realmConfiguration) { realm ->
            realm.where<MyDeviceLastSeenInfoEntity>().findAll().deleteAllFromRealm()
            entities.forEach {
                realm.insertOrUpdate(it)
            }
        }
    }

    override fun storeRoomAlgorithm(roomId: String, algorithm: String?) {
        doRealmTransaction(realmConfiguration) {
            CryptoRoomEntity.getOrCreate(it, roomId).let { entity ->
                entity.algorithm = algorithm
                // store anyway the new algorithm, but mark the room
                // as having been encrypted once whatever, this can never
                // go back to false
                if (algorithm == MXCRYPTO_ALGORITHM_MEGOLM) {
                    entity.wasEncryptedOnce = true
                }
            }
        }
    }

    override fun getRoomAlgorithm(roomId: String): String? {
        return doWithRealm(realmConfiguration) {
            CryptoRoomEntity.getById(it, roomId)?.algorithm
        }
    }

    override fun roomWasOnceEncrypted(roomId: String): Boolean {
        return doWithRealm(realmConfiguration) {
            CryptoRoomEntity.getById(it, roomId)?.wasEncryptedOnce ?: false
        }
    }

    override fun shouldEncryptForInvitedMembers(roomId: String): Boolean {
        return doWithRealm(realmConfiguration) {
            CryptoRoomEntity.getById(it, roomId)?.shouldEncryptForInvitedMembers
        }
                ?: false
    }

    override fun shouldShareHistory(roomId: String): Boolean {
        if (!isShareKeysOnInviteEnabled()) return false
        return doWithRealm(realmConfiguration) {
            CryptoRoomEntity.getById(it, roomId)?.shouldShareHistory
        }
                ?: false
    }

    override fun setShouldEncryptForInvitedMembers(roomId: String, shouldEncryptForInvitedMembers: Boolean) {
        doRealmTransaction(realmConfiguration) {
            CryptoRoomEntity.getOrCreate(it, roomId).shouldEncryptForInvitedMembers = shouldEncryptForInvitedMembers
        }
    }

    override fun setShouldShareHistory(roomId: String, shouldShareHistory: Boolean) {
        Timber.tag(loggerTag.value)
                .v("setShouldShareHistory for room $roomId is $shouldShareHistory")
        doRealmTransaction(realmConfiguration) {
            CryptoRoomEntity.getOrCreate(it, roomId).shouldShareHistory = shouldShareHistory
        }
    }

    override fun storeSession(olmSessionWrapper: OlmSessionWrapper, deviceKey: String) {
        var sessionIdentifier: String? = null

        try {
            sessionIdentifier = olmSessionWrapper.olmSession.sessionIdentifier()
        } catch (e: OlmException) {
            Timber.e(e, "## storeSession() : sessionIdentifier failed")
        }

        if (sessionIdentifier != null) {
            val key = OlmSessionEntity.createPrimaryKey(sessionIdentifier, deviceKey)

            doRealmTransaction(realmConfiguration) {
                val realmOlmSession = OlmSessionEntity().apply {
                    primaryKey = key
                    sessionId = sessionIdentifier
                    this.deviceKey = deviceKey
                    putOlmSession(olmSessionWrapper.olmSession)
                    lastReceivedMessageTs = olmSessionWrapper.lastReceivedMessageTs
                }

                it.insertOrUpdate(realmOlmSession)
            }
        }
    }

    override fun getDeviceSession(sessionId: String, deviceKey: String): OlmSessionWrapper? {
        val key = OlmSessionEntity.createPrimaryKey(sessionId, deviceKey)
        return doRealmQueryAndCopy(realmConfiguration) {
            it.where<OlmSessionEntity>()
                    .equalTo(OlmSessionEntityFields.PRIMARY_KEY, key)
                    .findFirst()
        }
                ?.let {
                    val olmSession = it.getOlmSession()
                    if (olmSession != null && it.sessionId != null) {
                        return@let OlmSessionWrapper(olmSession, it.lastReceivedMessageTs)
                    }
                    null
                }
    }

    override fun getLastUsedSessionId(deviceKey: String): String? {
        return doWithRealm(realmConfiguration) {
            it.where<OlmSessionEntity>()
                    .equalTo(OlmSessionEntityFields.DEVICE_KEY, deviceKey)
                    .sort(OlmSessionEntityFields.LAST_RECEIVED_MESSAGE_TS, Sort.DESCENDING)
                    .findFirst()
                    ?.sessionId
        }
    }

    override fun getDeviceSessionIds(deviceKey: String): List<String> {
        return doWithRealm(realmConfiguration) {
            it.where<OlmSessionEntity>()
                    .equalTo(OlmSessionEntityFields.DEVICE_KEY, deviceKey)
                    .sort(OlmSessionEntityFields.LAST_RECEIVED_MESSAGE_TS, Sort.DESCENDING)
                    .findAll()
                    .mapNotNull { sessionEntity ->
                        sessionEntity.sessionId
                    }
        }
    }

    override fun storeInboundGroupSessions(sessions: List<MXInboundMegolmSessionWrapper>) {
        if (sessions.isEmpty()) {
            return
        }

        doRealmTransaction(realmConfiguration) { realm ->
            sessions.forEach { wrapper ->

                val sessionIdentifier = try {
                    wrapper.session.sessionIdentifier()
                } catch (e: OlmException) {
                    Timber.e(e, "## storeInboundGroupSession() : sessionIdentifier failed")
                    return@forEach
                }

//                    val shouldShareHistory = session.roomId?.let { roomId ->
//                        CryptoRoomEntity.getById(realm, roomId)?.shouldShareHistory
//                    } ?: false
                val key = OlmInboundGroupSessionEntity.createPrimaryKey(sessionIdentifier, wrapper.sessionData.senderKey)

                val existing = realm.where<OlmInboundGroupSessionEntity>()
                        .equalTo(OlmInboundGroupSessionEntityFields.PRIMARY_KEY, key)
                        .findFirst()

                val realmOlmInboundGroupSession = OlmInboundGroupSessionEntity().apply {
                    primaryKey = key
                    store(wrapper)
                    backedUp = existing?.backedUp ?: false
                }

                Timber.v("## CRYPTO | shouldShareHistory: ${wrapper.sessionData.sharedHistory} for $key")
                realm.insertOrUpdate(realmOlmInboundGroupSession)
            }
        }
    }

    override fun getInboundGroupSession(sessionId: String, senderKey: String): MXInboundMegolmSessionWrapper? {
        val key = OlmInboundGroupSessionEntity.createPrimaryKey(sessionId, senderKey)

        return doWithRealm(realmConfiguration) { realm ->
            realm.where<OlmInboundGroupSessionEntity>()
                    .equalTo(OlmInboundGroupSessionEntityFields.PRIMARY_KEY, key)
                    .findFirst()
                    ?.toModel()
        }
    }

    override fun getInboundGroupSession(sessionId: String, senderKey: String, sharedHistory: Boolean): MXInboundMegolmSessionWrapper? {
        val key = OlmInboundGroupSessionEntity.createPrimaryKey(sessionId, senderKey)
        return doWithRealm(realmConfiguration) {
            it.where<OlmInboundGroupSessionEntity>()
                    .equalTo(OlmInboundGroupSessionEntityFields.SHARED_HISTORY, sharedHistory)
                    .equalTo(OlmInboundGroupSessionEntityFields.PRIMARY_KEY, key)
                    .findFirst()
                    ?.toModel()
        }
    }

    override fun getCurrentOutboundGroupSessionForRoom(roomId: String): OutboundGroupSessionWrapper? {
        return doWithRealm(realmConfiguration) { realm ->
            realm.where<CryptoRoomEntity>()
                    .equalTo(CryptoRoomEntityFields.ROOM_ID, roomId)
                    .findFirst()?.outboundSessionInfo?.let { entity ->
                        entity.getOutboundGroupSession()?.let {
                            OutboundGroupSessionWrapper(
                                    it,
                                    entity.creationTime ?: 0,
                                    entity.shouldShareHistory
                            )
                        }
                    }
        }
    }

    override fun storeCurrentOutboundGroupSessionForRoom(roomId: String, outboundGroupSession: OlmOutboundGroupSession?) {
        // we can do this async, as it's just for restoring on next launch
        // the olmdevice is caching the active instance
        // this is called for each sent message (so not high frequency), thus we can use basic realm async without
        // risk of reaching max async operation limit?
        doRealmTransactionAsync(realmConfiguration) { realm ->
            CryptoRoomEntity.getById(realm, roomId)?.let { entity ->
                // we should delete existing outbound session info if any
                entity.outboundSessionInfo?.deleteFromRealm()

                if (outboundGroupSession != null) {
                    val info = realm.createObject(OutboundGroupSessionInfoEntity::class.java).apply {
                        creationTime = clock.epochMillis()
                        // Store the room history visibility on the outbound session creation
                        shouldShareHistory = entity.shouldShareHistory
                        putOutboundGroupSession(outboundGroupSession)
                    }
                    entity.outboundSessionInfo = info
                }
            }
        }
    }

//    override fun needsRotationDueToVisibilityChange(roomId: String): Boolean {
//        return doWithRealm(realmConfiguration) { realm ->
//            CryptoRoomEntity.getById(realm, roomId)?.let { entity ->
//                entity.shouldShareHistory != entity.outboundSessionInfo?.shouldShareHistory
//            }
//        } ?: false
//    }

    /**
     * Note: the result will be only use to export all the keys and not to use the OlmInboundGroupSessionWrapper2,
     * so there is no need to use or update `inboundGroupSessionToRelease` for native memory management.
     */
    override fun getInboundGroupSessions(): List<MXInboundMegolmSessionWrapper> {
        return doWithRealm(realmConfiguration) { realm ->
            realm.where<OlmInboundGroupSessionEntity>()
                    .findAll()
                    .mapNotNull { it.toModel() }
        }
    }

    override fun getInboundGroupSessions(roomId: String): List<MXInboundMegolmSessionWrapper> {
        return doWithRealm(realmConfiguration) { realm ->
            realm.where<OlmInboundGroupSessionEntity>()
                    .equalTo(OlmInboundGroupSessionEntityFields.ROOM_ID, roomId)
                    .findAll()
                    .mapNotNull { it.toModel() }
        }
    }

    override fun removeInboundGroupSession(sessionId: String, senderKey: String) {
        val key = OlmInboundGroupSessionEntity.createPrimaryKey(sessionId, senderKey)

        doRealmTransaction(realmConfiguration) {
            it.where<OlmInboundGroupSessionEntity>()
                    .equalTo(OlmInboundGroupSessionEntityFields.PRIMARY_KEY, key)
                    .findAll()
                    .deleteAllFromRealm()
        }
    }

    /* ==========================================================================================
     * Keys backup
     * ========================================================================================== */

    override fun getKeyBackupVersion(): String? {
        return doRealmQueryAndCopy(realmConfiguration) {
            it.where<CryptoMetadataEntity>().findFirst()
        }?.backupVersion
    }

    override fun setKeyBackupVersion(keyBackupVersion: String?) {
        doRealmTransaction(realmConfiguration) {
            it.where<CryptoMetadataEntity>().findFirst()?.backupVersion = keyBackupVersion
        }
    }

    override fun getKeysBackupData(): KeysBackupDataEntity? {
        return doRealmQueryAndCopy(realmConfiguration) {
            it.where<KeysBackupDataEntity>().findFirst()
        }
    }

    override fun setKeysBackupData(keysBackupData: KeysBackupDataEntity?) {
        doRealmTransaction(realmConfiguration) {
            if (keysBackupData == null) {
                // Clear the table
                it.where<KeysBackupDataEntity>()
                        .findAll()
                        .deleteAllFromRealm()
            } else {
                // Only one object
                it.copyToRealmOrUpdate(keysBackupData)
            }
        }
    }

    override fun resetBackupMarkers() {
        doRealmTransaction(realmConfiguration) {
            it.where<OlmInboundGroupSessionEntity>()
                    .findAll()
                    .map { inboundGroupSession ->
                        inboundGroupSession.backedUp = false
                    }
        }
    }

    override fun markBackupDoneForInboundGroupSessions(olmInboundGroupSessionWrappers: List<MXInboundMegolmSessionWrapper>) {
        if (olmInboundGroupSessionWrappers.isEmpty()) {
            return
        }

        doRealmTransaction(realmConfiguration) { realm ->
            olmInboundGroupSessionWrappers.forEach { olmInboundGroupSessionWrapper ->
                try {
                    val sessionIdentifier =
                            tryOrNull("Failed to get session identifier") {
                                olmInboundGroupSessionWrapper.session.sessionIdentifier()
                            } ?: return@forEach
                    val key = OlmInboundGroupSessionEntity.createPrimaryKey(
                            sessionIdentifier,
                            olmInboundGroupSessionWrapper.sessionData.senderKey
                    )

                    val existing = realm.where<OlmInboundGroupSessionEntity>()
                            .equalTo(OlmInboundGroupSessionEntityFields.PRIMARY_KEY, key)
                            .findFirst()

                    if (existing != null) {
                        existing.backedUp = true
                    } else {
                        // ... might be in cache but not yet persisted, create a record to persist backedup state
                        val realmOlmInboundGroupSession = OlmInboundGroupSessionEntity().apply {
                            primaryKey = key
                            store(olmInboundGroupSessionWrapper)
                            backedUp = true
                        }

                        realm.insertOrUpdate(realmOlmInboundGroupSession)
                    }
                } catch (e: OlmException) {
                    Timber.e(e, "OlmException")
                }
            }
        }
    }

    override fun inboundGroupSessionsToBackup(limit: Int): List<MXInboundMegolmSessionWrapper> {
        return doWithRealm(realmConfiguration) {
            it.where<OlmInboundGroupSessionEntity>()
                    .equalTo(OlmInboundGroupSessionEntityFields.BACKED_UP, false)
                    .limit(limit.toLong())
                    .findAll()
                    .mapNotNull { it.toModel() }
        }
    }

    override fun inboundGroupSessionsCount(onlyBackedUp: Boolean): Int {
        return doWithRealm(realmConfiguration) {
            it.where<OlmInboundGroupSessionEntity>()
                    .apply {
                        if (onlyBackedUp) {
                            equalTo(OlmInboundGroupSessionEntityFields.BACKED_UP, true)
                        }
                    }
                    .count()
                    .toInt()
        }
    }

    override fun setGlobalBlacklistUnverifiedDevices(block: Boolean) {
        doRealmTransaction(realmConfiguration) {
            it.where<CryptoMetadataEntity>().findFirst()?.globalBlacklistUnverifiedDevices = block
        }
    }

    override fun enableKeyGossiping(enable: Boolean) {
        doRealmTransaction(realmConfiguration) {
            it.where<CryptoMetadataEntity>().findFirst()?.globalEnableKeyGossiping = enable
        }
    }

    override fun isKeyGossipingEnabled(): Boolean {
        return doWithRealm(realmConfiguration) {
            it.where<CryptoMetadataEntity>().findFirst()?.globalEnableKeyGossiping
        } ?: true
    }

    override fun getGlobalBlacklistUnverifiedDevices(): Boolean {
        return doWithRealm(realmConfiguration) {
            it.where<CryptoMetadataEntity>().findFirst()?.globalBlacklistUnverifiedDevices
        } ?: false
    }

    override fun isShareKeysOnInviteEnabled(): Boolean {
        return doWithRealm(realmConfiguration) {
            it.where<CryptoMetadataEntity>().findFirst()?.enableKeyForwardingOnInvite
        } ?: false
    }

    override fun enableShareKeyOnInvite(enable: Boolean) {
        doRealmTransaction(realmConfiguration) {
            it.where<CryptoMetadataEntity>().findFirst()?.enableKeyForwardingOnInvite = enable
        }
    }

    override fun setDeviceKeysUploaded(uploaded: Boolean) {
        doRealmTransaction(realmConfiguration) {
            it.where<CryptoMetadataEntity>().findFirst()?.deviceKeysSentToServer = uploaded
        }
    }

    override fun areDeviceKeysUploaded(): Boolean {
        return doWithRealm(realmConfiguration) {
            it.where<CryptoMetadataEntity>().findFirst()?.deviceKeysSentToServer
        } ?: false
    }

    override fun setRoomsListBlacklistUnverifiedDevices(roomIds: List<String>) {
        doRealmTransaction(realmConfiguration) {
            // Reset all
            it.where<CryptoRoomEntity>()
                    .findAll()
                    .forEach { room ->
                        room.blacklistUnverifiedDevices = false
                    }

            // Enable those in the list
            it.where<CryptoRoomEntity>()
                    .`in`(CryptoRoomEntityFields.ROOM_ID, roomIds.toTypedArray())
                    .findAll()
                    .forEach { room ->
                        room.blacklistUnverifiedDevices = true
                    }
        }
    }

    override fun getRoomsListBlacklistUnverifiedDevices(): List<String> {
        return doWithRealm(realmConfiguration) {
            it.where<CryptoRoomEntity>()
                    .equalTo(CryptoRoomEntityFields.BLACKLIST_UNVERIFIED_DEVICES, true)
                    .findAll()
                    .mapNotNull { cryptoRoom ->
                        cryptoRoom.roomId
                    }
        }
    }

    override fun getDeviceTrackingStatuses(): Map<String, Int> {
        return doWithRealm(realmConfiguration) {
            it.where<UserEntity>()
                    .findAll()
                    .associateBy { user ->
                        user.userId!!
                    }
                    .mapValues { entry ->
                        entry.value.deviceTrackingStatus
                    }
        }
    }

    override fun saveDeviceTrackingStatuses(deviceTrackingStatuses: Map<String, Int>) {
        doRealmTransaction(realmConfiguration) {
            deviceTrackingStatuses
                    .map { entry ->
                        UserEntity.getOrCreate(it, entry.key)
                                .deviceTrackingStatus = entry.value
                    }
        }
    }

    override fun getDeviceTrackingStatus(userId: String, defaultValue: Int): Int {
        return doWithRealm(realmConfiguration) {
            it.where<UserEntity>()
                    .equalTo(UserEntityFields.USER_ID, userId)
                    .findFirst()
                    ?.deviceTrackingStatus
        }
                ?: defaultValue
    }

    override fun getOutgoingRoomKeyRequest(requestBody: RoomKeyRequestBody): OutgoingKeyRequest? {
        return monarchy.fetchAllCopiedSync { realm ->
            realm.where<OutgoingKeyRequestEntity>()
                    .equalTo(OutgoingKeyRequestEntityFields.ROOM_ID, requestBody.roomId)
                    .equalTo(OutgoingKeyRequestEntityFields.MEGOLM_SESSION_ID, requestBody.sessionId)
        }.map {
            it.toOutgoingKeyRequest()
        }.firstOrNull {
            it.requestBody?.algorithm == requestBody.algorithm &&
                    it.requestBody?.roomId == requestBody.roomId &&
                    it.requestBody?.senderKey == requestBody.senderKey &&
                    it.requestBody?.sessionId == requestBody.sessionId
        }
    }

    override fun getOutgoingRoomKeyRequest(requestId: String): OutgoingKeyRequest? {
        return monarchy.fetchAllCopiedSync { realm ->
            realm.where<OutgoingKeyRequestEntity>()
                    .equalTo(OutgoingKeyRequestEntityFields.REQUEST_ID, requestId)
        }.map {
            it.toOutgoingKeyRequest()
        }.firstOrNull()
    }

    override fun getOutgoingRoomKeyRequest(roomId: String, sessionId: String, algorithm: String, senderKey: String): List<OutgoingKeyRequest> {
        // TODO this annoying we have to load all
        return monarchy.fetchAllCopiedSync { realm ->
            realm.where<OutgoingKeyRequestEntity>()
                    .equalTo(OutgoingKeyRequestEntityFields.ROOM_ID, roomId)
                    .equalTo(OutgoingKeyRequestEntityFields.MEGOLM_SESSION_ID, sessionId)
        }.map {
            it.toOutgoingKeyRequest()
        }.filter {
            it.requestBody?.algorithm == algorithm &&
                    it.requestBody?.senderKey == senderKey
        }
    }

    override fun getGossipingEventsTrail(): LiveData<PagedList<AuditTrail>> {
        val realmDataSourceFactory = monarchy.createDataSourceFactory { realm ->
            realm.where<AuditTrailEntity>().sort(AuditTrailEntityFields.AGE_LOCAL_TS, Sort.DESCENDING)
        }
        val dataSourceFactory = realmDataSourceFactory.map {
            AuditTrailMapper.map(it)
            // mm we can't map not null...
                    ?: createUnknownTrail()
        }
        return monarchy.findAllPagedWithChanges(
                realmDataSourceFactory,
                LivePagedListBuilder(
                        dataSourceFactory,
                        PagedList.Config.Builder()
                                .setPageSize(20)
                                .setEnablePlaceholders(false)
                                .setPrefetchDistance(1)
                                .build()
                )
        )
    }

    private fun createUnknownTrail() = AuditTrail(
            clock.epochMillis(),
            TrailType.Unknown,
            IncomingKeyRequestInfo(
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
            )
    )

    override fun <T> getGossipingEventsTrail(type: TrailType, mapper: ((AuditTrail) -> T)): LiveData<PagedList<T>> {
        val realmDataSourceFactory = monarchy.createDataSourceFactory { realm ->
            realm.where<AuditTrailEntity>()
                    .equalTo(AuditTrailEntityFields.TYPE, type.name)
                    .sort(AuditTrailEntityFields.AGE_LOCAL_TS, Sort.DESCENDING)
        }
        val dataSourceFactory = realmDataSourceFactory.map { entity ->
            (AuditTrailMapper.map(entity)
            // mm we can't map not null...
                    ?: createUnknownTrail()
                    ).let { mapper.invoke(it) }
        }
        return monarchy.findAllPagedWithChanges(
                realmDataSourceFactory,
                LivePagedListBuilder(
                        dataSourceFactory,
                        PagedList.Config.Builder()
                                .setPageSize(20)
                                .setEnablePlaceholders(false)
                                .setPrefetchDistance(1)
                                .build()
                )
        )
    }

    override fun getGossipingEvents(): List<AuditTrail> {
        return monarchy.fetchAllCopiedSync { realm ->
            realm.where<AuditTrailEntity>()
        }.mapNotNull {
            AuditTrailMapper.map(it)
        }
    }

    override fun getOrAddOutgoingRoomKeyRequest(
            requestBody: RoomKeyRequestBody,
            recipients: Map<String, List<String>>,
            fromIndex: Int
    ): OutgoingKeyRequest {
        // Insert the request and return the one passed in parameter
        lateinit var request: OutgoingKeyRequest
        doRealmTransaction(realmConfiguration) { realm ->

            val existing = realm.where<OutgoingKeyRequestEntity>()
                    .equalTo(OutgoingKeyRequestEntityFields.MEGOLM_SESSION_ID, requestBody.sessionId)
                    .equalTo(OutgoingKeyRequestEntityFields.ROOM_ID, requestBody.roomId)
                    .findAll()
                    .map {
                        it.toOutgoingKeyRequest()
                    }.also {
                        if (it.size > 1) {
                            // there should be one or zero but not more, worth warning
                            Timber.tag(loggerTag.value).w("There should not be more than one active key request per session")
                        }
                    }
                    .firstOrNull {
                        it.requestBody?.algorithm == requestBody.algorithm &&
                                it.requestBody?.sessionId == requestBody.sessionId &&
                                it.requestBody?.senderKey == requestBody.senderKey &&
                                it.requestBody?.roomId == requestBody.roomId
                    }

            if (existing == null) {
                request = realm.createObject(OutgoingKeyRequestEntity::class.java).apply {
                    this.requestId = RequestIdHelper.createUniqueRequestId()
                    this.setRecipients(recipients)
                    this.requestedIndex = fromIndex
                    this.requestState = OutgoingRoomKeyRequestState.UNSENT
                    this.setRequestBody(requestBody)
                    this.creationTimeStamp = clock.epochMillis()
                }.toOutgoingKeyRequest()
            } else {
                request = existing
            }
        }
        return request
    }

    override fun updateOutgoingRoomKeyRequestState(requestId: String, newState: OutgoingRoomKeyRequestState) {
        doRealmTransaction(realmConfiguration) { realm ->
            realm.where<OutgoingKeyRequestEntity>()
                    .equalTo(OutgoingKeyRequestEntityFields.REQUEST_ID, requestId)
                    .findFirst()?.apply {
                        this.requestState = newState
                        if (newState == OutgoingRoomKeyRequestState.UNSENT) {
                            // clear the old replies
                            this.replies.deleteAllFromRealm()
                        }
                    }
        }
    }

    override fun updateOutgoingRoomKeyRequiredIndex(requestId: String, newIndex: Int) {
        doRealmTransaction(realmConfiguration) { realm ->
            realm.where<OutgoingKeyRequestEntity>()
                    .equalTo(OutgoingKeyRequestEntityFields.REQUEST_ID, requestId)
                    .findFirst()?.apply {
                        this.requestedIndex = newIndex
                    }
        }
    }

    override fun updateOutgoingRoomKeyReply(
            roomId: String,
            sessionId: String,
            algorithm: String,
            senderKey: String,
            fromDevice: String?,
            event: Event
    ) {
        doRealmTransaction(realmConfiguration) { realm ->
            realm.where<OutgoingKeyRequestEntity>()
                    .equalTo(OutgoingKeyRequestEntityFields.ROOM_ID, roomId)
                    .equalTo(OutgoingKeyRequestEntityFields.MEGOLM_SESSION_ID, sessionId)
                    .findAll().firstOrNull { entity ->
                        entity.toOutgoingKeyRequest().let {
                            it.requestBody?.senderKey == senderKey &&
                                    it.requestBody?.algorithm == algorithm
                        }
                    }?.apply {
                        event.senderId?.let { addReply(it, fromDevice, event) }
                    }
        }
    }

    override fun deleteOutgoingRoomKeyRequest(requestId: String) {
        doRealmTransaction(realmConfiguration) { realm ->
            realm.where<OutgoingKeyRequestEntity>()
                    .equalTo(OutgoingKeyRequestEntityFields.REQUEST_ID, requestId)
                    .findFirst()?.deleteOnCascade()
        }
    }

    override fun deleteOutgoingRoomKeyRequestInState(state: OutgoingRoomKeyRequestState) {
        doRealmTransaction(realmConfiguration) { realm ->
            realm.where<OutgoingKeyRequestEntity>()
                    .equalTo(OutgoingKeyRequestEntityFields.REQUEST_STATE_STR, state.name)
                    .findAll()
                    // I delete like this because I want to cascade delete replies?
                    .onEach { it.deleteOnCascade() }
        }
    }

    override fun saveIncomingKeyRequestAuditTrail(
            requestId: String,
            roomId: String,
            sessionId: String,
            senderKey: String,
            algorithm: String,
            fromUser: String,
            fromDevice: String
    ) {
        monarchy.writeAsync { realm ->
            val now = clock.epochMillis()
            realm.createObject<AuditTrailEntity>().apply {
                this.ageLocalTs = now
                this.type = TrailType.IncomingKeyRequest.name
                val info = IncomingKeyRequestInfo(
                        roomId = roomId,
                        sessionId = sessionId,
                        senderKey = senderKey,
                        alg = algorithm,
                        userId = fromUser,
                        deviceId = fromDevice,
                        requestId = requestId
                )
                MoshiProvider.providesMoshi().adapter(IncomingKeyRequestInfo::class.java).toJson(info)?.let {
                    this.contentJson = it
                }
            }
        }
    }

    override fun saveWithheldAuditTrail(
            roomId: String,
            sessionId: String,
            senderKey: String,
            algorithm: String,
            code: WithHeldCode,
            userId: String,
            deviceId: String
    ) {
        monarchy.writeAsync { realm ->
            val now = clock.epochMillis()
            realm.createObject<AuditTrailEntity>().apply {
                this.ageLocalTs = now
                this.type = TrailType.OutgoingKeyWithheld.name
                val info = WithheldInfo(
                        roomId = roomId,
                        sessionId = sessionId,
                        senderKey = senderKey,
                        alg = algorithm,
                        code = code,
                        userId = userId,
                        deviceId = deviceId
                )
                MoshiProvider.providesMoshi().adapter(WithheldInfo::class.java).toJson(info)?.let {
                    this.contentJson = it
                }
            }
        }
    }

    override fun saveForwardKeyAuditTrail(
            roomId: String,
            sessionId: String,
            senderKey: String,
            algorithm: String,
            userId: String,
            deviceId: String,
            chainIndex: Long?
    ) {
        saveForwardKeyTrail(roomId, sessionId, senderKey, algorithm, userId, deviceId, chainIndex, false)
    }

    override fun saveIncomingForwardKeyAuditTrail(
            roomId: String,
            sessionId: String,
            senderKey: String,
            algorithm: String,
            userId: String,
            deviceId: String,
            chainIndex: Long?
    ) {
        saveForwardKeyTrail(roomId, sessionId, senderKey, algorithm, userId, deviceId, chainIndex, true)
    }

    private fun saveForwardKeyTrail(
            roomId: String,
            sessionId: String,
            senderKey: String,
            algorithm: String,
            userId: String,
            deviceId: String,
            chainIndex: Long?,
            incoming: Boolean
    ) {
        monarchy.writeAsync { realm ->
            val now = clock.epochMillis()
            realm.createObject<AuditTrailEntity>().apply {
                this.ageLocalTs = now
                this.type = if (incoming) TrailType.IncomingKeyForward.name else TrailType.OutgoingKeyForward.name
                val info = ForwardInfo(
                        roomId = roomId,
                        sessionId = sessionId,
                        senderKey = senderKey,
                        alg = algorithm,
                        userId = userId,
                        deviceId = deviceId,
                        chainIndex = chainIndex
                )
                MoshiProvider.providesMoshi().adapter(ForwardInfo::class.java).toJson(info)?.let {
                    this.contentJson = it
                }
            }
        }
    }

    /* ==========================================================================================
     * Cross Signing
     * ========================================================================================== */
    override fun getMyCrossSigningInfo(): MXCrossSigningInfo? {
        return doWithRealm(realmConfiguration) {
            it.where<CryptoMetadataEntity>().findFirst()?.userId
        }?.let {
            getCrossSigningInfo(it)
        }
    }

    override fun setMyCrossSigningInfo(info: MXCrossSigningInfo?) {
        doRealmTransaction(realmConfiguration) { realm ->
            realm.where<CryptoMetadataEntity>().findFirst()?.userId?.let { userId ->
                addOrUpdateCrossSigningInfo(realm, userId, info)
            }
        }
    }

    override fun setUserKeysAsTrusted(userId: String, trusted: Boolean) {
        doRealmTransaction(realmConfiguration) { realm ->
            val xInfoEntity = realm.where(CrossSigningInfoEntity::class.java)
                    .equalTo(CrossSigningInfoEntityFields.USER_ID, userId)
                    .findFirst()
            xInfoEntity?.crossSigningKeys?.forEach { info ->
                val level = info.trustLevelEntity
                if (level == null) {
                    val newLevel = realm.createObject(TrustLevelEntity::class.java)
                    newLevel.locallyVerified = trusted
                    newLevel.crossSignedVerified = trusted
                    info.trustLevelEntity = newLevel
                } else {
                    level.locallyVerified = trusted
                    level.crossSignedVerified = trusted
                }
            }
        }
    }

    override fun setDeviceTrust(userId: String, deviceId: String, crossSignedVerified: Boolean, locallyVerified: Boolean?) {
        doRealmTransaction(realmConfiguration) { realm ->
            realm.where(DeviceInfoEntity::class.java)
                    .equalTo(DeviceInfoEntityFields.PRIMARY_KEY, DeviceInfoEntity.createPrimaryKey(userId, deviceId))
                    .findFirst()?.let { deviceInfoEntity ->
                        val trustEntity = deviceInfoEntity.trustLevelEntity
                        if (trustEntity == null) {
                            realm.createObject(TrustLevelEntity::class.java).let {
                                it.locallyVerified = locallyVerified
                                it.crossSignedVerified = crossSignedVerified
                                deviceInfoEntity.trustLevelEntity = it
                            }
                        } else {
                            locallyVerified?.let { trustEntity.locallyVerified = it }
                            trustEntity.crossSignedVerified = crossSignedVerified
                        }
                    }
        }
    }

    override fun clearOtherUserTrust() {
        doRealmTransaction(realmConfiguration) { realm ->
            val xInfoEntities = realm.where(CrossSigningInfoEntity::class.java)
                    .findAll()
            xInfoEntities?.forEach { info ->
                // Need to ignore mine
                if (info.userId != userId) {
                    info.crossSigningKeys.forEach {
                        it.trustLevelEntity = null
                    }
                }
            }
        }
    }

    override fun updateUsersTrust(check: (String) -> Boolean) {
        doRealmTransaction(realmConfiguration) { realm ->
            val xInfoEntities = realm.where(CrossSigningInfoEntity::class.java)
                    .findAll()
            xInfoEntities?.forEach { xInfoEntity ->
                // Need to ignore mine
                if (xInfoEntity.userId == userId) return@forEach
                val mapped = mapCrossSigningInfoEntity(xInfoEntity)
                val currentTrust = mapped.isTrusted()
                val newTrust = check(mapped.userId)
                if (currentTrust != newTrust) {
                    xInfoEntity.crossSigningKeys.forEach { info ->
                        val level = info.trustLevelEntity
                        if (level == null) {
                            val newLevel = realm.createObject(TrustLevelEntity::class.java)
                            newLevel.locallyVerified = newTrust
                            newLevel.crossSignedVerified = newTrust
                            info.trustLevelEntity = newLevel
                        } else {
                            level.locallyVerified = newTrust
                            level.crossSignedVerified = newTrust
                        }
                    }
                }
            }
        }
    }

    override fun getOutgoingRoomKeyRequests(): List<OutgoingKeyRequest> {
        return monarchy.fetchAllMappedSync({ realm ->
            realm
                    .where(OutgoingKeyRequestEntity::class.java)
        }, { entity ->
            entity.toOutgoingKeyRequest()
        })
                .filterNotNull()
    }

    override fun getOutgoingRoomKeyRequests(inStates: Set<OutgoingRoomKeyRequestState>): List<OutgoingKeyRequest> {
        return monarchy.fetchAllMappedSync({ realm ->
            realm
                    .where(OutgoingKeyRequestEntity::class.java)
                    .`in`(OutgoingKeyRequestEntityFields.REQUEST_STATE_STR, inStates.map { it.name }.toTypedArray())
        }, { entity ->
            entity.toOutgoingKeyRequest()
        })
                .filterNotNull()
    }

    override fun getOutgoingRoomKeyRequestsPaged(): LiveData<PagedList<OutgoingKeyRequest>> {
        val realmDataSourceFactory = monarchy.createDataSourceFactory { realm ->
            realm
                    .where(OutgoingKeyRequestEntity::class.java)
        }
        val dataSourceFactory = realmDataSourceFactory.map {
            it.toOutgoingKeyRequest()
        }
        val trail = monarchy.findAllPagedWithChanges(
                realmDataSourceFactory,
                LivePagedListBuilder(
                        dataSourceFactory,
                        PagedList.Config.Builder()
                                .setPageSize(20)
                                .setEnablePlaceholders(false)
                                .setPrefetchDistance(1)
                                .build()
                )
        )
        return trail
    }

    override fun getCrossSigningInfo(userId: String): MXCrossSigningInfo? {
        return doWithRealm(realmConfiguration) { realm ->
            val crossSigningInfo = realm.where(CrossSigningInfoEntity::class.java)
                    .equalTo(CrossSigningInfoEntityFields.USER_ID, userId)
                    .findFirst()
            if (crossSigningInfo == null) {
                null
            } else {
                mapCrossSigningInfoEntity(crossSigningInfo)
            }
        }
    }

    private fun mapCrossSigningInfoEntity(xsignInfo: CrossSigningInfoEntity): MXCrossSigningInfo {
        val userId = xsignInfo.userId ?: ""
        return MXCrossSigningInfo(
                userId = userId,
                crossSigningKeys = xsignInfo.crossSigningKeys.mapNotNull {
                    crossSigningKeysMapper.map(userId, it)
                }
        )
    }

    override fun getLiveCrossSigningInfo(userId: String): LiveData<Optional<MXCrossSigningInfo>> {
        val liveData = monarchy.findAllMappedWithChanges(
                { realm: Realm ->
                    realm.where<CrossSigningInfoEntity>()
                            .equalTo(UserEntityFields.USER_ID, userId)
                },
                { mapCrossSigningInfoEntity(it) }
        )
        return Transformations.map(liveData) {
            it.firstOrNull().toOptional()
        }
    }

    override fun setCrossSigningInfo(userId: String, info: MXCrossSigningInfo?) {
        doRealmTransaction(realmConfiguration) { realm ->
            addOrUpdateCrossSigningInfo(realm, userId, info)
        }
    }

    override fun markMyMasterKeyAsLocallyTrusted(trusted: Boolean) {
        doRealmTransaction(realmConfiguration) { realm ->
            realm.where<CryptoMetadataEntity>().findFirst()?.userId?.let { myUserId ->
                CrossSigningInfoEntity.get(realm, myUserId)?.getMasterKey()?.let { xInfoEntity ->
                    val level = xInfoEntity.trustLevelEntity
                    if (level == null) {
                        val newLevel = realm.createObject(TrustLevelEntity::class.java)
                        newLevel.locallyVerified = trusted
                        xInfoEntity.trustLevelEntity = newLevel
                    } else {
                        level.locallyVerified = trusted
                    }
                }
            }
        }
    }

    private fun addOrUpdateCrossSigningInfo(realm: Realm, userId: String, info: MXCrossSigningInfo?): CrossSigningInfoEntity? {
        if (info == null) {
            // Delete known if needed
            CrossSigningInfoEntity.get(realm, userId)?.deleteFromRealm()
            return null
            // TODO notify, we might need to untrust things?
        } else {
            // Just override existing, caller should check and untrust id needed
            val existing = CrossSigningInfoEntity.getOrCreate(realm, userId)
            existing.crossSigningKeys.clearWith { it.deleteOnCascade() }
            existing.crossSigningKeys.addAll(
                    info.crossSigningKeys.map {
                        crossSigningKeysMapper.map(it)
                    }
            )
            return existing
        }
    }

    override fun addWithHeldMegolmSession(withHeldContent: RoomKeyWithHeldContent) {
        val roomId = withHeldContent.roomId ?: return
        val sessionId = withHeldContent.sessionId ?: return
        if (withHeldContent.algorithm != MXCRYPTO_ALGORITHM_MEGOLM) return
        doRealmTransaction(realmConfiguration) { realm ->
            WithHeldSessionEntity.getOrCreate(realm, roomId, sessionId)?.let {
                it.code = withHeldContent.code
                it.senderKey = withHeldContent.senderKey
                it.reason = withHeldContent.reason
            }
        }
    }

    override fun getWithHeldMegolmSession(roomId: String, sessionId: String): RoomKeyWithHeldContent? {
        return doWithRealm(realmConfiguration) { realm ->
            WithHeldSessionEntity.get(realm, roomId, sessionId)?.let {
                RoomKeyWithHeldContent(
                        roomId = roomId,
                        sessionId = sessionId,
                        algorithm = it.algorithm,
                        codeString = it.codeString,
                        reason = it.reason,
                        senderKey = it.senderKey
                )
            }
        }
    }

    override fun markedSessionAsShared(
            roomId: String?,
            sessionId: String,
            userId: String,
            deviceId: String,
            deviceIdentityKey: String,
            chainIndex: Int
    ) {
        doRealmTransaction(realmConfiguration) { realm ->
            SharedSessionEntity.create(
                    realm = realm,
                    roomId = roomId,
                    sessionId = sessionId,
                    userId = userId,
                    deviceId = deviceId,
                    deviceIdentityKey = deviceIdentityKey,
                    chainIndex = chainIndex
            )
        }
    }

    override fun getSharedSessionInfo(roomId: String?, sessionId: String, deviceInfo: CryptoDeviceInfo): IMXCryptoStore.SharedSessionResult {
        return doWithRealm(realmConfiguration) { realm ->
            SharedSessionEntity.get(
                    realm = realm,
                    roomId = roomId,
                    sessionId = sessionId,
                    userId = deviceInfo.userId,
                    deviceId = deviceInfo.deviceId,
                    deviceIdentityKey = deviceInfo.identityKey()
            )?.let {
                IMXCryptoStore.SharedSessionResult(true, it.chainIndex)
            } ?: IMXCryptoStore.SharedSessionResult(false, null)
        }
    }

    override fun getSharedWithInfo(roomId: String?, sessionId: String): MXUsersDevicesMap<Int> {
        return doWithRealm(realmConfiguration) { realm ->
            val result = MXUsersDevicesMap<Int>()
            SharedSessionEntity.get(realm, roomId, sessionId)
                    .groupBy { it.userId }
                    .forEach { (userId, shared) ->
                        shared.forEach {
                            result.setObject(userId, it.deviceId, it.chainIndex)
                        }
                    }

            result
        }
    }

    /**
     * Some entries in the DB can get a bit out of control with time.
     * So we need to tidy up a bit.
     */
    override fun tidyUpDataBase() {
        val prevWeekTs = clock.epochMillis() - 7 * 24 * 60 * 60 * 1_000
        doRealmTransaction(realmConfiguration) { realm ->

            // Clean the old ones?
            realm.where<OutgoingKeyRequestEntity>()
                    .lessThan(OutgoingKeyRequestEntityFields.CREATION_TIME_STAMP, prevWeekTs)
                    .findAll()
                    .also { Timber.i("## Crypto Clean up ${it.size} OutgoingKeyRequestEntity") }
                    .deleteAllFromRealm()

            // Only keep one month history

            val prevMonthTs = clock.epochMillis() - 4 * 7 * 24 * 60 * 60 * 1_000L
            realm.where<AuditTrailEntity>()
                    .lessThan(AuditTrailEntityFields.AGE_LOCAL_TS, prevMonthTs)
                    .findAll()
                    .also { Timber.i("## Crypto Clean up ${it.size} AuditTrailEntity") }
                    .deleteAllFromRealm()

            // Can we do something for WithHeldSessionEntity?
        }
    }
}
