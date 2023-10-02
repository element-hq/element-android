/*
 * Copyright 2023 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.crypto.store

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.zhuinden.monarchy.Monarchy
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.kotlin.where
import kotlinx.coroutines.runBlocking
import org.matrix.android.sdk.api.MatrixCoroutineDispatchers
import org.matrix.android.sdk.api.crypto.MXCRYPTO_ALGORITHM_MEGOLM
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.logger.LoggerTag
import org.matrix.android.sdk.api.session.crypto.GlobalCryptoConfig
import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.api.session.crypto.model.CryptoRoomInfo
import org.matrix.android.sdk.api.session.crypto.model.DeviceInfo
import org.matrix.android.sdk.api.session.events.model.content.EncryptionEventContent
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.api.util.toOptional
import org.matrix.android.sdk.internal.crypto.OlmMachine
import org.matrix.android.sdk.internal.crypto.model.MXInboundMegolmSessionWrapper
import org.matrix.android.sdk.internal.crypto.store.db.CryptoStoreAggregator
import org.matrix.android.sdk.internal.crypto.store.db.doRealmTransaction
import org.matrix.android.sdk.internal.crypto.store.db.doRealmTransactionAsync
import org.matrix.android.sdk.internal.crypto.store.db.doWithRealm
import org.matrix.android.sdk.internal.crypto.store.db.mapper.CryptoRoomInfoMapper
import org.matrix.android.sdk.internal.crypto.store.db.mapper.MyDeviceLastSeenInfoEntityMapper
import org.matrix.android.sdk.internal.crypto.store.db.model.AuditTrailEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.AuditTrailEntityFields
import org.matrix.android.sdk.internal.crypto.store.db.model.CryptoMetadataEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.CryptoRoomEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.CryptoRoomEntityFields
import org.matrix.android.sdk.internal.crypto.store.db.model.MyDeviceLastSeenInfoEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.MyDeviceLastSeenInfoEntityFields
import org.matrix.android.sdk.internal.crypto.store.db.model.OlmInboundGroupSessionEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.OlmInboundGroupSessionEntityFields
import org.matrix.android.sdk.internal.crypto.store.db.model.OutgoingKeyRequestEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.OutgoingKeyRequestEntityFields
import org.matrix.android.sdk.internal.crypto.store.db.model.createPrimaryKey
import org.matrix.android.sdk.internal.crypto.store.db.query.getById
import org.matrix.android.sdk.internal.crypto.store.db.query.getOrCreate
import org.matrix.android.sdk.internal.di.CryptoDatabase
import org.matrix.android.sdk.internal.di.DeviceId
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.session.SessionScope
import org.matrix.android.sdk.internal.util.time.Clock
import timber.log.Timber
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private val loggerTag = LoggerTag("RealmCryptoStore", LoggerTag.CRYPTO)

/**
 * In the transition phase, the rust SDK is still using parts to the realm crypto store,
 * this should be removed after full migration.
 */
@SessionScope
internal class RustCryptoStore @Inject constructor(
        @CryptoDatabase private val realmConfiguration: RealmConfiguration,
        private val clock: Clock,
        @UserId private val userId: String,
        @DeviceId private val deviceId: String,
        private val myDeviceLastSeenInfoEntityMapper: MyDeviceLastSeenInfoEntityMapper,
        private val olmMachine: dagger.Lazy<OlmMachine>,
        private val matrixCoroutineDispatchers: MatrixCoroutineDispatchers,
) : IMXCommonCryptoStore {

    // still needed on rust due to the global crypto settings
    init {
        // Ensure CryptoMetadataEntity is inserted in DB
        doRealmTransaction("init", realmConfiguration) { realm ->
            var currentMetadata = realm.where<CryptoMetadataEntity>().findFirst()

            var deleteAll = false

            if (currentMetadata != null) {
                // Check credentials
                // The device id may not have been provided in credentials.
                // Check it only if provided, else trust the stored one.
                if (currentMetadata.userId != userId || deviceId != currentMetadata.deviceId) {
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
                    deviceId = this@RustCryptoStore.deviceId
                }
            }
        }
    }

    /**
     * Retrieve a device by its identity key.
     *
     * @param userId The device owner userId.
     * @param identityKey the device identity key (`MXDeviceInfo.identityKey`)
     * @return the device or null if not found
     */
    override fun deviceWithIdentityKey(userId: String, identityKey: String): CryptoDeviceInfo? {
        // XXX make this suspendable?
        val knownDevices = runBlocking(matrixCoroutineDispatchers.io) {
            olmMachine.get().getUserDevices(userId)
        }
        return knownDevices
                .map { it.toCryptoDeviceInfo() }
                .firstOrNull {
                    it.identityKey() == identityKey
                }
    }

    /**
     * Needed for lazy migration of sessions from the legacy store.
     */
    override fun getInboundGroupSession(sessionId: String, senderKey: String): MXInboundMegolmSessionWrapper? {
        val key = OlmInboundGroupSessionEntity.createPrimaryKey(sessionId, senderKey)

        return doWithRealm(realmConfiguration) { realm ->
            realm.where<OlmInboundGroupSessionEntity>()
                    .equalTo(OlmInboundGroupSessionEntityFields.PRIMARY_KEY, key)
                    .findFirst()
                    ?.toModel()
        }
    }

    // ================================================
    // Things that should be migrated to another store than realm
    // ================================================

    private val monarchyWriteAsyncExecutor = Executors.newSingleThreadExecutor()

    private val monarchy = Monarchy.Builder()
            .setRealmConfiguration(realmConfiguration)
            .setWriteAsyncExecutor(monarchyWriteAsyncExecutor)
            .build()

    override fun open() {
        // nop
    }

    override fun tidyUpDataBase() {
        // These entities are not used in rust actually, but as they are not yet cleaned up, this will do it with time
        val prevWeekTs = clock.epochMillis() - 7 * 24 * 60 * 60 * 1_000
        doRealmTransaction("tidyUpDataBase", realmConfiguration) { realm ->

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

    override fun close() {
        val tasks = monarchyWriteAsyncExecutor.shutdownNow()
        Timber.w("Closing RealmCryptoStore, ${tasks.size} async task(s) cancelled")
        tryOrNull("Interrupted") {
            // Wait 1 minute max
            monarchyWriteAsyncExecutor.awaitTermination(1, TimeUnit.MINUTES)
        }
    }

    override fun getRoomAlgorithm(roomId: String): String? {
        return doWithRealm(realmConfiguration) {
            CryptoRoomEntity.getById(it, roomId)?.algorithm
        }
    }

    override fun getRoomCryptoInfo(roomId: String): CryptoRoomInfo? {
        return doWithRealm(realmConfiguration) { realm ->
            CryptoRoomEntity.getById(realm, roomId)?.let {
                CryptoRoomInfoMapper.map(it)
            }
        }
    }

    /**
     * This is a bit different than isRoomEncrypted.
     * A room is encrypted when there is a m.room.encryption state event in the room (malformed/invalid or not).
     * But the crypto layer has additional guaranty to ensure that encryption would never been reverted.
     * It's defensive coding out of precaution (if ever state is reset).
     */
    override fun roomWasOnceEncrypted(roomId: String): Boolean {
        return doWithRealm(realmConfiguration) {
            CryptoRoomEntity.getById(it, roomId)?.wasEncryptedOnce ?: false
        }
    }

    override fun setAlgorithmInfo(roomId: String, encryption: EncryptionEventContent?) {
        doRealmTransaction("setAlgorithmInfo", realmConfiguration) {
            CryptoRoomEntity.getOrCreate(it, roomId).let { entity ->
                entity.algorithm = encryption?.algorithm
                // store anyway the new algorithm, but mark the room
                // as having been encrypted once whatever, this can never
                // go back to false
                if (encryption?.algorithm == MXCRYPTO_ALGORITHM_MEGOLM) {
                    entity.wasEncryptedOnce = true
                    entity.rotationPeriodMs = encryption.rotationPeriodMs
                    entity.rotationPeriodMsgs = encryption.rotationPeriodMsgs
                }
            }
        }
    }

    override fun saveMyDevicesInfo(info: List<DeviceInfo>) {
        val entities = info.map { myDeviceLastSeenInfoEntityMapper.map(it) }
        doRealmTransactionAsync(realmConfiguration) { realm ->
            realm.where<MyDeviceLastSeenInfoEntity>().findAll().deleteAllFromRealm()
            entities.forEach {
                realm.insertOrUpdate(it)
            }
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
                { entity -> myDeviceLastSeenInfoEntityMapper.map(entity) }
        )
    }

    override fun getLiveMyDevicesInfo(deviceId: String): LiveData<Optional<DeviceInfo>> {
        val liveData = monarchy.findAllMappedWithChanges(
                { realm: Realm ->
                    realm.where<MyDeviceLastSeenInfoEntity>()
                            .equalTo(MyDeviceLastSeenInfoEntityFields.DEVICE_ID, deviceId)
                },
                { entity -> myDeviceLastSeenInfoEntityMapper.map(entity) }
        )

        return Transformations.map(liveData) {
            it.firstOrNull().toOptional()
        }
    }

    override fun storeData(cryptoStoreAggregator: CryptoStoreAggregator) {
        if (cryptoStoreAggregator.isEmpty()) {
            return
        }
        doRealmTransaction("storeData - CryptoStoreAggregator", realmConfiguration) { realm ->
            // setShouldShareHistory
            cryptoStoreAggregator.setShouldShareHistoryData.forEach {
                Timber.tag(loggerTag.value)
                        .v("setShouldShareHistory for room ${it.key} is ${it.value}")
                CryptoRoomEntity.getOrCreate(realm, it.key).shouldShareHistory = it.value
            }
            // setShouldEncryptForInvitedMembers
            cryptoStoreAggregator.setShouldEncryptForInvitedMembersData.forEach {
                CryptoRoomEntity.getOrCreate(realm, it.key).shouldEncryptForInvitedMembers = it.value
            }
        }
    }

    override fun shouldEncryptForInvitedMembers(roomId: String): Boolean {
        return doWithRealm(realmConfiguration) {
            CryptoRoomEntity.getById(it, roomId)?.shouldEncryptForInvitedMembers
        }
                ?: false
    }

    override fun setShouldShareHistory(roomId: String, shouldShareHistory: Boolean) {
        Timber.tag(loggerTag.value)
                .v("setShouldShareHistory for room $roomId is $shouldShareHistory")
        doRealmTransaction("setShouldShareHistory", realmConfiguration) {
            CryptoRoomEntity.getOrCreate(it, roomId).shouldShareHistory = shouldShareHistory
        }
    }

    override fun setShouldEncryptForInvitedMembers(roomId: String, shouldEncryptForInvitedMembers: Boolean) {
        doRealmTransaction("setShouldEncryptForInvitedMembers", realmConfiguration) {
            CryptoRoomEntity.getOrCreate(it, roomId).shouldEncryptForInvitedMembers = shouldEncryptForInvitedMembers
        }
    }

    override fun blockUnverifiedDevicesInRoom(roomId: String, block: Boolean) {
        doRealmTransaction("blockUnverifiedDevicesInRoom", realmConfiguration) { realm ->
            CryptoRoomEntity.getById(realm, roomId)
                    ?.blacklistUnverifiedDevices = block
        }
    }

    override fun setGlobalBlacklistUnverifiedDevices(block: Boolean) {
        doRealmTransaction("setGlobalBlacklistUnverifiedDevices", realmConfiguration) {
            it.where<CryptoMetadataEntity>().findFirst()?.globalBlacklistUnverifiedDevices = block
        }
    }

    override fun getLiveGlobalCryptoConfig(): LiveData<GlobalCryptoConfig> {
        val liveData = monarchy.findAllMappedWithChanges(
                { realm: Realm ->
                    realm
                            .where<CryptoMetadataEntity>()
                },
                {
                    GlobalCryptoConfig(
                            globalBlockUnverifiedDevices = it.globalBlacklistUnverifiedDevices,
                            globalEnableKeyGossiping = it.globalEnableKeyGossiping,
                            enableKeyForwardingOnInvite = it.enableKeyForwardingOnInvite
                    )
                }
        )
        return Transformations.map(liveData) {
            it.firstOrNull() ?: GlobalCryptoConfig(false, false, false)
        }
    }

    override fun getGlobalBlacklistUnverifiedDevices(): Boolean {
        return doWithRealm(realmConfiguration) {
            it.where<CryptoMetadataEntity>().findFirst()?.globalBlacklistUnverifiedDevices
        } ?: false
    }

    override fun getLiveBlockUnverifiedDevices(roomId: String): LiveData<Boolean> {
        val liveData = monarchy.findAllMappedWithChanges(
                { realm: Realm ->
                    realm.where<CryptoRoomEntity>()
                            .equalTo(CryptoRoomEntityFields.ROOM_ID, roomId)
                },
                {
                    it.blacklistUnverifiedDevices
                }
        )
        return Transformations.map(liveData) {
            it.firstOrNull() ?: false
        }
    }

    override fun getBlockUnverifiedDevices(roomId: String): Boolean {
        return doWithRealm(realmConfiguration) { realm ->
            realm.where<CryptoRoomEntity>()
                    .equalTo(CryptoRoomEntityFields.ROOM_ID, roomId)
                    .findFirst()
                    ?.blacklistUnverifiedDevices ?: false
        }
    }
}
