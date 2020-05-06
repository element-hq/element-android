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

package im.vector.matrix.android.api.session

import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import im.vector.matrix.android.api.auth.data.SessionParams
import im.vector.matrix.android.api.failure.GlobalError
import im.vector.matrix.android.api.pushrules.PushRuleService
import im.vector.matrix.android.api.session.account.AccountService
import im.vector.matrix.android.api.session.accountdata.AccountDataService
import im.vector.matrix.android.api.session.cache.CacheService
import im.vector.matrix.android.api.session.content.ContentUploadStateTracker
import im.vector.matrix.android.api.session.content.ContentUrlResolver
import im.vector.matrix.android.api.session.crypto.CryptoService
import im.vector.matrix.android.api.session.file.FileService
import im.vector.matrix.android.api.session.group.GroupService
import im.vector.matrix.android.api.session.homeserver.HomeServerCapabilitiesService
import im.vector.matrix.android.api.session.identity.IdentityService
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

/**
 * This interface defines interactions with a session.
 * An instance of a session will be provided by the SDK.
 */
interface Session :
        RoomService,
        RoomDirectoryService,
        GroupService,
        UserService,
        CacheService,
        SignOutService,
        FilterService,
        FileService,
        ProfileService,
        PushRuleService,
        PushersService,
        InitialSyncProgressService,
        HomeServerCapabilitiesService,
        SecureStorageService,
        AccountDataService,
        AccountService {

    /**
     * The params associated to the session
     */
    val sessionParams: SessionParams

    /**
     * The session is valid, i.e. it has a valid token so far
     */
    val isOpenable: Boolean

    /**
     * Useful shortcut to get access to the userId
     */
    val myUserId: String
        get() = sessionParams.credentials.userId

    /**
     * The sessionId
     */
    val sessionId: String

    /**
     * This method allow to open a session. It does start some service on the background.
     */
    @MainThread
    fun open()

    /**
     * Requires a one time background sync
     */
    fun requireBackgroundSync()

    /**
     * Launches infinite periodic background syncs
     * This does not work in doze mode :/
     * If battery optimization is on it can work in app standby but that's all :/
     */
    fun startAutomaticBackgroundSync(repeatDelay: Long = 30_000L)

    fun stopAnyBackgroundSync()

    /**
     * This method start the sync thread.
     */
    fun startSync(fromForeground: Boolean)

    /**
     * This method stop the sync thread.
     */
    fun stopSync()

    /**
     * This method allows to listen the sync state.
     * @return a [LiveData] of [SyncState].
     */
    fun getSyncStateLive(): LiveData<SyncState>

    /**
     * This methods return true if an initial sync has been processed
     */
    fun hasAlreadySynced(): Boolean

    /**
     * This method allow to close a session. It does stop some services.
     */
    fun close()

    /**
     * Returns the ContentUrlResolver associated to the session.
     */
    fun contentUrlResolver(): ContentUrlResolver

    /**
     * Returns the ContentUploadProgressTracker associated with the session
     */
    fun contentUploadProgressTracker(): ContentUploadStateTracker

    /**
     * Returns the cryptoService associated with the session
     */
    fun cryptoService(): CryptoService

    /**
     * Returns the identity service associated with the session
     */
    fun identityService(): IdentityService

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
     * A global session listener to get notified for some events.
     */
    interface Listener {
        /**
         * Possible cases:
         * - The access token is not valid anymore,
         * - a M_CONSENT_NOT_GIVEN error has been received from the homeserver
         */
        fun onGlobalError(globalError: GlobalError)
    }

    val sharedSecretStorageService: SharedSecretStorageService
}
