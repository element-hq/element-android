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

package org.matrix.android.sdk.api.session

import androidx.annotation.MainThread
import io.realm.RealmConfiguration
import okhttp3.OkHttpClient
import org.matrix.android.sdk.api.MatrixCoroutineDispatchers
import org.matrix.android.sdk.api.auth.data.SessionParams
import org.matrix.android.sdk.api.failure.GlobalError
import org.matrix.android.sdk.api.federation.FederationService
import org.matrix.android.sdk.api.session.account.AccountService
import org.matrix.android.sdk.api.session.accountdata.SessionAccountDataService
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
import org.matrix.android.sdk.api.session.statistics.StatisticsListener
import org.matrix.android.sdk.api.session.sync.FilterService
import org.matrix.android.sdk.api.session.sync.SyncService
import org.matrix.android.sdk.api.session.terms.TermsService
import org.matrix.android.sdk.api.session.thirdparty.ThirdPartyService
import org.matrix.android.sdk.api.session.typing.TypingUsersTracker
import org.matrix.android.sdk.api.session.user.UserService
import org.matrix.android.sdk.api.session.widgets.WidgetService

/**
 * This interface defines interactions with a session.
 * An instance of a session will be provided by the SDK.
 */
interface Session {

    val coroutineDispatchers: MatrixCoroutineDispatchers

    /**
     * The params associated to the session.
     */
    val sessionParams: SessionParams

    /**
     * The session is valid, i.e. it has a valid token so far.
     */
    val isOpenable: Boolean

    /**
     * Useful shortcut to get access to the userId.
     */
    val myUserId: String
        get() = sessionParams.userId

    /**
     * The sessionId.
     */
    val sessionId: String

    /**
     * This method allow to open a session. It does start some service on the background.
     */
    @MainThread
    fun open()

    /**
     * Clear cache of the session.
     */
    suspend fun clearCache()

    /**
     * This method allow to close a session. It does stop some services.
     */
    fun close()

    /**
     * Returns the ContentUrlResolver associated to the session.
     */
    fun contentUrlResolver(): ContentUrlResolver

    /**
     * Returns the ContentUploadProgressTracker associated with the session.
     */
    fun contentUploadProgressTracker(): ContentUploadStateTracker

    /**
     * Returns the TypingUsersTracker associated with the session.
     */
    fun typingUsersTracker(): TypingUsersTracker

    /**
     * Returns the ContentDownloadStateTracker associated with the session.
     */
    fun contentDownloadProgressTracker(): ContentDownloadStateTracker

    /**
     * Returns the cryptoService associated with the session.
     */
    fun cryptoService(): CryptoService

    /**
     * Returns the ContentScannerService associated with the session.
     */
    fun contentScannerService(): ContentScannerService

    /**
     * Returns the identity service associated with the session.
     */
    fun identityService(): IdentityService

    /**
     * Returns the HomeServerCapabilities service associated with the session.
     */
    fun homeServerCapabilitiesService(): HomeServerCapabilitiesService

    /**
     * Returns the RoomService associated with the session.
     */
    fun roomService(): RoomService

    /**
     * Returns the RoomDirectoryService associated with the session.
     */
    fun roomDirectoryService(): RoomDirectoryService

    /**
     * Returns the UserService associated with the session.
     */
    fun userService(): UserService

    /**
     * Returns the SignOutService associated with the session.
     */
    fun signOutService(): SignOutService

    /**
     * Returns the FilterService associated with the session.
     */
    fun filterService(): FilterService

    /**
     * Returns the PushRuleService associated with the session.
     */
    fun pushRuleService(): PushRuleService

    /**
     * Returns the PushersService associated with the session.
     */
    fun pushersService(): PushersService

    /**
     * Returns the EventService associated with the session.
     */
    fun eventService(): EventService

    /**
     * Returns the TermsService associated with the session.
     */
    fun termsService(): TermsService

    /**
     * Returns the SyncService associated with the session.
     */
    fun syncService(): SyncService

    /**
     * Returns the ProfileService associated with the session.
     */
    fun profileService(): ProfileService

    /**
     * Returns the PresenceService associated with the session.
     */
    fun presenceService(): PresenceService

    /**
     * Returns the AccountService associated with the session.
     */
    fun accountService(): AccountService

    /**
     * Returns the ToDeviceService associated with the session.
     */
    fun toDeviceService(): ToDeviceService

    /**
     * Returns the EventStreamService associated with the session.
     */
    fun eventStreamService(): EventStreamService

    /**
     * Returns the widget service associated with the session.
     */
    fun widgetService(): WidgetService

    /**
     * Returns the media service associated with the session.
     */
    fun mediaService(): MediaService

    /**
     * Returns the integration manager service associated with the session.
     */
    fun integrationManagerService(): IntegrationManagerService

    /**
     * Returns the call signaling service associated with the session.
     */
    fun callSignalingService(): CallSignalingService

    /**
     * Returns the file download service associated with the session.
     */
    fun fileService(): FileService

    /**
     * Returns the permalink service associated with the session.
     */
    fun permalinkService(): PermalinkService

    /**
     * Returns the search service associated with the session.
     */
    fun searchService(): SearchService

    /**
     * Returns the federation service associated with the session.
     */
    fun federationService(): FederationService

    /**
     * Returns the third party service associated with the session.
     */
    fun thirdPartyService(): ThirdPartyService

    /**
     * Returns the space service associated with the session.
     */
    fun spaceService(): SpaceService

    /**
     * Returns the open id service associated with the session.
     */
    fun openIdService(): OpenIdService

    /**
     * Returns the account data service associated with the session.
     */
    fun accountDataService(): SessionAccountDataService

    /**
     * Returns the SharedSecretStorageService associated with the session.
     */
    fun sharedSecretStorageService(): SharedSecretStorageService

    /**
     * Add a listener to the session.
     * @param listener the listener to add.
     */
    fun addListener(listener: Listener)

    /**
     * Remove a listener from the session.
     * @param listener the listener to remove.
     */
    fun removeListener(listener: Listener)

    /**
     * Will return a OkHttpClient which will manage pinned certificates and Proxy if configured.
     * It will not add any access-token to the request.
     * So it is exposed to let the app be able to download image with Glide or any other libraries which accept an OkHttp client.
     */
    fun getOkHttpClient(): OkHttpClient

    /**
     * A global session listener to get notified for some events.
     */
    interface Listener : StatisticsListener, SessionLifecycleObserver {
        /**
         * Called when the session received new invites to room so the client can react to it once.
         */
        fun onNewInvitedRoom(session: Session, roomId: String) = Unit

        /**
         * Possible cases:
         * - The access token is not valid anymore,
         * - a M_CONSENT_NOT_GIVEN error has been received from the homeserver;
         * See [GlobalError] for all the possible cases.
         */
        fun onGlobalError(session: Session, globalError: GlobalError) = Unit
    }

    fun getUiaSsoFallbackUrl(authenticationSessionId: String): String

    /**
     * Debug API, will return info about the DB.
     */
    fun getDbUsageInfo(): String

    /**
     * Debug API, return the list of all RealmConfiguration used by this session.
     */
    fun getRealmConfigurations(): List<RealmConfiguration>
}
