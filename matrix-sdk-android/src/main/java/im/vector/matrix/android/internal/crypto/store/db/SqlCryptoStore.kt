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

import androidx.lifecycle.LiveData
import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.api.session.crypto.crosssigning.MXCrossSigningInfo
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.util.Optional
import im.vector.matrix.android.internal.crypto.*
import im.vector.matrix.android.internal.crypto.model.CryptoCrossSigningKey
import im.vector.matrix.android.internal.crypto.model.CryptoDeviceInfo
import im.vector.matrix.android.internal.crypto.model.OlmInboundGroupSessionWrapper
import im.vector.matrix.android.internal.crypto.model.OlmSessionWrapper
import im.vector.matrix.android.internal.crypto.model.rest.RoomKeyRequestBody
import im.vector.matrix.android.internal.crypto.store.IMXCryptoStore
import im.vector.matrix.android.internal.crypto.store.PrivateKeysInfo
import im.vector.matrix.android.internal.crypto.store.db.model.KeysBackupDataEntity
import im.vector.matrix.android.internal.session.SessionScope
import im.vector.matrix.sqldelight.crypto.CryptoDatabase
import org.matrix.olm.OlmAccount
import javax.inject.Inject

@SessionScope
internal class SqlCryptoStore @Inject constructor(private val cryptoDatabase: CryptoDatabase,
                                                  private val credentials: Credentials) : IMXCryptoStore {
    override fun getDeviceId(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getOlmAccount(): OlmAccount {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getOrCreateOlmAccount(): OlmAccount {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getInboundGroupSessions(): List<OlmInboundGroupSessionWrapper> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getGlobalBlacklistUnverifiedDevices(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setGlobalBlacklistUnverifiedDevices(block: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getRoomsListBlacklistUnverifiedDevices(): List<String> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setRoomsListBlacklistUnverifiedDevices(roomIds: List<String>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getKeyBackupVersion(): String? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setKeyBackupVersion(keyBackupVersion: String?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getKeysBackupData(): KeysBackupDataEntity? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setKeysBackupData(keysBackupData: KeysBackupDataEntity?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getDeviceTrackingStatuses(): Map<String, Int> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getPendingIncomingRoomKeyRequests(): List<IncomingRoomKeyRequest> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getPendingIncomingGossipingRequests(): List<IncomingShareRequestCommon> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun storeIncomingGossipingRequest(request: IncomingShareRequestCommon, ageLocalTS: Long?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun hasData(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun deleteStore() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun open() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun close() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun storeDeviceId(deviceId: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun saveOlmAccount() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun storeUserDevice(userId: String?, deviceInfo: CryptoDeviceInfo?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getUserDevice(userId: String, deviceId: String): CryptoDeviceInfo? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun deviceWithIdentityKey(identityKey: String): CryptoDeviceInfo? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun storeUserDevices(userId: String, devices: Map<String, CryptoDeviceInfo>?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun storeUserCrossSigningKeys(userId: String, masterKey: CryptoCrossSigningKey?, selfSigningKey: CryptoCrossSigningKey?, userSigningKey: CryptoCrossSigningKey?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getUserDevices(userId: String): Map<String, CryptoDeviceInfo>? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getUserDeviceList(userId: String): List<CryptoDeviceInfo>? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getLiveDeviceList(userId: String): LiveData<List<CryptoDeviceInfo>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getLiveDeviceList(userIds: List<String>): LiveData<List<CryptoDeviceInfo>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getLiveDeviceList(): LiveData<List<CryptoDeviceInfo>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun storeRoomAlgorithm(roomId: String, algorithm: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getRoomAlgorithm(roomId: String): String? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun shouldEncryptForInvitedMembers(roomId: String): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setShouldEncryptForInvitedMembers(roomId: String, shouldEncryptForInvitedMembers: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun storeSession(olmSessionWrapper: OlmSessionWrapper, deviceKey: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getDeviceSessionIds(deviceKey: String): Set<String>? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getDeviceSession(sessionId: String?, deviceKey: String?): OlmSessionWrapper? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getLastUsedSessionId(deviceKey: String): String? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun storeInboundGroupSessions(sessions: List<OlmInboundGroupSessionWrapper>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getInboundGroupSession(sessionId: String, senderKey: String): OlmInboundGroupSessionWrapper? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun removeInboundGroupSession(sessionId: String, senderKey: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun resetBackupMarkers() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun markBackupDoneForInboundGroupSessions(olmInboundGroupSessionWrappers: List<OlmInboundGroupSessionWrapper>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun inboundGroupSessionsToBackup(limit: Int): List<OlmInboundGroupSessionWrapper> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun inboundGroupSessionsCount(onlyBackedUp: Boolean): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun saveDeviceTrackingStatuses(deviceTrackingStatuses: Map<String, Int>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getDeviceTrackingStatus(userId: String, defaultValue: Int): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getOutgoingRoomKeyRequest(requestBody: RoomKeyRequestBody): OutgoingRoomKeyRequest? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getOrAddOutgoingRoomKeyRequest(requestBody: RoomKeyRequestBody, recipients: Map<String, List<String>>): OutgoingRoomKeyRequest? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getOrAddOutgoingSecretShareRequest(secretName: String, recipients: Map<String, List<String>>): OutgoingSecretRequest? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun saveGossipingEvent(event: Event) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun updateGossipingRequestState(request: IncomingShareRequestCommon, state: GossipingRequestState) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getIncomingRoomKeyRequest(userId: String, deviceId: String, requestId: String): IncomingRoomKeyRequest? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun updateOutgoingGossipingRequestState(requestId: String, state: OutgoingGossipingRequestState) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun addNewSessionListener(listener: NewSessionListener) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun removeSessionListener(listener: NewSessionListener) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getMyCrossSigningInfo(): MXCrossSigningInfo? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setMyCrossSigningInfo(info: MXCrossSigningInfo?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getCrossSigningInfo(userId: String): MXCrossSigningInfo? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getLiveCrossSigningInfo(userId: String): LiveData<Optional<MXCrossSigningInfo>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setCrossSigningInfo(userId: String, info: MXCrossSigningInfo?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun markMyMasterKeyAsLocallyTrusted(trusted: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun storePrivateKeysInfo(msk: String?, usk: String?, ssk: String?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun storeSSKPrivateKey(ssk: String?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun storeUSKPrivateKey(usk: String?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getCrossSigningPrivateKeys(): PrivateKeysInfo? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setUserKeysAsTrusted(userId: String, trusted: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setDeviceTrust(userId: String, deviceId: String, crossSignedVerified: Boolean, locallyVerified: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun clearOtherUserTrust() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun updateUsersTrust(check: (String) -> Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getOutgoingRoomKeyRequests(): List<OutgoingRoomKeyRequest> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getOutgoingSecretKeyRequests(): List<OutgoingSecretRequest> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getOutgoingSecretRequest(secretName: String): OutgoingSecretRequest? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getIncomingRoomKeyRequests(): List<IncomingRoomKeyRequest> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getGossipingEventsTrail(): List<Event> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


}
