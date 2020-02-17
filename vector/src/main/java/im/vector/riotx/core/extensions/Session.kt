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

package im.vector.riotx.core.extensions

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.crypto.keysbackup.KeysBackupState
import im.vector.matrix.android.api.session.sync.FilterService
import im.vector.riotx.core.services.VectorSyncService
import im.vector.riotx.features.notifications.PushRuleTriggerListener
import im.vector.riotx.features.session.SessionListener
import timber.log.Timber

fun Session.configureAndStart(context: Context,
                              pushRuleTriggerListener: PushRuleTriggerListener,
                              sessionListener: SessionListener) {
    open()
    addListener(sessionListener)
    setFilter(FilterService.FilterPreset.RiotFilter)
    Timber.i("Configure and start session for ${this.myUserId}")
    startSyncing(context)
    refreshPushers()
    pushRuleTriggerListener.startWithSession(this)

    // TODO P1 From HomeActivity
    // @Inject lateinit var incomingVerificationRequestHandler: IncomingVerificationRequestHandler
    // @Inject lateinit var keyRequestHandler: KeyRequestHandler
}

fun Session.startSyncing(context: Context) {
    val applicationContext = context.applicationContext
    if (!hasAlreadySynced()) {
        VectorSyncService.newIntent(applicationContext, sessionId).also {
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
        startSync(isAtLeastStarted)
    }
}

/**
 * Tell is the session has unsaved e2e keys in the backup
 */
fun Session.hasUnsavedKeys(): Boolean {
    return cryptoService().inboundGroupSessionsCount(false) > 0
            && cryptoService().keysBackupService().state != KeysBackupState.ReadyToBackUp
}
