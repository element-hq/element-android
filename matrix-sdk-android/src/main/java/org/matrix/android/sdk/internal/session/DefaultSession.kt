/*
 * Copyright 2019 New Vector Ltd
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session

import androidx.annotation.MainThread
import dagger.Lazy
import io.realm.RealmConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.auth.data.SessionParams
import org.matrix.android.sdk.api.failure.GlobalError
import org.matrix.android.sdk.api.pushrules.PushRuleService
import org.matrix.android.sdk.api.session.InitialSyncProgressService
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.account.AccountService
import org.matrix.android.sdk.api.session.accountdata.AccountDataService
import org.matrix.android.sdk.api.session.cache.CacheService
import org.matrix.android.sdk.api.session.call.CallSignalingService
import org.matrix.android.sdk.api.session.content.ContentUploadStateTracker
import org.matrix.android.sdk.api.session.content.ContentUrlResolver
import org.matrix.android.sdk.api.session.crypto.CryptoService
import org.matrix.android.sdk.api.session.file.ContentDownloadStateTracker
import org.matrix.android.sdk.api.session.file.FileService
import org.matrix.android.sdk.api.session.group.GroupService
import org.matrix.android.sdk.api.session.homeserver.HomeServerCapabilitiesService
import org.matrix.android.sdk.api.session.integrationmanager.IntegrationManagerService
import org.matrix.android.sdk.api.session.permalinks.PermalinkService
import org.matrix.android.sdk.api.session.profile.ProfileService
import org.matrix.android.sdk.api.session.pushers.PushersService
import org.matrix.android.sdk.api.session.room.RoomDirectoryService
import org.matrix.android.sdk.api.session.room.RoomService
import org.matrix.android.sdk.api.session.securestorage.SecureStorageService
import org.matrix.android.sdk.api.session.securestorage.SharedSecretStorageService
import org.matrix.android.sdk.api.session.signout.SignOutService
import org.matrix.android.sdk.api.session.sync.FilterService
import org.matrix.android.sdk.api.session.terms.TermsService
import org.matrix.android.sdk.api.session.typing.TypingUsersTracker
import org.matrix.android.sdk.api.session.user.UserService
import org.matrix.android.sdk.api.session.widgets.WidgetService
import org.matrix.android.sdk.internal.auth.SessionParamsStore
import org.matrix.android.sdk.internal.crypto.DefaultCryptoService
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.di.SessionId
import org.matrix.android.sdk.internal.di.UnauthenticatedWithCertificate
import org.matrix.android.sdk.internal.di.WorkManagerProvider
import org.matrix.android.sdk.internal.session.identity.DefaultIdentityService
import org.matrix.android.sdk.internal.session.room.timeline.TimelineEventDecryptor
import org.matrix.android.sdk.internal.session.sync.SyncTokenStore
import org.matrix.android.sdk.internal.session.sync.job.SyncThread
import org.matrix.android.sdk.internal.session.sync.job.SyncWorker
import org.matrix.android.sdk.internal.task.TaskExecutor
import org.matrix.android.sdk.internal.util.MatrixCoroutineDispatchers
import org.matrix.android.sdk.internal.util.createUIHandler
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
        @SessionDatabase private val realmConfiguration: RealmConfiguration,
        private val lifecycleObservers: Set<@JvmSuppressWildcards SessionLifecycleObserver>,
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
        private val termsService: Lazy<TermsService>,
        private val cryptoService: Lazy<DefaultCryptoService>,
        private val defaultFileService: Lazy<FileService>,
        private val permalinkService: Lazy<PermalinkService>,
        private val secureStorageService: Lazy<SecureStorageService>,
        private val profileService: Lazy<ProfileService>,
        private val widgetService: Lazy<WidgetService>,
        private val syncThreadProvider: Provider<SyncThread>,
        private val contentUrlResolver: ContentUrlResolver,
        private val syncTokenStore: SyncTokenStore,
        private val sessionParamsStore: SessionParamsStore,
        private val contentUploadProgressTracker: ContentUploadStateTracker,
        private val typingUsersTracker: TypingUsersTracker,
        private val contentDownloadStateTracker: ContentDownloadStateTracker,
        private val initialSyncProgressService: Lazy<InitialSyncProgressService>,
        private val homeServerCapabilitiesService: Lazy<HomeServerCapabilitiesService>,
        private val accountDataService: Lazy<AccountDataService>,
        private val _sharedSecretStorageService: Lazy<SharedSecretStorageService>,
        private val accountService: Lazy<AccountService>,
        private val timelineEventDecryptor: TimelineEventDecryptor,
        private val coroutineDispatchers: MatrixCoroutineDispatchers,
        private val defaultIdentityService: DefaultIdentityService,
        private val integrationManagerService: IntegrationManagerService,
        private val taskExecutor: TaskExecutor,
        private val callSignalingService: Lazy<CallSignalingService>,
        @UnauthenticatedWithCertificate
        private val unauthenticatedWithCertificateOkHttpClient: Lazy<OkHttpClient>
) : Session,
        RoomService by roomService.get(),
        RoomDirectoryService by roomDirectoryService.get(),
        GroupService by groupService.get(),
        UserService by userService.get(),
        SignOutService by signOutService.get(),
        FilterService by filterService.get(),
        PushRuleService by pushRuleService.get(),
        PushersService by pushersService.get(),
        TermsService by termsService.get(),
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

    private val uiHandler = createUIHandler()

    override val isOpenable: Boolean
        get() = sessionParamsStore.get(sessionId)?.isTokenValid ?: false

    @MainThread
    override fun open() {
        assert(!isOpen)
        isOpen = true
        cryptoService.get().ensureDevice()
        uiHandler.post {
            lifecycleObservers.forEach { it.onStart() }
        }
        eventBus.register(this)
        timelineEventDecryptor.start()
    }

    override fun requireBackgroundSync() {
        SyncWorker.requireBackgroundSync(workManagerProvider, sessionId)
    }

    override fun startAutomaticBackgroundSync(timeOutInSeconds: Long, repeatDelayInSeconds: Long) {
        SyncWorker.automaticallyBackgroundSync(workManagerProvider, sessionId, timeOutInSeconds, repeatDelayInSeconds)
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
        uiHandler.post {
            lifecycleObservers.forEach { it.onStop() }
        }
        cryptoService.get().close()
        isOpen = false
        eventBus.unregister(this)
    }

    override fun getSyncStateLive() = getSyncThread().liveState()

    override fun getSyncState() = getSyncThread().currentState()

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
        uiHandler.post {
            lifecycleObservers.forEach { it.onClearCache() }
        }
        cacheService.get().clearCache(callback)
        workManagerProvider.cancelAllWorks()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onGlobalError(globalError: GlobalError) {
        if (globalError is GlobalError.InvalidToken
                && globalError.softLogout) {
            // Mark the token has invalid
            taskExecutor.executorScope.launch(Dispatchers.IO) {
                sessionParamsStore.setTokenInvalid(sessionId)
            }
        }

        sessionListeners.dispatchGlobalError(globalError)
    }

    override fun contentUrlResolver() = contentUrlResolver

    override fun contentUploadProgressTracker() = contentUploadProgressTracker

    override fun typingUsersTracker() = typingUsersTracker

    override fun contentDownloadProgressTracker(): ContentDownloadStateTracker = contentDownloadStateTracker

    override fun cryptoService(): CryptoService = cryptoService.get()

    override fun identityService() = defaultIdentityService

    override fun fileService(): FileService = defaultFileService.get()

    override fun permalinkService(): PermalinkService = permalinkService.get()

    override fun widgetService(): WidgetService = widgetService.get()

    override fun integrationManagerService() = integrationManagerService

    override fun callSignalingService(): CallSignalingService = callSignalingService.get()

    override fun getOkHttpClient(): OkHttpClient {
        return unauthenticatedWithCertificateOkHttpClient.get()
    }

    override fun addListener(listener: Session.Listener) {
        sessionListeners.addListener(listener)
    }

    override fun removeListener(listener: Session.Listener) {
        sessionListeners.removeListener(listener)
    }

    // For easy debugging
    override fun toString(): String {
        return "$myUserId - ${sessionParams.deviceId}"
    }
}
