/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.app.core.extensions

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import im.vector.app.core.services.VectorSyncAndroidService
import im.vector.app.features.session.VectorSessionStore
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysBackupState
import org.matrix.android.sdk.api.session.sync.FilterService
import timber.log.Timber

fun Session.configureAndStart(context: Context, startSyncing: Boolean = true) {
    Timber.i("Configure and start session for $myUserId. startSyncing: $startSyncing")
    open()
    filterService().setFilter(FilterService.FilterPreset.ElementFilter)
    if (startSyncing) {
        startSyncing(context)
    }
    pushersService().refreshPushers()
    context.singletonEntryPoint().webRtcCallManager().checkForProtocolsSupportIfNeeded()
}

fun Session.startSyncing(context: Context) {
    val applicationContext = context.applicationContext
    if (!syncService().hasAlreadySynced()) {
        // initial sync is done as a service so it can continue below app lifecycle
        VectorSyncAndroidService.newOneShotIntent(
                context = applicationContext,
                sessionId = sessionId
        )
                .let {
                    try {
                        ContextCompat.startForegroundService(applicationContext, it)
                    } catch (ex: Throwable) {
                        // TODO
                        Timber.e(ex)
                    }
                }
    } else {
        val isAtLeastStarted = ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        Timber.v("--> is at least started? $isAtLeastStarted")
        syncService().startSync(isAtLeastStarted)
    }
}

/**
 * Tell is the session has unsaved e2e keys in the backup.
 */
fun Session.hasUnsavedKeys(): Boolean {
    return cryptoService().inboundGroupSessionsCount(false) > 0 &&
            cryptoService().keysBackupService().getState() != KeysBackupState.ReadyToBackUp
}

fun Session.cannotLogoutSafely(): Boolean {
    // has some encrypted chat
    return hasUnsavedKeys() ||
            // has local cross signing keys
            (cryptoService().crossSigningService().allPrivateKeysKnown() &&
                    // That are not backed up
                    !sharedSecretStorageService().isRecoverySetup())
}

fun Session.vectorStore(context: Context) = VectorSessionStore(context, myUserId)
