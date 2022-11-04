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

package im.vector.app.core.di

import android.content.Context
import im.vector.app.ActiveSessionDataSource
import im.vector.app.core.extensions.startSyncing
import im.vector.app.core.pushers.UnifiedPushHelper
import im.vector.app.core.services.GuardServiceStarter
import im.vector.app.core.session.ConfigureAndStartSessionUseCase
import im.vector.app.features.call.webrtc.WebRtcCallManager
import im.vector.app.features.crypto.keysrequest.KeyRequestHandler
import im.vector.app.features.crypto.verification.IncomingVerificationRequestHandler
import im.vector.app.features.notifications.PushRuleTriggerListener
import im.vector.app.features.session.SessionListener
import kotlinx.coroutines.runBlocking
import org.matrix.android.sdk.api.auth.AuthenticationService
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.api.util.toOption
import timber.log.Timber
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActiveSessionHolder @Inject constructor(
        private val activeSessionDataSource: ActiveSessionDataSource,
        private val keyRequestHandler: KeyRequestHandler,
        private val incomingVerificationRequestHandler: IncomingVerificationRequestHandler,
        private val callManager: WebRtcCallManager,
        private val pushRuleTriggerListener: PushRuleTriggerListener,
        private val sessionListener: SessionListener,
        private val imageManager: ImageManager,
        private val unifiedPushHelper: UnifiedPushHelper,
        private val guardServiceStarter: GuardServiceStarter,
        private val sessionInitializer: SessionInitializer,
        private val applicationContext: Context,
        private val authenticationService: AuthenticationService,
        private val configureAndStartSessionUseCase: ConfigureAndStartSessionUseCase,
) {

    private var activeSessionReference: AtomicReference<Session?> = AtomicReference()

    fun setActiveSession(session: Session) {
        Timber.w("setActiveSession of ${session.myUserId}")
        activeSessionReference.set(session)
        activeSessionDataSource.post(session.toOption())

        keyRequestHandler.start(session)
        incomingVerificationRequestHandler.start(session)
        session.addListener(sessionListener)
        pushRuleTriggerListener.startWithSession(session)
        session.callSignalingService().addCallListener(callManager)
        imageManager.onSessionStarted(session)
        guardServiceStarter.start()
    }

    suspend fun clearActiveSession() {
        // Do some cleanup first
        getSafeActiveSession(startSync = false)?.let {
            Timber.w("clearActiveSession of ${it.myUserId}")
            it.callSignalingService().removeCallListener(callManager)
            it.removeListener(sessionListener)
        }

        activeSessionReference.set(null)
        activeSessionDataSource.post(Optional.empty())

        keyRequestHandler.stop()
        incomingVerificationRequestHandler.stop()
        pushRuleTriggerListener.stop()
        // No need to unregister the pusher, the sign out will (should?) do it server side.
        unifiedPushHelper.unregister(pushersManager = null)
        guardServiceStarter.stop()
    }

    fun hasActiveSession(): Boolean {
        return activeSessionReference.get() != null || authenticationService.hasAuthenticatedSessions()
    }

    fun getSafeActiveSession(startSync: Boolean = true): Session? {
        return runBlocking { getOrInitializeSession(startSync = startSync) }
    }

    fun getActiveSession(): Session {
        return getSafeActiveSession()
                ?: throw IllegalStateException("You should authenticate before using this")
    }

    suspend fun getOrInitializeSession(startSync: Boolean): Session? {
        return activeSessionReference.get()
                ?.also {
                    if (startSync && !it.syncService().isSyncThreadAlive()) {
                        it.startSyncing(applicationContext)
                    }
                }
                ?: sessionInitializer.tryInitialize(readCurrentSession = { activeSessionReference.get() }) { session ->
                    setActiveSession(session)
                    runBlocking {
                        configureAndStartSessionUseCase.execute(session, startSyncing = startSync)
                    }
                }
    }

    fun isWaitingForSessionInitialization() = activeSessionReference.get() == null && authenticationService.hasAuthenticatedSessions()

    // TODO Stop sync ?
//    fun switchToSession(sessionParams: SessionParams) {
//        val newActiveSession = authenticationService.getSession(sessionParams)
//        activeSession.set(newActiveSession)
//    }
}
