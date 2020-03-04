/*
 * Copyright 2016 OpenMarket Ltd
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

package im.vector.matrix.android.internal.crypto.store

import androidx.lifecycle.LiveData
import im.vector.matrix.android.api.session.crypto.crosssigning.MXCrossSigningInfo
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.util.Optional
import im.vector.matrix.android.internal.crypto.GossipingRequestState
import im.vector.matrix.android.internal.crypto.IncomingRoomKeyRequest
import im.vector.matrix.android.internal.crypto.IncomingShareRequestCommon
import im.vector.matrix.android.internal.crypto.NewSessionListener
import im.vector.matrix.android.internal.crypto.OutgoingGossipingRequestState
import im.vector.matrix.android.internal.crypto.OutgoingRoomKeyRequest
import im.vector.matrix.android.internal.crypto.OutgoingSecretRequest
import im.vector.matrix.android.internal.crypto.model.CryptoCrossSigningKey
import im.vector.matrix.android.internal.crypto.model.CryptoDeviceInfo
import im.vector.matrix.android.internal.crypto.model.OlmInboundGroupSessionWrapper
import im.vector.matrix.android.internal.crypto.model.OlmSessionWrapper
import im.vector.matrix.android.internal.crypto.model.rest.RoomKeyRequestBody
import im.vector.matrix.android.internal.crypto.store.db.model.KeysBackupDataEntity
import org.matrix.olm.OlmAccount

/**
 * the crypto data store
 */
internal interface IMXCryptoStore {

    /**
     * @return the device id
     */
    fun getDeviceId(): String

    /**
     * @return the olm account
     */
    fun getOlmAccount(): OlmAccount

    fun getOrCreateOlmAccount(): OlmAccount

    /**
     * Retrieve the known inbound group sessions.
     *
     * @return the list of all known group sessions, to export them.
     */
    fun getInboundGroupSessions(): List<OlmInboundGroupSessionWrapper>

    /**
     * @return true to unilaterally blacklist all unverified devices.
     */
    fun getGlobalBlacklistUnverifiedDevices(): Boolean

    /**
     * Set the global override for whether the client should ever send encrypted
     * messages to unverified devices.
     * If false, it can still be overridden per-room.
     * If true, it overrides the per-room settings.
     *
     * @param block true to unilaterally blacklist all
     */
    fun setGlobalBlacklistUnverifiedDevices(block: Boolean)

    /**
     * Provides the rooms ids list in which the messages are not encrypted for the unverified devices.
     *
     * @return the room Ids list
     */
    fun getRoomsListBlacklistUnverifiedDevices(): List<String>

    /**
     * Updates the rooms ids list in which the messages are not encrypted for the unverified devices.
     *
     * @param roomIds the room ids list
     */
    fun setRoomsListBlacklistUnverifiedDevices(roomIds: List<String>)

    /**
     * Get the current keys backup version
     */
    fun getKeyBackupVersion(): String?

    /**
     * Set the current keys backup version
     *
     * @param keyBackupVersion the keys backup version or null to delete it
     */
    fun setKeyBackupVersion(keyBackupVersion: String?)

    /**
     * Get the current keys backup local data
     */
    fun getKeysBackupData(): KeysBackupDataEntity?

    /**
     * Set the keys backup local data
     *
     * @param keysBackupData the keys backup local data, or null to erase data
     */
    fun setKeysBackupData(keysBackupData: KeysBackupDataEntity?)

    /**
     * @return the devices statuses map (userId -> tracking status)
     */
    fun getDeviceTrackingStatuses(): Map<String, Int>

    /**
     * @return the pending IncomingRoomKeyRequest requests
     */
    fun getPendingIncomingRoomKeyRequests(): List<IncomingRoomKeyRequest>

    fun getPendingIncomingGossipingRequests(): List<IncomingShareRequestCommon>
    fun storeIncomingGossipingRequest(request: IncomingShareRequestCommon, ageLocalTS: Long?)
//    fun getPendingIncomingSecretShareRequests(): List<IncomingSecretShareRequest>

    /**
     * Indicate if the store contains data for the passed account.
     *
     * @return true means that the user enabled the crypto in a previous session
     */
    fun hasData(): Boolean

    /**
     * Delete the crypto store for the passed credentials.
     */
    fun deleteStore()

    /**
     * open any existing crypto store
     */
    fun open()

    /**
     * Close the store
     */
    fun close()

    /**
     * Store the device id.
     *
     * @param deviceId the device id
     */
    fun storeDeviceId(deviceId: String)

    /**
     * Store the end to end account for the logged-in user.
     *
     * @param account the account to save
     */
    fun saveOlmAccount()

    /**
     * Store a device for a user.
     *
     * @param userId the user's id.
     * @param device the device to store.
     */
    fun storeUserDevice(userId: String?, deviceInfo: CryptoDeviceInfo?)

    /**
     * Retrieve a device for a user.
     *
     * @param deviceId the device id.
     * @param userId   the user's id.
     * @return the device
     */
    fun getUserDevice(userId: String, deviceId: String): CryptoDeviceInfo?

    /**
     * Retrieve a device by its identity key.
     *
     * @param identityKey the device identity key (`MXDeviceInfo.identityKey`)
     * @return the device or null if not found
     */
    fun deviceWithIdentityKey(identityKey: String): CryptoDeviceInfo?

    /**
     * Store the known devices for a user.
     *
     * @param userId  The user's id.
     * @param devices A map from device id to 'MXDevice' object for the device.
     */
    fun storeUserDevices(userId: String, devices: Map<String, CryptoDeviceInfo>?)

    fun storeUserCrossSigningKeys(userId: String,
                                  masterKey: CryptoCrossSigningKey?,
                                  selfSigningKey: CryptoCrossSigningKey?,
                                  userSigningKey: CryptoCrossSigningKey?)

    /**
     * Retrieve the known devices for a user.
     *
     * @param userId The user's id.
     * @return The devices map if some devices are known, else null
     */
    fun getUserDevices(userId: String): Map<String, CryptoDeviceInfo>?

    fun getUserDeviceList(userId: String): List<CryptoDeviceInfo>?

    fun getLiveDeviceList(userId: String): LiveData<List<CryptoDeviceInfo>>

    fun getLiveDeviceList(userIds: List<String>): LiveData<List<CryptoDeviceInfo>>

    // TODO temp
    fun getLiveDeviceList(): LiveData<List<CryptoDeviceInfo>>

    /**
     * Store the crypto algorithm for a room.
     *
     * @param roomId    the id of the room.
     * @param algorithm the algorithm.
     */
    fun storeRoomAlgorithm(roomId: String, algorithm: String)

    /**
     * Provides the algorithm used in a dedicated room.
     *
     * @param roomId the room id
     * @return the algorithm, null is the room is not encrypted
     */
    fun getRoomAlgorithm(roomId: String): String?

    fun shouldEncryptForInvitedMembers(roomId: String): Boolean

    fun setShouldEncryptForInvitedMembers(roomId: String, shouldEncryptForInvitedMembers: Boolean)

    /**
     * Store a session between the logged-in user and another device.
     *
     * @param olmSessionWrapper   the end-to-end session.
     * @param deviceKey the public key of the other device.
     */
    fun storeSession(olmSessionWrapper: OlmSessionWrapper, deviceKey: String)

    /**
     * Retrieve the end-to-end session ids between the logged-in user and another
     * device.
     *
     * @param deviceKey the public key of the other device.
     * @return A set of sessionId, or null if device is not known
     */
    fun getDeviceSessionIds(deviceKey: String): Set<String>?

    /**
     * Retrieve an end-to-end session between the logged-in user and another
     * device.
     *
     * @param sessionId the session Id.
     * @param deviceKey the public key of the other device.
     * @return The Base64 end-to-end session, or null if not found
     */
    fun getDeviceSession(sessionId: String, deviceKey: String): OlmSessionWrapper?

    /**
     * Retrieve the last used sessionId, regarding `lastReceivedMessageTs`, or null if no session exist
     *
     * @param deviceKey the public key of the other device.
     * @return last used sessionId, or null if not found
     */
    fun getLastUsedSessionId(deviceKey: String): String?

    /**
     * Store inbound group sessions.
     *
     * @param sessions the inbound group sessions to store.
     */
    fun storeInboundGroupSessions(sessions: List<OlmInboundGroupSessionWrapper>)

    /**
     * Retrieve an inbound group session.
     *
     * @param sessionId the session identifier.
     * @param senderKey the base64-encoded curve25519 key of the sender.
     * @return an inbound group session.
     */
    fun getInboundGroupSession(sessionId: String, senderKey: String): OlmInboundGroupSessionWrapper?

    /**
     * Remove an inbound group session
     *
     * @param sessionId the session identifier.
     * @param senderKey the base64-encoded curve25519 key of the sender.
     */
    fun removeInboundGroupSession(sessionId: String, senderKey: String)

    /* ==========================================================================================
     * Keys backup
     * ========================================================================================== */

    /**
     * Mark all inbound group sessions as not backed up.
     */
    fun resetBackupMarkers()

    /**
     * Mark inbound group sessions as backed up on the user homeserver.
     *
     * @param sessions the sessions
     */
    fun markBackupDoneForInboundGroupSessions(olmInboundGroupSessionWrappers: List<OlmInboundGroupSessionWrapper>)

    /**
     * Retrieve inbound group sessions that are not yet backed up.
     *
     * @param limit the maximum number of sessions to return.
     * @return an array of non backed up inbound group sessions.
     */
    fun inboundGroupSessionsToBackup(limit: Int): List<OlmInboundGroupSessionWrapper>

    /**
     * Number of stored inbound group sessions.
     *
     * @param onlyBackedUp if true, count only session marked as backed up.
     * @return a count.
     */
    fun inboundGroupSessionsCount(onlyBackedUp: Boolean): Int

    /**
     * Save the device statuses
     *
     * @param deviceTrackingStatuses the device tracking statuses
     */
    fun saveDeviceTrackingStatuses(deviceTrackingStatuses: Map<String, Int>)

    /**
     * Get the tracking status of a specified userId devices.
     *
     * @param userId       the user id
     * @param defaultValue the default value
     * @return the tracking status
     */
    fun getDeviceTrackingStatus(userId: String, defaultValue: Int): Int

    /**
     * Look for an existing outgoing room key request, and if none is found, return null
     *
     * @param requestBody the request body
     * @return an OutgoingRoomKeyRequest instance or null
     */
    fun getOutgoingRoomKeyRequest(requestBody: RoomKeyRequestBody): OutgoingRoomKeyRequest?

    /**
     * Look for an existing outgoing room key request, and if none is found, add a new one.
     *
     * @param request the request
     * @return either the same instance as passed in, or the existing one.
     */
    fun getOrAddOutgoingRoomKeyRequest(requestBody: RoomKeyRequestBody, recipients: Map<String, List<String>>): OutgoingRoomKeyRequest?

    fun getOrAddOutgoingSecretShareRequest(secretName: String, recipients: Map<String, List<String>>): OutgoingSecretRequest?

    fun saveGossipingEvent(event: Event)

    fun updateGossipingRequestState(request: IncomingShareRequestCommon, state: GossipingRequestState)

    /**
     * Search an IncomingRoomKeyRequest
     *
     * @param userId    the user id
     * @param deviceId  the device id
     * @param requestId the request id
     * @return an IncomingRoomKeyRequest if it exists, else null
     */
    fun getIncomingRoomKeyRequest(userId: String, deviceId: String, requestId: String): IncomingRoomKeyRequest?

    fun updateOutgoingGossipingRequestState(requestId: String, state: OutgoingGossipingRequestState)

    fun addNewSessionListener(listener: NewSessionListener)

    fun removeSessionListener(listener: NewSessionListener)

    // =============================================
    // CROSS SIGNING
    // =============================================

    /**
     * Gets the current crosssigning info
     */
    fun getMyCrossSigningInfo(): MXCrossSigningInfo?

    fun setMyCrossSigningInfo(info: MXCrossSigningInfo?)

    fun getCrossSigningInfo(userId: String): MXCrossSigningInfo?
    fun getLiveCrossSigningInfo(userId: String): LiveData<Optional<MXCrossSigningInfo>>
    fun setCrossSigningInfo(userId: String, info: MXCrossSigningInfo?)

    fun markMyMasterKeyAsLocallyTrusted(trusted: Boolean)

    fun storePrivateKeysInfo(msk: String?, usk: String?, ssk: String?)
    fun storeSSKPrivateKey(ssk: String?)
    fun storeUSKPrivateKey(usk: String?)

    fun getCrossSigningPrivateKeys(): PrivateKeysInfo?

    fun saveBackupRecoveryKey(recoveryKey: String?, version: String?)
    fun getKeyBackupRecoveryKeyInfo() : SavedKeyBackupKeyInfo?

    fun setUserKeysAsTrusted(userId: String, trusted: Boolean = true)
    fun setDeviceTrust(userId: String, deviceId: String, crossSignedVerified: Boolean, locallyVerified: Boolean)

    fun clearOtherUserTrust()

    fun updateUsersTrust(check: (String) -> Boolean)

    // Dev tools

    fun getOutgoingRoomKeyRequests(): List<OutgoingRoomKeyRequest>
    fun getOutgoingSecretKeyRequests(): List<OutgoingSecretRequest>
    fun getOutgoingSecretRequest(secretName: String): OutgoingSecretRequest?
    fun getIncomingRoomKeyRequests(): List<IncomingRoomKeyRequest>
    fun getGossipingEventsTrail(): List<Event>
}
