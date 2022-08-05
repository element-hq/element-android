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
import androidx.lifecycle.asLiveData
import androidx.paging.PagedList
import io.realm.kotlin.MutableRealm
import io.realm.kotlin.TypedRealm
import io.realm.kotlin.UpdatePolicy
import io.realm.kotlin.query.RealmQuery
import io.realm.kotlin.query.RealmSingleQuery
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
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
import org.matrix.android.sdk.internal.crypto.model.MXInboundMegolmSessionWrapper
import org.matrix.android.sdk.internal.crypto.model.OlmSessionWrapper
import org.matrix.android.sdk.internal.crypto.model.OutboundGroupSessionWrapper
import org.matrix.android.sdk.internal.crypto.store.IMXCryptoStore
import org.matrix.android.sdk.internal.crypto.store.db.mapper.CrossSigningKeysMapper
import org.matrix.android.sdk.internal.crypto.store.db.model.AuditTrailEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.AuditTrailMapper
import org.matrix.android.sdk.internal.crypto.store.db.model.CrossSigningInfoEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.CryptoMapper
import org.matrix.android.sdk.internal.crypto.store.db.model.CryptoMetadataEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.CryptoRoomEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.DeviceInfoEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.KeysBackupDataEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.MyDeviceLastSeenInfoEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.OlmInboundGroupSessionEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.OlmSessionEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.OutboundGroupSessionInfoEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.OutgoingKeyRequestEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.SharedSessionEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.TrustLevelEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.UserEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.WithHeldSessionEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.createPrimaryKey
import org.matrix.android.sdk.internal.crypto.store.db.query.crossSigningInfoEntityQueries
import org.matrix.android.sdk.internal.crypto.store.db.query.userEntityQueries
import org.matrix.android.sdk.internal.crypto.util.RequestIdHelper
import org.matrix.android.sdk.internal.database.RealmInstance
import org.matrix.android.sdk.internal.database.andIf
import org.matrix.android.sdk.internal.database.clearWith
import org.matrix.android.sdk.internal.database.deleteAll
import org.matrix.android.sdk.internal.database.deleteNullable
import org.matrix.android.sdk.internal.database.queryIn
import org.matrix.android.sdk.internal.di.CryptoDatabase
import org.matrix.android.sdk.internal.di.DeviceId
import org.matrix.android.sdk.internal.di.MoshiProvider
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.util.mapOptional
import org.matrix.android.sdk.internal.util.time.Clock
import org.matrix.olm.OlmAccount
import org.matrix.olm.OlmException
import org.matrix.olm.OlmOutboundGroupSession
import timber.log.Timber
import javax.inject.Inject

private val loggerTag = LoggerTag("RealmCryptoStore", LoggerTag.CRYPTO)

internal class RealmCryptoStore @Inject constructor(
        @CryptoDatabase private val realmInstance: RealmInstance,
        private val crossSigningKeysMapper: CrossSigningKeysMapper,
        @UserId private val userId: String,
        @DeviceId private val deviceId: String?,
        private val clock: Clock,
) : IMXCryptoStore {

    /* ==========================================================================================
     * Memory cache, to correctly release JNI objects
     * ========================================================================================== */

    // The olm account
    private var olmAccount: OlmAccount? = null

    private val newSessionListeners = ArrayList<NewSessionListener>()

    override fun addNewSessionListener(listener: NewSessionListener) {
        if (!newSessionListeners.contains(listener)) newSessionListeners.add(listener)
    }

    override fun removeSessionListener(listener: NewSessionListener) {
        newSessionListeners.remove(listener)
    }

    init {
        // Ensure CryptoMetadataEntity is inserted in DB
        realmInstance.blockingWrite {
            var currentMetadata = query(CryptoMetadataEntity::class).first().find()
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
                    deleteAll()
                }
                // Metadata not found, or database cleaned, create it
                val cryptoMetadata = CryptoMetadataEntity().apply {
                    userId = this@RealmCryptoStore.userId
                    deviceId = this@RealmCryptoStore.deviceId
                }
                copyToRealm(cryptoMetadata)
            }
        }
    }

    /* ==========================================================================================
     * Other data
     * ========================================================================================== */

    override fun hasData(): Boolean {
        return realmInstance.getBlockingRealm()
                .query(CryptoMetadataEntity::class)
                .count()
                .find() > 0
    }

    override fun deleteStore() {
        realmInstance.blockingWrite {
            deleteAll()
        }
    }

    override fun open() = Unit

    override fun close() {
        // Ensure no async request will be run later
        olmAccount?.releaseAccount()
    }

    private fun updateCryptoMetadata(operation: (CryptoMetadataEntity) -> Unit) {
        realmInstance.blockingWrite {
            val cryptoMetadata = queryCryptoMetadataEntity().find() ?: return@blockingWrite
            operation(cryptoMetadata)
        }
    }

    private fun TypedRealm.queryCryptoMetadataEntity(): RealmSingleQuery<CryptoMetadataEntity> {
        return query(CryptoMetadataEntity::class).first()
    }

    override fun storeDeviceId(deviceId: String) = updateCryptoMetadata {
        it.deviceId = deviceId
    }

    override fun getDeviceId(): String {
        return realmInstance.getBlockingRealm()
                .queryCryptoMetadataEntity()
                .find()
                ?.deviceId ?: ""
    }

    override fun saveOlmAccount() = updateCryptoMetadata {
        it.putOlmAccount(olmAccount)
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
        realmInstance.blockingWrite {
            val metaData = queryCryptoMetadataEntity().find()
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
        val primaryKey = DeviceInfoEntity.createPrimaryKey(userId, deviceId)
        return realmInstance.getBlockingRealm()
                .query(DeviceInfoEntity::class, "primaryKey == $0", primaryKey)
                .first()
                .find()
                ?.let { deviceInfo ->
                    CryptoMapper.mapToModel(deviceInfo)
                }
    }

    override fun deviceWithIdentityKey(identityKey: String): CryptoDeviceInfo? {
        return realmInstance.getBlockingRealm()
                .query(DeviceInfoEntity::class, "keysMapJson == $0", identityKey)
                .find()
                .map { CryptoMapper.mapToModel(it) }
                .firstOrNull {
                    it.identityKey() == identityKey
                }
    }

    override fun storeUserDevices(userId: String, devices: Map<String, CryptoDeviceInfo>?) {
        realmInstance.blockingWrite {
            val existingUserEntity = userEntityQueries().firstUserId(userId).find()
            if (devices == null) {
                Timber.d("Remove user $userId")
                deleteOnCascade(existingUserEntity)
            } else {
                val userEntity = if (existingUserEntity != null) {
                    existingUserEntity
                } else {
                    val newUserEntity = UserEntity().apply {
                        this.userId = userId
                    }
                    copyToRealm(newUserEntity)
                }
                // First delete the removed devices
                val deviceIds = devices.keys
                userEntity.devices.toTypedArray().iterator().let {
                    while (it.hasNext()) {
                        val deviceInfoEntity = it.next()
                        if (deviceInfoEntity.deviceId !in deviceIds) {
                            Timber.d("Remove device ${deviceInfoEntity.deviceId} of user $userId")
                            deleteOnCascade(deviceInfoEntity)
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
                        deleteNullable(existingDeviceInfoEntity.trustLevelEntity)
                        CryptoMapper.updateDeviceInfoEntity(existingDeviceInfoEntity, cryptoDeviceInfo)
                    }
                }
            }
        }
    }

    private fun MutableRealm.deleteOnCascade(userEntity: UserEntity?) {
        if (userEntity == null) return
        userEntity.devices.clearWith {
            deleteOnCascade(it)
        }
        deleteOnCascade(userEntity.crossSigningInfoEntity)
        delete(userEntity)
    }

    private fun MutableRealm.deleteOnCascade(crossSigningInfoEntity: CrossSigningInfoEntity?) {
        crossSigningInfoEntity?.crossSigningKeys?.clearWith { keyInfoEntity ->
            deleteNullable(keyInfoEntity.trustLevelEntity)
            delete(keyInfoEntity)
        }
        deleteNullable(crossSigningInfoEntity)
    }

    private fun MutableRealm.deleteOnCascade(deviceInfoEntity: DeviceInfoEntity) {
        deleteNullable(deviceInfoEntity.trustLevelEntity)
        delete(deviceInfoEntity)
    }

    private fun MutableRealm.getOrCreateUserEntity(userId: String): UserEntity {
        val existingUserEntity = userEntityQueries().firstUserId(userId).find()
        return if (existingUserEntity != null) {
            existingUserEntity
        } else {
            val newUserEntity = UserEntity().apply {
                this.userId = userId
            }
            copyToRealm(newUserEntity)
        }
    }

    private fun MutableRealm.getOrCreateCrossSigningInfoEntity(userId: String): CrossSigningInfoEntity {
        val existingEntity = crossSigningInfoEntityQueries().firstUserId(userId).find()
        return if (existingEntity != null) {
            existingEntity
        } else {
            val newEntity = CrossSigningInfoEntity().apply {
                this.userId = userId
            }
            copyToRealm(newEntity)
        }
    }

    override fun storeUserCrossSigningKeys(
            userId: String,
            masterKey: CryptoCrossSigningKey?,
            selfSigningKey: CryptoCrossSigningKey?,
            userSigningKey: CryptoCrossSigningKey?
    ) {
        realmInstance.blockingWrite {
            val userEntity = getOrCreateUserEntity(userId)
            if (masterKey == null || selfSigningKey == null) {
                // The user has disabled cross signing?
                deleteOnCascade(userEntity.crossSigningInfoEntity)
                userEntity.crossSigningInfoEntity = null
            } else {
                var shouldResetMyDevicesLocalTrust = false
                val signingInfo = getOrCreateCrossSigningInfoEntity(userId)
                // What should we do if we detect a change of the keys?
                val existingMaster = signingInfo.getMasterKey()
                if (existingMaster != null && existingMaster.publicKeyBase64 == masterKey.unpaddedBase64PublicKey) {
                    crossSigningKeysMapper.update(existingMaster, masterKey)
                } else {
                    Timber.d("## CrossSigning  MSK change for $userId")
                    val keyEntity = crossSigningKeysMapper.map(masterKey)
                    signingInfo.setMasterKey(keyEntity)
                    if (userId == this@RealmCryptoStore.userId) {
                        shouldResetMyDevicesLocalTrust = true
                        // my msk has changed! clear my private key
                        // Could we have some race here? e.g I am the one that did change the keys
                        // could i get this update to early and clear the private keys?
                        // -> initializeCrossSigning is guarding for that by storing all at once
                        queryCryptoMetadataEntity().find()?.xSignMasterPrivateKey = null
                    }
                }

                val existingSelfSigned = signingInfo.getSelfSignedKey()
                if (existingSelfSigned != null && existingSelfSigned.publicKeyBase64 == selfSigningKey.unpaddedBase64PublicKey) {
                    crossSigningKeysMapper.update(existingSelfSigned, selfSigningKey)
                } else {
                    Timber.d("## CrossSigning  SSK change for $userId")
                    val keyEntity = crossSigningKeysMapper.map(selfSigningKey)
                    signingInfo.setSelfSignedKey(keyEntity)
                    if (userId == this@RealmCryptoStore.userId) {
                        shouldResetMyDevicesLocalTrust = true
                        // my ssk has changed! clear my private key
                        queryCryptoMetadataEntity().find()?.xSignSelfSignedPrivateKey = null
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
                        if (userId == this@RealmCryptoStore.userId) {
                            shouldResetMyDevicesLocalTrust = true
                            // my usk has changed! clear my private key
                            queryCryptoMetadataEntity().find()?.xSignUserPrivateKey = null
                        }
                    }
                }

                // When my cross signing keys are reset, we consider clearing all existing device trust
                if (shouldResetMyDevicesLocalTrust) {
                    userEntityQueries()
                            .firstUserId(this@RealmCryptoStore.userId)
                            .find()
                            ?.devices?.forEach {
                                it.trustLevelEntity?.crossSignedVerified = false
                                it.trustLevelEntity?.locallyVerified = it.deviceId == deviceId
                            }
                }
                userEntity.crossSigningInfoEntity = signingInfo
            }

        }
    }

    override fun getCrossSigningPrivateKeys(): PrivateKeysInfo? {
        return realmInstance.getBlockingRealm()
                .queryCryptoMetadataEntity()
                .find()
                ?.let {
                    PrivateKeysInfo(
                            master = it.xSignMasterPrivateKey,
                            selfSigned = it.xSignSelfSignedPrivateKey,
                            user = it.xSignUserPrivateKey
                    )
                }
    }

    override fun getLiveCrossSigningPrivateKeys(): LiveData<Optional<PrivateKeysInfo>> {
        return realmInstance.queryFirst {
            it.queryCryptoMetadataEntity()
        }.mapOptional {
            PrivateKeysInfo(
                    master = it.xSignMasterPrivateKey,
                    selfSigned = it.xSignSelfSignedPrivateKey,
                    user = it.xSignUserPrivateKey
            )
        }.asLiveData()
    }

    override fun storePrivateKeysInfo(msk: String?, usk: String?, ssk: String?) = updateCryptoMetadata {
        Timber.v("## CRYPTO | *** storePrivateKeysInfo ${msk != null}, ${usk != null}, ${ssk != null}")
        it.xSignMasterPrivateKey = msk
        it.xSignUserPrivateKey = usk
        it.xSignSelfSignedPrivateKey = ssk
    }

    override fun saveBackupRecoveryKey(recoveryKey: String?, version: String?) = updateCryptoMetadata {
        it.keyBackupRecoveryKey = recoveryKey
        it.keyBackupRecoveryKeyVersion = version
    }

    override fun getKeyBackupRecoveryKeyInfo(): SavedKeyBackupKeyInfo? {
        return realmInstance.getBlockingRealm()
                .queryCryptoMetadataEntity()
                .find()
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

    override fun storeMSKPrivateKey(msk: String?) = updateCryptoMetadata {
        Timber.v("## CRYPTO | *** storeMSKPrivateKey ${msk != null} ")
        it.xSignMasterPrivateKey = msk
    }

    override fun storeSSKPrivateKey(ssk: String?) = updateCryptoMetadata {
        Timber.v("## CRYPTO | *** storeSSKPrivateKey ${ssk != null} ")
        it.xSignSelfSignedPrivateKey = ssk
    }

    override fun storeUSKPrivateKey(usk: String?) = updateCryptoMetadata {
        Timber.v("## CRYPTO | *** storeUSKPrivateKey ${usk != null} ")
        it.xSignUserPrivateKey = usk
    }

    override fun getUserDevices(userId: String): Map<String, CryptoDeviceInfo>? {
        return realmInstance.getBlockingRealm()
                .userEntityQueries()
                .firstUserId(userId)
                .find()
                ?.devices
                ?.map { deviceInfo ->
                    CryptoMapper.mapToModel(deviceInfo)
                }
                ?.associateBy { cryptoDevice ->
                    cryptoDevice.deviceId
                }
    }

    override fun getUserDeviceList(userId: String): List<CryptoDeviceInfo>? {
        return realmInstance.getBlockingRealm()
                .userEntityQueries()
                .firstUserId(userId)
                .find()
                ?.devices
                ?.map { deviceInfo ->
                    CryptoMapper.mapToModel(deviceInfo)
                }
    }

    private val userEntityToDeviceInfoMapper = { userEntity: UserEntity ->
        userEntity.devices.map { CryptoMapper.mapToModel(it) }
    }

    override fun getLiveDeviceList(userId: String): LiveData<List<CryptoDeviceInfo>> {
        return realmInstance.queryList(userEntityToDeviceInfoMapper) {
            it.userEntityQueries().byUserId(userId)
        }
                .map { it.flatten() }
                .asLiveData()
    }

    override fun getLiveDeviceList(userIds: List<String>): LiveData<List<CryptoDeviceInfo>> {
        return realmInstance.queryList(userEntityToDeviceInfoMapper) {
            it.userEntityQueries().byUserIds(userIds)
        }
                .map { it.flatten() }
                .asLiveData()
    }

    override fun getLiveDeviceList(): LiveData<List<CryptoDeviceInfo>> {
        return realmInstance.queryList(userEntityToDeviceInfoMapper) {
            it.userEntityQueries().all()
        }
                .map { it.flatten() }
                .asLiveData()
    }

    override fun getMyDevicesInfo(): List<DeviceInfo> {
        return realmInstance.getBlockingRealm()
                .queryMyDeviceLastSeenInfoEntity()
                .find()
                .map { it.toDomain() }
    }

    override fun getLiveMyDevicesInfo(): LiveData<List<DeviceInfo>> {
        val mapper = { entity: MyDeviceLastSeenInfoEntity ->
            entity.toDomain()
        }
        return realmInstance.queryList(mapper) {
            it.query(MyDeviceLastSeenInfoEntity::class)
        }.asLiveData()
    }

    private fun MyDeviceLastSeenInfoEntity.toDomain(): DeviceInfo {
        return DeviceInfo(
                deviceId = deviceId,
                lastSeenIp = lastSeenIp,
                lastSeenTs = lastSeenTs,
                displayName = displayName
        )
    }

    override fun saveMyDevicesInfo(info: List<DeviceInfo>) {
        realmInstance.blockingWrite {
            delete(queryMyDeviceLastSeenInfoEntity().find())
            info.forEach {
                val infoEntity = MyDeviceLastSeenInfoEntity().apply {
                    lastSeenTs = it.lastSeenTs
                    lastSeenIp = it.lastSeenIp
                    displayName = it.displayName
                    deviceId = it.deviceId
                }
                copyToRealm(infoEntity)
            }
        }
    }

    private fun TypedRealm.queryMyDeviceLastSeenInfoEntity(): RealmQuery<MyDeviceLastSeenInfoEntity> {
        return query(MyDeviceLastSeenInfoEntity::class)
    }

    private fun TypedRealm.queryCryptoRoomEntity(roomId: String): RealmSingleQuery<CryptoRoomEntity> {
        return query(CryptoRoomEntity::class, "roomId == $0", roomId).first()
    }

    private fun upsertCryptoRoomEntity(roomId: String, operation: (CryptoRoomEntity) -> Unit) {
        realmInstance.blockingWrite {
            val roomEntity = queryCryptoRoomEntity(roomId).find()
            if (roomEntity != null) {
                operation(roomEntity)
            } else {
                val newRoomEntity = CryptoRoomEntity().apply {
                    this.roomId = roomId
                }
                operation(newRoomEntity)
                copyToRealm(newRoomEntity)
            }
        }
    }

    override fun storeRoomAlgorithm(roomId: String, algorithm: String?) = upsertCryptoRoomEntity(roomId) { roomEntity ->
        roomEntity.algorithm = algorithm
        if (algorithm == MXCRYPTO_ALGORITHM_MEGOLM) {
            roomEntity.wasEncryptedOnce = true
        }
    }

    override fun getRoomAlgorithm(roomId: String): String? {
        return realmInstance.getBlockingRealm()
                .queryCryptoRoomEntity(roomId)
                .find()
                ?.algorithm
    }

    override fun roomWasOnceEncrypted(roomId: String): Boolean {
        return realmInstance.getBlockingRealm()
                .queryCryptoRoomEntity(roomId)
                .find()
                ?.wasEncryptedOnce ?: false
    }

    override fun shouldEncryptForInvitedMembers(roomId: String): Boolean {
        return realmInstance.getBlockingRealm()
                .queryCryptoRoomEntity(roomId)
                .find()
                ?.shouldEncryptForInvitedMembers ?: false
    }

    override fun shouldShareHistory(roomId: String): Boolean {
        return realmInstance.getBlockingRealm()
                .queryCryptoRoomEntity(roomId)
                .find()
                ?.shouldShareHistory ?: false
    }

    override fun setShouldEncryptForInvitedMembers(roomId: String, shouldEncryptForInvitedMembers: Boolean) = upsertCryptoRoomEntity(roomId) {
        it.shouldEncryptForInvitedMembers = shouldEncryptForInvitedMembers
    }

    override fun setShouldShareHistory(roomId: String, shouldShareHistory: Boolean) = upsertCryptoRoomEntity(roomId) {
        Timber.tag(loggerTag.value)
                .v("setShouldShareHistory for room $roomId is $shouldShareHistory")
        it.shouldShareHistory = shouldShareHistory
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
            realmInstance.blockingWrite {
                val realmOlmSession = OlmSessionEntity().apply {
                    primaryKey = key
                    sessionId = sessionIdentifier
                    this.deviceKey = deviceKey
                    putOlmSession(olmSessionWrapper.olmSession)
                    lastReceivedMessageTs = olmSessionWrapper.lastReceivedMessageTs
                }
                copyToRealm(realmOlmSession, updatePolicy = UpdatePolicy.ALL)
            }
        }
    }

    override fun getDeviceSession(sessionId: String, deviceKey: String): OlmSessionWrapper? {
        val key = OlmSessionEntity.createPrimaryKey(sessionId, deviceKey)
        return realmInstance.getBlockingRealm()
                .query(OlmSessionEntity::class, "primaryKey == $0", key)
                .first()
                .find()
                ?.let {
                    val olmSession = it.getOlmSession()
                    if (olmSession != null && it.sessionId != null) {
                        return@let OlmSessionWrapper(olmSession, it.lastReceivedMessageTs)
                    }
                    null
                }
    }

    override fun getLastUsedSessionId(deviceKey: String): String? {
        return realmInstance.getBlockingRealm()
                .queryOlmSessionEntity(deviceKey)
                .sort("lastReceivedMessageTs", io.realm.kotlin.query.Sort.DESCENDING)
                .find()
                .firstOrNull()
                ?.sessionId
    }

    override fun getDeviceSessionIds(deviceKey: String): List<String> {
        return realmInstance.getBlockingRealm()
                .queryOlmSessionEntity(deviceKey)
                .sort("lastReceivedMessageTs", io.realm.kotlin.query.Sort.DESCENDING)
                .find()
                .mapNotNull { sessionEntity ->
                    sessionEntity.sessionId
                }
    }

    private fun TypedRealm.queryOlmSessionEntity(deviceKey: String): RealmQuery<OlmSessionEntity> {
        return query(OlmSessionEntity::class, "deviceKey == $0", deviceKey)
    }

    override fun storeInboundGroupSessions(sessions: List<MXInboundMegolmSessionWrapper>) {
        if (sessions.isEmpty()) {
            return
        }
        realmInstance.blockingWrite {
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

                val existing = queryOlmInboundGroupSessionEntity(key)
                        .first()
                        .find()

                val realmOlmInboundGroupSession = OlmInboundGroupSessionEntity().apply {
                    primaryKey = key
                    store(wrapper)
                    backedUp = existing?.backedUp ?: false
                }
                Timber.v("## CRYPTO | shouldShareHistory: ${wrapper.sessionData.sharedHistory} for $key")
                copyToRealm(realmOlmInboundGroupSession, updatePolicy = UpdatePolicy.ALL)
            }
        }
    }

    override fun getInboundGroupSession(sessionId: String, senderKey: String): MXInboundMegolmSessionWrapper? {
        return realmInstance.getBlockingRealm()
                .queryOlmInboundGroupSessionEntity(sessionId, senderKey)
                .first()
                .find()
                ?.toModel()
    }

    override fun getInboundGroupSession(sessionId: String, senderKey: String, sharedHistory: Boolean): MXInboundMegolmSessionWrapper? {
        return realmInstance.getBlockingRealm()
                .queryOlmInboundGroupSessionEntity(sessionId, senderKey)
                .query("sharedHistory == $0", sharedHistory)
                .first()
                .find()
                ?.toModel()
    }

    override fun getCurrentOutboundGroupSessionForRoom(roomId: String): OutboundGroupSessionWrapper? {
        return realmInstance.getBlockingRealm()
                .queryCryptoRoomEntity(roomId)
                .find()
                ?.outboundSessionInfo?.let { entity ->
                    entity.getOutboundGroupSession()?.let {
                        OutboundGroupSessionWrapper(
                                it,
                                entity.creationTime ?: 0,
                                entity.shouldShareHistory
                        )
                    }
                }
    }

    override fun storeCurrentOutboundGroupSessionForRoom(roomId: String, outboundGroupSession: OlmOutboundGroupSession?) {
        // we can do this async, as it's just for restoring on next launch
        // the olmdevice is caching the active instance
        // this is called for each sent message (so not high frequency), thus we can use basic realm async without
        // risk of reaching max async operation limit?
        realmInstance.asyncWrite {
            queryCryptoRoomEntity(roomId).find()?.let { entity ->
                deleteNullable(entity.outboundSessionInfo)
                if (outboundGroupSession != null) {
                    val info = OutboundGroupSessionInfoEntity().apply {
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
        return realmInstance.getBlockingRealm()
                .query(OlmInboundGroupSessionEntity::class)
                .find()
                .mapNotNull { it.toModel() }
    }

    override fun getInboundGroupSessions(roomId: String): List<MXInboundMegolmSessionWrapper> {
        return realmInstance.getBlockingRealm()
                .query(OlmInboundGroupSessionEntity::class, "roomId == $0", roomId)
                .find()
                .mapNotNull { it.toModel() }
    }

    override fun removeInboundGroupSession(sessionId: String, senderKey: String) {
        realmInstance.blockingWrite {
            val olmInboundGroupSessions = queryOlmInboundGroupSessionEntity(sessionId, senderKey).find()
            delete(olmInboundGroupSessions)
        }
    }

    private fun TypedRealm.queryOlmInboundGroupSessionEntity(sessionId: String?, senderKey: String?): RealmQuery<OlmInboundGroupSessionEntity> {
        val key = OlmInboundGroupSessionEntity.createPrimaryKey(sessionId, senderKey)
        return queryOlmInboundGroupSessionEntity(key)
    }

    private fun TypedRealm.queryOlmInboundGroupSessionEntity(primaryKey: String): RealmQuery<OlmInboundGroupSessionEntity> {
        return query(OlmInboundGroupSessionEntity::class, "primaryKey == $0", primaryKey)
    }

    /* ==========================================================================================
     * Keys backup
     * ========================================================================================== */

    override fun getKeyBackupVersion(): String? {
        return realmInstance.getBlockingRealm()
                .queryCryptoMetadataEntity()
                .find()
                ?.backupVersion
    }

    override fun setKeyBackupVersion(keyBackupVersion: String?) = updateCryptoMetadata {
        it.backupVersion = keyBackupVersion
    }

    override fun getKeysBackupData(): KeysBackupDataEntity? {
        return realmInstance.getBlockingRealm()
                .query(KeysBackupDataEntity::class)
                .first()
                .find()
    }

    override fun setKeysBackupData(keysBackupData: KeysBackupDataEntity?) {
        realmInstance.blockingWrite {
            if (keysBackupData == null) {
                // Clear the table
                delete(
                        query(KeysBackupDataEntity::class).find()
                )
            } else {
                // Only one object
                copyToRealm(keysBackupData, updatePolicy = UpdatePolicy.ALL)
            }

        }
    }

    override fun resetBackupMarkers() {
        realmInstance.blockingWrite {
            query(OlmInboundGroupSessionEntity::class)
                    .find()
                    .forEach { inboundGroupSession ->
                        inboundGroupSession.backedUp = false
                    }

        }
    }

    override fun markBackupDoneForInboundGroupSessions(olmInboundGroupSessionWrappers: List<MXInboundMegolmSessionWrapper>) {
        if (olmInboundGroupSessionWrappers.isEmpty()) {
            return
        }
        realmInstance.blockingWrite {
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

                    val existing = queryOlmInboundGroupSessionEntity(key).first().find()

                    if (existing != null) {
                        existing.backedUp = true
                    } else {
                        // ... might be in cache but not yet persisted, create a record to persist backedup state
                        val realmOlmInboundGroupSession = OlmInboundGroupSessionEntity().apply {
                            primaryKey = key
                            store(olmInboundGroupSessionWrapper)
                            backedUp = true
                        }
                        copyToRealm(realmOlmInboundGroupSession)
                    }
                } catch (e: OlmException) {
                    Timber.e(e, "OlmException")
                }
            }
        }
    }

    override fun inboundGroupSessionsToBackup(limit: Int): List<MXInboundMegolmSessionWrapper> {
        return realmInstance.getBlockingRealm()
                .query(OlmInboundGroupSessionEntity::class, "backedUp == false")
                .limit(limit)
                .find()
                .mapNotNull { it.toModel() }
    }

    override fun inboundGroupSessionsCount(onlyBackedUp: Boolean): Int {
        return realmInstance.getBlockingRealm()
                .query(OlmInboundGroupSessionEntity::class)
                .andIf(onlyBackedUp) {
                    query("backedUp == true")
                }
                .count()
                .find()
                .toInt()
    }

    override fun setGlobalBlacklistUnverifiedDevices(block: Boolean) {
        realmInstance.blockingWrite {
            queryCryptoMetadataEntity().find()?.globalBlacklistUnverifiedDevices = block
        }
    }

    override fun enableKeyGossiping(enable: Boolean) {
        realmInstance.blockingWrite {
            queryCryptoMetadataEntity().find()?.globalEnableKeyGossiping = enable
        }
    }

    override fun isKeyGossipingEnabled(): Boolean {
        return realmInstance.getBlockingRealm()
                .queryCryptoMetadataEntity().find()?.globalEnableKeyGossiping
                ?: true
    }

    override fun getGlobalBlacklistUnverifiedDevices(): Boolean {
        return realmInstance.getBlockingRealm()
                .queryCryptoMetadataEntity().find()?.globalBlacklistUnverifiedDevices
                ?: false
    }

    override fun isShareKeysOnInviteEnabled(): Boolean {
        return realmInstance.getBlockingRealm()
                .queryCryptoMetadataEntity().find()?.enableKeyForwardingOnInvite
                ?: false
    }

    override fun enableShareKeyOnInvite(enable: Boolean) {
        realmInstance.blockingWrite {
            queryCryptoMetadataEntity().find()?.enableKeyForwardingOnInvite = enable
        }
    }

    override fun setDeviceKeysUploaded(uploaded: Boolean) {
        realmInstance.blockingWrite {
            queryCryptoMetadataEntity().find()?.deviceKeysSentToServer = uploaded
        }
    }

    override fun areDeviceKeysUploaded(): Boolean {
        return realmInstance.getBlockingRealm()
                .queryCryptoMetadataEntity()
                .find()
                ?.deviceKeysSentToServer ?: false
    }

    override fun setRoomsListBlacklistUnverifiedDevices(roomIds: List<String>) {
        realmInstance.blockingWrite {
            // Reset all
            query(CryptoRoomEntity::class)
                    .find()
                    .forEach { room ->
                        room.blacklistUnverifiedDevices = false
                    }

            // Enable those in the list
            query(CryptoRoomEntity::class)
                    .queryIn("roomId", roomIds)
                    .find()
                    .forEach { room ->
                        room.blacklistUnverifiedDevices = true
                    }
        }
    }

    override fun getRoomsListBlacklistUnverifiedDevices(): List<String> {
        return realmInstance.getBlockingRealm()
                .query(CryptoRoomEntity::class, "blacklistUnverifiedDevices == true")
                .find()
                .mapNotNull { cryptoRoomEntity ->
                    cryptoRoomEntity.roomId
                }
    }

    override fun getDeviceTrackingStatuses(): Map<String, Int> {
        return realmInstance.getBlockingRealm()
                .userEntityQueries()
                .all()
                .find()
                .associateBy { user ->
                    user.userId!!
                }
                .mapValues { entry ->
                    entry.value.deviceTrackingStatus
                }
    }

    override fun saveDeviceTrackingStatuses(deviceTrackingStatuses: Map<String, Int>) {
        realmInstance.blockingWrite {
            deviceTrackingStatuses
                    .map { entry ->
                        upsertUserEntity(entry.key) {
                            it.deviceTrackingStatus = entry.value
                        }
                    }
        }
    }

    private fun MutableRealm.upsertUserEntity(userId: String, operation: (UserEntity) -> Unit) {
        val userEntity = userEntityQueries().firstUserId(userId).find()
        if (userEntity != null) {
            operation(userEntity)
        } else {
            val newUserEntity = UserEntity().apply {
                this.userId = userId
            }
            operation(newUserEntity)
            copyToRealm(newUserEntity)
        }
    }

    override fun getDeviceTrackingStatus(userId: String, defaultValue: Int): Int {
        return realmInstance.getBlockingRealm()
                .userEntityQueries()
                .firstUserId(userId)
                .find()
                ?.deviceTrackingStatus ?: defaultValue
    }

    override fun getOutgoingRoomKeyRequest(requestBody: RoomKeyRequestBody): OutgoingKeyRequest? {
        return realmInstance.getBlockingRealm()
                .query(OutgoingKeyRequestEntity::class)
                .query("roomId == $0", requestBody.roomId)
                .query("megolmSessionId == $0", requestBody.sessionId)
                .find()
                .map {
                    it.toOutgoingKeyRequest()
                }.firstOrNull {
                    it.requestBody?.algorithm == requestBody.algorithm &&
                            it.requestBody?.roomId == requestBody.roomId &&
                            it.requestBody?.senderKey == requestBody.senderKey &&
                            it.requestBody?.sessionId == requestBody.sessionId
                }
    }

    override fun getOutgoingRoomKeyRequest(requestId: String): OutgoingKeyRequest? {
        return realmInstance.getBlockingRealm()
                .query(OutgoingKeyRequestEntity::class, "requestId == $0", requestId)
                .first()
                .find()
                ?.toOutgoingKeyRequest()
    }

    override fun getOutgoingRoomKeyRequest(roomId: String, sessionId: String, algorithm: String, senderKey: String): List<OutgoingKeyRequest> {
        // TODO this annoying we have to load all
        return realmInstance.getBlockingRealm()
                .query(OutgoingKeyRequestEntity::class)
                .query("roomId == $0", roomId)
                .query("megolmSessionId == $0", sessionId)
                .find()
                .map {
                    it.toOutgoingKeyRequest()
                }.filter {
                    it.requestBody?.algorithm == algorithm &&
                            it.requestBody?.senderKey == senderKey
                }
    }

    override fun getGossipingEventsTrail(): LiveData<PagedList<AuditTrail>> {
        val pagedListConfig = PagedList.Config.Builder()
                .setPageSize(20)
                .setEnablePlaceholders(false)
                .setPrefetchDistance(1)
                .build()

        val mapper = { entity: AuditTrailEntity ->
            AuditTrailMapper.map(entity)
            // mm we can't map not null...
                    ?: createUnknownTrail()
        }
        return realmInstance.queryPagedList(pagedListConfig, mapper) { realm ->
            realm.query(AuditTrailEntity::class)
                    .sort("ageLocalTs", io.realm.kotlin.query.Sort.DESCENDING)
        }.asLiveData()
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
        val pagedListConfig = PagedList.Config.Builder()
                .setPageSize(20)
                .setEnablePlaceholders(false)
                .setPrefetchDistance(1)
                .build()

        val pagedMapper = { entity: AuditTrailEntity ->
            (AuditTrailMapper.map(entity)
            // mm we can't map not null...
                    ?: createUnknownTrail()
                    ).let { mapper.invoke(it) }
        }
        return realmInstance.queryPagedList(pagedListConfig, pagedMapper) {
            it.query(AuditTrailEntity::class, "type == $0", type.name)
                    .sort("ageLocalTs", io.realm.kotlin.query.Sort.DESCENDING)
        }.asLiveData()
    }

    override fun getGossipingEvents(): List<AuditTrail> {
        return realmInstance.getBlockingRealm()
                .query(AuditTrailEntity::class)
                .find()
                .mapNotNull {
                    AuditTrailMapper.map(it)
                }
    }

    override fun getOrAddOutgoingRoomKeyRequest(
            requestBody: RoomKeyRequestBody,
            recipients: Map<String, List<String>>,
            fromIndex: Int
    ): OutgoingKeyRequest {
        // Insert the request and return the one passed in parameter
        return realmInstance.blockingWrite {
            val existing = query(OutgoingKeyRequestEntity::class)
                    .query("megolmSessionId == $0", requestBody.sessionId)
                    .query("roomId == $0", requestBody.roomId)
                    .find()
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
                val newEntity = OutgoingKeyRequestEntity().apply {
                    this.requestId = RequestIdHelper.createUniqueRequestId()
                    this.setRecipients(recipients)
                    this.requestedIndex = fromIndex
                    this.requestState = OutgoingRoomKeyRequestState.UNSENT
                    this.setRequestBody(requestBody)
                    this.creationTimeStamp = clock.epochMillis()
                }
                copyToRealm(newEntity)
                newEntity.toOutgoingKeyRequest()
            } else {
                existing
            }
        }
    }

    override fun updateOutgoingRoomKeyRequestState(requestId: String, newState: OutgoingRoomKeyRequestState) {
        realmInstance.blockingWrite {
            query(OutgoingKeyRequestEntity::class, "requestId == $0", requestId)
                    .first()
                    .find()?.apply {
                        this.requestState = newState
                        if (newState == OutgoingRoomKeyRequestState.UNSENT) {
                            // clear the old replies
                            delete(this.replies)
                        }
                    }
        }
    }

    override fun updateOutgoingRoomKeyRequiredIndex(requestId: String, newIndex: Int) {
        realmInstance.blockingWrite {
            query(OutgoingKeyRequestEntity::class, "requestId == $0", requestId)
                    .first()
                    .find()?.apply {
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
        realmInstance.blockingWrite {
            query(OutgoingKeyRequestEntity::class)
                    .query("roomId == $0", roomId)
                    .query("megolmSessionId == $0", sessionId)
                    .find()
                    .firstOrNull { entity ->
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
        realmInstance.blockingWrite {
            val outgoingKeyRequestEntity = query(OutgoingKeyRequestEntity::class, "requestId == $0", requestId)
                    .first()
                    .find() ?: return@blockingWrite
            deleteOnCascade(outgoingKeyRequestEntity)
        }
    }

    override fun deleteOutgoingRoomKeyRequestInState(state: OutgoingRoomKeyRequestState) {
        realmInstance.blockingWrite {
            query(OutgoingKeyRequestEntity::class, "requestStateStr == $0", state.name)
                    .find()
                    .onEach {
                        // I delete like this because I want to cascade delete replies?
                        deleteOnCascade(it)
                    }

        }
    }

    private fun MutableRealm.deleteOnCascade(outgoingKeyRequestEntity: OutgoingKeyRequestEntity) {
        delete(outgoingKeyRequestEntity.replies)
        delete(outgoingKeyRequestEntity)
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
        realmInstance.asyncWrite {
            val now = clock.epochMillis()
            val auditTrailEntity = AuditTrailEntity().apply {
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
            copyToRealm(auditTrailEntity)
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
        realmInstance.asyncWrite {
            val now = clock.epochMillis()
            val auditTrailEntity = AuditTrailEntity().apply {
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
            copyToRealm(auditTrailEntity)
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
        realmInstance.asyncWrite {
            val now = clock.epochMillis()
            val auditTrailEntity = AuditTrailEntity().apply {
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
            copyToRealm(auditTrailEntity)
        }
    }

    /* ==========================================================================================
     * Cross Signing
     * ========================================================================================== */
    override fun getMyCrossSigningInfo(): MXCrossSigningInfo? {
        val realm = realmInstance.getBlockingRealm()
        val userId = realm.queryCryptoMetadataEntity()
                .find()
                ?.userId ?: return null
        return getCrossSigningInfo(userId)
    }

    override fun setMyCrossSigningInfo(info: MXCrossSigningInfo?) {
        addOrUpdateCrossSigningInfo(info) { realm ->
            realm.queryCryptoMetadataEntity()
                    .find()
                    ?.userId
        }
    }

    override fun setUserKeysAsTrusted(userId: String, trusted: Boolean) {
        realmInstance.blockingWrite {
            val xInfoEntity = crossSigningInfoEntityQueries().firstUserId(userId).find()
            xInfoEntity?.crossSigningKeys?.forEach { info ->
                val level = info.trustLevelEntity
                if (level == null) {
                    val newLevel = TrustLevelEntity()
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
        realmInstance.blockingWrite {
            val primaryKey = DeviceInfoEntity.createPrimaryKey(userId, deviceId)
            query(DeviceInfoEntity::class, "primaryKey == $0", primaryKey)
                    .first()
                    .find()
                    ?.let { deviceInfoEntity ->
                        val trustEntity = deviceInfoEntity.trustLevelEntity
                        if (trustEntity == null) {
                            deviceInfoEntity.trustLevelEntity = TrustLevelEntity().apply {
                                this.locallyVerified = locallyVerified
                                this.crossSignedVerified = crossSignedVerified
                            }
                        } else {
                            locallyVerified?.let { trustEntity.locallyVerified = it }
                            trustEntity.crossSignedVerified = crossSignedVerified
                        }
                    }
        }
    }

    override fun clearOtherUserTrust() {
        realmInstance.blockingWrite {
            val xInfoEntities = query(CrossSigningInfoEntity::class).find()
            xInfoEntities.forEach { info ->
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
        realmInstance.blockingWrite {
            val xInfoEntities = query(CrossSigningInfoEntity::class).find()
            xInfoEntities.forEach { xInfoEntity ->
                // Need to ignore mine
                if (xInfoEntity.userId == userId) return@forEach
                val mapped = mapCrossSigningInfoEntity(xInfoEntity)
                val currentTrust = mapped.isTrusted()
                val newTrust = check(mapped.userId)
                if (currentTrust != newTrust) {
                    xInfoEntity.crossSigningKeys.forEach { info ->
                        val level = info.trustLevelEntity
                        if (level == null) {
                            val newLevel = TrustLevelEntity()
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
        return realmInstance.getBlockingRealm()
                .query(OutgoingKeyRequestEntity::class)
                .find()
                .map { entity ->
                    entity.toOutgoingKeyRequest()
                }
    }

    override fun getOutgoingRoomKeyRequests(inStates: Set<OutgoingRoomKeyRequestState>): List<OutgoingKeyRequest> {
        return realmInstance.getBlockingRealm()
                .query(OutgoingKeyRequestEntity::class)
                .queryIn("requestStateStr", inStates.map { it.name })
                .find()
                .map {
                    it.toOutgoingKeyRequest()
                }
    }

    override fun getOutgoingRoomKeyRequestsPaged(): LiveData<PagedList<OutgoingKeyRequest>> {
        val pagedListConfig = PagedList.Config.Builder()
                .setPageSize(20)
                .setEnablePlaceholders(false)
                .setPrefetchDistance(1)
                .build()
        val mapper = { entity: OutgoingKeyRequestEntity ->
            entity.toOutgoingKeyRequest()
        }
        return realmInstance.queryPagedList(pagedListConfig, mapper) {
            it.query(OutgoingKeyRequestEntity::class)
        }.asLiveData()
    }

    override fun getCrossSigningInfo(userId: String): MXCrossSigningInfo? {
        return realmInstance.getBlockingRealm()
                .crossSigningInfoEntityQueries()
                .firstUserId(userId)
                .find()
                ?.let { crossSigningInfo ->
                    mapCrossSigningInfoEntity(crossSigningInfo)
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
        return realmInstance.queryFirst {
            it.crossSigningInfoEntityQueries().firstUserId(userId)
        }.mapOptional {
            mapCrossSigningInfoEntity(it)
        }.asLiveData()
    }

    override fun setCrossSigningInfo(userId: String, info: MXCrossSigningInfo?) {
        addOrUpdateCrossSigningInfo(info) { userId }
    }

    override fun markMyMasterKeyAsLocallyTrusted(trusted: Boolean) {
        realmInstance.blockingWrite {
            val myUserId = queryCryptoMetadataEntity().find()?.userId ?: return@blockingWrite
            crossSigningInfoEntityQueries().firstUserId(myUserId).find()?.getMasterKey()?.let { xInfoEntity ->
                val level = xInfoEntity.trustLevelEntity
                if (level == null) {
                    val newLevel = TrustLevelEntity()
                    newLevel.locallyVerified = trusted
                    xInfoEntity.trustLevelEntity = newLevel
                } else {
                    level.locallyVerified = trusted
                }
            }
        }
    }

    private fun addOrUpdateCrossSigningInfo(info: MXCrossSigningInfo?, userIdQuery: (TypedRealm) -> String?): CrossSigningInfoEntity? {
        return realmInstance.blockingWrite {
            val userId = userIdQuery(this) ?: return@blockingWrite null
            if (info == null) {
                // Delete known if needed
                crossSigningInfoEntityQueries().firstUserId(userId).find()?.also {
                    delete(it)
                }
                null
                // TODO notify, we might need to untrust things?
            } else {
                // Just override existing, caller should check and untrust id needed
                val crossSigningKeys = info.crossSigningKeys.map {
                    crossSigningKeysMapper.map(it)
                }
                val crossSigningInfoEntity = CrossSigningInfoEntity().apply {
                    this.userId = userId
                    this.crossSigningKeys.addAll(crossSigningKeys)
                }
                copyToRealm(crossSigningInfoEntity, updatePolicy = UpdatePolicy.ALL)
            }
        }
    }

    override fun addWithHeldMegolmSession(withHeldContent: RoomKeyWithHeldContent) {
        val roomId = withHeldContent.roomId ?: return
        val sessionId = withHeldContent.sessionId ?: return
        if (withHeldContent.algorithm != MXCRYPTO_ALGORITHM_MEGOLM) return
        realmInstance.blockingWrite {
            val operation: (WithHeldSessionEntity) -> Unit = { withHeldSessionEntity ->
                withHeldSessionEntity.code = withHeldContent.code
                withHeldSessionEntity.senderKey = withHeldContent.senderKey
                withHeldSessionEntity.reason = withHeldContent.reason
            }
            val withHeldSessionEntity = queryWithHeldSessionEntity(roomId, sessionId).find()
            if (withHeldSessionEntity != null) {
                operation(withHeldSessionEntity)
            } else {
                val newWithHeldSessionEntity = WithHeldSessionEntity()
                operation(newWithHeldSessionEntity)
                copyToRealm(newWithHeldSessionEntity)
            }

        }
    }

    override fun getWithHeldMegolmSession(roomId: String, sessionId: String): RoomKeyWithHeldContent? {
        return realmInstance.getBlockingRealm()
                .queryWithHeldSessionEntity(roomId, sessionId)
                .find()
                ?.let {
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

    private fun TypedRealm.queryWithHeldSessionEntity(roomId: String, sessionId: String): RealmSingleQuery<WithHeldSessionEntity> {
        return query(WithHeldSessionEntity::class, "algorithm == $0", MXCRYPTO_ALGORITHM_MEGOLM)
                .query("roomId == $0", roomId)
                .query("sessionId == $0", sessionId)
                .first()
    }

    override fun markedSessionAsShared(
            roomId: String?,
            sessionId: String,
            userId: String,
            deviceId: String,
            deviceIdentityKey: String,
            chainIndex: Int
    ) {
        realmInstance.blockingWrite {
            val sharedSessionEntity = SharedSessionEntity().apply {
                this.roomId = roomId
                this.algorithm = MXCRYPTO_ALGORITHM_MEGOLM
                this.sessionId = sessionId
                this.userId = userId
                this.deviceId = deviceId
                this.deviceIdentityKey = deviceIdentityKey
                this.chainIndex = chainIndex
            }
            copyToRealm(sharedSessionEntity)
        }
    }

    override fun getSharedSessionInfo(roomId: String?, sessionId: String, deviceInfo: CryptoDeviceInfo): IMXCryptoStore.SharedSessionResult {
        return realmInstance.getBlockingRealm()
                .querySharedSessionEntity(roomId, sessionId)
                .forDeviceInfo(deviceInfo)
                .find()
                ?.let {
                    IMXCryptoStore.SharedSessionResult(true, it.chainIndex)
                } ?: IMXCryptoStore.SharedSessionResult(false, null)
    }

    override fun getSharedWithInfo(roomId: String?, sessionId: String): MXUsersDevicesMap<Int> {
        val result = MXUsersDevicesMap<Int>()
        realmInstance.getBlockingRealm()
                .querySharedSessionEntity(roomId, sessionId)
                .find()
                .groupBy { it.userId }
                .forEach { (userId, shared) ->
                    shared.forEach {
                        result.setObject(userId, it.deviceId, it.chainIndex)
                    }
                }
        return result
    }

    private fun TypedRealm.querySharedSessionEntity(roomId: String?, sessionId: String): RealmQuery<SharedSessionEntity> {
        return query(SharedSessionEntity::class, "algorithm == $0", MXCRYPTO_ALGORITHM_MEGOLM)
                .query("roomId == $0", roomId)
                .query("sessionId == $0", sessionId)
    }

    private fun RealmQuery<SharedSessionEntity>.forDeviceInfo(deviceInfo: CryptoDeviceInfo): RealmSingleQuery<SharedSessionEntity> {
        return query("userId == $0", deviceInfo.userId)
                .query("deviceId == $0", deviceInfo.deviceId)
                .query("deviceIdentityKey == $0", deviceInfo.identityKey())
                .first()
    }

    /**
     * Some entries in the DB can get a bit out of control with time.
     * So we need to tidy up a bit.
     */
    override fun tidyUpDataBase() {
        realmInstance.blockingWrite {
            // Clean the old ones?
            val prevWeekTs = clock.epochMillis() - 7 * 24 * 60 * 60 * 1_000
            val outgoingRequestsToDelete = query(OutgoingKeyRequestEntity::class, "creationTimeStamp < $0", prevWeekTs).find()
            Timber.i("## Crypto Clean up ${outgoingRequestsToDelete.size} OutgoingKeyRequestEntity")
            delete(outgoingRequestsToDelete)

            // Only keep one month history
            val prevMonthTs = clock.epochMillis() - 4 * 7 * 24 * 60 * 60 * 1_000L
            val auditTrailsToDelete = query(AuditTrailEntity::class, "ageLocalTs < $0", prevMonthTs).find()
            Timber.i("## Crypto Clean up ${auditTrailsToDelete.size} AuditTrailEntity")
            delete(auditTrailsToDelete)
            // Can we do something for WithHeldSessionEntity?
        }
    }
}
