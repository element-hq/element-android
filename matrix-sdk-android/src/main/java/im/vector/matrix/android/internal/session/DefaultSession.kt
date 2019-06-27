/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.matrix.android.internal.session

import android.content.Context
import android.os.Looper
import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.auth.data.SessionParams
import im.vector.matrix.android.api.listeners.ProgressListener
import im.vector.matrix.android.api.pushrules.PushRuleService
import im.vector.matrix.android.api.pushrules.rest.PushRule
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.cache.CacheService
import im.vector.matrix.android.api.session.content.ContentUploadStateTracker
import im.vector.matrix.android.api.session.content.ContentUrlResolver
import im.vector.matrix.android.api.session.crypto.keysbackup.KeysBackupService
import im.vector.matrix.android.api.session.crypto.keyshare.RoomKeysRequestListener
import im.vector.matrix.android.api.session.crypto.sas.SasVerificationService
import im.vector.matrix.android.api.session.events.model.Content
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.group.Group
import im.vector.matrix.android.api.session.group.GroupService
import im.vector.matrix.android.api.session.group.model.GroupSummary
import im.vector.matrix.android.api.session.pushers.Pusher
import im.vector.matrix.android.api.session.pushers.PushersService
import im.vector.matrix.android.api.session.room.Room
import im.vector.matrix.android.api.session.room.RoomDirectoryService
import im.vector.matrix.android.api.session.room.RoomService
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.matrix.android.api.session.room.model.create.CreateRoomParams
import im.vector.matrix.android.api.session.room.model.roomdirectory.PublicRoomsParams
import im.vector.matrix.android.api.session.room.model.roomdirectory.PublicRoomsResponse
import im.vector.matrix.android.api.session.room.model.thirdparty.ThirdPartyProtocol
import im.vector.matrix.android.api.session.signout.SignOutService
import im.vector.matrix.android.api.session.sync.FilterService
import im.vector.matrix.android.api.session.sync.SyncState
import im.vector.matrix.android.api.session.user.UserService
import im.vector.matrix.android.api.session.user.model.User
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.api.util.MatrixCallbackDelegate
import im.vector.matrix.android.internal.crypto.CryptoManager
import im.vector.matrix.android.internal.crypto.CryptoModule
import im.vector.matrix.android.internal.crypto.MXEventDecryptionResult
import im.vector.matrix.android.internal.crypto.model.ImportRoomKeysResult
import im.vector.matrix.android.internal.crypto.model.MXDeviceInfo
import im.vector.matrix.android.internal.crypto.model.MXEncryptEventContentResult
import im.vector.matrix.android.internal.crypto.model.MXUsersDevicesMap
import im.vector.matrix.android.internal.crypto.model.rest.DevicesListResponse
import im.vector.matrix.android.internal.crypto.model.rest.RoomKeyRequestBody
import im.vector.matrix.android.internal.database.LiveEntityObserver
import im.vector.matrix.android.internal.di.MatrixKoinComponent
import im.vector.matrix.android.internal.di.MatrixKoinHolder
import im.vector.matrix.android.internal.session.content.ContentModule
import im.vector.matrix.android.internal.session.group.GroupModule
import im.vector.matrix.android.internal.session.notification.BingRuleWatcher
import im.vector.matrix.android.internal.session.room.RoomModule
import im.vector.matrix.android.internal.session.signout.SignOutModule
import im.vector.matrix.android.internal.session.sync.SyncModule
import im.vector.matrix.android.internal.session.sync.job.SyncThread
import im.vector.matrix.android.internal.session.sync.job.SyncWorker
import im.vector.matrix.android.internal.session.user.UserModule
import org.koin.core.scope.Scope
import org.koin.standalone.inject
import timber.log.Timber
import java.util.*


internal class DefaultSession(override val sessionParams: SessionParams) : Session, MatrixKoinComponent {

    companion object {
        const val SCOPE: String = "session"
    }

    private lateinit var scope: Scope

    private val monarchy by inject<Monarchy>()
    private val liveEntityUpdaters by inject<List<LiveEntityObserver>>()
    private val sessionListeners by inject<SessionListeners>()
    private val roomService by inject<RoomService>()
    private val roomDirectoryService by inject<RoomDirectoryService>()
    private val groupService by inject<GroupService>()
    private val userService by inject<UserService>()
    private val filterService by inject<FilterService>()
    private val cacheService by inject<CacheService>()
    private val signOutService by inject<SignOutService>()
    private val cryptoService by inject<CryptoManager>()
    private val syncThread by inject<SyncThread>()
    private val contentUrlResolver by inject<ContentUrlResolver>()
    private val contentUploadProgressTracker by inject<ContentUploadStateTracker>()
    private val pushRuleService by inject<PushRuleService>()
    private val pushersService by inject<PushersService>()
    private var isOpen = false

    private val bingRuleWatcher by inject<BingRuleWatcher>()

    @MainThread
    override fun open() {
        assertMainThread()
        assert(!isOpen)
        isOpen = true
        val sessionModule = SessionModule(sessionParams).definition
        val syncModule = SyncModule().definition
        val roomModule = RoomModule().definition
        val groupModule = GroupModule().definition
        val signOutModule = SignOutModule().definition
        val userModule = UserModule().definition
        val contentModule = ContentModule().definition
        val cryptoModule = CryptoModule().definition
        MatrixKoinHolder.instance.loadModules(listOf(sessionModule,
                syncModule,
                roomModule,
                groupModule,
                userModule,
                signOutModule,
                contentModule,
                cryptoModule))
        scope = getKoin().getOrCreateScope(SCOPE)
        if (!monarchy.isMonarchyThreadOpen) {
            monarchy.openManually()
        }
        liveEntityUpdaters.forEach { it.start() }
        bingRuleWatcher.start()
    }

    override fun requireBackgroundSync() {
        SyncWorker.requireBackgroundSync()
    }

    override fun startAutomaticBackgroundSync(repeatDelay: Long) {
        SyncWorker.automaticallyBackgroundSync(0, repeatDelay)
    }

    override fun stopAnyBackgroundSync() {
        SyncWorker.stopAnyBackgroundSync()
    }

    @MainThread
    override fun startSync() {
        assert(isOpen)
        if (!syncThread.isAlive) {
            syncThread.start()
        } else {
            syncThread.restart()
            Timber.w("Attempt to start an already started thread")
        }
    }

    @MainThread
    override fun stopSync() {
        assert(isOpen)
        syncThread.kill()
    }

    @MainThread
    override fun close() {
        assertMainThread()
        assert(isOpen)
        liveEntityUpdaters.forEach { it.dispose() }
        cryptoService.close()
        bingRuleWatcher.dispose()
        if (monarchy.isMonarchyThreadOpen) {
            monarchy.closeManually()
        }
        scope.close()
        isOpen = false
    }

    override fun syncState(): LiveData<SyncState> {
        return syncThread.liveState()
    }

    @MainThread
    override fun signOut(callback: MatrixCallback<Unit>) {
        Timber.w("SIGN_OUT: start")

        assert(isOpen)
        //Timber.w("SIGN_OUT: kill sync thread")
        //syncThread.kill()

        Timber.w("SIGN_OUT: call webservice")
        return signOutService.signOut(object : MatrixCallback<Unit> {
            override fun onSuccess(data: Unit) {
                Timber.w("SIGN_OUT: call webservice -> SUCCESS: clear cache")

                // Clear the cache
                cacheService.clearCache(object : MatrixCallback<Unit> {
                    override fun onSuccess(data: Unit) {
                        Timber.w("SIGN_OUT: clear cache -> SUCCESS: clear crypto cache")
                        cryptoService.clearCryptoCache(MatrixCallbackDelegate(callback))
                    }

                    override fun onFailure(failure: Throwable) {
                        // ignore error
                        Timber.e("SIGN_OUT: clear cache -> ERROR: ignoring")
                        onSuccess(Unit)
                    }
                })
            }

            override fun onFailure(failure: Throwable) {
                // Ignore failure
                Timber.e("SIGN_OUT: call webservice -> ERROR: ignoring")
                onSuccess(Unit)
            }
        })
    }

    override fun contentUrlResolver(): ContentUrlResolver {
        return contentUrlResolver
    }

    override fun contentUploadProgressTracker(): ContentUploadStateTracker {
        return contentUploadProgressTracker
    }

    override fun addListener(listener: Session.Listener) {
        sessionListeners.addListener(listener)
    }

    override fun removeListener(listener: Session.Listener) {
        sessionListeners.removeListener(listener)
    }

    // ROOM SERVICE

    override fun createRoom(createRoomParams: CreateRoomParams, callback: MatrixCallback<String>) {
        assert(isOpen)
        return roomService.createRoom(createRoomParams, callback)
    }

    override fun getRoom(roomId: String): Room? {
        assert(isOpen)
        return roomService.getRoom(roomId)
    }


    override fun liveRoomSummaries(): LiveData<List<RoomSummary>> {
        assert(isOpen)
        return roomService.liveRoomSummaries()
    }

    // ROOM DIRECTORY SERVICE

    override fun getPublicRooms(server: String?, publicRoomsParams: PublicRoomsParams, callback: MatrixCallback<PublicRoomsResponse>): Cancelable {
        assert(isOpen)
        return roomDirectoryService.getPublicRooms(server, publicRoomsParams, callback)
    }

    override fun joinRoom(roomId: String, callback: MatrixCallback<Unit>) {
        assert(isOpen)
        return roomDirectoryService.joinRoom(roomId, callback)
    }

    override fun getThirdPartyProtocol(callback: MatrixCallback<Map<String, ThirdPartyProtocol>>) {
        assert(isOpen)
        return roomDirectoryService.getThirdPartyProtocol(callback)
    }

    // GROUP SERVICE

    override fun getGroup(groupId: String): Group? {
        assert(isOpen)
        return groupService.getGroup(groupId)
    }

    override fun liveGroupSummaries(): LiveData<List<GroupSummary>> {
        assert(isOpen)
        return groupService.liveGroupSummaries()
    }

    override fun setFilter(filterPreset: FilterService.FilterPreset) {
        assert(isOpen)
        return filterService.setFilter(filterPreset)
    }

    override fun clearCache(callback: MatrixCallback<Unit>) {
        assert(isOpen)
        // syncThread.pause()
        cacheService.clearCache(object : MatrixCallbackDelegate<Unit>(callback) {
            override fun onSuccess(data: Unit) {
                // Restart the sync
                //  syncThread.restart()

                super.onSuccess(data)
            }
        })
    }

    // USER SERVICE

    override fun getUser(userId: String): User? {
        assert(isOpen)
        return userService.getUser(userId)
    }

    override fun observeUser(userId: String): LiveData<User?> {
        assert(isOpen)
        return userService.observeUser(userId)
    }

    // CRYPTO SERVICE

    override fun setDeviceName(deviceId: String, deviceName: String, callback: MatrixCallback<Unit>) {
        cryptoService.setDeviceName(deviceId, deviceName, callback)
    }

    override fun deleteDevice(deviceId: String, callback: MatrixCallback<Unit>) {
        cryptoService.deleteDevice(deviceId, callback)
    }

    override fun deleteDeviceWithUserPassword(deviceId: String, authSession: String?, password: String, callback: MatrixCallback<Unit>) {
        cryptoService.deleteDeviceWithUserPassword(deviceId, authSession, password, callback)
    }

    override fun getCryptoVersion(context: Context, longFormat: Boolean): String {
        return cryptoService.getCryptoVersion(context, longFormat)
    }

    override fun isCryptoEnabled(): Boolean {
        return cryptoService.isCryptoEnabled()
    }

    override fun getSasVerificationService(): SasVerificationService {
        return cryptoService.getSasVerificationService()
    }

    override fun getKeysBackupService(): KeysBackupService {
        return cryptoService.getKeysBackupService()
    }

    override fun isRoomBlacklistUnverifiedDevices(roomId: String?): Boolean {
        return cryptoService.isRoomBlacklistUnverifiedDevices(roomId)
    }

    override fun setWarnOnUnknownDevices(warn: Boolean) {
        cryptoService.setWarnOnUnknownDevices(warn)
    }

    override fun setDeviceVerification(verificationStatus: Int, deviceId: String, userId: String) {
        cryptoService.setDeviceVerification(verificationStatus, deviceId, userId)
    }

    override fun getUserDevices(userId: String): MutableList<MXDeviceInfo> {
        return cryptoService.getUserDevices(userId)
    }

    override fun setDevicesKnown(devices: List<MXDeviceInfo>, callback: MatrixCallback<Unit>?) {
        cryptoService.setDevicesKnown(devices, callback)
    }

    override fun deviceWithIdentityKey(senderKey: String, algorithm: String): MXDeviceInfo? {
        return cryptoService.deviceWithIdentityKey(senderKey, algorithm)
    }

    override fun getMyDevice(): MXDeviceInfo {
        return cryptoService.getMyDevice()
    }

    override fun getDevicesList(callback: MatrixCallback<DevicesListResponse>) {
        cryptoService.getDevicesList(callback)
    }

    override fun inboundGroupSessionsCount(onlyBackedUp: Boolean): Int {
        return cryptoService.inboundGroupSessionsCount(onlyBackedUp)
    }

    override fun getGlobalBlacklistUnverifiedDevices(): Boolean {
        return cryptoService.getGlobalBlacklistUnverifiedDevices()
    }

    override fun setGlobalBlacklistUnverifiedDevices(block: Boolean) {
        cryptoService.setGlobalBlacklistUnverifiedDevices(block)
    }

    override fun setRoomUnBlacklistUnverifiedDevices(roomId: String) {
        cryptoService.setRoomUnBlacklistUnverifiedDevices(roomId)
    }

    override fun getDeviceTrackingStatus(userId: String): Int {
        return cryptoService.getDeviceTrackingStatus(userId)
    }

    override fun importRoomKeys(roomKeysAsArray: ByteArray, password: String, progressListener: ProgressListener?, callback: MatrixCallback<ImportRoomKeysResult>) {
        cryptoService.importRoomKeys(roomKeysAsArray, password, progressListener, callback)
    }

    override fun exportRoomKeys(password: String, callback: MatrixCallback<ByteArray>) {
        cryptoService.exportRoomKeys(password, callback)
    }

    override fun setRoomBlacklistUnverifiedDevices(roomId: String) {
        cryptoService.setRoomBlacklistUnverifiedDevices(roomId)
    }

    override fun isRoomEncrypted(roomId: String): Boolean {
        return cryptoService.isRoomEncrypted(roomId)
    }

    override fun encryptEventContent(eventContent: Content, eventType: String, roomId: String, callback: MatrixCallback<MXEncryptEventContentResult>) {
        cryptoService.encryptEventContent(eventContent, eventType, roomId, callback)
    }

    override fun getDeviceInfo(userId: String, deviceId: String?): MXDeviceInfo? {
        return cryptoService.getDeviceInfo(userId, deviceId)
    }

    override fun reRequestRoomKeyForEvent(event: Event) {
        cryptoService.reRequestRoomKeyForEvent(event)
    }

    override fun cancelRoomKeyRequest(requestBody: RoomKeyRequestBody) {
        cryptoService.cancelRoomKeyRequest(requestBody)
    }

    override fun addRoomKeysRequestListener(listener: RoomKeysRequestListener) {
        cryptoService.addRoomKeysRequestListener(listener)
    }

    override fun decryptEvent(event: Event, timeline: String): MXEventDecryptionResult? {
        return cryptoService.decryptEvent(event, timeline)
    }

    override fun decryptEventAsync(event: Event, timeline: String, callback: MatrixCallback<MXEventDecryptionResult?>) {
        return cryptoService.decryptEventAsync(event, timeline, callback)
    }

    override fun getEncryptionAlgorithm(roomId: String): String? {
        return cryptoService.getEncryptionAlgorithm(roomId)
    }

    override fun shouldEncryptForInvitedMembers(roomId: String): Boolean {
        return cryptoService.shouldEncryptForInvitedMembers(roomId)
    }

    override fun downloadKeys(userIds: List<String>, forceDownload: Boolean, callback: MatrixCallback<MXUsersDevicesMap<MXDeviceInfo>>) {
        cryptoService.downloadKeys(userIds, forceDownload, callback)
    }

    override fun clearCryptoCache(callback: MatrixCallback<Unit>) {
        cryptoService.clearCryptoCache(callback)
    }

    // PUSH RULE SERVICE

    override fun addPushRuleListener(listener: PushRuleService.PushRuleListener) {
        pushRuleService.addPushRuleListener(listener)
    }

    override fun removePushRuleListener(listener: PushRuleService.PushRuleListener) {
        pushRuleService.removePushRuleListener(listener)
    }

    // Private methods *****************************************************************************

    private fun assertMainThread() {
        if (Looper.getMainLooper().thread !== Thread.currentThread()) {
            throw IllegalStateException("This method can only be called on the main thread!")
        }
    }

    override fun refreshPushers() {
        pushersService.refreshPushers()
    }

    override fun addHttpPusher(
            pushkey: String,
            appId: String,
            profileTag: String,
            lang: String,
            appDisplayName: String,
            deviceDisplayName: String,
            url: String,
            append: Boolean,
            withEventIdOnly: Boolean): UUID {
        return pushersService
                .addHttpPusher(
                        pushkey, appId, profileTag, lang, appDisplayName, deviceDisplayName, url, append, withEventIdOnly
                )
    }

    override fun removeHttpPusher(pushkey: String, appId: String, callback: MatrixCallback<Unit>) {
        pushersService.removeHttpPusher(pushkey, appId, callback)
    }

    override fun livePushers(): LiveData<List<Pusher>> {
        return pushersService.livePushers()
    }

    override fun pushers(): List<Pusher> {
        return pushersService.pushers()
    }

    override fun getPushRules(scope: String): List<PushRule> {
        return pushRuleService.getPushRules(scope)
    }

    override fun updatePushRuleEnableStatus(kind: String, pushRule: PushRule, enabled: Boolean, callback: MatrixCallback<Unit>) {
        pushRuleService.updatePushRuleEnableStatus(kind, pushRule, enabled, callback)
    }

    override fun fetchPushRules(scope: String) {
        pushRuleService.fetchPushRules(scope)
    }


}