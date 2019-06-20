/*
 * Copyright 2018 New Vector Ltd
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

package im.vector.matrix.android.internal.crypto.store.db

import android.text.TextUtils
import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.internal.crypto.IncomingRoomKeyRequest
import im.vector.matrix.android.internal.crypto.OutgoingRoomKeyRequest
import im.vector.matrix.android.internal.crypto.model.MXDeviceInfo
import im.vector.matrix.android.internal.crypto.model.OlmInboundGroupSessionWrapper
import im.vector.matrix.android.internal.crypto.model.OlmSessionWrapper
import im.vector.matrix.android.internal.crypto.model.rest.RoomKeyRequestBody
import im.vector.matrix.android.internal.crypto.store.IMXCryptoStore
import im.vector.matrix.android.internal.crypto.store.db.model.*
import im.vector.matrix.android.internal.crypto.store.db.query.delete
import im.vector.matrix.android.internal.crypto.store.db.query.getById
import im.vector.matrix.android.internal.crypto.store.db.query.getOrCreate
import im.vector.matrix.android.internal.session.SessionScope
import io.realm.RealmConfiguration
import io.realm.Sort
import io.realm.kotlin.where
import org.matrix.olm.OlmAccount
import org.matrix.olm.OlmException
import timber.log.Timber
import kotlin.collections.set

// enableFileEncryption is used to migrate the previous store
@SessionScope
internal class RealmCryptoStore(private val enableFileEncryption: Boolean = false,
                                private val realmConfiguration: RealmConfiguration,
                                private val credentials: Credentials) : IMXCryptoStore {

    /* ==========================================================================================
     * Memory cache, to correctly release JNI objects
     * ========================================================================================== */

    // The olm account
    private var olmAccount: OlmAccount? = null

    // Cache for OlmSession, to release them properly
    private val olmSessionsToRelease = HashMap<String, OlmSessionWrapper>()

    // Cache for InboundGroupSession, to release them properly
    private val inboundGroupSessionToRelease = HashMap<String, OlmInboundGroupSessionWrapper>()

    /* ==========================================================================================
     * Other data
     * ========================================================================================== */

    override fun hasData(): Boolean {
        return doWithRealm(realmConfiguration) {
            !it.isEmpty
                    // Check if there is a MetaData object
                    && it.where<CryptoMetadataEntity>().count() > 0
        }
    }

    override fun deleteStore() {
        doRealmTransaction(realmConfiguration) {
            it.deleteAll()
        }
    }

    override fun open() {
        // Ensure CryptoMetadataEntity is inserted in DB
        doWithRealm(realmConfiguration) { realm ->
            var currentMetadata = realm.where<CryptoMetadataEntity>().findFirst()

            var deleteAll = false

            if (currentMetadata != null) {
                // Check credentials
                // The device id may not have been provided in credentials.
                // Check it only if provided, else trust the stored one.
                if (!TextUtils.equals(currentMetadata.userId, credentials.userId)
                        || (credentials.deviceId != null && !TextUtils.equals(credentials.deviceId, currentMetadata.deviceId))) {
                    Timber.w("## open() : Credentials do not match, close this store and delete data")
                    deleteAll = true
                    currentMetadata = null
                }
            }

            if (currentMetadata == null) {
                realm.executeTransaction {
                    if (deleteAll) {
                        it.deleteAll()
                    }

                    // Metadata not found, or database cleaned, create it
                    it.createObject(CryptoMetadataEntity::class.java, credentials.userId).apply {
                        deviceId = credentials.deviceId
                    }
                }
            }
        }
    }

    override fun close() {
        olmSessionsToRelease.forEach {
            it.value.olmSession.releaseSession()
        }
        olmSessionsToRelease.clear()

        inboundGroupSessionToRelease.forEach {
            it.value.olmInboundGroupSession?.releaseSession()
        }
        inboundGroupSessionToRelease.clear()

        olmAccount?.releaseAccount()
    }

    override fun storeDeviceId(deviceId: String) {
        doRealmTransaction(realmConfiguration) {
            it.where<CryptoMetadataEntity>().findFirst()?.deviceId = deviceId
        }
    }

    override fun getDeviceId(): String {
        return doRealmQueryAndCopy(realmConfiguration) {
            it.where<CryptoMetadataEntity>().findFirst()
        }?.deviceId ?: ""
    }

    override fun storeAccount(account: OlmAccount) {
        olmAccount = account

        doRealmTransaction(realmConfiguration) {
            it.where<CryptoMetadataEntity>().findFirst()?.putOlmAccount(account)
        }
    }

    override fun getAccount(): OlmAccount? {
        if (olmAccount == null) {
            olmAccount = doRealmQueryAndCopy(realmConfiguration) { it.where<CryptoMetadataEntity>().findFirst() }?.getOlmAccount()
        }

        return olmAccount
    }

    override fun storeUserDevice(userId: String?, deviceInfo: MXDeviceInfo?) {
        if (userId == null || deviceInfo == null) {
            return
        }

        doRealmTransaction(realmConfiguration) {
            val user = UserEntity.getOrCreate(it, userId)

            // Create device info
            val deviceInfoEntity = DeviceInfoEntity.getOrCreate(it, userId, deviceInfo.deviceId).apply {
                deviceId = deviceInfo.deviceId
                identityKey = deviceInfo.identityKey()
                putDeviceInfo(deviceInfo)
            }

            if (!user.devices.contains(deviceInfoEntity)) {
                user.devices.add(deviceInfoEntity)
            }
        }
    }

    override fun getUserDevice(deviceId: String, userId: String): MXDeviceInfo? {
        return doRealmQueryAndCopy(realmConfiguration) {
            it.where<DeviceInfoEntity>()
                    .equalTo(DeviceInfoEntityFields.PRIMARY_KEY, DeviceInfoEntity.createPrimaryKey(userId, deviceId))
                    .findFirst()
        }
                ?.getDeviceInfo()
    }

    override fun deviceWithIdentityKey(identityKey: String): MXDeviceInfo? {
        return doRealmQueryAndCopy(realmConfiguration) {
            it.where<DeviceInfoEntity>()
                    .equalTo(DeviceInfoEntityFields.IDENTITY_KEY, identityKey)
                    .findFirst()
        }
                ?.getDeviceInfo()
    }

    override fun storeUserDevices(userId: String, devices: Map<String, MXDeviceInfo>) {
        if (userId == null) {
            return
        }
        doRealmTransaction(realmConfiguration) { realm ->
            if (devices == null) {
                // Remove the user
                UserEntity.delete(realm, userId)
            } else {
                UserEntity.getOrCreate(realm, userId)
                        .let { u ->
                            // Add the devices
                            // Ensure all other devices are deleted
                            u.devices.deleteAllFromRealm()

                            u.devices.addAll(
                                    devices.map {
                                        DeviceInfoEntity.getOrCreate(realm, userId, it.value.deviceId).apply {
                                            deviceId = it.value.deviceId
                                            identityKey = it.value.identityKey()
                                            putDeviceInfo(it.value)
                                        }
                                    }
                            )
                        }
            }
        }
    }

    override fun getUserDevices(userId: String): Map<String, MXDeviceInfo>? {
        return doRealmQueryAndCopy(realmConfiguration) {
            it.where<UserEntity>()
                    .equalTo(UserEntityFields.USER_ID, userId)
                    .findFirst()
        }
                ?.devices
                ?.mapNotNull { it.getDeviceInfo() }
                ?.associateBy { it.deviceId }
    }

    override fun storeRoomAlgorithm(roomId: String, algorithm: String) {
        doRealmTransaction(realmConfiguration) {
            CryptoRoomEntity.getOrCreate(it, roomId).algorithm = algorithm
        }
    }

    override fun getRoomAlgorithm(roomId: String): String? {
        return doRealmQueryAndCopy(realmConfiguration) {
            CryptoRoomEntity.getById(it, roomId)
        }
                ?.algorithm
    }

    override fun shouldEncryptForInvitedMembers(roomId: String): Boolean {
        return doRealmQueryAndCopy(realmConfiguration) {
            CryptoRoomEntity.getById(it, roomId)
        }
                ?.shouldEncryptForInvitedMembers ?: false
    }

    override fun setShouldEncryptForInvitedMembers(roomId: String, shouldEncryptForInvitedMembers: Boolean) {
        doRealmTransaction(realmConfiguration) {
            CryptoRoomEntity.getOrCreate(it, roomId).shouldEncryptForInvitedMembers = shouldEncryptForInvitedMembers
        }
    }

    override fun storeSession(olmSessionWrapper: OlmSessionWrapper, deviceKey: String) {
        var sessionIdentifier: String? = null

        try {
            sessionIdentifier = olmSessionWrapper.olmSession.sessionIdentifier()
        } catch (e: OlmException) {
            Timber.e(e, "## storeSession() : sessionIdentifier failed " + e.message)
        }

        if (sessionIdentifier != null) {
            val key = OlmSessionEntity.createPrimaryKey(sessionIdentifier, deviceKey)

            // Release memory of previously known session, if it is not the same one
            if (olmSessionsToRelease[key]?.olmSession != olmSessionWrapper.olmSession) {
                olmSessionsToRelease[key]?.olmSession?.releaseSession()
            }

            olmSessionsToRelease[key] = olmSessionWrapper

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

    override fun getDeviceSession(sessionId: String?, deviceKey: String?): OlmSessionWrapper? {
        if (sessionId == null || deviceKey == null) {
            return null
        }

        val key = OlmSessionEntity.createPrimaryKey(sessionId, deviceKey)

        // If not in cache (or not found), try to read it from realm
        if (olmSessionsToRelease[key] == null) {
            doRealmQueryAndCopy(realmConfiguration) {
                it.where<OlmSessionEntity>()
                        .equalTo(OlmSessionEntityFields.PRIMARY_KEY, key)
                        .findFirst()
            }
                    ?.let {
                        val olmSession = it.getOlmSession()
                        if (olmSession != null && it.sessionId != null) {
                            olmSessionsToRelease[key] = OlmSessionWrapper(olmSession, it.lastReceivedMessageTs)
                        }
                    }
        }

        return olmSessionsToRelease[key]
    }

    override fun getLastUsedSessionId(deviceKey: String): String? {
        return doRealmQueryAndCopy(realmConfiguration) {
            it.where<OlmSessionEntity>()
                    .equalTo(OlmSessionEntityFields.DEVICE_KEY, deviceKey)
                    .sort(OlmSessionEntityFields.LAST_RECEIVED_MESSAGE_TS, Sort.DESCENDING)
                    .findFirst()
        }
                ?.sessionId
    }

    override fun getDeviceSessionIds(deviceKey: String): MutableSet<String> {
        return doRealmQueryAndCopyList(realmConfiguration) {
            it.where<OlmSessionEntity>()
                    .equalTo(OlmSessionEntityFields.DEVICE_KEY, deviceKey)
                    .findAll()
        }
                .mapNotNull {
                    it.sessionId
                }
                .toMutableSet()
    }

    override fun storeInboundGroupSessions(sessions: List<OlmInboundGroupSessionWrapper>) {
        if (sessions.isEmpty()) {
            return
        }

        doRealmTransaction(realmConfiguration) {
            sessions.forEach { session ->
                var sessionIdentifier: String? = null

                try {
                    sessionIdentifier = session.olmInboundGroupSession?.sessionIdentifier()
                } catch (e: OlmException) {
                    Timber.e(e, "## storeInboundGroupSession() : sessionIdentifier failed " + e.message)
                }

                if (sessionIdentifier != null) {
                    val key = OlmInboundGroupSessionEntity.createPrimaryKey(sessionIdentifier, session.senderKey)

                    // Release memory of previously known session, if it is not the same one
                    if (inboundGroupSessionToRelease[key] != session) {
                        inboundGroupSessionToRelease[key]?.olmInboundGroupSession?.releaseSession()
                    }

                    inboundGroupSessionToRelease[key] = session

                    val realmOlmInboundGroupSession = OlmInboundGroupSessionEntity().apply {
                        primaryKey = key
                        sessionId = sessionIdentifier
                        senderKey = session.senderKey
                        putInboundGroupSession(session)
                    }

                    it.insertOrUpdate(realmOlmInboundGroupSession)
                }
            }
        }
    }

    override fun getInboundGroupSession(sessionId: String, senderKey: String): OlmInboundGroupSessionWrapper? {
        val key = OlmInboundGroupSessionEntity.createPrimaryKey(sessionId, senderKey)

        // If not in cache (or not found), try to read it from realm
        if (inboundGroupSessionToRelease[key] == null) {
            doRealmQueryAndCopy(realmConfiguration) {
                it.where<OlmInboundGroupSessionEntity>()
                        .equalTo(OlmInboundGroupSessionEntityFields.PRIMARY_KEY, key)
                        .findFirst()
            }
                    ?.getInboundGroupSession()
                    ?.let {
                        inboundGroupSessionToRelease[key] = it
                    }
        }

        return inboundGroupSessionToRelease[key]
    }

    /**
     * Note: the result will be only use to export all the keys and not to use the OlmInboundGroupSessionWrapper,
     * so there is no need to use or update `inboundGroupSessionToRelease` for native memory management
     */
    override fun getInboundGroupSessions(): MutableList<OlmInboundGroupSessionWrapper> {
        return doRealmQueryAndCopyList(realmConfiguration) {
            it.where<OlmInboundGroupSessionEntity>()
                    .findAll()
        }
                .mapNotNull {
                    it.getInboundGroupSession()
                }
                .toMutableList()
    }

    override fun removeInboundGroupSession(sessionId: String, senderKey: String) {
        val key = OlmInboundGroupSessionEntity.createPrimaryKey(sessionId, senderKey)

        // Release memory of previously known session
        inboundGroupSessionToRelease[key]?.olmInboundGroupSession?.releaseSession()
        inboundGroupSessionToRelease.remove(key)

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

    override fun markBackupDoneForInboundGroupSessions(olmInboundGroupSessionWrappers: List<OlmInboundGroupSessionWrapper>) {
        if (olmInboundGroupSessionWrappers.isEmpty()) {
            return
        }

        doRealmTransaction(realmConfiguration) {
            olmInboundGroupSessionWrappers.forEach { olmInboundGroupSessionWrapper ->
                try {
                    val key = OlmInboundGroupSessionEntity.createPrimaryKey(
                            olmInboundGroupSessionWrapper.olmInboundGroupSession?.sessionIdentifier(),
                            olmInboundGroupSessionWrapper.senderKey)

                    it.where<OlmInboundGroupSessionEntity>()
                            .equalTo(OlmInboundGroupSessionEntityFields.PRIMARY_KEY, key)
                            .findFirst()
                            ?.backedUp = true
                } catch (e: OlmException) {
                    Timber.e(e, "OlmException")
                }
            }
        }
    }

    override fun inboundGroupSessionsToBackup(limit: Int): List<OlmInboundGroupSessionWrapper> {
        return doRealmQueryAndCopyList(realmConfiguration) {
            it.where<OlmInboundGroupSessionEntity>()
                    .equalTo(OlmInboundGroupSessionEntityFields.BACKED_UP, false)
                    .limit(limit.toLong())
                    .findAll()
        }.mapNotNull { inboundGroupSession ->
            inboundGroupSession.getInboundGroupSession()
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

    override fun getGlobalBlacklistUnverifiedDevices(): Boolean {
        return doRealmQueryAndCopy(realmConfiguration) {
            it.where<CryptoMetadataEntity>().findFirst()
        }?.globalBlacklistUnverifiedDevices
                ?: false
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

    override fun getRoomsListBlacklistUnverifiedDevices(): MutableList<String> {
        return doRealmQueryAndCopyList(realmConfiguration) {
            it.where<CryptoRoomEntity>()
                    .equalTo(CryptoRoomEntityFields.BLACKLIST_UNVERIFIED_DEVICES, true)
                    .findAll()
        }
                .mapNotNull {
                    it.roomId
                }
                .toMutableList()
    }

    override fun getDeviceTrackingStatuses(): MutableMap<String, Int> {
        return doRealmQueryAndCopyList(realmConfiguration) {
            it.where<UserEntity>()
                    .findAll()
        }
                .associateBy {
                    it.userId!!
                }
                .mapValues {
                    it.value.deviceTrackingStatus
                }
                .toMutableMap()
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
        return doRealmQueryAndCopy(realmConfiguration) {
            it.where<UserEntity>()
                    .equalTo(UserEntityFields.USER_ID, userId)
                    .findFirst()
        }
                ?.deviceTrackingStatus
                ?: defaultValue
    }

    override fun getOutgoingRoomKeyRequest(requestBody: RoomKeyRequestBody): OutgoingRoomKeyRequest? {
        return doRealmQueryAndCopy(realmConfiguration) {
            it.where<OutgoingRoomKeyRequestEntity>()
                    .equalTo(OutgoingRoomKeyRequestEntityFields.REQUEST_BODY_ALGORITHM, requestBody.algorithm)
                    .equalTo(OutgoingRoomKeyRequestEntityFields.REQUEST_BODY_ROOM_ID, requestBody.roomId)
                    .equalTo(OutgoingRoomKeyRequestEntityFields.REQUEST_BODY_SENDER_KEY, requestBody.senderKey)
                    .equalTo(OutgoingRoomKeyRequestEntityFields.REQUEST_BODY_SESSION_ID, requestBody.sessionId)
                    .findFirst()
        }
                ?.toOutgoingRoomKeyRequest()
    }

    override fun getOrAddOutgoingRoomKeyRequest(request: OutgoingRoomKeyRequest): OutgoingRoomKeyRequest? {
        if (request.requestBody == null) {
            return null
        }

        val existingOne = getOutgoingRoomKeyRequest(request.requestBody!!)

        if (existingOne != null) {
            return existingOne
        }

        // Insert the request and return the one passed in parameter
        doRealmTransaction(realmConfiguration) {
            it.createObject(OutgoingRoomKeyRequestEntity::class.java, request.requestId).apply {
                putRequestBody(request.requestBody)
                putRecipients(request.recipients)
                cancellationTxnId = request.cancellationTxnId
                state = request.state.ordinal
            }
        }

        return request
    }

    override fun getOutgoingRoomKeyRequestByState(states: Set<OutgoingRoomKeyRequest.RequestState>): OutgoingRoomKeyRequest? {
        return doRealmQueryAndCopy(realmConfiguration) {
            it.where<OutgoingRoomKeyRequestEntity>()
                    .`in`(OutgoingRoomKeyRequestEntityFields.STATE, states.map { it.ordinal }.toTypedArray())
                    .findFirst()
        }
                ?.toOutgoingRoomKeyRequest()
    }

    override fun updateOutgoingRoomKeyRequest(request: OutgoingRoomKeyRequest) {
        doRealmTransaction(realmConfiguration) {
            val obj = OutgoingRoomKeyRequestEntity().apply {
                requestId = request.requestId
                cancellationTxnId = request.cancellationTxnId
                state = request.state.ordinal
                putRecipients(request.recipients)
                putRequestBody(request.requestBody)
            }

            it.insertOrUpdate(obj)
        }
    }

    override fun deleteOutgoingRoomKeyRequest(transactionId: String) {
        doRealmTransaction(realmConfiguration) {
            it.where<OutgoingRoomKeyRequestEntity>()
                    .equalTo(OutgoingRoomKeyRequestEntityFields.REQUEST_ID, transactionId)
                    .findFirst()
                    ?.deleteFromRealm()
        }
    }

    override fun storeIncomingRoomKeyRequest(incomingRoomKeyRequest: IncomingRoomKeyRequest?) {
        if (incomingRoomKeyRequest == null) {
            return
        }

        doRealmTransaction(realmConfiguration) {
            // Delete any previous store request with the same parameters
            it.where<IncomingRoomKeyRequestEntity>()
                    .equalTo(IncomingRoomKeyRequestEntityFields.USER_ID, incomingRoomKeyRequest.userId)
                    .equalTo(IncomingRoomKeyRequestEntityFields.DEVICE_ID, incomingRoomKeyRequest.deviceId)
                    .equalTo(IncomingRoomKeyRequestEntityFields.REQUEST_ID, incomingRoomKeyRequest.requestId)
                    .findAll()
                    .deleteAllFromRealm()

            // Then store it
            it.createObject(IncomingRoomKeyRequestEntity::class.java).apply {
                userId = incomingRoomKeyRequest.userId
                deviceId = incomingRoomKeyRequest.deviceId
                requestId = incomingRoomKeyRequest.requestId
                putRequestBody(incomingRoomKeyRequest.requestBody)
            }
        }
    }

    override fun deleteIncomingRoomKeyRequest(incomingRoomKeyRequest: IncomingRoomKeyRequest) {
        doRealmTransaction(realmConfiguration) {
            it.where<IncomingRoomKeyRequestEntity>()
                    .equalTo(IncomingRoomKeyRequestEntityFields.USER_ID, incomingRoomKeyRequest.userId)
                    .equalTo(IncomingRoomKeyRequestEntityFields.DEVICE_ID, incomingRoomKeyRequest.deviceId)
                    .equalTo(IncomingRoomKeyRequestEntityFields.REQUEST_ID, incomingRoomKeyRequest.requestId)
                    .findAll()
                    .deleteAllFromRealm()
        }
    }

    override fun getIncomingRoomKeyRequest(userId: String, deviceId: String, requestId: String): IncomingRoomKeyRequest? {
        return doRealmQueryAndCopy(realmConfiguration) {
            it.where<IncomingRoomKeyRequestEntity>()
                    .equalTo(IncomingRoomKeyRequestEntityFields.USER_ID, userId)
                    .equalTo(IncomingRoomKeyRequestEntityFields.DEVICE_ID, deviceId)
                    .equalTo(IncomingRoomKeyRequestEntityFields.REQUEST_ID, requestId)
                    .findFirst()
        }
                ?.toIncomingRoomKeyRequest()
    }

    override fun getPendingIncomingRoomKeyRequests(): MutableList<IncomingRoomKeyRequest> {
        return doRealmQueryAndCopyList(realmConfiguration) {
            it.where<IncomingRoomKeyRequestEntity>()
                    .findAll()
        }
                .map {
                    it.toIncomingRoomKeyRequest()
                }
                .toMutableList()
    }
}