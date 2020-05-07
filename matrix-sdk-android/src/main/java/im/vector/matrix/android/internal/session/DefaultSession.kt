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

import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import dagger.Lazy
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.auth.data.SessionParams
import im.vector.matrix.android.api.failure.GlobalError
import im.vector.matrix.android.api.pushrules.PushRuleService
import im.vector.matrix.android.api.session.InitialSyncProgressService
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.account.AccountService
import im.vector.matrix.android.api.session.accountdata.AccountDataService
import im.vector.matrix.android.api.session.cache.CacheService
import im.vector.matrix.android.api.session.content.ContentUploadStateTracker
import im.vector.matrix.android.api.session.content.ContentUrlResolver
import im.vector.matrix.android.api.session.crypto.CryptoService
import im.vector.matrix.android.api.session.file.FileService
import im.vector.matrix.android.api.session.group.GroupService
import im.vector.matrix.android.api.session.homeserver.HomeServerCapabilitiesService
import im.vector.matrix.android.api.session.profile.ProfileService
import im.vector.matrix.android.api.session.pushers.PushersService
import im.vector.matrix.android.api.session.room.RoomDirectoryService
import im.vector.matrix.android.api.session.room.RoomService
import im.vector.matrix.android.api.session.securestorage.SecureStorageService
import im.vector.matrix.android.api.session.securestorage.SharedSecretStorageService
import im.vector.matrix.android.api.session.signout.SignOutService
import im.vector.matrix.android.api.session.sync.FilterService
import im.vector.matrix.android.api.session.sync.SyncState
import im.vector.matrix.android.api.session.user.UserService
import im.vector.matrix.android.internal.auth.SessionParamsStore
import im.vector.matrix.android.internal.crypto.DefaultCryptoService
import im.vector.matrix.android.internal.crypto.crosssigning.ShieldTrustUpdater
import im.vector.matrix.android.internal.database.LiveEntityObserver
import im.vector.matrix.android.internal.di.SessionId
import im.vector.matrix.android.internal.di.WorkManagerProvider
import im.vector.matrix.android.internal.session.identity.DefaultIdentityService
import im.vector.matrix.android.internal.session.room.timeline.TimelineEventDecryptor
import im.vector.matrix.android.internal.session.sync.SyncTokenStore
import im.vector.matrix.android.internal.session.sync.job.SyncThread
import im.vector.matrix.android.internal.session.sync.job.SyncWorker
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Provider

@SessionScope
internal class DefaultSession @Inject constructor(
        override val sessionParams: SessionParams,
        private val workManagerProvider: WorkManagerProvider,
        private val eventBus: EventBus,
        @SessionId
        override val sessionId: String,
        private val liveEntityObservers: Set<@JvmSuppressWildcards LiveEntityObserver>,
        private val sessionListeners: SessionListeners,
        private val roomService: Lazy<RoomService>,
        private val roomDirectoryService: Lazy<RoomDirectoryService>,
        private val groupService: Lazy<GroupService>,
        private val userService: Lazy<UserService>,
        private val filterService: Lazy<FilterService>,
        private val cacheService: Lazy<CacheService>,
        private val signOutService: Lazy<SignOutService>,
        private val pushRuleService: Lazy<PushRuleService>,
        private val pushersService: Lazy<PushersService>,
        private val cryptoService: Lazy<DefaultCryptoService>,
        private val fileService: Lazy<FileService>,
        private val secureStorageService: Lazy<SecureStorageService>,
        private val profileService: Lazy<ProfileService>,
        private val syncThreadProvider: Provider<SyncThread>,
        private val contentUrlResolver: ContentUrlResolver,
        private val syncTokenStore: SyncTokenStore,
        private val sessionParamsStore: SessionParamsStore,
        private val contentUploadProgressTracker: ContentUploadStateTracker,
        private val initialSyncProgressService: Lazy<InitialSyncProgressService>,
        private val homeServerCapabilitiesService: Lazy<HomeServerCapabilitiesService>,
        private val accountDataService: Lazy<AccountDataService>,
        private val _sharedSecretStorageService: Lazy<SharedSecretStorageService>,
        private val accountService: Lazy<AccountService>,
        private val timelineEventDecryptor: TimelineEventDecryptor,
        private val shieldTrustUpdater: ShieldTrustUpdater,
        private val coroutineDispatchers: MatrixCoroutineDispatchers,
        private val defaultIdentityService: DefaultIdentityService)
    : Session,
        RoomService by roomService.get(),
        RoomDirectoryService by roomDirectoryService.get(),
        GroupService by groupService.get(),
        UserService by userService.get(),
        SignOutService by signOutService.get(),
        FilterService by filterService.get(),
        PushRuleService by pushRuleService.get(),
        PushersService by pushersService.get(),
        FileService by fileService.get(),
        InitialSyncProgressService by initialSyncProgressService.get(),
        SecureStorageService by secureStorageService.get(),
        HomeServerCapabilitiesService by homeServerCapabilitiesService.get(),
        ProfileService by profileService.get(),
        AccountDataService by accountDataService.get(),
        AccountService by accountService.get() {

    override val sharedSecretStorageService: SharedSecretStorageService
        get() = _sharedSecretStorageService.get()

    private var isOpen = false

    private var syncThread: SyncThread? = null

    override val isOpenable: Boolean
        get() = sessionParamsStore.get(sessionId)?.isTokenValid ?: false

    @MainThread
    override fun open() {
        assert(!isOpen)
        isOpen = true
        liveEntityObservers.forEach { it.start() }
        eventBus.register(this)
        timelineEventDecryptor.start()
        shieldTrustUpdater.start()
        defaultIdentityService.start()
    }

    override fun requireBackgroundSync() {
        SyncWorker.requireBackgroundSync(workManagerProvider, sessionId)
    }

    override fun startAutomaticBackgroundSync(repeatDelay: Long) {
        SyncWorker.automaticallyBackgroundSync(workManagerProvider, sessionId, 0, repeatDelay)
    }

    override fun stopAnyBackgroundSync() {
        SyncWorker.stopAnyBackgroundSync(workManagerProvider)
    }

    override fun startSync(fromForeground: Boolean) {
        Timber.i("Starting sync thread")
        assert(isOpen)
        val localSyncThread = getSyncThread()
        localSyncThread.setInitialForeground(fromForeground)
        if (!localSyncThread.isAlive) {
            localSyncThread.start()
        } else {
            localSyncThread.restart()
            Timber.w("Attempt to start an already started thread")
        }
    }

    override fun stopSync() {
        assert(isOpen)
        syncThread?.kill()
        syncThread = null
    }

    override fun close() {
        assert(isOpen)
        stopSync()
        timelineEventDecryptor.destroy()
        liveEntityObservers.forEach { it.dispose() }
        cryptoService.get().close()
        isOpen = false
        eventBus.unregister(this)
        shieldTrustUpdater.stop()
        GlobalScope.launch(coroutineDispatchers.main) {
            // This has to be done on main thread
            defaultIdentityService.stop()
        }
    }

    override fun getSyncStateLive(): LiveData<SyncState> {
        return getSyncThread().liveState()
    }

    override fun hasAlreadySynced(): Boolean {
        return syncTokenStore.getLastToken() != null
    }

    private fun getSyncThread(): SyncThread {
        return syncThread ?: syncThreadProvider.get().also {
            syncThread = it
        }
    }

    override fun clearCache(callback: MatrixCallback<Unit>) {
        stopSync()
        stopAnyBackgroundSync()
        liveEntityObservers.forEach { it.cancelProcess() }
        cacheService.get().clearCache(callback)
        workManagerProvider.cancelAllWorks()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onGlobalError(globalError: GlobalError) {
        if (globalError is GlobalError.InvalidToken
                && globalError.softLogout) {
            // Mark the token has invalid
            GlobalScope.launch(Dispatchers.IO) {
                sessionParamsStore.setTokenInvalid(sessionId)
            }
        }

        sessionListeners.dispatchGlobalError(globalError)
    }

    override fun contentUrlResolver() = contentUrlResolver

    override fun contentUploadProgressTracker() = contentUploadProgressTracker

    override fun cryptoService(): CryptoService = cryptoService.get()

    override fun identityService() = defaultIdentityService

    override fun addListener(listener: Session.Listener) {
        sessionListeners.addListener(listener)
    }

    override fun removeListener(listener: Session.Listener) {
        sessionListeners.removeListener(listener)
    }

    // For easy debugging
    override fun toString(): String {
        return "$myUserId - ${sessionParams.credentials.deviceId}"
    }
}
