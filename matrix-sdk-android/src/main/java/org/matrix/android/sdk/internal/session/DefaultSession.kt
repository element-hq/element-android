/*
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
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.matrix.android.sdk.api.MatrixCoroutineDispatchers
import org.matrix.android.sdk.api.auth.data.SessionParams
import org.matrix.android.sdk.api.failure.GlobalError
import org.matrix.android.sdk.api.federation.FederationService
import org.matrix.android.sdk.api.session.EventStreamService
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.SessionLifecycleObserver
import org.matrix.android.sdk.api.session.ToDeviceService
import org.matrix.android.sdk.api.session.account.AccountService
import org.matrix.android.sdk.api.session.accountdata.SessionAccountDataService
import org.matrix.android.sdk.api.session.cache.CacheService
import org.matrix.android.sdk.api.session.call.CallSignalingService
import org.matrix.android.sdk.api.session.content.ContentUploadStateTracker
import org.matrix.android.sdk.api.session.content.ContentUrlResolver
import org.matrix.android.sdk.api.session.contentscanner.ContentScannerService
import org.matrix.android.sdk.api.session.crypto.CryptoService
import org.matrix.android.sdk.api.session.events.EventService
import org.matrix.android.sdk.api.session.file.ContentDownloadStateTracker
import org.matrix.android.sdk.api.session.file.FileService
import org.matrix.android.sdk.api.session.homeserver.HomeServerCapabilitiesService
import org.matrix.android.sdk.api.session.identity.IdentityService
import org.matrix.android.sdk.api.session.integrationmanager.IntegrationManagerService
import org.matrix.android.sdk.api.session.media.MediaService
import org.matrix.android.sdk.api.session.openid.OpenIdService
import org.matrix.android.sdk.api.session.permalinks.PermalinkService
import org.matrix.android.sdk.api.session.presence.PresenceService
import org.matrix.android.sdk.api.session.profile.ProfileService
import org.matrix.android.sdk.api.session.pushers.PushersService
import org.matrix.android.sdk.api.session.pushrules.PushRuleService
import org.matrix.android.sdk.api.session.room.RoomDirectoryService
import org.matrix.android.sdk.api.session.room.RoomService
import org.matrix.android.sdk.api.session.search.SearchService
import org.matrix.android.sdk.api.session.securestorage.SharedSecretStorageService
import org.matrix.android.sdk.api.session.signout.SignOutService
import org.matrix.android.sdk.api.session.space.SpaceService
import org.matrix.android.sdk.api.session.sync.FilterService
import org.matrix.android.sdk.api.session.sync.SyncService
import org.matrix.android.sdk.api.session.terms.TermsService
import org.matrix.android.sdk.api.session.thirdparty.ThirdPartyService
import org.matrix.android.sdk.api.session.typing.TypingUsersTracker
import org.matrix.android.sdk.api.session.user.UserService
import org.matrix.android.sdk.api.session.widgets.WidgetService
import org.matrix.android.sdk.api.util.appendParamToUrl
import org.matrix.android.sdk.internal.auth.SSO_UIA_FALLBACK_PATH
import org.matrix.android.sdk.internal.auth.SessionParamsStore
import org.matrix.android.sdk.internal.crypto.DefaultCryptoService
import org.matrix.android.sdk.internal.database.tools.RealmDebugTools
import org.matrix.android.sdk.internal.di.ContentScannerDatabase
import org.matrix.android.sdk.internal.di.CryptoDatabase
import org.matrix.android.sdk.internal.di.IdentityDatabase
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.di.SessionId
import org.matrix.android.sdk.internal.di.UnauthenticatedWithCertificate
import org.matrix.android.sdk.internal.di.WorkManagerProvider
import org.matrix.android.sdk.internal.network.GlobalErrorHandler
import org.matrix.android.sdk.internal.util.createUIHandler
import javax.inject.Inject

@SessionScope
internal class DefaultSession @Inject constructor(
        override val sessionParams: SessionParams,
        private val workManagerProvider: WorkManagerProvider,
        private val globalErrorHandler: GlobalErrorHandler,
        @SessionId
        override val sessionId: String,
        override val coroutineDispatchers: MatrixCoroutineDispatchers,
        @SessionDatabase private val realmConfiguration: RealmConfiguration,
        @CryptoDatabase private val realmConfigurationCrypto: RealmConfiguration,
        @IdentityDatabase private val realmConfigurationIdentity: RealmConfiguration,
        @ContentScannerDatabase private val realmConfigurationContentScanner: RealmConfiguration,
        private val lifecycleObservers: Set<@JvmSuppressWildcards SessionLifecycleObserver>,
        private val sessionListeners: SessionListeners,
        private val roomService: Lazy<RoomService>,
        private val roomDirectoryService: Lazy<RoomDirectoryService>,
        private val userService: Lazy<UserService>,
        private val filterService: Lazy<FilterService>,
        private val federationService: Lazy<FederationService>,
        private val cacheService: Lazy<CacheService>,
        private val signOutService: Lazy<SignOutService>,
        private val pushRuleService: Lazy<PushRuleService>,
        private val pushersService: Lazy<PushersService>,
        private val termsService: Lazy<TermsService>,
        private val searchService: Lazy<SearchService>,
        private val cryptoService: Lazy<DefaultCryptoService>,
        private val defaultFileService: Lazy<FileService>,
        private val permalinkService: Lazy<PermalinkService>,
        private val profileService: Lazy<ProfileService>,
        private val syncService: Lazy<SyncService>,
        private val mediaService: Lazy<MediaService>,
        private val widgetService: Lazy<WidgetService>,
        private val contentUrlResolver: ContentUrlResolver,
        private val sessionParamsStore: SessionParamsStore,
        private val contentUploadProgressTracker: ContentUploadStateTracker,
        private val typingUsersTracker: TypingUsersTracker,
        private val contentDownloadStateTracker: ContentDownloadStateTracker,
        private val homeServerCapabilitiesService: Lazy<HomeServerCapabilitiesService>,
        private val accountDataService: Lazy<SessionAccountDataService>,
        private val sharedSecretStorageService: Lazy<SharedSecretStorageService>,
        private val accountService: Lazy<AccountService>,
        private val eventService: Lazy<EventService>,
        private val contentScannerService: Lazy<ContentScannerService>,
        private val identityService: Lazy<IdentityService>,
        private val integrationManagerService: Lazy<IntegrationManagerService>,
        private val thirdPartyService: Lazy<ThirdPartyService>,
        private val callSignalingService: Lazy<CallSignalingService>,
        private val spaceService: Lazy<SpaceService>,
        private val openIdService: Lazy<OpenIdService>,
        private val presenceService: Lazy<PresenceService>,
        private val toDeviceService: Lazy<ToDeviceService>,
        private val eventStreamService: Lazy<EventStreamService>,
        @UnauthenticatedWithCertificate
        private val unauthenticatedWithCertificateOkHttpClient: Lazy<OkHttpClient>,
        private val sessionState: SessionState,
) : Session,
        GlobalErrorHandler.Listener {

    private val uiHandler = createUIHandler()

    override val isOpenable: Boolean
        get() = sessionParamsStore.get(sessionId)?.isTokenValid ?: false

    @MainThread
    override fun open() {
        sessionState.setIsOpen(true)
        globalErrorHandler.listener = this
        cryptoService.get().ensureDevice()
        uiHandler.post {
            lifecycleObservers.forEach {
                it.onSessionStarted(this)
            }
            dispatchTo(sessionListeners) { session, listener ->
                listener.onSessionStarted(session)
            }
        }
    }

    override fun close() {
        assert(sessionState.isOpen)
        syncService.get().stopSync()
        // timelineEventDecryptor.destroy()
        uiHandler.post {
            lifecycleObservers.forEach { it.onSessionStopped(this) }
            dispatchTo(sessionListeners) { session, listener ->
                listener.onSessionStopped(session)
            }
        }
        cryptoService.get().close()
        globalErrorHandler.listener = null
        sessionState.setIsOpen(false)
    }

    override suspend fun clearCache() {
        syncService.get().stopSync()
        syncService.get().stopAnyBackgroundSync()
        uiHandler.post {
            lifecycleObservers.forEach {
                it.onClearCache(this)
            }
            dispatchTo(sessionListeners) { session, listener ->
                listener.onClearCache(session)
            }
        }
        withContext(NonCancellable) {
            cacheService.get().clearCache()
        }
        workManagerProvider.cancelAllWorks()
    }

    override fun onGlobalError(globalError: GlobalError) {
        dispatchTo(sessionListeners) { session, listener ->
            listener.onGlobalError(session, globalError)
        }
    }

    override fun contentUrlResolver() = contentUrlResolver
    override fun contentUploadProgressTracker() = contentUploadProgressTracker
    override fun typingUsersTracker() = typingUsersTracker
    override fun contentDownloadProgressTracker(): ContentDownloadStateTracker = contentDownloadStateTracker

    override fun cryptoService(): CryptoService = cryptoService.get()
    override fun contentScannerService(): ContentScannerService = contentScannerService.get()
    override fun identityService(): IdentityService = identityService.get()
    override fun homeServerCapabilitiesService(): HomeServerCapabilitiesService = homeServerCapabilitiesService.get()
    override fun roomService(): RoomService = roomService.get()
    override fun roomDirectoryService(): RoomDirectoryService = roomDirectoryService.get()
    override fun userService(): UserService = userService.get()
    override fun signOutService(): SignOutService = signOutService.get()
    override fun filterService(): FilterService = filterService.get()
    override fun pushRuleService(): PushRuleService = pushRuleService.get()
    override fun pushersService(): PushersService = pushersService.get()
    override fun eventService(): EventService = eventService.get()
    override fun termsService(): TermsService = termsService.get()
    override fun syncService(): SyncService = syncService.get()
    override fun profileService(): ProfileService = profileService.get()
    override fun presenceService(): PresenceService = presenceService.get()
    override fun accountService(): AccountService = accountService.get()
    override fun toDeviceService(): ToDeviceService = toDeviceService.get()
    override fun eventStreamService(): EventStreamService = eventStreamService.get()
    override fun fileService(): FileService = defaultFileService.get()
    override fun permalinkService(): PermalinkService = permalinkService.get()
    override fun widgetService(): WidgetService = widgetService.get()
    override fun mediaService(): MediaService = mediaService.get()
    override fun integrationManagerService(): IntegrationManagerService = integrationManagerService.get()
    override fun callSignalingService(): CallSignalingService = callSignalingService.get()
    override fun searchService(): SearchService = searchService.get()
    override fun federationService(): FederationService = federationService.get()
    override fun thirdPartyService(): ThirdPartyService = thirdPartyService.get()
    override fun spaceService(): SpaceService = spaceService.get()
    override fun openIdService(): OpenIdService = openIdService.get()
    override fun accountDataService(): SessionAccountDataService = accountDataService.get()
    override fun sharedSecretStorageService(): SharedSecretStorageService = sharedSecretStorageService.get()

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

    override fun getUiaSsoFallbackUrl(authenticationSessionId: String): String {
        val hsBas = sessionParams.homeServerConnectionConfig
                .homeServerUriBase
                .toString()
                .trim { it == '/' }
        return buildString {
            append(hsBas)
            append(SSO_UIA_FALLBACK_PATH)
            appendParamToUrl("session", authenticationSessionId)
        }
    }

    override fun getDbUsageInfo() = buildString {
        append(RealmDebugTools(realmConfiguration).getInfo("Session"))
        append(RealmDebugTools(realmConfigurationCrypto).getInfo("Crypto"))
        append(RealmDebugTools(realmConfigurationIdentity).getInfo("Identity"))
        append(RealmDebugTools(realmConfigurationContentScanner).getInfo("ContentScanner"))
    }

    override fun getRealmConfigurations(): List<RealmConfiguration> {
        return listOf(
                realmConfiguration,
                realmConfigurationCrypto,
                realmConfigurationIdentity,
                realmConfigurationContentScanner,
        )
    }
}
