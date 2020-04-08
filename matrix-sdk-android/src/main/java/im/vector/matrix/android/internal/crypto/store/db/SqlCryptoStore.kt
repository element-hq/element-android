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

package im.vector.matrix.android.internal.crypto.store.db

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.squareup.moshi.JsonDataException
import com.squareup.sqldelight.Query
import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.api.session.crypto.MXCryptoError
import im.vector.matrix.android.api.session.crypto.crosssigning.MXCrossSigningInfo
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.LocalEcho
import im.vector.matrix.android.api.session.room.send.SendState
import im.vector.matrix.android.api.util.Optional
import im.vector.matrix.android.internal.crypto.*
import im.vector.matrix.android.internal.crypto.algorithms.olm.OlmDecryptionResult
import im.vector.matrix.android.internal.crypto.crosssigning.DeviceTrustLevel
import im.vector.matrix.android.internal.crypto.model.*
import im.vector.matrix.android.internal.crypto.model.rest.RoomKeyRequestBody
import im.vector.matrix.android.internal.crypto.store.IMXCryptoStore
import im.vector.matrix.android.internal.crypto.store.PrivateKeysInfo
import im.vector.matrix.android.internal.crypto.store.SavedKeyBackupKeyInfo
import im.vector.matrix.android.internal.crypto.store.db.SqliteCryptoMapper.getRecipientsData
import im.vector.matrix.android.internal.crypto.store.db.SqliteCryptoMapper.getRequestedKeyInfo
import im.vector.matrix.android.internal.crypto.store.db.SqliteCryptoMapper.getRequestedSecretName
import im.vector.matrix.android.internal.crypto.store.db.SqliteCryptoMapper.mapToEntity
import im.vector.matrix.android.internal.crypto.store.db.SqliteCryptoMapper.mapToModel
import im.vector.matrix.android.internal.crypto.store.db.SqliteCryptoMapper.toIncomingGossipingRequest
import im.vector.matrix.android.internal.crypto.store.db.SqliteCryptoMapper.toOutgoingGossipingRequest
import im.vector.matrix.android.internal.crypto.store.db.model.KeysBackupDataEntity
import im.vector.matrix.android.internal.database.mapper.ContentMapper
import im.vector.matrix.android.internal.di.MoshiProvider
import im.vector.matrix.android.internal.session.SessionScope
import im.vector.matrix.sqldelight.crypto.*
import org.matrix.olm.OlmAccount
import org.matrix.olm.OlmException
import org.matrix.olm.OlmSession
import timber.log.Timber
import javax.inject.Inject

@SessionScope
internal class SqlCryptoStore @Inject constructor(private val cryptoDatabase: CryptoDatabase,
                                                  private val credentials: Credentials) : IMXCryptoStore {

    private val crossSigningInfoQueries: CrossSigningInfoQueries = cryptoDatabase.crossSigningInfoQueries
    private val metadataQueries: CryptoMetadataQueries = cryptoDatabase.cryptoMetadataQueries
    private val roomQueries: CryptoRoomQueries = cryptoDatabase.cryptoRoomQueries
    private val userQueries: CryptoUserQueries = cryptoDatabase.cryptoUserQueries
    private val deviceInfoQueries: DeviceInfoQueries = cryptoDatabase.deviceInfoQueries
    private val gossipingEventQueries: GossipingEventQueries = cryptoDatabase.gossipingEventQueries
    private val incomingGossipingRequestQueries = cryptoDatabase.incomingGossipingRequestQueries
    private val keysBackupDataQueries: KeysBackupDataQueries = cryptoDatabase.keysBackupDataQueries
    private val olmInboundGroupSessionQueries: OlmInboundGroupSessionQueries = cryptoDatabase.olmInboundGroupSessionQueries
    private val olmSessionQueries: OlmSessionQueries = cryptoDatabase.olmSessionQueries
    private val outgoingGossipingRequestQueries: OutgoingGossipingRequestQueries = cryptoDatabase.outgoingGossipingRequestQueries

    // The olm account
    private var olmAccount: OlmAccount? = null

    // Cache for OlmSession, to release them properly
    private val olmSessionsToRelease = HashMap<String, OlmSessionWrapper>()

    // Cache for InboundGroupSession, to release them properly
    private val inboundGroupSessionToRelease = HashMap<String, OlmInboundGroupSessionWrapper>()

    private val newSessionListeners = ArrayList<NewSessionListener>()

    override fun hasData(): Boolean {
        return metadataQueries.count().executeAsOne() > 0
    }

    override fun deleteStore() {
        crossSigningInfoQueries.deleteAll()
        metadataQueries.deleteAll()
        roomQueries.deleteAll()
        userQueries.deleteAll()
        deviceInfoQueries.deleteAll()
        gossipingEventQueries.deleteAll()
        incomingGossipingRequestQueries.deleteAll()
        keysBackupDataQueries.deleteAll()
        olmInboundGroupSessionQueries.deleteAll()
        olmSessionQueries.deleteAll()
        outgoingGossipingRequestQueries.deleteAll()
    }

    override fun open() {
        // NOP
    }

    override fun close() {
        // NOP
    }

    override fun getDeviceId(): String {
        return metadataQueries.getAll().executeAsList().firstOrNull()?.device_id ?: ""
    }

    override fun saveOlmAccount() {
        val olmAccountData = serializeForSqlite(olmAccount)
        metadataQueries.updateOlmAccountData(olmAccountData)
    }

    override fun getOlmAccount(): OlmAccount {
        return olmAccount!!
    }

    override fun getOrCreateOlmAccount(): OlmAccount {
        return metadataQueries
                .getAll()
                .executeAsList()
                .firstOrNull()
                ?.olm_account_data
                ?.let { olmAccountData ->
                    return deserializeFromSqlite<OlmAccount>(olmAccountData)!!.also {
                        olmAccount = it
                    }
                }
                ?: run {
                    return OlmAccount().also {
                        metadataQueries.updateOlmAccountData(serializeForSqlite(it))
                        olmAccount = it
                    }
                }
    }

    override fun storeUserDevice(userId: String?, deviceInfo: CryptoDeviceInfo?) {
        if (userId == null || deviceInfo == null) {
            return
        }

        userQueries.getByUserId(userId).executeAsOneOrNull()
                ?: userQueries.insertOrUpdate(CryptoUserEntity.Impl(user_id = userId, device_tracking_status = 0))

        deviceInfoQueries.insertOrUpdate(mapToEntity(deviceInfo))
    }

    override fun getUserDevice(userId: String, deviceId: String): CryptoDeviceInfo? {
        return deviceInfoQueries
                .getByUserIdAndDeviceId(userId, deviceId)
                .executeAsOneOrNull()
                ?.let { deviceInfoEntity ->
                    mapToModel(deviceInfoEntity)
                }
    }

    override fun deviceWithIdentityKey(identityKey: String): CryptoDeviceInfo? {
        return deviceInfoQueries.getByIdentityKey(identityKey).executeAsOneOrNull()?.let {
            mapToModel(it)
        }
    }

    override fun storeUserDevices(userId: String, devices: Map<String, CryptoDeviceInfo>?) {
        if (devices == null) {
            userQueries.deleteByUserId(userId)
        } else {
            devices.forEach { storeUserDevice(userId, it.value) }
        }
    }

    override fun getUserDevices(userId: String): Map<String, CryptoDeviceInfo>? {
        return deviceInfoQueries
                .getByUserId(userId)
                .executeAsList()
                .map { mapToModel(it) }
                .associateBy { it.deviceId }
    }

    override fun getUserDeviceList(userId: String): List<CryptoDeviceInfo>? {
        return deviceInfoQueries
                .getByUserId(userId)
                .executeAsList()
                .map { mapToModel(it) }
    }

    override fun getLiveDeviceList(userId: String): LiveData<List<CryptoDeviceInfo>> {
        val query = deviceInfoQueries.getByUserId(userId)
        return MutableLiveData<List<CryptoDeviceInfo>>().apply {
            query.addListener(object : Query.Listener {
                override fun queryResultsChanged() {
                    postValue(query.executeAsList().map { mapToModel(it) })
                }
            })
        }
    }

    override fun getLiveDeviceList(userIds: List<String>): LiveData<List<CryptoDeviceInfo>> {
        return MediatorLiveData<List<CryptoDeviceInfo>>().also { liveData ->
            userIds.forEach {
                liveData.addSource(getLiveDeviceList(it)) { cryptoDeviceInfoList ->
                    liveData.value = cryptoDeviceInfoList
                }
            }
        }
    }

    override fun getLiveDeviceList(): LiveData<List<CryptoDeviceInfo>> {
        val query = deviceInfoQueries.getAll()
        return MutableLiveData<List<CryptoDeviceInfo>>().apply {
            query.addListener(object : Query.Listener {
                override fun queryResultsChanged() {
                    postValue(query.executeAsList().map { mapToModel(it) })
                }
            })
        }
    }

    override fun storeUserCrossSigningKeys(userId: String, masterKey: CryptoCrossSigningKey?, selfSigningKey: CryptoCrossSigningKey?, userSigningKey: CryptoCrossSigningKey?) {
        userQueries.getByUserId(userId).executeAsOneOrNull()
                ?: userQueries.insertOrUpdate(CryptoUserEntity.Impl(user_id = userId, device_tracking_status = 0))

        if (masterKey == null || selfSigningKey == null) {
            // The user has disabled cross signing?
            userQueries.deleteByUserId(userId)
            crossSigningInfoQueries.deleteByUserId(userId)
        } else {

        }
    }

    override fun getInboundGroupSessions(): List<OlmInboundGroupSessionWrapper> {
        return olmInboundGroupSessionQueries
                .getAll()
                .executeAsList()
                .mapNotNull {
                    deserializeFromSqlite<OlmInboundGroupSessionWrapper>(
                            it.olm_inbound_group_session_data
                    )
                }
    }

    override fun getGlobalBlacklistUnverifiedDevices(): Boolean {
        return metadataQueries
                .getAll()
                .executeAsOneOrNull()
                ?.global_blacklist_unverified_devices ?: false
    }

    override fun setGlobalBlacklistUnverifiedDevices(block: Boolean) {
        metadataQueries
                .updateGlobalBlacklistUnverifiedDevices(block)
    }

    override fun getRoomsListBlacklistUnverifiedDevices(): List<String> {
        return roomQueries
                .getBlacklistUnverifiedDevices(true)
                .executeAsList()
                .map { it.room_id }
    }

    override fun setRoomsListBlacklistUnverifiedDevices(roomIds: List<String>) {
        // Reset all
        roomQueries
                .getAll()
                .executeAsList()
                .forEach {
                    roomQueries
                            .updateBlacklistUnverifiedDevices(false, it.room_id)
                }

        // Enable those in the list
        roomIds.forEach { roomId ->
            roomQueries
                    .updateBlacklistUnverifiedDevices(true, roomId)
        }
    }

    override fun getKeyBackupVersion(): String? {
        return metadataQueries
                .getAll()
                .executeAsOneOrNull()
                ?.backup_version
    }

    override fun setKeyBackupVersion(keyBackupVersion: String?) {
        metadataQueries
                .updateKeyBackupVersion(keyBackupVersion)
    }

    override fun getKeysBackupData(): KeysBackupDataEntity? {
        return null // TODO. Why are we using Realm entity?
    }

    override fun setKeysBackupData(keysBackupData: KeysBackupDataEntity?) {
        // TODO. Why are we using Realm entity?
    }

    override fun getDeviceTrackingStatuses(): Map<String, Int> {
        val statuses = mutableMapOf<String, Int>()
        userQueries
                .getAll()
                .executeAsList()
                .forEach {
                    statuses[it.user_id] = it.device_tracking_status
                }
        return statuses
    }

    override fun getPendingIncomingRoomKeyRequests(): List<IncomingRoomKeyRequest> {
        return incomingGossipingRequestQueries
                .getByTypeAndRequestState(
                        GossipRequestType.KEY.name,
                        GossipingRequestState.PENDING.name
                ).executeAsList()
                .map { entity ->
                    IncomingRoomKeyRequest(
                            userId = entity.other_user_id,
                            deviceId = entity.other_device_id,
                            requestId = entity.request_id,
                            requestBody = getRequestedKeyInfo(entity.type, entity.requested_info_str),
                            localCreationTimestamp = entity.local_creation_timestamp
                    )
                }
    }

    override fun getPendingIncomingGossipingRequests(): List<IncomingShareRequestCommon> {
        return incomingGossipingRequestQueries
                .getByType(GossipRequestType.KEY.name)
                .executeAsList()
                .mapNotNull { entity ->
                    when (entity.type) {
                        GossipRequestType.KEY.name    -> {
                            IncomingRoomKeyRequest(
                                    userId = entity.other_user_id,
                                    deviceId = entity.other_device_id,
                                    requestId = entity.request_id,
                                    requestBody = getRequestedKeyInfo(entity.type, entity.requested_info_str),
                                    localCreationTimestamp = entity.local_creation_timestamp
                            )
                        }
                        GossipRequestType.SECRET.name -> {
                            IncomingSecretShareRequest(
                                    userId = entity.other_user_id,
                                    deviceId = entity.other_device_id,
                                    requestId = entity.request_id,
                                    secretName = getRequestedSecretName(entity.type, entity.requested_info_str),
                                    localCreationTimestamp = entity.local_creation_timestamp
                            )
                        }
                        else                          -> null
                    }
                }
    }

    override fun storeIncomingGossipingRequest(request: IncomingShareRequestCommon, ageLocalTS: Long?) {
        incomingGossipingRequestQueries
                .insertOrUpdate(
                        IncomingGossipingRequestEntity.Impl(
                                other_device_id = request.deviceId,
                                other_user_id = request.userId,
                                request_id = request.requestId ?: "",
                                request_state = GossipingRequestState.PENDING.name,
                                local_creation_timestamp = request.localCreationTimestamp
                                        ?: System.currentTimeMillis(),
                                type = if (request is IncomingSecretShareRequest) GossipRequestType.SECRET.name else if (request is IncomingRoomKeyRequest) GossipRequestType.KEY.name else "",
                                requested_info_str = if (request is IncomingSecretShareRequest) request.secretName else if (request is IncomingRoomKeyRequest) request.requestBody?.toJson() else ""
                        )
                )
    }

    override fun storeDeviceId(deviceId: String) {
        metadataQueries.updateDeviceId(deviceId)
    }

    override fun storeRoomAlgorithm(roomId: String, algorithm: String) {
        val roomEntity = roomQueries.getById(roomId).executeAsOneOrNull()
        if (roomEntity == null) {
            roomQueries
                    .insertOrUpdate(CryptoRoomEntity.Impl(
                            room_id = roomId,
                            algorithm = algorithm,
                            blacklist_unverified_devices = false,
                            should_encrypt_for_invited_members = false
                    ))
        } else {
            roomQueries.updateRoomAlgorithm(algorithm, roomId)
        }
    }

    override fun getRoomAlgorithm(roomId: String): String? {
        return roomQueries
                .getRoomAlgorithm(roomId)
                .executeAsOneOrNull()
                ?.algorithm
    }

    override fun shouldEncryptForInvitedMembers(roomId: String): Boolean {
        return roomQueries
                .shouldEncryptForInvitedMembers(roomId)
                .executeAsOneOrNull() ?: false
    }

    override fun setShouldEncryptForInvitedMembers(roomId: String, shouldEncryptForInvitedMembers: Boolean) {
        roomQueries.updateShouldEncryptForInvitedMembers(shouldEncryptForInvitedMembers, roomId)
    }

    override fun storeSession(olmSessionWrapper: OlmSessionWrapper, deviceKey: String) {
        var sessionIdentifier: String? = null

        try {
            sessionIdentifier = olmSessionWrapper.olmSession.sessionIdentifier()
        } catch (e: OlmException) {
            Timber.e(e, "## storeSession() : sessionIdentifier failed")
        }

        if (sessionIdentifier != null) {
            val key = "$sessionIdentifier|$deviceKey"

            // Release memory of previously known session, if it is not the same one
            if (olmSessionsToRelease[key]?.olmSession != olmSessionWrapper.olmSession) {
                olmSessionsToRelease[key]?.olmSession?.releaseSession()
            }

            olmSessionsToRelease[key] = olmSessionWrapper

            olmSessionQueries
                    .insertOrUpdate(
                            OlmSessionEntity.Impl(
                                    session_id = sessionIdentifier,
                                    device_key = deviceKey,
                                    olm_session_data = serializeForSqlite(olmSessionWrapper.olmSession),
                                    last_received_message_ts = olmSessionWrapper.lastReceivedMessageTs
                            )
                    )
        }
    }

    override fun getDeviceSessionIds(deviceKey: String): Set<String>? {
        return olmSessionQueries
                .getByDeviceKey(deviceKey)
                .executeAsList()
                .map { it.session_id }
                .toMutableSet()
    }

    override fun getDeviceSession(sessionId: String?, deviceKey: String?): OlmSessionWrapper? {
        if (sessionId == null || deviceKey == null) {
            return null
        }

        val key = "$sessionId|$deviceKey"

        if (olmSessionsToRelease[key] == null) {
            olmSessionQueries
                    .getBySessionIdAndDeviceKey(sessionId, deviceKey)
                    .executeAsOneOrNull()
                    ?.let {
                        deserializeFromSqlite<OlmSession>(it.olm_session_data)?.let { olmSession ->
                            olmSessionsToRelease[key] = OlmSessionWrapper(olmSession, it.last_received_message_ts)
                        }
                    }
        }

        return olmSessionsToRelease[key]
    }

    override fun getLastUsedSessionId(deviceKey: String): String? {
        return olmSessionQueries
                .getByDeviceKeyDescending(deviceKey)
                .executeAsOneOrNull()
                ?.session_id
    }

    override fun storeInboundGroupSessions(sessions: List<OlmInboundGroupSessionWrapper>) {
        if (sessions.isEmpty()) {
            return
        }

        olmInboundGroupSessionQueries.transaction {
            sessions.forEach { session ->
                var sessionIdentifier: String? = null

                try {
                    sessionIdentifier = session.olmInboundGroupSession?.sessionIdentifier()
                } catch (e: OlmException) {
                    Timber.e(e, "## storeInboundGroupSession() : sessionIdentifier failed")
                }

                if (sessionIdentifier != null) {
                    val key = "$sessionIdentifier|$session.senderKey"

                    // Release memory of previously known session, if it is not the same one
                    if (inboundGroupSessionToRelease[key] != session) {
                        inboundGroupSessionToRelease[key]?.olmInboundGroupSession?.releaseSession()
                    }

                    inboundGroupSessionToRelease[key] = session

                    olmInboundGroupSessionQueries
                            .insertOrUpdate(OlmInboundGroupSessionEntity.Impl(
                                    session_id = sessionIdentifier,
                                    sender_key = session.senderKey ?: "",
                                    olm_inbound_group_session_data = serializeForSqlite(session),
                                    backed_up = false
                            ))
                }
            }
        }
    }

    override fun getInboundGroupSession(sessionId: String, senderKey: String): OlmInboundGroupSessionWrapper? {
        val key = "$sessionId|$senderKey"

        if (inboundGroupSessionToRelease[key] == null) {
            olmInboundGroupSessionQueries
                    .getBySessionIdAndSenderKey(sessionId, senderKey)
                    .executeAsOneOrNull()
                    ?.let { entity ->
                        deserializeFromSqlite<OlmInboundGroupSessionWrapper>(entity.olm_inbound_group_session_data)?.let {
                            inboundGroupSessionToRelease[key] = it
                        }
                    }
        }

        return inboundGroupSessionToRelease[key]
    }

    override fun removeInboundGroupSession(sessionId: String, senderKey: String) {
        val key = "$sessionId|$senderKey"

        // Release memory of previously known session
        inboundGroupSessionToRelease[key]?.olmInboundGroupSession?.releaseSession()
        inboundGroupSessionToRelease.remove(key)

        olmInboundGroupSessionQueries
                .deleteBySessionIdAndSenderKey(sessionId, senderKey)
    }

    override fun resetBackupMarkers() {
        olmInboundGroupSessionQueries.setAllBackedUp(false)
    }

    override fun markBackupDoneForInboundGroupSessions(olmInboundGroupSessionWrappers: List<OlmInboundGroupSessionWrapper>) {
        if (olmInboundGroupSessionWrappers.isEmpty()) {
            return
        }

        olmInboundGroupSessionWrappers.forEach { olmInboundGroupSessionWrapper ->
            val sessionId = olmInboundGroupSessionWrapper.olmInboundGroupSession?.sessionIdentifier()
            val senderKey = olmInboundGroupSessionWrapper.senderKey
            if (sessionId != null && senderKey != null) {
                olmInboundGroupSessionQueries.setBackedUp(true, sessionId, senderKey)
            }
        }
    }

    override fun inboundGroupSessionsToBackup(limit: Int): List<OlmInboundGroupSessionWrapper> {
        return olmInboundGroupSessionQueries
                .getAllBackedUp(false, limit.toLong())
                .executeAsList()
                .mapNotNull {
                    deserializeFromSqlite<OlmInboundGroupSessionWrapper>(it.olm_inbound_group_session_data)
                }
    }

    override fun inboundGroupSessionsCount(onlyBackedUp: Boolean): Int {
        return if (onlyBackedUp) {
            olmInboundGroupSessionQueries.countBackedUp(true).executeAsOne().toInt()
        } else {
            olmInboundGroupSessionQueries.countAll().executeAsOne().toInt()
        }
    }

    override fun saveDeviceTrackingStatuses(deviceTrackingStatuses: Map<String, Int>) {
        userQueries.transaction {
            deviceTrackingStatuses.map { entry ->
                userQueries.insertOrUpdate(CryptoUserEntity.Impl(user_id = entry.key, device_tracking_status = entry.value))
            }
        }
    }

    override fun getDeviceTrackingStatus(userId: String, defaultValue: Int): Int {
        return userQueries
                .getByUserId(userId)
                .executeAsOneOrNull()
                ?.device_tracking_status ?: defaultValue
    }

    override fun getOutgoingRoomKeyRequest(requestBody: RoomKeyRequestBody): OutgoingRoomKeyRequest? {
        return outgoingGossipingRequestQueries
                .getByType(GossipRequestType.KEY.name)
                .executeAsList()
                .mapNotNull {
                    toOutgoingGossipingRequest(it) as? OutgoingRoomKeyRequest
                }.firstOrNull {
                    it.requestBody?.algorithm == requestBody.algorithm
                    it.requestBody?.roomId == requestBody.roomId
                    it.requestBody?.senderKey == requestBody.senderKey
                    it.requestBody?.sessionId == requestBody.sessionId
                }
    }

    override fun getOrAddOutgoingRoomKeyRequest(requestBody: RoomKeyRequestBody, recipients: Map<String, List<String>>): OutgoingRoomKeyRequest? {
        // Insert the request and return the one passed in parameter
        var request: OutgoingRoomKeyRequest? = null
        outgoingGossipingRequestQueries.transaction {
            val existing = outgoingGossipingRequestQueries
                    .getByType(GossipRequestType.KEY.name)
                    .executeAsList()
                    .mapNotNull {
                        toOutgoingGossipingRequest(it) as? OutgoingRoomKeyRequest
                    }.firstOrNull {
                        it.requestBody?.algorithm == requestBody.algorithm
                                && it.requestBody?.sessionId == requestBody.sessionId
                                && it.requestBody?.senderKey == requestBody.senderKey
                                && it.requestBody?.roomId == requestBody.roomId
                    }
            if (existing == null) {
                val entity = OutgoingGossipingRequestEntity.Impl(
                        request_id = LocalEcho.createLocalEchoId(),
                        type = GossipRequestType.KEY.name,
                        recipients_data = getRecipientsData(recipients),
                        requested_info = requestBody.toJson(),
                        request_state = OutgoingGossipingRequestState.UNSENT.name
                )
                outgoingGossipingRequestQueries.insertOrUpdate(entity)
                request = toOutgoingGossipingRequest(entity) as? OutgoingRoomKeyRequest
            } else {
                request = existing
            }
        }
        return request
    }

    override fun getOrAddOutgoingSecretShareRequest(secretName: String, recipients: Map<String, List<String>>): OutgoingSecretRequest? {
        // Insert the request and return the one passed in parameter
        var request: OutgoingSecretRequest? = null
        outgoingGossipingRequestQueries.transaction {
            val existing = outgoingGossipingRequestQueries
                    .getByTypeAndRequestedInfo(GossipRequestType.KEY.name, secretName)
                    .executeAsList()
                    .mapNotNull {
                        toOutgoingGossipingRequest(it) as? OutgoingSecretRequest
                    }.firstOrNull()
            request = if (existing == null) {
                val entity = OutgoingGossipingRequestEntity.Impl(
                        request_id = LocalEcho.createLocalEchoId(),
                        type = GossipRequestType.SECRET.name,
                        recipients_data = getRecipientsData(recipients),
                        requested_info = secretName,
                        request_state = OutgoingGossipingRequestState.UNSENT.name
                )
                outgoingGossipingRequestQueries.insertOrUpdate(entity)
                toOutgoingGossipingRequest(entity) as? OutgoingSecretRequest
            } else {
                existing
            }
        }
        return request
    }

    override fun saveGossipingEvent(event: Event) {
        val now = System.currentTimeMillis()
        val ageLocalTs = event.unsignedData?.age?.let { now - it } ?: now
        gossipingEventQueries
                .insertOrUpdate(
                        GossipingEventEntity.Impl(
                                type = event.type,
                                age_local_ts = ageLocalTs,
                                content = ContentMapper.map(event.content),
                                decryption_error_code = event.mCryptoError?.name,
                                decryption_result_json = MoshiProvider.providesMoshi().adapter<OlmDecryptionResult>(OlmDecryptionResult::class.java).toJson(event.mxDecryptionResult),
                                sender = event.senderId ?: "",
                                send_state = SendState.SYNCED.name
                        )
                )
    }

    override fun updateGossipingRequestState(request: IncomingShareRequestCommon, state: GossipingRequestState) {
        incomingGossipingRequestQueries
                .updateRequestState(
                        state.name,
                        request.userId,
                        request.deviceId,
                        request.requestId ?: ""
                )
    }

    override fun getIncomingRoomKeyRequest(userId: String, deviceId: String, requestId: String): IncomingRoomKeyRequest? {
        return incomingGossipingRequestQueries
                .getIncomingRoomKeyRequest(GossipRequestType.KEY.name, userId, deviceId)
                .executeAsList()
                .mapNotNull {
                    toIncomingGossipingRequest(it) as? IncomingRoomKeyRequest
                }.firstOrNull()
    }

    override fun updateOutgoingGossipingRequestState(requestId: String, state: OutgoingGossipingRequestState) {
        outgoingGossipingRequestQueries.updateRequestState(state.name, requestId)
    }

    override fun addNewSessionListener(listener: NewSessionListener) {
        if (!newSessionListeners.contains(listener)) newSessionListeners.add(listener)
    }

    override fun removeSessionListener(listener: NewSessionListener) {
        newSessionListeners.remove(listener)
    }

    override fun getMyCrossSigningInfo(): MXCrossSigningInfo? {
        return metadataQueries
                .getAll()
                .executeAsOneOrNull()
                ?.user_id
                ?.let { getCrossSigningInfo(it) }
    }

    override fun setMyCrossSigningInfo(info: MXCrossSigningInfo?) {
        crossSigningInfoQueries.transaction {
            if (info == null) {
                // Delete known if needed
                info?.userId?.let { crossSigningInfoQueries.deleteByUserId(it) }
                // TODO notify, we might need to untrust things?
            } else {
                info.crossSigningKeys.forEach { cryptoCrossSigningKey ->
                    crossSigningInfoQueries
                            .insertOrUpdate(
                                    CrossSigningInfoEntity.Impl(
                                            user_id = info.userId,
                                            signatures = serializeForSqlite(cryptoCrossSigningKey.signatures),
                                            public_key_base64 = cryptoCrossSigningKey.unpaddedBase64PublicKey,
                                            usages = cryptoCrossSigningKey.usages ?: emptyList(),
                                            locally_verified = false,
                                            cross_signed_verified = false
                                    )
                            )
                }
            }
        }
    }

    override fun getCrossSigningInfo(userId: String): MXCrossSigningInfo? {
        return mapCrossSigningInfoEntity(userId)
    }

    private fun mapCrossSigningInfoEntity(userId: String): MXCrossSigningInfo {
        val crossSigningKeyList = mutableListOf<CryptoCrossSigningKey>()
        val crossSigningInfo = MXCrossSigningInfo(userId, crossSigningKeyList)

        val crossSigningInfoEntityList = crossSigningInfoQueries.getByUserId(userId).executeAsList()

        crossSigningInfoEntityList.forEach { crossSigningInfoEntity ->
            val pubKey = crossSigningInfoEntity.public_key_base64 ?: return@forEach

            crossSigningKeyList.add(CryptoCrossSigningKey(
                    userId = crossSigningInfoEntity.user_id,
                    signatures = deserializeFromSqlite(crossSigningInfoEntity.signatures),
                    trustLevel = DeviceTrustLevel(
                            crossSigningInfoEntity.cross_signed_verified,
                            crossSigningInfoEntity.locally_verified
                    ),
                    keys = mapOf("ed25519:$pubKey" to pubKey),
                    usages = crossSigningInfoEntity.usages
            ))
        }
        return crossSigningInfo
    }

    override fun getLiveCrossSigningInfo(userId: String): LiveData<Optional<MXCrossSigningInfo>> {
        val query = crossSigningInfoQueries.getByUserId(userId)

        return MutableLiveData<Optional<MXCrossSigningInfo>>().apply {
            query.addListener(object : Query.Listener {
                override fun queryResultsChanged() {
                    postValue(Optional(getCrossSigningInfo(userId)))
                }
            })
        }
    }

    override fun setCrossSigningInfo(userId: String, info: MXCrossSigningInfo?) {
        crossSigningInfoQueries.transaction {
            if (info == null) {
                // Delete known if needed
                info?.userId?.let { crossSigningInfoQueries.deleteByUserId(it) }
                // TODO notify, we might need to untrust things?
            } else {
                info.crossSigningKeys.forEach { cryptoCrossSigningKey ->
                    crossSigningInfoQueries
                            .insertOrUpdate(
                                    CrossSigningInfoEntity.Impl(
                                            user_id = info.userId,
                                            signatures = serializeForSqlite(cryptoCrossSigningKey.signatures),
                                            public_key_base64 = cryptoCrossSigningKey.unpaddedBase64PublicKey,
                                            usages = cryptoCrossSigningKey.usages ?: emptyList(),
                                            locally_verified = false,
                                            cross_signed_verified = false
                                    )
                            )
                }
            }
        }
    }

    override fun markMyMasterKeyAsLocallyTrusted(trusted: Boolean) {
        metadataQueries.getAll().executeAsOneOrNull()?.user_id?.let { myUserId ->
            crossSigningInfoQueries.transaction {
                crossSigningInfoQueries
                        .getByUserId(myUserId)
                        .executeAsList()
                        .firstOrNull { it.usages.contains(KeyUsage.MASTER.value) }
                        ?.let { xInfoEntity ->
                            crossSigningInfoQueries
                                    .updateLocallyVerifiedWithRowId(trusted, xInfoEntity._id)
                        }
            }
        }
    }

    override fun storePrivateKeysInfo(msk: String?, usk: String?, ssk: String?) {
        metadataQueries.updateKeys(msk, usk, ssk)
    }

    override fun storeSSKPrivateKey(ssk: String?) {
        metadataQueries.updateSelfSignedPrivateKey(ssk)
    }

    override fun storeUSKPrivateKey(usk: String?) {
        metadataQueries.updateUserPrivateKey(usk)
    }

    override fun getCrossSigningPrivateKeys(): PrivateKeysInfo? {
        return metadataQueries
                .getAll()
                .executeAsOneOrNull()
                ?.let {
                    PrivateKeysInfo(
                            master = it.x_sign_master_private_key,
                            user = it.x_sign_user_private_key,
                            selfSigned = it.x_sign_self_signed_private_key
                    )
                }
    }

    override fun setUserKeysAsTrusted(userId: String, trusted: Boolean) {
        crossSigningInfoQueries.updateVerified(
                locallyVerified = trusted,
                crossSignedVerified = trusted,
                userId = userId
        )
    }

    override fun setDeviceTrust(userId: String, deviceId: String, crossSignedVerified: Boolean, locallyVerified: Boolean) {
        deviceInfoQueries.updateVerified(locallyVerified, crossSignedVerified, userId, deviceId)
    }

    override fun clearOtherUserTrust() {
        crossSigningInfoQueries.clearOtherUserTrust(credentials.userId)
    }

    override fun updateUsersTrust(check: (String) -> Boolean) {
        crossSigningInfoQueries.transaction {
            crossSigningInfoQueries
                    .getAll()
                    .executeAsList()
                    .forEach { xInfoEntity ->
                        // Need to ignore mine
                        if (xInfoEntity.user_id == credentials.userId) return@forEach
                        val mapped = mapCrossSigningInfoEntity(xInfoEntity.user_id)
                        val currentTrust = mapped.isTrusted()
                        val newTrust = check(mapped.userId)
                        if (currentTrust != newTrust) {
                            crossSigningInfoQueries
                                    .updateVerified(
                                            locallyVerified = newTrust,
                                            crossSignedVerified = newTrust,
                                            userId = xInfoEntity.user_id
                                    )
                        }
                    }
        }
    }

    override fun getOutgoingRoomKeyRequests(): List<OutgoingRoomKeyRequest> {
        return outgoingGossipingRequestQueries
                .getByType(GossipRequestType.KEY.name)
                .executeAsList()
                .mapNotNull { toOutgoingGossipingRequest(it) as? OutgoingRoomKeyRequest }
    }

    override fun getOutgoingSecretKeyRequests(): List<OutgoingSecretRequest> {
        return outgoingGossipingRequestQueries
                .getByType(GossipRequestType.KEY.name)
                .executeAsList()
                .mapNotNull { toOutgoingGossipingRequest(it) as? OutgoingSecretRequest }
    }

    override fun getOutgoingSecretRequest(secretName: String): OutgoingSecretRequest? {
        return outgoingGossipingRequestQueries
                .getByTypeAndRequestedInfo(GossipRequestType.KEY.name, secretName)
                .executeAsList()
                .firstOrNull()
                ?.let { toOutgoingGossipingRequest(it) as? OutgoingSecretRequest }
    }

    override fun getIncomingRoomKeyRequests(): List<IncomingRoomKeyRequest> {
        return incomingGossipingRequestQueries
                .getAll()
                .executeAsList()
                .mapNotNull { toIncomingGossipingRequest(it) as? IncomingRoomKeyRequest }
    }

    override fun getGossipingEventsTrail(): List<Event> {
        return gossipingEventQueries
                .getAll()
                .executeAsList()
                .map {
                    Event(
                            type = it.type,
                            content = ContentMapper.map(it.content),
                            senderId = it.sender
                    ).also { event ->
                        event.ageLocalTs = it.age_local_ts
                        event.sendState = it.send_state?.let { SendState.valueOf(it) }
                                ?: SendState.UNKNOWN
                        it.decryption_result_json?.let { json ->
                            try {
                                event.mxDecryptionResult = MoshiProvider.providesMoshi().adapter(OlmDecryptionResult::class.java).fromJson(json)
                            } catch (t: JsonDataException) {
                                Timber.e(t, "Failed to parse decryption result")
                            }
                        }
                        // TODO get the full crypto error object
                        event.mCryptoError = it.decryption_error_code?.let { errorCode ->
                            MXCryptoError.ErrorType.valueOf(errorCode)
                        }
                    }
                }
    }

    override fun saveBackupRecoveryKey(recoveryKey: String?, version: String?) {
        metadataQueries.updateKeyBackupRecoveryKey(recoveryKey, version)
    }

    override fun getKeyBackupRecoveryKeyInfo(): SavedKeyBackupKeyInfo? {
        return metadataQueries
                .getAll()
                .executeAsOneOrNull()
                ?.let { metadataEntity ->
                    val key = metadataEntity.key_backup_recovery_key
                    val version = metadataEntity.key_backup_recovery_key_version
                    if (!key.isNullOrBlank() && !version.isNullOrBlank()) {
                        SavedKeyBackupKeyInfo(recoveryKey = key, version = version)
                    } else {
                        null
                    }
                }
    }
}
